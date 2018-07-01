package io.intrepid.pickpocket

import java.util.*

private val random by lazy { Random() }

actual fun randomDigit(max: Int) = random.nextInt(max)

