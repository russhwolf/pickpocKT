package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import kotlin.random.Random

interface GuessableProvider {
    fun newGuessable(codeLength: Int, digits: Int): Guessable

    fun loadGuessable(settings: Settings): Guessable?
    fun clearSavedGuessable(settings: Settings)
}

class LockProvider : GuessableProvider {
    override fun newGuessable(codeLength: Int, digits: Int): Lock = Lock(newCombo(codeLength, digits))

    override fun loadGuessable(settings: Settings): Lock? = Lock.load(settings)

    override fun clearSavedGuessable(settings: Settings) = Lock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

