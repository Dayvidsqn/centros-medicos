package com.example.centrosmedicos.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.centrosmedicos.model.CentroMedico
import com.example.centrosmedicos.model.Especialidad
import com.example.centrosmedicos.model.HorarioAtencion
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FirebaseRepository {

    private val db = FirebaseDatabase.getInstance()
    private val refEspecialidades = db.getReference("especialidades")
    private val refCentros = db.getReference("centros_medicos")

    // ─── ESPECIALIDADES ──────────────────────────────────────────────────────

    suspend fun getEspecialidades(): List<Especialidad> {
        val snapshot = refEspecialidades.get().await()
        return snapshot.children.mapNotNull { child ->
            child.getValue(Especialidad::class.java)?.also { it.id = child.key ?: "" }
        }
    }

    suspend fun saveEspecialidad(especialidad: Especialidad): String {
        return if (especialidad.id.isBlank()) {
            val newRef = refEspecialidades.push()
            newRef.setValue(especialidad).await()
            newRef.key ?: ""
        } else {
            refEspecialidades.child(especialidad.id).setValue(especialidad).await()
            especialidad.id
        }
    }

    suspend fun deleteEspecialidad(id: String) {
        refEspecialidades.child(id).removeValue().await()
    }

    // ─── CENTROS MÉDICOS ─────────────────────────────────────────────────────

    suspend fun getCentrosMedicos(): List<CentroMedico> {
        val snapshot = refCentros.get().await()
        return snapshot.children.mapNotNull { child ->
            child.getValue(CentroMedico::class.java)?.also { it.id = child.key ?: "" }
        }
    }

    suspend fun getCentrosByEspecialidad(especialidadId: String): List<CentroMedico> {
        // Filtramos en cliente: RTDB no soporta queries sobre campos de map anidado fácilmente
        return getCentrosMedicos().filter { centro ->
            centro.especialidades[especialidadId] == true
        }
    }

    suspend fun saveCentroMedico(centro: CentroMedico): String {
        return if (centro.id.isBlank()) {
            val newRef = refCentros.push()
            newRef.setValue(centro).await()
            newRef.key ?: ""
        } else {
            refCentros.child(centro.id).setValue(centro).await()
            centro.id
        }
    }

    suspend fun deleteCentroMedico(id: String) {
        refCentros.child(id).removeValue().await()
    }

    // ─── IMAGEN: COMPRESIÓN Y BASE64 ─────────────────────────────────────────

    /**
     * Convierte la URI de la imagen de galería a Base64 comprimida.
     * Redimensiona a máx 400x400px y comprime al 50% JPEG para
     * mantenerse bien por debajo del límite de 1MB por nodo de RTDB.
     */
    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Redimensionar manteniendo proporción
        val maxDim = 400
        val scaled = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val (w, h) = if (ratio > 1) Pair(maxDim, (maxDim / ratio).toInt())
                         else Pair((maxDim * ratio).toInt(), maxDim)
            Bitmap.createScaledBitmap(originalBitmap, w, h, true)
        } else originalBitmap

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    // ─── SEED: DATOS INICIALES REALES DE LIMA ────────────────────────────────

    /**
     * Llama esta función UNA sola vez al primer arranque.
     * Inserta especialidades y centros médicos reales de Lima.
     */
    suspend fun seedIfEmpty() {
        val snap = refEspecialidades.limitToFirst(1).get().await()
        if (snap.exists()) return

        // 1. Especialidades
        val especialidades = listOf(
            Especialidad(nombre = "Cardiología",        descripcion = "Enfermedades del corazón",        icono = "❤️"),
            Especialidad(nombre = "Hepatología",        descripcion = "Enfermedades del hígado",         icono = "🫀"),
            Especialidad(nombre = "Traumatología",      descripcion = "Lesiones óseas y musculares",     icono = "🦴"),
            Especialidad(nombre = "Neurología",         descripcion = "Sistema nervioso",                icono = "🧠"),
            Especialidad(nombre = "Emergencia General", descripcion = "Urgencias y emergencias",         icono = "🚑"),
            Especialidad(nombre = "Neumología",         descripcion = "Sistema respiratorio",            icono = "🫁"),
            Especialidad(nombre = "Gastroenterología",  descripcion = "Sistema digestivo",               icono = "🏥")
        )
        val ids = mutableListOf<String>()
        for (esp in especialidades) {
            val ref = refEspecialidades.push()
            ref.setValue(esp).await()
            ids.add(ref.key ?: "")
        }

        val cardioId = ids[0]; val hepatoId = ids[1]; val traumaId = ids[2]
        val neuroId  = ids[3]; val emergId  = ids[4]; val neumoId  = ids[5]; val gastroId = ids[6]

        // Helper para construir el mapa de especialidades
        fun espMap(vararg espIds: String): Map<String, Boolean> =
            espIds.associateWith { true }

        // Horarios de ejemplo comunes
        val horarioEstandar = listOf(
            HorarioAtencion("Lunes",    "08:00", "20:00"),
            HorarioAtencion("Martes",   "08:00", "20:00"),
            HorarioAtencion("Miércoles","08:00", "20:00"),
            HorarioAtencion("Jueves",   "08:00", "20:00"),
            HorarioAtencion("Viernes",  "08:00", "20:00"),
            HorarioAtencion("Sábado",   "08:00", "14:00")
        )
        val horario24h = listOf(
            HorarioAtencion("Lunes",    "00:00", "23:59"),
            HorarioAtencion("Martes",   "00:00", "23:59"),
            HorarioAtencion("Miércoles","00:00", "23:59"),
            HorarioAtencion("Jueves",   "00:00", "23:59"),
            HorarioAtencion("Viernes",  "00:00", "23:59"),
            HorarioAtencion("Sábado",   "00:00", "23:59"),
            HorarioAtencion("Domingo",  "00:00", "23:59")
        )

        // 2. Centros médicos reales de Lima
        val centros = listOf(
            CentroMedico(
                nombre = "Hospital Nacional Edgardo Rebagliati Martins",
                direccion = "Av. Rebagliati 490, Jesús María, Lima",
                telefono = "+51 1 265-4901",
                tipo = "Hospital",
                latitud = -12.0781, longitud = -77.0367,
                especialidades = espMap(cardioId, neuroId, emergId, traumaId, neumoId),
                horarios = horario24h
            ),
            CentroMedico(
                nombre = "Hospital Nacional Arzobispo Loayza",
                direccion = "Av. Alfonso Ugarte 848, Cercado de Lima",
                telefono = "+51 1 613-8900",
                tipo = "Hospital",
                latitud = -12.0498, longitud = -77.0427,
                especialidades = espMap(cardioId, hepatoId, emergId, gastroId, neumoId),
                horarios = horario24h
            ),
            CentroMedico(
                nombre = "Clínica Anglo Americana",
                direccion = "Av. Alfredo Salazar 350, San Isidro, Lima",
                telefono = "+51 1 616-8900",
                tipo = "Clínica",
                latitud = -12.1005, longitud = -77.0335,
                especialidades = espMap(cardioId, hepatoId, traumaId, neuroId, emergId),
                horarios = horarioEstandar
            ),
            CentroMedico(
                nombre = "Clínica Internacional",
                direccion = "Jr. Washington 1471, Lima Centro",
                telefono = "+51 1 619-6161",
                tipo = "Clínica",
                latitud = -12.0528, longitud = -77.0358,
                especialidades = espMap(cardioId, emergId, neumoId, gastroId),
                horarios = horarioEstandar
            ),
            CentroMedico(
                nombre = "Hospital María Auxiliadora",
                direccion = "Av. Miguel Iglesias 968, San Juan de Miraflores",
                telefono = "+51 1 217-8800",
                tipo = "Hospital",
                latitud = -12.1667, longitud = -76.9667,
                especialidades = espMap(emergId, traumaId, neumoId, hepatoId),
                horarios = horario24h
            ),
            CentroMedico(
                nombre = "Clínica San Borja",
                direccion = "Av. Guardia Civil 337, San Borja, Lima",
                telefono = "+51 1 475-4000",
                tipo = "Clínica",
                latitud = -12.1069, longitud = -77.0018,
                especialidades = espMap(cardioId, neuroId, traumaId, emergId),
                horarios = horarioEstandar
            ),
            CentroMedico(
                nombre = "Hospital Nacional Hipólito Unanue",
                direccion = "Av. César Vallejo 1390, El Agustino, Lima",
                telefono = "+51 1 326-2577",
                tipo = "Hospital",
                latitud = -12.0553, longitud = -76.9989,
                especialidades = espMap(emergId, hepatoId, gastroId, neumoId, traumaId),
                horarios = horario24h
            ),
            CentroMedico(
                nombre = "Clínica Ricardo Palma",
                direccion = "Av. Javier Prado Este 1066, San Isidro",
                telefono = "+51 1 224-2224",
                tipo = "Clínica",
                latitud = -12.0939, longitud = -77.0272,
                especialidades = espMap(cardioId, hepatoId, gastroId, neuroId),
                horarios = horarioEstandar
            )
        )
        for (centro in centros) {
            refCentros.push().setValue(centro).await()
        }
    }
}
