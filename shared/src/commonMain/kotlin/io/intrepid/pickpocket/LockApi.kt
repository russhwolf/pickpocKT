package io.intrepid.pickpocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import kotlinx.serialization.Serializable

interface LockApi {
    suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse
    suspend fun getUsers(): GetUsersResponse
}

@Serializable
data class PickLockRequest(val guess: String, val token: String)

@Serializable
data class PickLockResponse(val result: Result) {
    @Serializable
    data class Result(val close: Int, val correct: Int)
}

@Serializable
data class GetUsersResponse(val result: List<User>) {
    @Serializable
    data class User(val userId: String, val combinationLength: Int)
}

private const val HOST = "5gbad1ceal.execute-api.us-east-1.amazonaws.com/release"

class LockClient(httpClientEngine: HttpClientEngine) : LockApi {
    private val httpClient = HttpClient(httpClientEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer().apply {
                setMapper(PickLockRequest::class, PickLockRequest.serializer())
                setMapper(PickLockResponse::class, PickLockResponse.serializer())
                setMapper(GetUsersResponse::class, GetUsersResponse.serializer())
            }
        }
    }

    override suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse =
        httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = HOST
                encodedPath = "picklock/$user"
                body = pickLockRequest
            }
            headers { set(HttpHeaders.ContentType, "application/json") }
        }

    override suspend fun getUsers(): GetUsersResponse =
        httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = HOST
                encodedPath = "users"
            }
        }
}
