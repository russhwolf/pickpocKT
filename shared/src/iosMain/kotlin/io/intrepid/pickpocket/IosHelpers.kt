package io.intrepid.pickpocket

import com.russhwolf.settings.PlatformSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSUserDefaults

fun CoroutineScope.launchInput(lockViewModel: LockViewModel, character: String) =
    launch { lockViewModel.input(character[0]) }

fun CoroutineScope.launchStartWeb(lockViewModel: LockViewModel) =
    launch { lockViewModel.startWeb() }

fun SupervisorJob() = kotlinx.coroutines.SupervisorJob()

fun createViewModel(defaults: NSUserDefaults) = LockViewModel(PlatformSettings(defaults))
