package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlin.random.Random

interface GuessableProvider {
    fun newGuessable(codeLength: Int, digits: Int): Guessable

    fun loadGuessable(): Guessable?
    fun clearSavedGuessable()
}

class LockProvider(private val settings: Settings) : GuessableProvider {
    override fun newGuessable(codeLength: Int, digits: Int): Lock = Lock(newCombo(codeLength, digits))

    override fun loadGuessable(): Lock? = Lock.load(settings)

    override fun clearSavedGuessable() = Lock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}

class WebLockProvider(httpClient: HttpClient, private val settings: Settings) : GuessableProvider {
    private val name = "Paul" // TODO make name selectable
    private val api = LockClient(httpClient)

    override fun loadGuessable(): Guessable? = WebLock.load(settings, api)

    override fun clearSavedGuessable() = WebLock.clear(settings)

    override fun newGuessable(codeLength: Int, digits: Int): Guessable = WebLock(name, api)
}
