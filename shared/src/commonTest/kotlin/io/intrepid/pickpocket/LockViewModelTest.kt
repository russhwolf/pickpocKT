package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import kotlinx.coroutines.isActive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val STATE_INITIAL = ViewState(
    guess = "",
    codeLength = 0,
    results = listOf(),
    locked = true,
    enabled = false,
    startButtonsVisible = true,
    resetButtonVisible = false,
    localConfigVisible = false,
    webUsers = null,
    mode = null,
    lockName = ""
)

private val STATE_STARTED = ViewState(
    guess = "",
    codeLength = 3,
    results = listOf(),
    locked = true,
    enabled = true,
    startButtonsVisible = false,
    resetButtonVisible = true,
    localConfigVisible = false,
    webUsers = null,
    mode = Mode.LOCAL,
    lockName = "MockLock (length 3)"
)

private val WEB_USERS = listOf(
    WebLockProvider.User("Foo", 3),
    WebLockProvider.User("Bar", 4)
)

class LockViewModelTest {

    private val mockLock = MockLock()
    private val mockLocalLockProvider = MockLocalLockProvider(mockLock)
    private val mockWebLockProvider = MockWebLockProvider(mockLock)
    private val mockSettings = MockSettings()
    private val mockLogger: Logger = {}
    private val mockViewStateListener = MockViewStateListener()

    private val viewModel =
        LockViewModel(mockSettings, mockWebLockProvider, mockLocalLockProvider, mockLogger, mockViewStateListener)

    @Test
    fun `initial state`() {
        mockViewStateListener.expect(STATE_INITIAL, "ViewModel should have expected initial state")
    }

    @Test
    fun `launch local selector`() {
        viewModel.startLocal()

        mockViewStateListener.expect(
            STATE_INITIAL.copy(localConfigVisible = true),
            "Start local should launch local config"
        )
    }

    @Test
    fun `launch web selector`() = runBlocking {
        viewModel.startWeb()

        mockViewStateListener.expect(
            STATE_INITIAL.copy(webUsers = WEB_USERS),
            "Start web should show web users"
        )
    }

    @Test
    fun `start game local`() {
        viewModel.startLocal()
        viewModel.selectLocalLength("3")

        mockViewStateListener.expect(STATE_STARTED, "Valid local length should start local game")
    }

    @Test
    fun `invalid start local`() {
        viewModel.startLocal()
        viewModel.selectLocalLength("F")

        mockViewStateListener.expect(STATE_INITIAL, "Invalid local length should reset")
    }

    @Test
    fun `start game web`() = runBlocking {
        viewModel.startWeb()
        viewModel.selectWebUser(WebLockProvider.User("Test", 3))

        mockViewStateListener.expect(STATE_STARTED.copy(mode = Mode.WEB), "Valid web user should start web game")
    }

    @Test
    fun `invalid start web`() = runBlocking {
        viewModel.startWeb()
        viewModel.selectWebUser(null)

        mockViewStateListener.expect(STATE_INITIAL, "Invalid web user should reset")
    }

    @Test
    fun `incomplete guess`() = runBlocking {
        viewModel.selectLocalLength("3")
        viewModel.input('3')

        mockViewStateListener.expect(STATE_STARTED.copy(guess = "3"), "First input should update guess state")
    }

    @Test
    fun `incorrect guess`() = runBlocking {
        mockLock.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))

        viewModel.selectLocalLength("3")
        viewModel.input('3')
        viewModel.input('2')
        viewModel.input('4')

        mockViewStateListener.expect(
            STATE_STARTED.copy(
                results = listOf(
                    GuessListItem(
                        guess = "324",
                        numCorrect = 1,
                        numMisplaced = 1
                    )
                )
            ),
            "Complete input should evaluate guess"
        )
    }

    @Test
    fun `correct guess`() = runBlocking {
        viewModel.selectLocalLength("3")
        mockLock.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))
        viewModel.input('3')
        viewModel.input('2')
        viewModel.input('4')
        mockLock.setNextResult(GuessResult(numCorrect = 0, numMisplaced = 2))
        viewModel.input('3')
        viewModel.input('1')
        viewModel.input('4')
        mockLock.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.input('1')
        viewModel.input('2')
        viewModel.input('3')

        mockViewStateListener.expect(
            ViewState(
                guess = "",
                codeLength = 3,
                results = listOf(
                    GuessListItem(
                        guess = "324",
                        numCorrect = 1,
                        numMisplaced = 1
                    ),
                    GuessListItem(
                        guess = "314",
                        numCorrect = 0,
                        numMisplaced = 2
                    ),
                    GuessListItem(
                        guess = "123",
                        numCorrect = 3,
                        numMisplaced = 0
                    )
                ),
                locked = false,
                enabled = false,
                startButtonsVisible = false,
                resetButtonVisible = true,
                localConfigVisible = false,
                webUsers = null,
                mode = null,
                lockName = "MockLock (length 3)"
            ),
            "Correct guess should show expected win state"
        )
    }

    @Test
    fun `revert state if web call fails`() = runBlocking {
        mockWebLockProvider.lock = MockCrashingLock()

        viewModel.selectWebUser(WebLockProvider.User("Test", 3))
        viewModel.input('1')
        viewModel.input('2')
        viewModel.input('3')

        mockViewStateListener.expect(
            STATE_STARTED.copy(mode = Mode.WEB, guess = "12"),
            "Web guess failure should undo last input"
        )
    }

    @Test
    fun `reset mid-game`() = runBlocking {
        viewModel.selectLocalLength("3")
        viewModel.input('3')
        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL, "Reset action should reinitialize mid-game state")
    }

    @Test
    fun `reset post-game`() = runBlocking {
        mockLock.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.selectLocalLength("3")
        viewModel.input('1')
        viewModel.input('2')
        viewModel.input('3')
        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL, "Reset action should reinitialize post-game state")
    }

    @Test
    fun `set new listener`() {
        val mockStateListener2 = MockViewStateListener()

        mockViewStateListener.expect(STATE_INITIAL, "Original listener should see initial state")
        mockStateListener2.expect(null, "New listener should see no state before set")

        viewModel.setListener(mockStateListener2)

        mockViewStateListener.expect(STATE_INITIAL, "Original listener should be unchanged after new listener set")
        mockStateListener2.expect(STATE_INITIAL, "New listener should see current state after set")

        viewModel.selectLocalLength("3")

        mockViewStateListener.expect(STATE_INITIAL, "Original listener should receive no update")
        mockStateListener2.expect(STATE_STARTED, "New listener should receive updated state")
    }

    @Test
    fun `ViewModel lifecycle`() {
        assertTrue(
            viewModel.coroutineContext.isActive,
            "Initialized ViewModel should have active coroutineContext"
        )
        viewModel.deinit()
        assertFalse(
            viewModel.coroutineContext.isActive,
            "ViewModel should have inactive coroutineContext after deinit()"
        )

        // Listener no longer receives updates
        viewModel.selectLocalLength("3")
        mockViewStateListener.expect(STATE_INITIAL, "Listener should receive no updates after deinit()")
    }
}

private class MockLocalLockProvider(var lock: Lock) : LocalLockProvider {
    // TODO better save/load mocks
    override fun loadLock(): Lock? = null

    override fun clearSavedLock() = Unit

    override fun newLock(length: Int): Lock = lock
}

private class MockWebLockProvider(var lock: Lock) : WebLockProvider {
    // TODO better save/load mocks
    override fun loadLock(): Lock? = null

    override fun clearSavedLock() = Unit

    override fun newLock(user: WebLockProvider.User): Lock = lock

    override suspend fun getUsers(): List<WebLockProvider.User> = WEB_USERS
}

private class MockLock : Lock {
    override val codeLength: Int = 3
    override val name: String = "MockLock"

    private lateinit var result: GuessResult

    fun setNextResult(result: GuessResult) {
        this.result = result
    }

    override fun save(settings: Settings) = Unit

    override suspend fun submitGuess(guess: String): GuessResult = result
}

private class MockCrashingLock : Lock {
    override val codeLength: Int = 3
    override val name: String = "MockLock"

    override fun save(settings: Settings) = Unit

    override suspend fun submitGuess(guess: String): GuessResult {
        throw RuntimeException("Test Error!")
    }
}

private class MockViewStateListener : ViewStateListener {

    private var state: ViewState? = null

    override fun invoke(p1: ViewState) {
        this.state = p1
    }

    fun expect(expectedState: ViewState?, message: String) {
        assertEquals(expectedState, state, "Received unexpected state! $message")
    }
}

// TODO Mock this better in order to test save/load logic
private class MockSettings : Settings {
    override fun clear() = Unit
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
    override fun getFloat(key: String, defaultValue: Float): Float = defaultValue
    override fun getInt(key: String, defaultValue: Int): Int = defaultValue
    override fun getLong(key: String, defaultValue: Long): Long = defaultValue
    override fun getString(key: String, defaultValue: String): String = defaultValue
    override fun hasKey(key: String): Boolean = false
    override fun putBoolean(key: String, value: Boolean) = Unit
    override fun putDouble(key: String, value: Double) = Unit
    override fun putFloat(key: String, value: Float) = Unit
    override fun putInt(key: String, value: Int) = Unit
    override fun putLong(key: String, value: Long) = Unit
    override fun putString(key: String, value: String) = Unit
    override fun remove(key: String) = Unit
}

