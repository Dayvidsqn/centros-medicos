package com.example.centrosmedicos.ui.especialidad

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.centrosmedicos.databinding.ActivityFormEspecialidadBinding
import com.example.centrosmedicos.model.Especialidad
import com.example.centrosmedicos.repository.FirebaseRepository
import kotlinx.coroutines.launch

class FormEspecialidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormEspecialidadBinding
    private val repo = FirebaseRepository()
    private var editId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormEspecialidadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Modo edición: pre-cargar datos
        editId = intent.getStringExtra("id") ?: ""
        if (editId.isNotBlank()) {
            binding.etNombre.setText(intent.getStringExtra("nombre"))
            binding.etDescripcion.setText(intent.getStringExtra("descripcion"))
            binding.etIcono.setText(intent.getStringExtra("icono"))
            supportActionBar?.title = "Editar Especialidad"
        } else {
            supportActionBar?.title = "Nueva Especialidad"
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun guardar() {
        val nombre = binding.etNombre.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val icono = binding.etIcono.text.toString().trim()

        if (nombre.isBlank()) {
            binding.etNombre.error = "Campo requerido"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false

        val especialidad = Especialidad(
            id = editId,
            nombre = nombre,
            descripcion = descripcion,
            icono = icono.ifBlank { "🏥" }
        )

        lifecycleScope.launch {
            try {
                repo.saveEspecialidad(especialidad)
                Toast.makeText(this@FormEspecialidadActivity, "Guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@FormEspecialidadActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
