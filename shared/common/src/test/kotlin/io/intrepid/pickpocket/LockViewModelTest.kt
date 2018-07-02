package io.intrepid.pickpocket

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

    private lateinit var mockGuessableProvider: MockGuessableProvider
    private lateinit var mockViewStateListener: MockViewStateListener

    private lateinit var viewModel: LockViewModel

    @BeforeTest
    fun setup() {
        mockGuessableProvider = MockGuessableProvider()
        mockViewStateListener = MockViewStateListener()

        viewModel = LockViewModel(mockGuessableProvider, mockViewStateListener)
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
    fun `incomplete guess`() {
        viewModel.reset()
        viewModel.input('3')

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
    fun `incorrect guess`() {
        mockGuessableProvider.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))

        viewModel.reset()
        viewModel.input('3')
        viewModel.input('2')
        viewModel.input('4')

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
    fun `correct guess`() {
        viewModel.reset()
        mockGuessableProvider.setNextResult(GuessResult(numCorrect = 1, numMisplaced = 1))
        viewModel.input('3')
        viewModel.input('2')
        viewModel.input('4')
        mockGuessableProvider.setNextResult(GuessResult(numCorrect = 0, numMisplaced = 2))
        viewModel.input('3')
        viewModel.input('1')
        viewModel.input('4')
        mockGuessableProvider.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.input('1')
        viewModel.input('2')
        viewModel.input('3')

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
    fun `reset mid-game`() {
        viewModel.reset()
        viewModel.input('3')
        viewModel.reset()

        mockViewStateListener.expect(STATE_INITIAL)
    }

    @Test
    fun `reset post-game`() {
        mockGuessableProvider.setNextResult(GuessResult(numCorrect = 3, numMisplaced = 0))
        viewModel.reset()
        viewModel.input('1')
        viewModel.input('2')
        viewModel.input('3')
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

private class MockGuessableProvider : GuessableProvider {
    override fun newGuessable(codeLength: Int, digits: Int): Guessable = object : Guessable {
        override fun submitGuess(guess: String): GuessResult = result
    }

    private lateinit var result: GuessResult

    fun setNextResult(result: GuessResult) {
        this.result = result
    }
}

private class MockViewStateListener : ViewStateListener {

    private var state: ViewState? = null

    override fun invoke(state: ViewState) {
        this.state = state
    }

    fun expect(expectedState: ViewState?) {
        assertEquals(expectedState, state, "Received unexpected state!")
    }
}
