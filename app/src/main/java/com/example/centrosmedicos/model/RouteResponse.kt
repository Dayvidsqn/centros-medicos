package com.example.centrosmedicos.model

import com.google.gson.annotations.SerializedName

data class RouteResponse(
    @SerializedName("features") val features: List<Feature>
)

data class Feature(
    @SerializedName("geometry") val geometry: Geometry,
    @SerializedName("properties") val properties: RouteProperties
)

data class Geometry(
    @SerializedName("coordinates") val coordinates: List<List<Double>>
)

data class RouteProperties(
    @SerializedName("summary") val summary: RouteSummary
)

data class RouteSummary(
    @SerializedName("distance") val distance: Double,  // metros
    @SerializedName("duration") val duration: Double   // segundos
)
