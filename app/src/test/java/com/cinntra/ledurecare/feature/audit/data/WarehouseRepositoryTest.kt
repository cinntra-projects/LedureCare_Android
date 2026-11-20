package com.cinntra.ledurecare.feature.audit.data

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.feature.audit.domain.Warehouse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class WarehouseRepositoryTest {

    private val json: Json = NetworkModule.provideJson()
    private val api = mockk<WarehouseApi>()

    private fun repository(scope: TestScope) =
        WarehouseRepositoryImpl(api, json, UnconfinedTestDispatcher(scope.testScheduler))

    private fun page(body: String) = json.parseToJsonElement(body)

    /** The shape of a real `GET api/warehouses/` response (one warehouse, extra fields kept). */
    private val liveResponse = """
        {
          "status": 200,
          "message": "Success",
          "data": {
            "count": 7, "total_pages": 1, "current_page": 1, "page_size": null, "next": null, "previous": null,
            "results": [
              {
                "id": 5, "name": "Jaipur",
                "corporate_address": "", "corporate_city": "", "corporate_state": "", "corporate_pincode": "",
                "corporate_country": "India",
                "dispatch_address": "A-85ARoad No.9VKI Area", "dispatch_city": "Jaipur",
                "dispatch_state": "Rajasthan", "dispatch_pincode": "302013", "dispatch_country": "India",
                "email": "accounts@ledure.com", "phone": "0120-4317328", "pan": "AACCL8907C",
                "gstin": "36AACCL8907C1ZM", "cin": "U31500DL2015PLC282834", "is_active": true,
                "created_at": "2026-10-31T20:32:26.231879+05:30", "updated_at": "2026-10-31T20:32:26.231925+05:30"
              }
            ]
          },
          "errors": {}
        }
    """.trimIndent()

    @Test
    fun `live response maps id, name and the dispatch address`() {
        val warehouses = page(liveResponse).warehouses(json)

        assertEquals(listOf(Warehouse(5, "Jaipur", "A-85ARoad No.9VKI Area, Jaipur")), warehouses.map { it.copy(details = "") })
        // The server's whole object is kept, to be sent back when a DLI is submitted.
        assertTrue(warehouses.single().details.contains("\"gstin\":\"36AACCL8907C1ZM\""))
        assertEquals("Jaipur", warehouses.single().label)
    }

    @Test
    fun `the label is just the name when there is no address`() {
        assertEquals("Noida", Warehouse(1, "Noida").label)
    }

    @Test
    fun `inactive warehouses are left out`() {
        val warehouses = page(
            """{"data": {"results": [
                {"id": 1, "name": "Open", "is_active": true},
                {"id": 2, "name": "Closed", "is_active": false}]}}"""
        ).warehouses(json)
        assertEquals(listOf(1), warehouses.map { it.id })
    }

    @Test
    fun `blank names and addresses are handled`() {
        val warehouses = page(
            """{"data": {"results": [
                {"id": 9, "name": "  ", "dispatch_address": " ", "dispatch_city": null},
                {"id": 10, "name": "Patna", "dispatch_address": "", "dispatch_city": "Patna"}]}}"""
        ).warehouses(json)
        assertEquals("Warehouse 9", warehouses[0].name)
        assertEquals("", warehouses[0].address)
        assertEquals("Patna", warehouses[1].address)
    }

    @Test
    fun `an entry the app cannot read does not break the list`() {
        val warehouses = page(
            """{"data": {"results": [{"name": "No id"}, {"id": 3, "name": "Good"}]}}"""
        ).warehouses(json)
        assertEquals(listOf(3), warehouses.map { it.id })
    }

    @Test
    fun `a single page is fetched once`() = runTest {
        coEvery { api.getWarehouses(any()) } returns page(liveResponse)

        val result = repository(this).getWarehouses()

        assertEquals(listOf(5), (result as AppResult.Success).data.map { it.id })
        coVerify(exactly = 1) { api.getWarehouses(any()) }
    }

    @Test
    fun `further pages are fetched and merged`() = runTest {
        coEvery { api.getWarehouses(null) } returns page(
            """{"data": {"total_pages": 3, "results": [{"id": 1, "name": "A"}]}}"""
        )
        coEvery { api.getWarehouses(2) } returns page("""{"data": {"total_pages": 3, "results": [{"id": 2, "name": "B"}]}}""")
        coEvery { api.getWarehouses(3) } returns page("""{"data": {"total_pages": 3, "results": [{"id": 3, "name": "C"}, {"id": 1, "name": "A"}]}}""")

        val result = repository(this).getWarehouses()

        assertEquals(listOf(1, 2, 3), (result as AppResult.Success).data.map { it.id }) // repeats dropped
        coVerifyOrder { api.getWarehouses(null); api.getWarehouses(2); api.getWarehouses(3) }
    }

    @Test
    fun `an absurd page count is capped`() = runTest {
        coEvery { api.getWarehouses(any()) } returns page("""{"data": {"total_pages": 100000, "results": []}}""")
        repository(this).getWarehouses()
        coVerify(atMost = 20) { api.getWarehouses(any()) }
    }

    @Test
    fun `a server error is returned with its message`() = runTest {
        coEvery { api.getWarehouses(any()) } throws HttpException(
            Response.error<Any>(
                403,
                """{"status":403,"message":"You do not have permission.","data":{},"errors":{}}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        )

        val result = repository(this).getWarehouses()

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Http(403, "You do not have permission."), (result as AppResult.Failure).error)
    }
}
