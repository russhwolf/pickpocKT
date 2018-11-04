package io.intrepid.pickpocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext
import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.russhwolf.settings.string
import kotlin.properties.Delegates

private const val DIGITS = 6
private const val CODE_LENGTH = 3

class LockViewModel(
    private val settings: Settings,
    private val guessableProvider: GuessableProvider = LockProvider(settings),
    private var listener: ViewStateListener? = null
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = Job()

    private var lock: Guessable? = guessableProvider.loadGuessable()

    private var state: ViewState by Delegates.observable(ViewState.load(settings)) { _, _, newValue ->
        newValue.save(settings)
        listener?.invoke(newValue)
    }

    init {
        listener?.invoke(state)
    }

    fun deinit() {
        listener = null
        coroutineContext.cancel()
    }

    fun setListener(listener: ViewStateListener?) {
        this.listener = listener
        listener?.invoke(state)
    }

    fun reset() {
        if (state.enabled || !state.locked) {
            // reset
            lock = null
            guessableProvider.clearSavedGuessable()
            state = ViewState()
        } else {
            // start
            val lock = guessableProvider.newGuessable(CODE_LENGTH, DIGITS)
            this.lock = lock
            lock.save(settings)
            state = state.copy(enabled = true)
        }
        coroutineContext.cancelChildren()
    }

    suspend fun input(character: String) {
        val guess = state.guess + character[0]
        if (guess.length == CODE_LENGTH) {
            processGuess(guess)
        } else {
            state = state.copy(guess = guess)
        }
    }

    private suspend fun processGuess(guess: String) {
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

interface State {
    val guess: String
    val results: List<GuessListItem>
    val locked: Boolean
    val enabled: Boolean
}

data class ViewState(
    override val guess: String,
    override val results: List<GuessListItem>,
    override val locked: Boolean,
    override val enabled: Boolean
) : State {
    companion object {
        operator fun invoke() = ViewState(
            guess = "",
            results = listOf(),
            locked = true,
            enabled = false
        )
    }
}

private fun State.save(settings: Settings): SavedState = SavedState(settings).also {
    it.guess = this.guess
    it.locked = this.locked
    it.enabled = this.enabled
    it.results = this.results
}

private fun ViewState.Companion.load(settings: Settings): ViewState = SavedState(settings).let { state ->
    ViewState(
        guess = state.guess,
        results = state.results,
        locked = state.locked,
        enabled = state.enabled
    )
}

private class SavedState(private val settings: Settings) : State {
    override var guess by settings.string("guess", "")
    override var locked: Boolean by settings.boolean("locked", true)
    override var enabled: Boolean by settings.boolean("enabled", false)
    override var results: List<GuessListItem>
        get() = List(settings["results_size", 0]) { index ->
            GuessListItem(
                guess = settings["results${index}_guess", ""],
                numCorrect = settings["results${index}_numCorrect", 0],
                numMisplaced = settings["results${index}_numMisplaced", 0]
            )
        }
        set(value) {
            settings["results_size"] = value.size
            value.forEachIndexed { index, guessListItem ->
                settings["results${index}_guess"] = guessListItem.guess
                settings["results${index}_numCorrect"] = guessListItem.numCorrect
                settings["results${index}_numMisplaced"] = guessListItem.numMisplaced
            }
        }
}

typealias ViewStateListener = (ViewState) -> Unit

data class GuessListItem(val guess: String, val numCorrect: Int, val numMisplaced: Int)
