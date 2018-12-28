package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import kotlin.random.Random

interface LockProvider {
    fun newLock(): Lock
    fun loadLock(): Lock?
    fun clearSavedLock()
}

class LocalLockProvider(private val settings: Settings) : LockProvider {
    companion object {
        private const val DIGITS = 6
        private const val CODE_LENGTH = 3
    }

    override fun newLock(): LocalLock = LocalLock(newCombo(CODE_LENGTH, DIGITS))

    override fun loadLock(): LocalLock? = LocalLock.load(settings)

    override fun clearSavedLock() = LocalLock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

class WebLockProvider(httpClientEngine: HttpClientEngine, private val settings: Settings) : LockProvider {
    private val name = "Paul" // TODO make name selectable
    private val codeLength = 3 // TODO make this dynamic
    private val api = LockClient(httpClientEngine)

    override fun loadLock(): Lock? = WebLock.load(settings, api)

    override fun clearSavedLock() = WebLock.clear(settings)

    override fun newLock(): Lock = WebLock(name, codeLength, api)
}
