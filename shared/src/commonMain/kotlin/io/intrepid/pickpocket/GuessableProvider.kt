package io.intrepid.pickpocket

interface GuessableProvider {
    fun newGuessable(codeLength: Int, digits: Int): Guessable
}

class LockProvider : GuessableProvider {
    override fun newGuessable(codeLength: Int, digits: Int): Lock = Lock(newCombo(codeLength, digits))

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = randomDigit(digits) + 1
}

expect fun randomDigit(max: Int): Int
