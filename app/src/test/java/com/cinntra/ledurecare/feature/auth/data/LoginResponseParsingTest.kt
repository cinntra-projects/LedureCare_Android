package com.cinntra.ledurecare.feature.auth.data

import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.feature.auth.data.remote.ApiEnvelope
import com.cinntra.ledurecare.feature.auth.data.remote.LoginDataDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginResponseParsingTest {

    private val json = NetworkModule.provideJson()

    @Test
    fun `server login response maps to domain user`() {
        val body = """
            {
              "status": 200,
              "message": "Login successful.",
              "data": {
                "access": "dummy.jwt.token",
                "employee": {
                  "id": 1, "name": "Admin", "email": "admin@ledure.com", "phone": "",
                  "employee_code": null,
                  "role": {"id": 1, "name": "Admin", "code": "admin"},
                  "is_active": true, "must_change_password": false, "is_self": false,
                  "zone": ["E1 (North East)"], "branchDetails": []
                },
                "permissions": ["dashboard.view", "returns.view"],
                "must_change_password": false
              },
              "errors": {}
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<LoginDataDto>>(body)
        val user = envelope.data!!.toEntity().toDomain()

        assertEquals("dummy.jwt.token", envelope.data!!.access)
        assertEquals("1", user.id)
        assertEquals("Admin", user.name)
        assertEquals("admin", user.roleCode)
    }
}
