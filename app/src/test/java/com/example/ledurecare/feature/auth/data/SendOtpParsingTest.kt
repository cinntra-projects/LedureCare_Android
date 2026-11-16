package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.feature.auth.data.remote.ApiEnvelope
import com.example.ledurecare.feature.auth.data.remote.OtpSentDto
import com.example.ledurecare.feature.auth.data.remote.SendOtpRequestDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SendOtpParsingTest {

    private val json = NetworkModule.provideJson()

    @Test
    fun `request body is exactly the phone number`() {
        assertEquals("""{"phone":"8750290129"}""", json.encodeToString(SendOtpRequestDto("8750290129")))
    }

    @Test
    fun `live response gives the masked email`() {
        val body = """
            {
              "status": 200,
              "message": "OTP sent to your registered email.",
              "data": {"email": "r**************2@cinntra.com"},
              "errors": {}
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<OtpSentDto>>(body)

        assertEquals("OTP sent to your registered email.", envelope.message)
        assertEquals("r**************2@cinntra.com", envelope.data?.email)
    }

    @Test
    fun `a response without an email still parses`() {
        val envelope = json.decodeFromString<ApiEnvelope<OtpSentDto>>("""{"status": 200, "message": "OK", "data": {}}""")
        assertNull(envelope.data?.email)
    }
}
