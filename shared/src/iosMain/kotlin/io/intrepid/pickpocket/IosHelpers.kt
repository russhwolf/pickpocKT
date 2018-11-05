package io.intrepid.pickpocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.ios.IosClient
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import com.russhwolf.settings.PlatformSettings
import com.russhwolf.settings.Settings
import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JSON
import platform.Foundation.NSUserDefaults

fun getIosWebLockProvider(settings: Settings): LockProvider =
    WebLockProvider(HttpClient(IosClient()) {
        install(JsonFeature) {
            // TODO make this less manual
            serializer = object : JsonSerializer {
                override suspend fun read(type: TypeInfo, response: HttpResponse): Any {
                    return JSON.parse(PickLockResponse.serializer(), response.readText())
                }

                override fun write(data: Any): OutgoingContent {
                    val content = JSON.stringify(PickLockRequest.serializer(), data as PickLockRequest)
                    return TextContent(content, ContentType.Application.Json)
                }

            }
        }
    }, settings)

fun CoroutineScope.launchInput(lockViewModel: LockViewModel, character: String) =
    launch { lockViewModel.input(character) }

fun Job() = kotlinx.coroutines.Job()

fun createContext(dispatcher: CoroutineContext, job: Job) = dispatcher + job

fun createSettings(delegate: NSUserDefaults) = PlatformSettings(delegate)
