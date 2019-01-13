package io.intrepid.pickpocket

import com.russhwolf.settings.Settings

interface WebLockProvider {
    fun loadLock(): Lock?
    fun clearSavedLock()
    fun newLock(user: User): Lock
    suspend fun getUsers(): List<User>

    data class User(val name: String, val codeLength: Int) {
        override fun toString(): String = "$name (length $codeLength)"
    }
}

internal class WebLockProviderImpl(private val api: LockApi, private val settings: Settings) : WebLockProvider {
    override fun loadLock(): Lock? = WebLock.load(settings, api)
    override fun clearSavedLock() = WebLock.clear(settings)
    override fun newLock(user: WebLockProvider.User): Lock = WebLock(user.name, user.codeLength, api)
    override suspend fun getUsers(): List<WebLockProvider.User> =
        api.getUsers().result.map { WebLockProvider.User(it.userId, it.combinationLength) }

}
