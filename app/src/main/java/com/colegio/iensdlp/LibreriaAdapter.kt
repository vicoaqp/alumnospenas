package com.colegio.iensdlp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter

data class LibreriaItem(
    val cantidad: Int,
    val dni: String,
    val estado: String,
    val estadodos: String,
    val fecha: String,
    val grado: String,
    val nameestud: String,
    val nametext: String,
    val seccion: String,
    val tipoTexto: String
)

class LibreriaAdapter : ListAdapter<LibreriaItem, LibreriaAdapter.LibreriaViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibreriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_libreria, parent, false)
        return LibreriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibreriaViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class LibreriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val estadoTextView: TextView = itemView.findViewById(R.id.tvEstado)
        private val estadodosTextView: TextView = itemView.findViewById(R.id.tvEstadodos)
        private val fechaTextView: TextView = itemView.findViewById(R.id.tvFecha)
        private val nametextTextView: TextView = itemView.findViewById(R.id.tvNameText)
        private val tipoTextoTextView: TextView = itemView.findViewById(R.id.tvTipoTexto)

        fun bind(item: LibreriaItem) {
            estadoTextView.text = item.estado
            estadodosTextView.text = item.estadodos
            fechaTextView.text = item.fecha
            nametextTextView.text = item.nametext
            tipoTextoTextView.text = item.tipoTexto
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LibreriaItem>() {
        override fun areItemsTheSame(oldItem: LibreriaItem, newItem: LibreriaItem): Boolean = oldItem.dni == newItem.dni
        override fun areContentsTheSame(oldItem: LibreriaItem, newItem: LibreriaItem): Boolean = oldItem == newItem
    }
}