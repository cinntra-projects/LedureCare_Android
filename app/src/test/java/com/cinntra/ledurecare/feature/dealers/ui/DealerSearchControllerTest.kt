package com.cinntra.ledurecare.feature.dealers.ui

import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.dealers.domain.DealerPage
import com.cinntra.ledurecare.feature.dealers.domain.DealerRepository
import com.cinntra.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.cinntra.ledurecare.feature.dealers.domain.matches
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DealerSearchControllerTest {

    private val everyone = List(45) { Dealer("D$it", "Dealer $it", "D$it", if (it % 2 == 0) "Noida" else "Meerut") }

    /** A server that matches on name, code or city and pages the matches. Records every request. */
    private class FakeServer(private val dealers: List<Dealer>) : DealerRepository {
        data class Call(val query: String, val page: Int, val pageSize: Int)

        val calls = mutableListOf<Call>()
        var failures = ArrayDeque<AppError>()
        var latencyMs = 0L

        override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> {
            calls += Call(query, page, pageSize)
            if (latencyMs > 0) delay(latencyMs)
            failures.removeFirstOrNull()?.let { return AppResult.Failure(it) }
            val matching = dealers.filter { it.matches(query) }
            val totalPages = ((matching.size + pageSize - 1) / pageSize).coerceAtLeast(1)
            return AppResult.Success(
                DealerPage(matching.drop((page - 1) * pageSize).take(pageSize), page, totalPages, matching.size)
            )
        }
    }

    private fun TestScope.controller(server: FakeServer, pageSize: Int = 20) =
        DealerSearchController(SearchDealersUseCase(server), this, debounceMs = 300, pageSize = pageSize)

    // ---------------------------------------------------------------- first page

    @Test
    fun `reset loads the first page with no query`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)

        c.reset()
        assertTrue(c.state.value.isLoading)
        advanceUntilIdle()

        with(c.state.value) {
            assertEquals(20, items.size)
            assertEquals(45, totalCount)
            assertEquals(1, page)
            assertTrue(hasMore)
            assertFalse(isLoading)
            assertTrue(hasLoaded)
            assertNull(error)
        }
        assertEquals(listOf(FakeServer.Call("", 1, 20)), server.calls)
    }

    @Test
    fun `nothing is reported as loaded before the first answer`() = runTest {
        val c = controller(FakeServer(everyone).apply { latencyMs = 500 })
        c.reset()
        assertFalse(c.state.value.hasLoaded)
        advanceUntilIdle()
        assertTrue(c.state.value.hasLoaded)
    }

    // ---------------------------------------------------------------- more pages

    @Test
    fun `load more appends the next page until the pages run out`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        c.loadMore()
        advanceUntilIdle()
        assertEquals(40, c.state.value.items.size)
        assertTrue(c.state.value.hasMore)

        c.loadMore()
        advanceUntilIdle()
        assertEquals(45, c.state.value.items.size)
        assertFalse(c.state.value.hasMore)
        assertEquals(everyone.map { it.id }, c.state.value.items.map { it.id })

        c.loadMore() // nothing left: no request
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), server.calls.map { it.page })
    }

    @Test
    fun `load more is ignored while a page is already on its way`() = runTest {
        val server = FakeServer(everyone).apply { latencyMs = 100 }
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        c.loadMore()
        runCurrent()
        assertTrue(c.state.value.isLoadingMore)
        c.loadMore()
        c.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), server.calls.map { it.page })
    }

    @Test
    fun `a page that repeats dealers does not duplicate them`() = runTest {
        val overlapping = object : DealerRepository {
            override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> =
                AppResult.Success(
                    DealerPage(
                        dealers = if (page == 1) everyone.take(3) else everyone.subList(2, 5),
                        page = page, totalPages = 2, totalCount = 5,
                    )
                )
        }
        val c = DealerSearchController(SearchDealersUseCase(overlapping), this, debounceMs = 0)
        c.reset()
        advanceUntilIdle()
        c.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("D0", "D1", "D2", "D3", "D4"), c.state.value.items.map { it.id })
    }

    // ---------------------------------------------------------------- searching

    @Test
    fun `typing waits for a pause before asking the server`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()
        server.calls.clear()

        c.setQuery("noida")
        assertEquals("noida", c.state.value.query) // shown at once
        assertTrue(c.state.value.isLoading)
        advanceTimeBy(299)
        assertTrue(server.calls.isEmpty())
        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf(FakeServer.Call("noida", 1, 20)), server.calls)
        assertEquals(23, c.state.value.totalCount)
        assertTrue(c.state.value.items.all { it.city == "Noida" })
    }

    @Test
    fun `fast typing sends only the final text`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()
        server.calls.clear()

        "dealer 4".forEachIndexed { index, _ ->
            c.setQuery("dealer 4".take(index + 1))
            advanceTimeBy(100)
        }
        advanceUntilIdle()

        assertEquals(listOf("dealer 4"), server.calls.map { it.query })
    }

    @Test
    fun `an answer for an old query is dropped`() = runTest {
        val server = FakeServer(everyone).apply { latencyMs = 1_000 }
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        c.setQuery("noida")
        advanceTimeBy(400) // the request for "noida" is in flight
        c.setQuery("meerut")
        advanceUntilIdle()

        assertEquals("meerut", c.state.value.query)
        assertTrue(c.state.value.items.all { it.city == "Meerut" })
    }

    @Test
    fun `a new search starts from the first page and forgets the old results`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()
        c.loadMore()
        advanceUntilIdle()
        assertEquals(40, c.state.value.items.size)

        c.setQuery("dealer 1")
        assertTrue(c.state.value.items.isEmpty()) // cleared while the new search runs
        advanceUntilIdle()
        assertEquals(1, c.state.value.page)
    }

    @Test
    fun `typing the same text again does nothing`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()
        c.setQuery("noida")
        advanceUntilIdle()
        server.calls.clear()

        c.setQuery("noida")
        advanceUntilIdle()
        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun `no matches is a loaded empty list`() = runTest {
        val c = controller(FakeServer(everyone))
        c.setQuery("zzz")
        advanceUntilIdle()
        with(c.state.value) {
            assertTrue(items.isEmpty())
            assertEquals(0, totalCount)
            assertTrue(hasLoaded)
            assertFalse(hasMore)
        }
    }

    @Test
    fun `reset clears the query`() = runTest {
        val c = controller(FakeServer(everyone))
        c.setQuery("noida")
        advanceUntilIdle()
        c.reset()
        advanceUntilIdle()
        assertEquals("", c.state.value.query)
        assertEquals(45, c.state.value.totalCount)
    }

    // ---------------------------------------------------------------- failures

    @Test
    fun `a failed first page shows the error and retry loads it`() = runTest {
        val server = FakeServer(everyone).apply { failures.addLast(AppError.Network) }
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        assertEquals(AppError.Network, c.state.value.error)
        assertTrue(c.state.value.items.isEmpty())
        assertTrue(c.state.value.hasLoaded)

        c.retry()
        advanceUntilIdle()
        assertNull(c.state.value.error)
        assertEquals(20, c.state.value.items.size)
        assertEquals(listOf(1, 1), server.calls.map { it.page })
    }

    @Test
    fun `a failed later page keeps what is shown, and retry asks for that page again`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        server.failures.addLast(AppError.Http(500, "Server error."))
        c.loadMore()
        advanceUntilIdle()
        assertEquals(AppError.Http(500, "Server error."), c.state.value.error)
        assertEquals(20, c.state.value.items.size)

        c.loadMore() // blocked until retried, so a failing page is not hammered while scrolling
        advanceUntilIdle()
        assertEquals(listOf(1, 2), server.calls.map { it.page })

        c.retry()
        advanceUntilIdle()
        assertNull(c.state.value.error)
        assertEquals(40, c.state.value.items.size)
        assertEquals(listOf(1, 2, 2), server.calls.map { it.page })
    }

    @Test
    fun `a failed search shows the error for that query`() = runTest {
        val server = FakeServer(everyone)
        val c = controller(server)
        c.reset()
        advanceUntilIdle()

        server.failures.addLast(AppError.Http(403, "You do not have permission."))
        c.setQuery("noida")
        advanceUntilIdle()

        assertEquals(AppError.Http(403, "You do not have permission."), c.state.value.error)
        assertEquals("noida", c.state.value.query)
        assertTrue(c.state.value.items.isEmpty())
    }
}
