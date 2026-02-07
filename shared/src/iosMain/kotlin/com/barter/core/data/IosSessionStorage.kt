package com.barter.core.data

import platform.Foundation.NSUserDefaults

class IosSessionStorage : SessionStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun saveSession(userJson: String) {
        defaults.setObject(userJson, forKey = KEY_USER)
    }

    override fun loadSession(): String? = defaults.stringForKey(KEY_USER)

    override fun clearSession() {
        defaults.removeObjectForKey(KEY_USER)
    }

    private companion object {
        const val KEY_USER = "current_user"
    }
}
