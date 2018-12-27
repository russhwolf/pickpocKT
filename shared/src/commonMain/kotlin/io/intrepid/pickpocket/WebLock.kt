package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign
import com.russhwolf.settings.set

private const val API_TOKEN = "331f6ac6-3a63-11e7-ae72-12ad2ae1db2b"
private const val KEY_WEB_LOCK = "web_lock"

class WebLock(private val user: String, private val api: LockApi) : Lock {
    override fun save(settings: Settings) {
        settings[KEY_WEB_LOCK] = user
    }

    override suspend fun submitGuess(guess: String): GuessResult {
        val formattedGuess = guess.toList().joinToString(prefix = "[", postfix = "]", separator = ",")
        val result = try {
            api.pickLock(user, PickLockRequest(formattedGuess, API_TOKEN)).result
        } catch (e: Exception) {
            error("${e::class.simpleName}: ${e.message}")
        }
        return GuessResult(
            numCorrect = result.correct,
            numMisplaced = result.close
        )
    }

    companion object {
        fun load(settings: Settings, api: LockApi): WebLock? =
            if (KEY_WEB_LOCK in settings) WebLock(settings[KEY_WEB_LOCK, ""], api) else null

        fun clear(settings: Settings) {
            settings -= KEY_WEB_LOCK
        }
    }
}
