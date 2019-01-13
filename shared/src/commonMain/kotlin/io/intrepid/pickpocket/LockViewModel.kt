package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class LockViewModel(
    private val settings: Settings,
    private val api: LockApi = LockClient(httpClientEngine),
    private val webLockProvider: LockProvider<WebLockProvider.User> = WebLockProvider(api, settings),
    private val localLockProvider: LockProvider<Int> = LocalLockProvider(settings),
    private var listener: ViewStateListener? = null
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = job + MainDispatcher

    private var state: ViewState by Delegates.observable(ViewState.load(settings)) { _, _, newValue ->
        newValue.save(settings)
        listener?.invoke(newValue)
    }

    private var lock: Lock? = when (state.mode) {
        Mode.LOCAL -> localLockProvider.loadLock()
        Mode.WEB -> webLockProvider.loadLock()
        null -> null
    }

    init {
        listener?.invoke(state)
    }

    fun deinit() {
        listener = null
        job.cancel()
    }

    fun setListener(listener: ViewStateListener?) {
        this.listener = listener
        listener?.invoke(state)
    }

    fun startLocal() {
        state = state.copy(localConfigVisible = true)
    }

    suspend fun startWeb() {
        // TODO loading UI
        // TODO clean up serialization repetiton
        val users = api.getUsers().result.map { WebLockProvider.User(it.userId, it.combinationLength) }
        state = state.copy(webConfigOptions = users)
    }

    fun selectLocalLength(codeLengthInput: String) {
        val codeLength = codeLengthInput.toIntOrNull() ?: 0
        if (codeLength > 0) {
            start(Mode.LOCAL, localLockProvider.newLock(codeLength))
        } else {
            dismissLocalLengthInput()
        }
    }

    fun dismissLocalLengthInput() {
        state = state.copy(localConfigVisible = false)
    }


    fun selectWebUser(user: WebLockProvider.User?) {
        if (user != null) {
            start(Mode.WEB, webLockProvider.newLock(user))
        } else {
            dismissWebUserInput()
        }
    }

    fun dismissWebUserInput() {
        state = state.copy(webConfigOptions = null)
    }

    private fun start(mode: Mode, lock: Lock) {
        this.lock = lock
        lock.save(settings)
        state = state.copy(
            enabled = true,
            codeLength = lock.codeLength,
            startButtonsVisible = false,
            resetButtonVisible = true,
            localConfigVisible = false,
            webConfigOptions = null,
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
        val prevState = state
        try {
            state = state.copy(enabled = false)
            val guess = state.guess + character
            state = state.copy(guess = guess)
            if (guess.length == lock?.codeLength) {
                processGuess(guess)
            }
            state = state.copy(enabled = state.locked)
        } catch (e: Throwable) {
            // TODO logger?
            println("Error on input! ${e.message}")
            state = prevState
        }
    }

    private suspend fun processGuess(guess: String) {
        val lock = lock ?: return

        val (numCorrect, numMisplaced) = lock.submitGuess(guess)
        val complete = numCorrect == lock.codeLength && numMisplaced == 0
        state = state.copy(
            guess = "",
            results = state.results + GuessListItem(guess, numCorrect, numMisplaced),
            locked = !complete,
            mode = if (complete) null else state.mode
        )
    }
}

data class ViewState(
    val guess: String = "",
    val codeLength: Int = 0,
    val results: List<GuessListItem> = listOf(),
    val locked: Boolean = true,
    val enabled: Boolean = false,
    val startButtonsVisible: Boolean = true,
    val resetButtonVisible: Boolean = false,
    val localConfigVisible: Boolean = false,
    val webConfigOptions: List<WebLockProvider.User>? = null,
    val mode: Mode? = null
) {
    companion object
}

private const val KEY_GUESS = "guess"
private const val KEY_LOCKED = "locked"
private const val KEY_ENABLED = "enabled"
private const val KEY_RESULTS_SIZE = "results_size"
private const val KEY_START_BUTTONS_VISIBLE = "startButtonsVisible"
private const val KEY_RESET_BUTTON_VISIBLE = "resetButtonVisible"
private const val KEY_MODE = "mode"
private fun resultGuessKey(index: Int) = "results${index}_guess"
private fun resultNumCorrectKey(index: Int) = "results${index}_numCorrect"
private fun resultNumMisplacedKey(index: Int) = "results${index}_numMisplaced"

private fun ViewState.save(settings: Settings) {
    settings[KEY_GUESS] = guess
    settings[KEY_RESULTS_SIZE] = results.size
    results.forEachIndexed { index, guessListItem ->
        settings[resultGuessKey(index)] = guessListItem.guess
        settings[resultNumCorrectKey(index)] = guessListItem.numCorrect
        settings[resultNumMisplacedKey(index)] = guessListItem.numMisplaced
    }
    settings[KEY_LOCKED] = locked
    settings[KEY_ENABLED] = enabled
    settings[KEY_START_BUTTONS_VISIBLE] = startButtonsVisible
    settings[KEY_RESET_BUTTON_VISIBLE] = resetButtonVisible
    if (mode == null) {
        settings -= KEY_MODE
    } else {
        settings[KEY_MODE] = mode.name
    }
}

private fun ViewState.Companion.load(settings: Settings): ViewState =
    ViewState(
        guess = settings[KEY_GUESS, ""],
        results = List(settings[KEY_RESULTS_SIZE, 0]) { index ->
            GuessListItem(
                guess = settings[resultGuessKey(index), ""],
                numCorrect = settings[resultNumCorrectKey(index), 0],
                numMisplaced = settings[resultNumMisplacedKey(index), 0]
            )
        },
        locked = settings[KEY_LOCKED, true],
        enabled = settings[KEY_ENABLED, false],
        startButtonsVisible = settings[KEY_START_BUTTONS_VISIBLE, true],
        resetButtonVisible = settings[KEY_RESET_BUTTON_VISIBLE, false],
        mode = when (settings[KEY_MODE, ""]) {
            Mode.LOCAL.name -> Mode.LOCAL
            Mode.WEB.name -> Mode.WEB
            else -> null
        }
    )

typealias ViewStateListener = (ViewState) -> Unit

data class GuessListItem(val guess: String, val numCorrect: Int, val numMisplaced: Int)

enum class Mode { LOCAL, WEB }
