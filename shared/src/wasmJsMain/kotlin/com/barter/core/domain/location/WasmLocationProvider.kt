package com.barter.core.domain.location

class WasmLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): LocationResult? = null
}
