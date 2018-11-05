package io.intrepid.pickpocket

import com.russhwolf.settings.Settings

interface Lock {
    suspend fun submitGuess(guess: String): GuessResult

    fun save(settings: Settings)
}

data class GuessResult(val numCorrect: Int, val numMisplaced: Int)
