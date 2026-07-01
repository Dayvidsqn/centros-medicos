package com.example.centrosmedicos.ui.especialidad

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centrosmedicos.adapter.EspecialidadAdapter
import com.example.centrosmedicos.databinding.ActivityListEspecialidadBinding
import com.example.centrosmedicos.model.Especialidad
import com.example.centrosmedicos.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ListEspecialidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListEspecialidadBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: EspecialidadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListEspecialidadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = EspecialidadAdapter(
            onEdit = { esp ->
                val intent = Intent(this, FormEspecialidadActivity::class.java)
                intent.putExtra("id", esp.id)
                intent.putExtra("nombre", esp.nombre)
                intent.putExtra("descripcion", esp.descripcion)
                intent.putExtra("icono", esp.icono)
                startActivity(intent)
            },
            onDelete = { esp -> confirmarEliminar(esp) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAgregar.setOnClickListener {
            startActivity(Intent(this, FormEspecialidadActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEspecialidades()
    }

    private fun cargarEspecialidades() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val lista = repo.getEspecialidades()
                adapter.actualizar(lista)
                binding.tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@ListEspecialidadActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmarEliminar(esp: Especialidad) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar especialidad")
            .setMessage("¿Eliminar '${esp.nombre}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.deleteEspecialidad(esp.id)
                        cargarEspecialidades()
                    } catch (e: Exception) {
                        Toast.makeText(this@ListEspecialidadActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
