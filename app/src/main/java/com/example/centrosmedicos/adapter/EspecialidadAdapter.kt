package com.example.centrosmedicos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.centrosmedicos.databinding.ItemEspecialidadBinding
import com.example.centrosmedicos.model.Especialidad

class EspecialidadAdapter(
    private var lista: MutableList<Especialidad> = mutableListOf(),
    private val onEdit: (Especialidad) -> Unit,
    private val onDelete: (Especialidad) -> Unit
) : RecyclerView.Adapter<EspecialidadAdapter.VH>() {

    inner class VH(val binding: ItemEspecialidadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEspecialidadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val esp = lista[position]
        holder.binding.tvIcono.text = esp.icono
        holder.binding.tvNombre.text = esp.nombre
        holder.binding.tvDescripcion.text = esp.descripcion
        holder.binding.btnEditar.setOnClickListener { onEdit(esp) }
        holder.binding.btnEliminar.setOnClickListener { onDelete(esp) }
    }

    override fun getItemCount() = lista.size

    fun actualizar(nuevaLista: List<Especialidad>) {
        lista = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }
}
