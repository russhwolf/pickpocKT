package io.intrepid.pickpocket

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.ios.Ios

actual val httpClientEngine: HttpClientEngine = Ios.create()
