package com.barter.core.data

import android.content.Context

class AndroidSessionStorage(context: Context) : SessionStorage {

    private val prefs = context.getSharedPreferences("barter_session", Context.MODE_PRIVATE)

    override fun saveSession(userJson: String) {
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    override fun loadSession(): String? = prefs.getString(KEY_USER, null)

    override fun clearSession() {
        prefs.edit().remove(KEY_USER).apply()
    }

    private companion object {
        const val KEY_USER = "current_user"
    }
}
