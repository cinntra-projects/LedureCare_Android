package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.feature.auth.data.remote.ApiEnvelope
import com.example.ledurecare.feature.auth.data.remote.LoginDataDto
import com.example.ledurecare.feature.auth.data.remote.VerifyOtpRequestDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VerifyOtpParsingTest {

    private val json = NetworkModule.provideJson()

    @Test
    fun `request body is the phone and the otp`() {
        assertEquals(
            """{"phone":"8750290129","otp":"390150"}""",
            json.encodeToString(VerifyOtpRequestDto(phone = "8750290129", otp = "390150")),
        )
    }

    @Test
    fun `live verify response maps to the signed in user`() {
        // Shape of a real response; the token is a dummy and the zone list is shortened.
        val body = """
            {
              "status": 200,
              "message": "Login successful.",
              "data": {
                "access": "dummy.jwt.token",
                "employee": {
                  "id": 12, "name": "Ritanshu Juneja", "email": "ritanshu.juneja@cinntra.com",
                  "phone": "8750290129", "employee_code": "BTV1012016",
                  "role": {"id": 5, "name": "Accounts Team", "code": "accounts_team"},
                  "is_active": true, "must_change_password": false, "is_self": false,
                  "zone": ["Accounts Payables", "Branch"],
                  "branchDetails": [{"sap_code": "3", "name": "LLL-PATNA"}, {"sap_code": "2", "name": ""}],
                  "created_at": "2026-10-22T10:26:06.492632+05:30"
                },
                "permissions": ["dashboard.view", "reports.view", "returns.accounts_review"],
                "must_change_password": false
              },
              "errors": {}
            }
        """.trimIndent()

        val data = json.decodeFromString<ApiEnvelope<LoginDataDto>>(body).data!!
        val user = data.toEntity().toDomain()

        assertEquals("dummy.jwt.token", data.access)
        assertEquals("12", user.id)
        assertEquals("Ritanshu Juneja", user.name)
        assertEquals("8750290129", user.phone)
        assertEquals("Accounts Team", user.roleName)
        assertEquals("accounts_team", user.roleCode)
        assertFalse(user.mustChangePassword)
    }
}
