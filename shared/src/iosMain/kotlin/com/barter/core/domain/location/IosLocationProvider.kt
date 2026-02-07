package com.barter.core.domain.location

class IosLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): LocationResult? = null
}
