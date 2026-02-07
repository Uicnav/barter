package com.barter.core.data

interface SessionStorage {
    fun saveSession(userJson: String)
    fun loadSession(): String?
    fun clearSession()
}
