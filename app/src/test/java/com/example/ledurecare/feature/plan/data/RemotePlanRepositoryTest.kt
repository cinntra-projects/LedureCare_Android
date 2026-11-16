package com.example.ledurecare.feature.plan.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.plan.domain.DateRange
import com.example.ledurecare.feature.plan.domain.SkippedDealer
import com.example.ledurecare.feature.plan.domain.VisitChange
import com.example.ledurecare.feature.plan.domain.VisitStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RemotePlanRepositoryTest {

    private val json: Json = NetworkModule.provideJson()
    private val api = mockk<PlanApi>()

    private fun repository(scope: TestScope) =
        RemotePlanRepository(api, json, UnconfinedTestDispatcher(scope.testScheduler))

    private fun body(text: String) = json.parseToJsonElement(text)

    private val nov = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

    /** The shape of a real `GET api/plans/` response. */
    private val listResponse = """
        {
          "status": 200, "message": "Success",
          "data": {
            "count": 2, "total_pages": 1, "current_page": 1, "page_size": 20, "next": null, "previous": null,
            "results": [
              {"id": 2, "dealer_code": "C001", "dealer_name": "North Dealer", "city": "Delhi",
               "plan_date": "2026-11-20", "created_at": "2026-11-12T10:30:00Z"},
              {"id": 1, "dealer_code": "18062", "dealer_name": "Ganpati Traders (Assam)", "city": "Sivasagar",
               "plan_date": "2026-11-20", "created_at": "2026-11-12T10:30:00Z"}
            ]
          },
          "errors": {}
        }
    """.trimIndent()

    private fun http(code: Int, text: String) =
        HttpException(Response.error<Any>(code, text.toResponseBody("application/json".toMediaType())))

    // ---- request bodies, exactly as the API documents them

    @Test
    fun `create request matches the API spec`() {
        val request = CreatePlansRequestDto(
            planDate = "2026-11-20",
            dealers = listOf(
                PlanDealerDto("18062", "Ganpati Traders (Assam)", "Sivasagar"),
                PlanDealerDto("C001", "North Dealer", null),
            ),
        )
        assertEquals(
            """{"plan_date":"2026-11-20","dealers":[{"dealer_code":"18062","dealer_name":"Ganpati Traders (Assam)","city":"Sivasagar"},{"dealer_code":"C001","dealer_name":"North Dealer"}]}""",
            json.encodeToString(request),
        )
    }

    @Test
    fun `update request sends only what changed`() {
        assertEquals("""{"plan_date":"2026-11-25"}""", json.encodeToString(UpdatePlanRequestDto(planDate = "2026-11-25")))
        assertEquals(
            """{"dealer_code":"C001","dealer_name":"North Dealer","city":"Delhi"}""",
            json.encodeToString(UpdatePlanRequestDto(dealerCode = "C001", dealerName = "North Dealer", city = "Delhi")),
        )
        assertEquals("""{"city":"Agra"}""", json.encodeToString(UpdatePlanRequestDto(city = "Agra")))
    }

    // ---- listing

    @Test
    fun `listing fetches the range and exposes the plans, oldest first`() = runTest {
        coEvery { api.getPlans("2026-11-01", "2026-11-30") } returns body(listResponse)
        val repo = repository(this)

        assertEquals(AppResult.Success(Unit), repo.refresh(nov))

        val visits = repo.observeVisits(nov).first()
        assertEquals(listOf(1L, 2L), visits.map { it.id }) // same date: by id
        with(visits[1]) {
            assertEquals("C001", dealerId)
            assertEquals("C001", bpCode)
            assertEquals("North Dealer", dealerName)
            assertEquals("Delhi", city)
            assertEquals(LocalDate.of(2026, 11, 20), date)
            assertEquals(VisitStatus.Planned, status)
        }
    }

    @Test
    fun `observing a range shows only plans inside it`() = runTest {
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        val repo = repository(this)
        repo.refresh(nov)

        assertEquals(2, repo.observeVisits(DateRange(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20))).first().size)
        assertEquals(0, repo.observeVisits(DateRange(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 30))).first().size)
    }

    @Test
    fun `a refresh replaces what was known inside the range, so deleted or moved plans vanish`() = runTest {
        val repo = repository(this)
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        repo.refresh(nov)

        coEvery { api.getPlans(any(), any()) } returns body(
            """{"data": {"results": [{"id": 1, "dealer_code": "18062", "dealer_name": "Ganpati", "city": "", "plan_date": "2026-11-20"}]}}"""
        )
        repo.refresh(nov)

        assertEquals(listOf(1L), repo.observeVisits(nov).first().map { it.id })
    }

    @Test
    fun `a refresh leaves plans outside its range alone`() = runTest {
        val repo = repository(this)
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        repo.refresh(nov)

        coEvery { api.getPlans(any(), any()) } returns body("""{"data": {"results": []}}""")
        repo.refresh(DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)))

        assertEquals(2, repo.observeVisits(nov).first().size)
    }

    @Test
    fun `entries without a usable date or dealer code are skipped`() = runTest {
        coEvery { api.getPlans(any(), any()) } returns body(
            """{"data": {"results": [
                {"id": 1, "dealer_code": "A", "dealer_name": "Ok", "plan_date": "2026-11-05"},
                {"id": 2, "dealer_code": "B", "dealer_name": "No date"},
                {"id": 3, "dealer_code": " ", "dealer_name": "No code", "plan_date": "2026-11-06"},
                {"id": 4, "dealer_code": "C", "dealer_name": "Bad date", "plan_date": "soon"}]}}"""
        )
        val repo = repository(this)
        repo.refresh(nov)
        assertEquals(listOf(1L), repo.observeVisits(nov).first().map { it.id })
    }

    @Test
    fun `a failed fetch reports the server message and keeps what was shown`() = runTest {
        val repo = repository(this)
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        repo.refresh(nov)

        coEvery { api.getPlans(any(), any()) } throws http(403, """{"status":403,"message":"You do not have permission.","errors":{}}""")
        val result = repo.refresh(nov)

        assertEquals(AppResult.Failure(AppError.Http(403, "You do not have permission.")), result)
        assertEquals(2, repo.observeVisits(nov).first().size)
    }

    // ---- adding

    private val dealers = listOf(
        Dealer("18062", "Ganpati Traders (Assam)", "18062", "Sivasagar"),
        Dealer("C001", "North Dealer", "C001", "Delhi"),
    )

    private val createResponse = """
        {
          "status": 201, "message": "2 plan(s) created.",
          "data": {
            "created": [{"id": 1, "dealer_code": "18062", "dealer_name": "Ganpati Traders (Assam)", "city": "Sivasagar",
                         "plan_date": "2026-11-20", "created_at": "2026-11-12T10:30:00Z"}],
            "skipped": [{"dealer_code": "C001", "reason": "Already planned for this date."}]
          }
        }
    """.trimIndent()

    @Test
    fun `adding sends the date and each dealer as bp-lookup gave it`() = runTest {
        val sent = slot<CreatePlansRequestDto>()
        coEvery { api.createPlans(capture(sent)) } returns body(createResponse)

        repository(this).addVisits(LocalDate.of(2026, 11, 20), dealers)

        assertEquals("2026-11-20", sent.captured.planDate)
        assertEquals(
            listOf(PlanDealerDto("18062", "Ganpati Traders (Assam)", "Sivasagar"), PlanDealerDto("C001", "North Dealer", "Delhi")),
            sent.captured.dealers,
        )
    }

    @Test
    fun `adding returns what was created and what was skipped, and shows the new plans`() = runTest {
        coEvery { api.createPlans(any()) } returns body(createResponse)
        val repo = repository(this)

        val result = repo.addVisits(LocalDate.of(2026, 11, 20), dealers) as AppResult.Success

        assertEquals(listOf(1L), result.data.createdIds)
        assertEquals(listOf(SkippedDealer("C001", "Already planned for this date.")), result.data.skipped)
        assertEquals(listOf("Ganpati Traders (Assam)"), repo.observeVisits(nov).first().map { it.dealerName })
    }

    @Test
    fun `a dealer without a city sends no city`() = runTest {
        val sent = slot<CreatePlansRequestDto>()
        coEvery { api.createPlans(capture(sent)) } returns body("""{"data": {"created": [], "skipped": []}}""")
        repository(this).addVisits(LocalDate.of(2026, 11, 20), listOf(Dealer("X", "No City Ltd", "X", "")))
        assertNull(sent.captured.dealers.single().city)
    }

    @Test
    fun `when every dealer is skipped the server's 400 message comes back`() = runTest {
        coEvery { api.createPlans(any()) } throws http(
            400,
            """{"status":400,"message":"All dealers are already planned for this date.","data":{},"errors":{}}""",
        )
        val result = repository(this).addVisits(LocalDate.of(2026, 11, 20), dealers)
        assertEquals(AppResult.Failure(AppError.Http(400, "All dealers are already planned for this date.")), result)
    }

    @Test
    fun `no permission is reported as the server worded it`() = runTest {
        coEvery { api.createPlans(any()) } throws http(403, """{"status":403,"message":"Permission denied.","errors":{}}""")
        val result = repository(this).addVisits(LocalDate.of(2026, 11, 20), dealers)
        assertEquals(AppResult.Failure(AppError.Http(403, "Permission denied.")), result)
    }

    // ---- updating

    private suspend fun TestScope.repoWithPlans() = repository(this).also {
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        it.refresh(nov)
    }

    private val updateResponse = """
        {"status": 200, "message": "Plan updated.",
         "data": {"id": 2, "dealer_code": "C001", "dealer_name": "North Dealer", "city": "Delhi",
                  "plan_date": "2026-11-25", "created_at": "2026-11-12T10:30:00Z"}}
    """.trimIndent()

    @Test
    fun `rescheduling sends only the date and moves the plan in the list`() = runTest {
        val repo = repoWithPlans()
        val sent = slot<UpdatePlanRequestDto>()
        coEvery { api.updatePlan(2, capture(sent)) } returns body(updateResponse)

        val result = repo.updateVisit(2, VisitChange(date = LocalDate.of(2026, 11, 25)))

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(UpdatePlanRequestDto(planDate = "2026-11-25"), sent.captured)
        val moved = repo.observeVisits(DateRange(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 25))).first()
        assertEquals(listOf(2L), moved.map { it.id })
        assertEquals(listOf(1L), repo.observeVisits(DateRange(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20))).first().map { it.id })
    }

    @Test
    fun `changing the dealer sends code, name and the new dealer's city together`() = runTest {
        val repo = repoWithPlans()
        val sent = slot<UpdatePlanRequestDto>()
        coEvery { api.updatePlan(1, capture(sent)) } returns body(
            """{"data": {"id": 1, "dealer_code": "C001", "dealer_name": "North Dealer", "city": "Delhi", "plan_date": "2026-11-20"}}"""
        )

        repo.updateVisit(1, VisitChange(dealer = dealers[1]))

        assertEquals(UpdatePlanRequestDto(dealerCode = "C001", dealerName = "North Dealer", city = "Delhi"), sent.captured)
        assertEquals("North Dealer", repo.getVisit(1)?.dealerName)
    }

    @Test
    fun `if the answer cannot be read the requested change is applied as sent`() = runTest {
        val repo = repoWithPlans()
        coEvery { api.updatePlan(any(), any()) } returns body("""{"status": 200, "message": "Plan updated."}""")

        repo.updateVisit(2, VisitChange(date = LocalDate.of(2026, 11, 28)))

        assertEquals(LocalDate.of(2026, 11, 28), repo.getVisit(2)?.date)
    }

    @Test
    fun `a rejected update leaves the plan as it was`() = runTest {
        val repo = repoWithPlans()
        coEvery { api.updatePlan(any(), any()) } throws http(400, """{"status":400,"message":"Validation failed.","errors":{"plan_date":["Enter a valid date."]}}""")

        val result = repo.updateVisit(2, VisitChange(date = LocalDate.of(2026, 11, 25)))

        assertEquals(AppResult.Failure(AppError.Http(400, "Enter a valid date.")), result)
        assertEquals(LocalDate.of(2026, 11, 20), repo.getVisit(2)?.date)
    }

    // ---- looking one up, done marks, clearing

    @Test
    fun `a known plan is found without another call`() = runTest {
        val repo = repoWithPlans()
        assertEquals("North Dealer", repo.getVisit(2)?.dealerName)
        coVerify(exactly = 1) { api.getPlans(any(), any()) }
    }

    @Test
    fun `an unknown plan triggers one full load, then is found or not`() = runTest {
        val repo = repository(this)
        coEvery { api.getPlans(null, null) } returns body(listResponse)

        assertEquals("Ganpati Traders (Assam)", repo.getVisit(1)?.dealerName)
        assertNull(repo.getVisit(999))
    }

    @Test
    fun `marking done is remembered across refreshes`() = runTest {
        val repo = repoWithPlans()
        repo.markDone(2)
        assertEquals(VisitStatus.Done, repo.getVisit(2)?.status)

        repo.refresh(nov) // the server knows nothing about "done"
        assertEquals(VisitStatus.Done, repo.getVisit(2)?.status)
        assertEquals(VisitStatus.Planned, repo.getVisit(1)?.status)
    }

    @Test
    fun `clearing forgets plans and done marks`() = runTest {
        val repo = repoWithPlans()
        repo.markDone(2)
        repo.clear()

        assertTrue(repo.observeVisits(nov).first().isEmpty())
        coEvery { api.getPlans(any(), any()) } returns body(listResponse)
        repo.refresh(nov)
        assertEquals(VisitStatus.Planned, repo.getVisit(2)?.status)
    }
}
