package io.intrepid.pickpocket

interface Guessable {
    fun submitGuess(guess: String): GuessResult
}

/**
 * This is the Lock class which will hold a secret code and return [GuessResult] responses when guesses are made.
 */
class Lock(private val code: String) : Guessable {
    override fun submitGuess(guess: String): GuessResult {
        val numCorrect = numCorrect(guess, code)
        val numMisplaced = totalMatches(guess, code) - numCorrect
        return GuessResult(numCorrect, numMisplaced)
    }
}

private fun numCorrect(guess: String, code: String): Int =
    guess.zip(code).sumBy { if (it.first == it.second) 1 else 0 }

private fun totalMatches(guess: String, code: String): Int {
    for (guessChar in guess) {
        for (codeChar in code) {
            if (guessChar == codeChar) {
                return 1 + totalMatches(
                    guess.replaceFirst("$guessChar", ""),
                    code.replaceFirst("$codeChar", "")
                )
            }
        }
    }
    return 0
}

private fun String.sorted(): String = toList().sorted().joinToString(separator = "")

data class GuessResult(val numCorrect: Int, val numMisplaced: Int)
