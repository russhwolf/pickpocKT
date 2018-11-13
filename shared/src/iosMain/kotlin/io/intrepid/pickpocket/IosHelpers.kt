package io.intrepid.pickpocket

import com.russhwolf.settings.PlatformSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.CoroutineContext

fun getIosWebLockProvider(settings: Settings): LockProvider = WebLockProvider(httpClientEngine, settings)

fun CoroutineScope.launchInput(lockViewModel: LockViewModel, character: String) =
    launch { lockViewModel.input(character) }

fun Job() = kotlinx.coroutines.Job()

fun createContext(dispatcher: CoroutineContext, job: Job) = dispatcher + job

fun createSettings(delegate: NSUserDefaults) = PlatformSettings(delegate)
