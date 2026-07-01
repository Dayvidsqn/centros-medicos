package com.example.centrosmedicos.ui.centromedico

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.centrosmedicos.adapter.CentroMedicoAdapter
import com.example.centrosmedicos.databinding.ActivityListCentroMedicoBinding
import com.example.centrosmedicos.model.CentroMedico
import com.example.centrosmedicos.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ListCentroMedicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListCentroMedicoBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: CentroMedicoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListCentroMedicoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CentroMedicoAdapter(
            onEdit = { centro ->
                val intent = Intent(this, FormCentroMedicoActivity::class.java)
                intent.putExtra("centroId", centro.id)
                startActivity(intent)
            },
            onDelete = { centro -> confirmarEliminar(centro) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAgregar.setOnClickListener {
            startActivity(Intent(this, FormCentroMedicoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarCentros()
    }

    private fun cargarCentros() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val lista = repo.getCentrosMedicos()
                adapter.actualizar(lista)
                binding.tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@ListCentroMedicoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmarEliminar(centro: CentroMedico) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar centro médico")
            .setMessage("¿Eliminar '${centro.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.deleteCentroMedico(centro.id)
                        cargarCentros()
                    } catch (e: Exception) {
                        Toast.makeText(this@ListCentroMedicoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
