package com.example.centrosmedicos.repository

import com.example.centrosmedicos.model.RouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    /**
     * Ruta en auto considerando las condiciones de tráfico.
     * OpenRouteService profile: driving-car
     * Docs: https://openrouteservice.org/dev/#/api-docs/v2/directions/{profile}/get
     */
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start", encoded = true) start: String,   // "lon,lat"
        @Query("end", encoded = true) end: String         // "lon,lat"
    ): Response<RouteResponse>
}
