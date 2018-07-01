package io.intrepid.pickpocket

import kotlin.properties.Delegates

private const val DIGITS = 6
private const val CODE_LENGTH = 3

class LockViewModel(private val guessableProvider: GuessableProvider = LockProvider(), private var listener: ViewState.Listener? = null) {
    private var lock: Guessable? = null

    private var state: ViewState by Delegates.observable(ViewState()) { _, _, newValue ->
        listener?.onStateChanged(newValue)
    }

    init {
        listener?.onStateChanged(state)
    }

    fun setListener(listener: ViewState.Listener?) {
        this.listener = listener
        listener?.onStateChanged(state)
    }

    fun reset() {
        if (state.enabled || !state.locked) {
            // reset
            lock = null
            state = ViewState()
        } else {
            // start
            val lock = guessableProvider.newGuessable(CODE_LENGTH, DIGITS)
            this.lock = lock
            state = state.copy(enabled = true)
        }
    }

    fun input(char: Char) {
        val guess = state.guess + char
        if (guess.length == CODE_LENGTH) {
            processGuess(guess)
        } else {
            state = state.copy(guess = guess)
        }
    }

    private fun processGuess(guess: String) {
        val lock = lock ?: return

        val (numCorrect, numMisplaced) = lock.submitGuess(guess)
        val complete = numCorrect == CODE_LENGTH && numMisplaced == 0
        state = state.copy(
            guess = "",
            results = state.results + GuessListItem(guess, numCorrect, numMisplaced),
            locked = !complete,
            enabled = !complete
        )
    }
}

data class ViewState(
    val guess: String,
    val results: List<GuessListItem>,
    val locked: Boolean,
    val enabled: Boolean
) {
    companion object {
        operator fun invoke() = ViewState(
            guess = "",
            results = listOf(),
            locked = true,
            enabled = false
        )
    }

    interface Listener {
        fun onStateChanged(state: ViewState)
    }
}

data class GuessListItem(val guess: String, val numCorrect: Int, val numMisplaced: Int)
