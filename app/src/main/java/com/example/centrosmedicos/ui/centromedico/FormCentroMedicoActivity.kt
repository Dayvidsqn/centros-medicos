package com.example.centrosmedicos.ui.centromedico

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.centrosmedicos.databinding.ActivityFormCentroMedicoBinding
import com.example.centrosmedicos.model.CentroMedico
import com.example.centrosmedicos.model.HorarioAtencion
import com.example.centrosmedicos.repository.FirebaseRepository
import kotlinx.coroutines.launch

class FormCentroMedicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormCentroMedicoBinding
    private val repo = FirebaseRepository()

    private var editId: String = ""
    private var imagenBase64Actual: String = ""

    private val galeriaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    // Mostrar preview
                    binding.ivFachada.setImageURI(uri)
                    // Convertir a Base64 comprimido para guardar en RTDB
                    imagenBase64Actual = repo.uriToBase64(this, uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormCentroMedicoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tipos = listOf("Hospital", "Clínica", "Centro de Salud")
        binding.spinnerTipo.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, tipos)

        binding.btnSeleccionarImagen.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galeriaLauncher.launch(intent)
        }

        binding.btnGuardar.setOnClickListener { guardar() }

        editId = intent.getStringExtra("centroId") ?: ""

        lifecycleScope.launch {
            cargarEspecialidades()
            if (editId.isNotBlank()) cargarParaEditar()
            else supportActionBar?.title = "Nuevo Centro Médico"
        }
    }

    private suspend fun cargarEspecialidades() {
        try {
            val lista = repo.getEspecialidades()
            binding.llEspecialidades.removeAllViews()
            lista.forEach { esp ->
                val cb = CheckBox(this)
                cb.text = "${esp.icono} ${esp.nombre}"
                cb.tag = esp.id
                binding.llEspecialidades.addView(cb)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando especialidades", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun cargarParaEditar() {
        supportActionBar?.title = "Editar Centro Médico"
        try {
            val lista = repo.getCentrosMedicos()
            val centro = lista.find { it.id == editId } ?: return
            binding.etNombre.setText(centro.nombre)
            binding.etDireccion.setText(centro.direccion)
            binding.etTelefono.setText(centro.telefono)
            binding.etLatitud.setText(centro.latitud.toString())
            binding.etLongitud.setText(centro.longitud.toString())

            val tipos = listOf("Hospital", "Clínica", "Centro de Salud")
            binding.spinnerTipo.setSelection(tipos.indexOf(centro.tipo).coerceAtLeast(0))

            // Mostrar imagen guardada como Base64
            if (centro.imagenBase64.isNotBlank()) {
                imagenBase64Actual = centro.imagenBase64
                val bytes = Base64.decode(centro.imagenBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivFachada.setImageBitmap(bmp)
            }

            // Marcar especialidades
            for (i in 0 until binding.llEspecialidades.childCount) {
                val cb = binding.llEspecialidades.getChildAt(i) as? CheckBox
                cb?.isChecked = centro.especialidades[cb?.tag] == true
            }

            if (centro.horarios.isNotEmpty()) {
                binding.etHorarios.setText(
                    centro.horarios.joinToString("\n") { "${it.dia}: ${it.apertura} - ${it.cierre}" }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando datos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardar() {
        val nombre   = binding.etNombre.text.toString().trim()
        val direccion = binding.etDireccion.text.toString().trim()
        val telefono  = binding.etTelefono.text.toString().trim()
        val latStr    = binding.etLatitud.text.toString().trim()
        val lonStr    = binding.etLongitud.text.toString().trim()

        if (nombre.isBlank() || latStr.isBlank() || lonStr.isBlank()) {
            Toast.makeText(this, "Nombre y coordenadas son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        val lat = latStr.toDoubleOrNull() ?: run {
            Toast.makeText(this, "Latitud inválida", Toast.LENGTH_SHORT).show(); return
        }
        val lon = lonStr.toDoubleOrNull() ?: run {
            Toast.makeText(this, "Longitud inválida", Toast.LENGTH_SHORT).show(); return
        }

        // Mapa de especialidades (id -> true)
        val espMap = mutableMapOf<String, Boolean>()
        for (i in 0 until binding.llEspecialidades.childCount) {
            val cb = binding.llEspecialidades.getChildAt(i) as? CheckBox
            if (cb?.isChecked == true) espMap[cb.tag as String] = true
        }

        // Parsear horarios
        val horariosTexto = binding.etHorarios.text.toString().trim()
        val horarios = mutableListOf<HorarioAtencion>()
        if (horariosTexto.isNotBlank()) {
            horariosTexto.lines().forEach { linea ->
                val partes = linea.split(":")
                if (partes.size >= 2) {
                    val dia = partes[0].trim()
                    val horas = partes.drop(1).joinToString(":").split("-")
                    if (horas.size == 2)
                        horarios.add(HorarioAtencion(dia, horas[0].trim(), horas[1].trim()))
                }
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false

        lifecycleScope.launch {
            try {
                val centro = CentroMedico(
                    id = editId,
                    nombre = nombre,
                    direccion = direccion,
                    telefono = telefono,
                    tipo = binding.spinnerTipo.selectedItem.toString(),
                    latitud = lat,
                    longitud = lon,
                    imagenBase64 = imagenBase64Actual,
                    especialidades = espMap,
                    horarios = horarios
                )
                repo.saveCentroMedico(centro)
                Toast.makeText(this@FormCentroMedicoActivity, "Guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@FormCentroMedicoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
