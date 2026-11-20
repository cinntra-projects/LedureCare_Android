package com.cinntra.ledurecare.core.common

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ServerMessageTest {

    private fun message(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))).serverMessage()

    @Test
    fun `plain message`() {
        assertEquals(
            "Invalid email or password.",
            message(401, """{"status":401,"message":"Invalid email or password.","data":{},"errors":{}}"""),
        )
    }

    @Test
    fun `a field error wins over the generic validation message`() {
        assertEquals(
            "This field may not be blank.",
            message(400, """{"message":"Validation failed.","errors":{"email":["This field may not be blank."],"password":["This field may not be blank."]}}"""),
        )
    }

    @Test
    fun `a field error given as a plain string also works`() {
        assertEquals("Too short.", message(400, """{"message":"Validation failed.","errors":{"new_password":"Too short."}}"""))
    }

    @Test
    fun `empty errors fall back to the message`() {
        assertEquals("Validation failed.", message(400, """{"message":"Validation failed.","errors":{}}"""))
    }

    @Test
    fun `a body that is not json has no message`() {
        assertNull(message(502, "<html>Bad gateway</html>"))
        assertNull(message(500, ""))
    }
}
