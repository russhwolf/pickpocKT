package io.intrepid.pickpocket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class WebLockTest {
    @Test
    fun `format guess on submit`() = runBlocking {
        var verifiedUser: String? = null
        var verifiedRequest: PickLockRequest? = null
        val verifier: (String, PickLockRequest) -> Unit = { user, request ->
            verifiedUser = user
            verifiedRequest = request
        }
        val response = PickLockResponse(PickLockResponse.Result(2, 1))
        val api = object : LockApi {
            override suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse {
                verifier.invoke(user, pickLockRequest)
                return response
            }

            override suspend fun getUsers(): GetUsersResponse = throw NotImplementedError()
        }
        val lock = WebLock("Test", 3, api, "1234567890ABCDEF")

        val expected = GuessResult(1, 2)
        val result = lock.submitGuess("123")

        assertEquals("Test", verifiedUser, "User not passed correctly!")
        assertEquals(PickLockRequest("[1,2,3]", "1234567890ABCDEF"), verifiedRequest, "Request not generated correctly")
        assertEquals(expected, result, "Result not handled correctly")
    }

    @Test
    fun `report failed submit`() = runBlocking {
        val testException = Exception("Expected Test Failure")
        val api = object : LockApi {
            override suspend fun pickLock(user: String, pickLockRequest: PickLockRequest): PickLockResponse {
                throw testException
            }

            override suspend fun getUsers(): GetUsersResponse = throw NotImplementedError()
        }
        val lock = WebLock("Test", 3, api, "1234567890ABCDEF")

        val error = try {
            lock.submitGuess("123")
            fail("submitGuess() should throw Exception")
        } catch (throwable: Throwable) {
            throwable
        }

        assertTrue(error is RuntimeException && error.cause == testException, "Unexpected error from submitGuess()")
    }
}
