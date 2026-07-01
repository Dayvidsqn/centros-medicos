package com.example.centrosmedicos.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Especialidad(
    @get:Exclude var id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String = ""
) {
    constructor() : this("", "", "", "")
}
