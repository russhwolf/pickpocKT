package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.minusAssign
import com.russhwolf.settings.set

private const val API_TOKEN = "331f6ac6-3a63-11e7-ae72-12ad2ae1db2b"
private const val KEY_WEB_LOCK_NAME = "web_lock_name"
private const val KEY_WEB_LOCK_LENGTH = "web_lock_length"

class WebLock(
    override val name: String,
    override val codeLength: Int,
    private val api: LockApi,
    private val apiToken: String = API_TOKEN
) : Lock {
    override fun save(settings: Settings) {
        settings[KEY_WEB_LOCK_NAME] = name
        settings[KEY_WEB_LOCK_LENGTH] = codeLength
    }

    override suspend fun submitGuess(guess: String): GuessResult {
        val formattedGuess = guess.toList().joinToString(prefix = "[", postfix = "]", separator = ",")
        val result = try {
            api.pickLock(name, PickLockRequest(formattedGuess, apiToken)).result
        } catch (e: Exception) {
            throw RuntimeException("An error occurred while submitting guess!", e)
        }
        return GuessResult(
            numCorrect = result.correct,
            numMisplaced = result.close
        )
    }

    companion object {
        fun load(settings: Settings, api: LockApi): WebLock? =
            if (KEY_WEB_LOCK_NAME in settings && KEY_WEB_LOCK_LENGTH in settings) {
                WebLock(settings[KEY_WEB_LOCK_NAME, ""], settings[KEY_WEB_LOCK_LENGTH, 0], api)
            } else {
                null
            }

        fun clear(settings: Settings) {
            settings -= KEY_WEB_LOCK_NAME
            settings -= KEY_WEB_LOCK_LENGTH
        }
    }
}
