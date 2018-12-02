package io.intrepid.pickpocket

import kotlin.test.Test
import kotlin.test.assertEquals

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
        val lock = WebLock("Test", api)

        val expected = GuessResult(1, 2)
        val result = lock.submitGuess("123")

        assertEquals("Test", verifiedUser, "User not passed correctly!")
        assertEquals(PickLockRequest("[1,2,3]", "331f6ac6-3a63-11e7-ae72-12ad2ae1db2b"), verifiedRequest, "Request not generated correctly")
        assertEquals(expected, result, "Result not handled correctly")
    }

}
