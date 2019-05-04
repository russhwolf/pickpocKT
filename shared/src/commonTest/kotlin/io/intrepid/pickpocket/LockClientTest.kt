package io.intrepid.pickpocket

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.content.TextContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals

private const val USER = "Test"
private val REQUEST = PickLockRequest("[1,2,3]", "1234567890ABCDEF")

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
        val client = LockClient(MockEngine {
            // Verify that we serialized our inputs correctly
            assertEquals(
                "https://5gbad1ceal.execute-api.us-east-1.amazonaws.com/release/users",
                url.toString(),
                "Passed incorrect URL!"
            )
            MockHttpResponse(
                call = call,
                status = HttpStatusCode.OK,
                content = ByteReadChannel(
                    """{"result":[{"userId":"JackBlack","combinationLength":4},{"userId":"Paul","combinationLength":3},{"userId":"JohnM","combinationLength":4},{"userId":"Jimmy","combinationLength":4}]}"""
                ),
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

    private fun mockPickLockEngine(close: Int, correct: Int) = MockEngine {
        // Verify that we serialized our inputs correctly
        assertEquals(
            "https://5gbad1ceal.execute-api.us-east-1.amazonaws.com/release/picklock/Test",
            url.toString(),
            "Passed incorrect URL!"
        )
        assertEquals(
            """{"guess":"[1,2,3]","token":"1234567890ABCDEF"}""",
            (content as TextContent).text,
            "Passed incorrect body!"
        )
        MockHttpResponse(
            call = call,
            status = HttpStatusCode.OK,
            content = ByteReadChannel("""{"result": {"close": $close, "correct": $correct}}"""),
            headers = Headers.build { set(HttpHeaders.ContentType, "application/json") }
        )
    }
}

