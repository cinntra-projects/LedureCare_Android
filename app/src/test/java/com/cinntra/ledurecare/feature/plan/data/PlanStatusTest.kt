package com.cinntra.ledurecare.feature.plan.data

import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanStatusTest {

    private fun dto(status: String?, returnId: Int? = null) =
        PlanDto(id = 1, dealerCode = "D1", dealerName = "Dealer", planDate = "2026-11-16", status = status, returnId = returnId?.let { JsonPrimitive(it) })

    @Test
    fun `return_id parses as a number or as text`() {
        val json = NetworkModule.provideJson()
        val asNumber = json.decodeFromString(PlanDto.serializer(), """{"id":1,"return_id":41}""")
        val asText = json.decodeFromString(PlanDto.serializer(), """{"id":1,"return_id":"41"}""")
        val none = json.decodeFromString(PlanDto.serializer(), """{"id":1,"return_id":null}""")
        assertEquals(41, asNumber.returnIdValue)
        assertEquals(41, asText.returnIdValue)
        assertEquals(null, none.returnIdValue)
    }

    @Test
    fun `planned status stays planned`() {
        assertEquals(VisitStatus.Planned, dto("planned").toDomain()!!.status)
    }

    @Test
    fun `completed status is done and keeps the DLI id`() {
        val visit = dto("Completed", returnId = 41).toDomain()!!
        assertEquals(VisitStatus.Done, visit.status)
        assertEquals(41, visit.returnId)
    }

    @Test
    fun `a missing status is planned`() {
        assertEquals(VisitStatus.Planned, dto(null).toDomain()!!.status)
    }
}
