package com.barter.core.domain.location

class DesktopLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): LocationResult? = null
}
