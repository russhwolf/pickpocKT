package io.intrepid.pickpocket

import com.russhwolf.settings.ExperimentalListener
import com.russhwolf.settings.Settings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val STATE_INITIAL = ViewState(
    guess = "",
    results = listOf(),
    locked = true,
    enabled = false
)

private val STATE_STARTED = ViewState(
    guess = "",
    results = listOf(),
    locked = true,
    enabled = true
)

class LockViewModelTest {

    private lateinit var mockLockProvider: MockLockProvider
    private lateinit var mockSettings: MockSettings
    private lateinit var mockViewStateListener: MockViewStateListener

    private lateinit var viewModel: LockViewModel

    @BeforeTest
    fun setup() {
        mockLockProvider = MockLockProvider()
        mockSettings = MockSettings()
        mockViewStateListener = MockViewStateListener()

        viewModel = LockViewModel(mockSettings, mockLockProvider, mockViewStateListener)
    }

    @Test
    fun `initial state`() {
        mockViewStateListener.expect(STATE_INITIAL)
    }

    @Test
    fun `start game`() {
        viewModel.reset()

        mockViewStateListener.expect(STATE_STARTED)
    }

    @Test
    fun `incomplete guess`() = runBlocking {
        viewModel.reset()
        viewModel.input("3")

        mockViewStateListener.expect(
            ViewState(
                guess = "3",
                results = listOf(),
                locked = true,
                enabled = true
            )
        )
    }

    @Test
    fun `incorrect guess`() = runBlocking {
        mockLockProvider.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))

        viewModel.reset()
        viewModel.input("3")
        viewModel.input("2")
        viewModel.input("4")

        mockViewStateListener.expect(
            ViewState(
                guess = "",
                results = listOf(
                    GuessListItem(
                        guess = "324",
                        numCorrect = 1,
                        numMisplaced = 1
                    )
                ),
                locked = true,
                enabled = true

            )
        )
    }

    @Test
    fun `correct guess`() = runBlocking {
        viewModel.reset()
        mockLockProvider.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))
        viewModel.input("3")
        viewModel.input("2")
        viewModel.input("4")
        mockLockProvider.setNextResult(GuessResult(numCorrect = 0, numMisplaced = 2))
        viewModel.input("3")
        viewModel.input("1")
        viewModel.input("4")
        mockLockProvider.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.input("1")
        viewModel.input("2")
        viewModel.input("3")

        mockViewStateListener.expect(
            ViewState(
                guess = "",
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
                enabled = false
            )
        )
    }

    @Test
    fun `reset mid-game`() = runBlocking {
        viewModel.reset()
        viewModel.input("3")
        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL)
    }

    @Test
    fun `reset post-game`() = runBlocking {
        mockLockProvider.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.reset()
        viewModel.input("1")
        viewModel.input("2")
        viewModel.input("3")
        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL)
    }

    @Test
    fun `set new listener`() {
        val mockStateListener2 = MockViewStateListener()

        mockViewStateListener.expect(STATE_INITIAL)
        mockStateListener2.expect(null)

        viewModel.setListener(mockStateListener2)

        mockViewStateListener.expect(STATE_INITIAL)
        mockStateListener2.expect(STATE_INITIAL)

        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL)
        mockStateListener2.expect(STATE_STARTED)
    }
}

private class MockLockProvider : LockProvider {
    // TODO better save/load mocks
    override fun loadLock(): Lock? = null

    override fun clearSavedLock() = Unit

    override fun newLock(codeLength: Int, digits: Int): Lock = object : Lock {
        override fun save(settings: Settings) = Unit

        override suspend fun submitGuess(guess: String): GuessResult = result
    }

    private lateinit var result: GuessResult

    fun setNextResult(result: GuessResult) {
        this.result = result
    }
}

private class MockViewStateListener : ViewStateListener {

    private var state: ViewState? = null

    override fun invoke(p1: ViewState) {
        this.state = p1
    }

    fun expect(expectedState: ViewState?) {
        assertEquals(expectedState, state, "Received unexpected state!")
    }
}

// TODO Mock this better in order to test save/load logic
private class MockSettings: Settings {
    @ExperimentalListener
    override fun addListener(key: String, callback: () -> Unit): Settings.Listener = throw NotImplementedError()
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
    @ExperimentalListener
    override fun removeListener(listener: Settings.Listener) = Unit
}

