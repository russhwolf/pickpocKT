package io.intrepid.pickpocket

import kotlin.test.Test
import kotlin.test.assertEquals

class LockTest {
    @Test
    fun `run all test cases`() {
        for (testCase in testCases) {
            val lock = Lock(testCase.code)
            val result = lock.submitGuess(testCase.guess)
            assertEquals(testCase.result, result, "Incorrect result for code=${testCase.code} guess=${testCase.guess}")
        }
    }
}

private val testCases = listOf(
    TestCase("123", "411", GuessResult(0, 1)),
    TestCase("411", "123", GuessResult(0, 1)),
    TestCase("123", "114", GuessResult(1, 0)),

    TestCase("123", "123", GuessResult(3, 0)),
    TestCase("123", "456", GuessResult(0, 0)),
    TestCase("123", "156", GuessResult(1, 0)),
    TestCase("123", "416", GuessResult(0, 1)),
    TestCase("123", "411", GuessResult(0, 1)),
    TestCase("123", "114", GuessResult(1, 0)),

    TestCase("123", "345", GuessResult(0, 1))
)

private data class TestCase(val code: String, val guess: String, val result: GuessResult)
