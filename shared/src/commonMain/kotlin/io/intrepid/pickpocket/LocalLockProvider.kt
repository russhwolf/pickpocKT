package io.intrepid.pickpocket

import com.russhwolf.settings.Settings
import kotlin.random.Random

interface LocalLockProvider {
    fun newLock(length: Int): Lock
    fun loadLock(): Lock?
    fun clearSavedLock()
}

internal class LocalLockProviderImpl(private val settings: Settings) : LocalLockProvider {
    companion object {
        private const val DIGITS = 6
    }

    override fun newLock(length: Int): LocalLock = LocalLock(newCombo(length, DIGITS))
    override fun loadLock(): LocalLock? = LocalLock.load(settings)
    override fun clearSavedLock() = LocalLock.clear(settings)

    private fun newCombo(codeLength: Int, digits: Int): String =
        List(codeLength) { newDigit(digits) }.joinToString(separator = "")

    private fun newDigit(digits: Int): Int = Random.nextInt(digits) + 1
}
