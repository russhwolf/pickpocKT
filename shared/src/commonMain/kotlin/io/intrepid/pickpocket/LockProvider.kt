package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.random.Random

interface LockProvider {
    fun newLock(codeLength: Int, digits: Int): Lock

    fun loadLock(): Lock?
    fun clearSavedLock()
}

class LocalLockProvider(private val settings: Settings) : LockProvider {
    override fun newLock(codeLength: Int, digits: Int): LocalLock = LocalLock(newCombo(codeLength, digits))

    override fun loadLock(): LocalLock? = LocalLock.load(settings)

    override fun clearSavedLock() = LocalLock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

class WebLockProvider(httpClientEngine: HttpClientEngine, private val settings: Settings) : LockProvider {
    private val name = "Paul" // TODO make name selectable
    private val api = LockClient(httpClientEngine)

    override fun loadLock(): Lock? = WebLock.load(settings, api)

    override fun clearSavedLock() = WebLock.clear(settings)

    override fun newLock(codeLength: Int, digits: Int): Lock = WebLock(name, api)
}
