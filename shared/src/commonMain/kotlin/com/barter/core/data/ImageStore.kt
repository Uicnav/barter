package com.barter.core.data

import com.barter.core.util.currentTimeMillis
import kotlin.random.Random

object ImageStore {
    private val store = mutableMapOf<String, ByteArray>()

    fun store(bytes: ByteArray): String {
        val key = "local_${currentTimeMillis()}_${Random.nextInt(10000)}"
        store[key] = bytes
        return key
    }

    fun load(key: String): ByteArray? = store[key]

    fun isLocal(key: String): Boolean = key in store
}
