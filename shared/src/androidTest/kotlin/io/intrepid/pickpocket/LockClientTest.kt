package io.intrepid.pickpocket

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequest
import io.ktor.content.TextContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO move this to common (requires common ktor-client-mock; currently JVM-only)

private val USER = "Test"
private val REQUEST = PickLockRequest("[1,2,3]", "1234567890ABCDEF")

class LockClientTest {

    @Test
    fun `all correct`() = runBlocking {
        val client = LockClient(mockClient(0, 3))
        val result = client.pickLock(USER, REQUEST)
        assertEquals(
            PickLockResponse(PickLockResponse.Result(0, 3)),
            result,
            "Parsed incorrect result!"
        )
    }

    @Test
    fun `some correct`() = runBlocking {
        val client = LockClient(mockClient(1, 1))
        val result = client.pickLock(USER, REQUEST)
        assertEquals(
            PickLockResponse(PickLockResponse.Result(1, 1)),
            result,
            "Parsed incorrect result!"
        )
    }

    private fun mockClient(close: Int, correct: Int) = HttpClient(MockEngine) {
        install(JsonFeature)
        engine {
            check = { request: HttpRequest, call: HttpClientCall ->
                // Verify that we serialized our inputs correctly
                assertEquals(
                    "https://5gbad1ceal.execute-api.us-east-1.amazonaws.com/release/picklock/Test",
                    request.url.toString(),
                    "Passed incorrect URL!"
                )
                assertEquals(
                    """{"guess":"[1,2,3]","token":"1234567890ABCDEF"}""",
                    (request.content as TextContent).text,
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
    }
}
