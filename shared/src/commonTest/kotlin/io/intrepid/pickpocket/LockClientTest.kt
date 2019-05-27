package io.intrepid.pickpocket

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.content.TextContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.InternalAPI
import kotlinx.coroutines.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals

private const val USER = "Test"
private val REQUEST = PickLockRequest("[1,2,3]", "1234567890ABCDEF")

@UseExperimental(InternalAPI::class)
class LockClientTest {

    @Test
    fun `all correct`() = runBlocking {
        val client = LockClient(mockPickLockEngine(0, 3))
        val result = client.pickLock(USER, REQUEST)
        assertEquals(
            PickLockResponse(PickLockResponse.Result(0, 3)),
            result,
            "Parsed incorrect result!"
        )
    }

    @Test
    fun `some correct`() = runBlocking {
        val client = LockClient(mockPickLockEngine(1, 1))
        val result = client.pickLock(USER, REQUEST)
        assertEquals(
            PickLockResponse(PickLockResponse.Result(1, 1)),
            result,
            "Parsed incorrect result!"
        )
    }

    @Test
    fun `get users`() = runBlocking {
        val client = LockClient(MockEngine { request ->
            // Verify that we serialized our inputs correctly
            assertEquals(
                "https://5gbad1ceal.execute-api.us-east-1.amazonaws.com/release/users",
                request.url.toString(),
                "Passed incorrect URL!"
            )
            respond(
                content = ByteReadChannel(
                    """{"result":[{"userId":"JackBlack","combinationLength":4},{"userId":"Paul","combinationLength":3},{"userId":"JohnM","combinationLength":4},{"userId":"Jimmy","combinationLength":4}]}"""
                ),
                status = HttpStatusCode.OK,
                headers = Headers.build { set(HttpHeaders.ContentType, "application/json") }
            )
        })
        val result = client.getUsers()

        assertEquals(
            GetUsersResponse(
                listOf(
                    GetUsersResponse.User("JackBlack", 4),
                    GetUsersResponse.User("Paul", 3),
                    GetUsersResponse.User("JohnM", 4),
                    GetUsersResponse.User("Jimmy", 4)
                )
            ),
            result,
            "Parsed incorrect result!"
        )
    }

    private fun mockPickLockEngine(close: Int, correct: Int) = MockEngine { request ->
        // Verify that we serialized our inputs correctly
        assertEquals(
            "https://5gbad1ceal.execute-api.us-east-1.amazonaws.com/release/picklock/Test",
            request.url.toString(),
            "Passed incorrect URL!"
        )
        assertEquals(
            """{"guess":"[1,2,3]","token":"1234567890ABCDEF"}""",
            (request.body as TextContent).text,
            "Passed incorrect body!"
        )
        respond(
            content = ByteReadChannel("""{"result": {"close": $close, "correct": $correct}}"""),
            status = HttpStatusCode.OK,
            headers = Headers.build { set(HttpHeaders.ContentType, "application/json") }
        )
    }
}

