package com.cinntra.ledurecare.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimberHttpLoggerTest {

    @Test
    fun `a json object body is pretty printed`() {
        val out = TimberHttpLogger.format("""{"status":200,"data":{"email":"a@b.com"}}""")
        assertTrue(out.lines().size > 3)
        assertTrue(out.contains("\"email\": \"a@b.com\""))
    }

    @Test
    fun `a json array body is pretty printed`() {
        assertTrue(TimberHttpLogger.format("""[{"id":1},{"id":2}]""").lines().size > 3)
    }

    @Test
    fun `request lines and headers are left as they are`() {
        listOf(
            "--> POST http://103.197.76.50:8107/api/auth/login/ http/1.1",
            "Content-Type: application/json; charset=UTF-8",
            "Authorization: ██",
            "<-- 200 OK http://103.197.76.50:8107/api/auth/login/ (312ms)",
            "--> END POST (54-byte body)",
        ).forEach { assertEquals(it, TimberHttpLogger.format(it)) }
    }

    @Test
    fun `text that only looks like json is left as it is`() {
        assertEquals("{not json", TimberHttpLogger.format("{not json"))
        assertEquals("[Bad gateway]", TimberHttpLogger.format("[Bad gateway]"))
    }
}
