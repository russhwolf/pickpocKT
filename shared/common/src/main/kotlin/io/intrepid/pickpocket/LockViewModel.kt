package io.intrepid.pickpocket

import kotlin.properties.Delegates

private const val DIGITS = 6
private const val CODE_LENGTH = 3

class LockViewModel(
    private val guessableProvider: GuessableProvider = LockProvider(),
    private var listener: ViewStateListener? = null
) {
    private var lock: Guessable? = null

    private var state: ViewState by Delegates.observable(ViewState()) { _, _, newValue ->
        listener?.invoke(newValue)
    }

    init {
        listener?.invoke(state)
    }

    fun setListener(listener: ViewStateListener?) {
        this.listener = listener
        listener?.invoke(state)
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

    fun input(character: String) {
        val guess = state.guess + character[0]
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

}

typealias ViewStateListener = (ViewState) -> Unit

data class GuessListItem(val guess: String, val numCorrect: Int, val numMisplaced: Int)
