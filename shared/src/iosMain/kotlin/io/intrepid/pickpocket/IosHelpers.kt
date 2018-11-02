package io.intrepid.pickpocket

import com.russhwolf.settings.PlatformSettings
import platform.Foundation.NSUserDefaults

fun createSettings(delegate: NSUserDefaults) = PlatformSettings(delegate)
