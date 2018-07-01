package io.intrepid.pickpocket

import platform.posix.arc4random_uniform

actual fun randomDigit(max: Int): Int = arc4random_uniform(max)

