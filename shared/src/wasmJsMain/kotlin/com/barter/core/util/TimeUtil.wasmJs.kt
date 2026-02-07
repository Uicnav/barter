package com.barter.core.util

actual fun currentTimeMillis(): Long = js("Date.now()").toLong()
