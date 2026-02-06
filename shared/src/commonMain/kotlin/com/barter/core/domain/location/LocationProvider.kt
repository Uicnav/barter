package com.barter.core.domain.location

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
)

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationResult?
}
