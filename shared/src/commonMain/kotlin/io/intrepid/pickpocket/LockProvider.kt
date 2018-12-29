package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import kotlin.random.Random

interface LockProvider<in T> {
    fun newLock(config: T): Lock
    fun loadLock(): Lock?
    fun clearSavedLock()
}

class LocalLockProvider(private val settings: Settings) : LockProvider<Int> {
    companion object {
        private const val DIGITS = 6
    }

    override fun newLock(config: Int): LocalLock = LocalLock(newCombo(config, DIGITS))

    override fun loadLock(): LocalLock? = LocalLock.load(settings)

    override fun clearSavedLock() = LocalLock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

class WebLockProvider(private val api: LockApi, private val settings: Settings) : LockProvider<WebLockProvider.User> {
    override fun loadLock(): Lock? = WebLock.load(settings, api)

    override fun clearSavedLock() = WebLock.clear(settings)

    override fun newLock(config: User): Lock = WebLock(config.name, config.codeLength, api)

    data class User(val name: String, val codeLength: Int) {
        override fun toString(): String = "$name (length $codeLength)"
    }
}
