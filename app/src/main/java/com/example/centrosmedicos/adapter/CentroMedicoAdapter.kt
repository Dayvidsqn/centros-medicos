package com.example.centrosmedicos.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.centrosmedicos.databinding.ItemCentroMedicoBinding
import com.example.centrosmedicos.model.CentroMedico

class CentroMedicoAdapter(
    private var lista: MutableList<CentroMedico> = mutableListOf(),
    private val onEdit: (CentroMedico) -> Unit,
    private val onDelete: (CentroMedico) -> Unit
) : RecyclerView.Adapter<CentroMedicoAdapter.VH>() {

    inner class VH(val binding: ItemCentroMedicoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCentroMedicoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val centro = lista[position]
        holder.binding.tvNombre.text = centro.nombre
        holder.binding.tvDireccion.text = centro.direccion
        holder.binding.tvTipo.text = centro.tipo
        holder.binding.tvTelefono.text = centro.telefono

        // Mostrar imagen desde Base64
        if (centro.imagenBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(centro.imagenBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.binding.ivFachada.setImageBitmap(bmp)
            } catch (e: Exception) {
                holder.binding.ivFachada.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.binding.ivFachada.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.binding.btnEditar.setOnClickListener { onEdit(centro) }
        holder.binding.btnEliminar.setOnClickListener { onDelete(centro) }
    }

    override fun getItemCount() = lista.size

    fun actualizar(nuevaLista: List<CentroMedico>) {
        lista = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }
}
