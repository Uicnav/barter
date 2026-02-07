package com.barter.core.data

import java.util.prefs.Preferences

class DesktopSessionStorage : SessionStorage {

    private val prefs = Preferences.userNodeForPackage(DesktopSessionStorage::class.java)

    override fun saveSession(userJson: String) {
        prefs.put(KEY_USER, userJson)
        prefs.flush()
    }

    override fun loadSession(): String? = prefs.get(KEY_USER, null)

    override fun clearSession() {
        prefs.remove(KEY_USER)
        prefs.flush()
    }

    private companion object {
        const val KEY_USER = "current_user"
    }
}
