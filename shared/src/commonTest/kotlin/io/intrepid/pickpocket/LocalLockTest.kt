package io.intrepid.pickpocket

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalLockTest {
    @Test
    fun `run all test cases`() = runBlocking {
        testCase(code = "123", guess = "411", numCorrect = 0, numMisplaced = 1)
        testCase(code = "411", guess = "123", numCorrect = 0, numMisplaced = 1)
        testCase(code = "123", guess = "114", numCorrect = 1, numMisplaced = 0)

        testCase(code = "123", guess = "123", numCorrect = 3, numMisplaced = 0)
        testCase(code = "123", guess = "456", numCorrect = 0, numMisplaced = 0)
        testCase(code = "123", guess = "156", numCorrect = 1, numMisplaced = 0)
        testCase(code = "123", guess = "416", numCorrect = 0, numMisplaced = 1)
        testCase(code = "123", guess = "411", numCorrect = 0, numMisplaced = 1)
        testCase(code = "123", guess = "114", numCorrect = 1, numMisplaced = 0)

        testCase(code = "123", guess = "345", numCorrect = 0, numMisplaced = 1)
        testCase(code = "12", guess = "23", numCorrect = 0, numMisplaced = 1)
        testCase(code = "", guess = "", numCorrect = 0, numMisplaced = 0)
    }
}

suspend fun testCase(code: String, guess: String, numCorrect: Int, numMisplaced: Int) {
    val lock = LocalLock(code)
    val result = lock.submitGuess(guess)
    assertEquals(
        GuessResult(numCorrect = numCorrect, numMisplaced = numMisplaced),
        result,
        "Incorrect result for code=$code guess=$guess"
    )
}
