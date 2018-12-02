package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign
import com.russhwolf.settings.set

/**
 * This is the Lock class which will hold a secret code and return [GuessResult] responses when guesses are made.
 */
class LocalLock(private val code: String) : Lock {
    override suspend fun submitGuess(guess: String): GuessResult {
        val numCorrect = numCorrect(guess, code)
        val numMisplaced = totalMatches(guess, code) - numCorrect
        return GuessResult(numCorrect, numMisplaced)
    }

    override fun save(settings: Settings) {
        settings["code"] = code
    }

    companion object {
        fun load(settings: Settings): LocalLock? = if ("code" in settings) LocalLock(settings["code", "111"]) else null

        fun clear(settings: Settings) {
            settings -= "code"
        }
    }
}

private fun numCorrect(guess: String, code: String): Int =
    guess.zip(code).count { it.first == it.second }

private fun totalMatches(guess: String, code: String): Int {
    guess.forEachIndexed { guessIndex, guessChar ->
        code.forEachIndexed { codeIndex, codeChar ->
            if (guessChar == codeChar) {
                return 1 + totalMatches(
                    guess.removeRange(0, guessIndex + 1),
                    code.removeRange(codeIndex, codeIndex + 1)
                )
            }
        }
    }
    return 0
}

