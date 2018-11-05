package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign

/**
 * This is the Lock class which will hold a secret code and return [GuessResult] responses when guesses are made.
 */
class LocalLock(private val code: String) : Lock {
    override suspend fun submitGuess(guess: String): GuessResult {
        val numCorrect = numCorrect(guess, code)
        val numMisplaced = totalMatches(guess, code) - numCorrect
        return GuessResult(numCorrect, numMisplaced)
    }

    override fun save(settings: Settings) = settings.putString("code", code)

    companion object {
        fun load(settings: Settings): LocalLock? = if ("code" in settings) LocalLock(settings["code", "111"]) else null

        fun clear(settings: Settings) {
            settings -= "code"
        }
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

