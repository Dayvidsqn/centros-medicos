package com.example.centrosmedicos.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centrosmedicos.model.CentroMedico
import com.example.centrosmedicos.model.Especialidad
import com.example.centrosmedicos.repository.FirebaseRepository
import com.example.centrosmedicos.repository.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class MainViewModel : ViewModel() {

    private val repo = FirebaseRepository()

    // Reemplaza con tu API key de OpenRouteService (gratis en openrouteservice.org)
    private val ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImViMTY3Njg1MzcxMzRhMmM5MzBjMTc1YjBiOWNlMjY1IiwiaCI6Im11cm11cjY0In0="

    private val _especialidades = MutableLiveData<List<Especialidad>>()
    val especialidades: LiveData<List<Especialidad>> = _especialidades

    private val _routeCoordinates = MutableLiveData<List<List<Double>>?>()
    val routeCoordinates: LiveData<List<List<Double>>?> = _routeCoordinates

    private val _nearestCentro = MutableLiveData<CentroMedico?>()
    val nearestCentro: LiveData<CentroMedico?> = _nearestCentro

    private val _allMarkers = MutableLiveData<List<CentroMedico>>()
    val allMarkers: LiveData<List<CentroMedico>> = _allMarkers

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadEspecialidades()
        seedData()
    }

    private fun seedData() = viewModelScope.launch {
        try { repo.seedIfEmpty() } catch (e: Exception) { /* silencioso */ }
    }

    fun loadEspecialidades() = viewModelScope.launch {
        try {
            _especialidades.value = repo.getEspecialidades()
        } catch (e: Exception) {
            _error.value = "Error cargando especialidades: ${e.message}"
        }
    }

    fun findNearestAndRoute(especialidadId: String, userLocation: LatLng) {
        viewModelScope.launch {
            _loading.value = true
            _routeCoordinates.value = null
            _nearestCentro.value = null

            try {
                val centros = withContext(Dispatchers.IO) {
                    repo.getCentrosByEspecialidad(especialidadId)
                }
                _allMarkers.value = centros

                if (centros.isEmpty()) {
                    _error.value = "No hay centros con esa especialidad registrada"
                    _loading.value = false
                    return@launch
                }

                var bestDistance = Double.MAX_VALUE
                var bestCoords: List<List<Double>>? = null
                var bestCentro: CentroMedico? = null

                for (centro in centros) {
                    try {
                        val start = "${userLocation.longitude},${userLocation.latitude}"
                        val end = "${centro.longitud},${centro.latitud}"
                        val resp = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.getRoute(ORS_API_KEY, start, end)
                        }
                        if (resp.isSuccessful) {
                            val dist = resp.body()?.features?.firstOrNull()
                                ?.properties?.summary?.distance ?: Double.MAX_VALUE
                            if (dist < bestDistance) {
                                bestDistance = dist
                                bestCoords = resp.body()?.features?.firstOrNull()?.geometry?.coordinates
                                bestCentro = centro
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "ORS error para ${centro.nombre}: ${e.message}")
                        // Fallback haversine si la API falla
                        val hDist = haversineKm(
                            userLocation.latitude, userLocation.longitude,
                            centro.latitud, centro.longitud
                        )
                        if (hDist < bestDistance) {
                            bestDistance = hDist
                            bestCentro = centro
                        }
                    }
                }

                _nearestCentro.value = bestCentro
                _routeCoordinates.value = bestCoords

            } catch (e: Exception) {
                _error.value = "Error al calcular ruta: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
