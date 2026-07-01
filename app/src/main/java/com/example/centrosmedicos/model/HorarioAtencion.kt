package com.example.centrosmedicos.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class HorarioAtencion(
    val dia: String = "",
    val apertura: String = "",
    val cierre: String = ""
) {
    constructor() : this("", "", "")
    override fun toString() = "$dia: $apertura - $cierre"
}
