package com.barter.core.data

class WasmSessionStorage : SessionStorage {
    override fun saveSession(userJson: String) {}
    override fun loadSession(): String? = null
    override fun clearSession() {}
}
