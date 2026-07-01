package com.example.centrosmedicos.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CentroMedico(
    // id no se guarda en el nodo, se asigna desde la key del snapshot
    @get:Exclude var id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val tipo: String = "Hospital",          // "Hospital" | "Clínica" | "Centro de Salud"
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    // imagen guardada como Base64 comprimida directamente en RTDB
    val imagenBase64: String = "",
    // mapa de especialidadId -> true  (estructura idiomática en RTDB)
    val especialidades: Map<String, Boolean> = emptyMap(),
    // lista de horarios como map para RTDB
    val horarios: List<HorarioAtencion> = emptyList()
) {
    constructor() : this("", "", "", "", "Hospital", 0.0, 0.0, "", emptyMap(), emptyList())

    /** Devuelve los IDs de especialidades marcadas como true */
    @Exclude
    fun getEspecialidadIds(): List<String> = especialidades.filter { it.value }.keys.toList()
}
