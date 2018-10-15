package io.intrepid.pickpocket

import kotlin.random.Random

interface GuessableProvider {
    fun newGuessable(codeLength: Int, digits: Int): Guessable
}

class LockProvider : GuessableProvider {
    override fun newGuessable(codeLength: Int, digits: Int): Lock = Lock(newCombo(codeLength, digits))

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

