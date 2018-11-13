package io.intrepid.pickpocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import kotlinx.serialization.Serializable

interface LockApi {
    suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse
}

@Serializable
data class PickLockRequest(val guess: String, val token: String)

@Serializable
data class PickLockResponse(val result: Result) {
    @Serializable
    data class Result(val close: Int, val correct: Int)
}

class LockClient(httpClientEngine: HttpClientEngine) : LockApi {
    private val httpClient = HttpClient(httpClientEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer().apply {
                setMapper(PickLockRequest::class, PickLockRequest.serializer())
                setMapper(PickLockResponse::class, PickLockResponse.serializer())
                setMapper(PickLockResponse.Result::class, PickLockResponse.Result.serializer())
            }
        }
    }

    override suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse =
        httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "5gbad1ceal.execute-api.us-east-1.amazonaws.com/release"
                encodedPath = "picklock/$user"
                body = pickLockRequest
            }
            headers { set(HttpHeaders.ContentType, "application/json") }
        }
}
