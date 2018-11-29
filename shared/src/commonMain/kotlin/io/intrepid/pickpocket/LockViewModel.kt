package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign
import com.russhwolf.settings.set
import com.russhwolf.settings.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private const val DIGITS = 6
private const val CODE_LENGTH = 3

class LockViewModel(
    private val settings: Settings,
    private val webLockProvider: LockProvider = WebLockProvider(httpClientEngine, settings),
    private val localLockProvider: LockProvider = LocalLockProvider(settings),
    private var listener: ViewStateListener? = null
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob()

    private var lock: Lock? = localLockProvider.loadLock()

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

    fun startLocal() = start(Mode.LOCAL)
    fun startWeb() = start(Mode.WEB)
    private fun start(mode: Mode) {
        val lockProvider = when (mode) {
            Mode.LOCAL -> localLockProvider
            Mode.WEB -> webLockProvider
        }
        val lock = lockProvider.newLock(CODE_LENGTH, DIGITS)
        this.lock = lock
        lock.save(settings)
        state = state.copy(
            enabled = true,
            startButtonsVisible = false,
            resetButtonVisible = true,
            mode = mode
        )
        coroutineContext.cancelChildren()
    }

    fun reset() {
        lock = null
        localLockProvider.clearSavedLock()
        webLockProvider.clearSavedLock()
        state = ViewState()
        coroutineContext.cancelChildren()
    }

    suspend fun input(character: Char) {
        state = state.copy(enabled = false)
        val guess = state.guess + character
        state = state.copy(guess = guess)
        if (guess.length == CODE_LENGTH) {
            processGuess(guess)
        }
        state = state.copy(enabled = state.locked)
    }

    private suspend fun processGuess(guess: String) {
        val lock = lock ?: return

        val (numCorrect, numMisplaced) = lock.submitGuess(guess)
        val complete = numCorrect == CODE_LENGTH && numMisplaced == 0
        state = state.copy(
            guess = "",
            results = state.results + GuessListItem(guess, numCorrect, numMisplaced),
            locked = !complete,
            mode = if (complete) null else state.mode
        )
    }
}

interface State {
    val guess: String
    val results: List<GuessListItem>
    val locked: Boolean
    val enabled: Boolean
    val startButtonsVisible: Boolean
    val resetButtonVisible: Boolean
    val mode: Mode?
}

data class ViewState(
    override val guess: String,
    override val results: List<GuessListItem>,
    override val locked: Boolean,
    override val enabled: Boolean,
    override val startButtonsVisible: Boolean,
    override val resetButtonVisible: Boolean,
    override val mode: Mode?
) : State {
    companion object {
        operator fun invoke() = ViewState(
            guess = "",
            results = listOf(),
            locked = true,
            enabled = false,
            startButtonsVisible = true,
            resetButtonVisible = false,
            mode = null
        )
    }
}

private fun State.save(settings: Settings): SavedState = SavedState(settings).also {
    it.guess = this.guess
    it.locked = this.locked
    it.enabled = this.enabled
    it.results = this.results
    it.startButtonsVisible = this.startButtonsVisible
    it.resetButtonVisible = this.resetButtonVisible
    it.mode = this.mode
}

private fun ViewState.Companion.load(settings: Settings): ViewState = SavedState(settings).let { state ->
    ViewState(
        guess = state.guess,
        results = state.results,
        locked = state.locked,
        enabled = state.enabled,
        startButtonsVisible = state.startButtonsVisible,
        resetButtonVisible = state.resetButtonVisible,
        mode = state.mode
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
    override var startButtonsVisible by settings.boolean("startButtonsVisible", true)
    override var resetButtonVisible by settings.boolean("resetButtonVisible", false)
    override var mode: Mode?
        get() = when(settings["mode", ""]) {
            Mode.LOCAL.name -> Mode.LOCAL
            Mode.WEB.name -> Mode.WEB
            else -> null
        }
        set(value) = if (value == null) {
            settings -= "mode"
        } else {
            settings["mode"] = value.name
        }
}

typealias ViewStateListener = (ViewState) -> Unit

data class GuessListItem(val guess: String, val numCorrect: Int, val numMisplaced: Int)

enum class Mode { LOCAL, WEB }
