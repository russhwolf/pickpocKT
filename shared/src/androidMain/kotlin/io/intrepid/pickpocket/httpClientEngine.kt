package io.intrepid.pickpocket

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig

actual val httpClientEngine: HttpClientEngine = AndroidClientEngine(AndroidEngineConfig())
