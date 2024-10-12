package com.colegio.iensdlp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceAdapter : ListAdapter<Attendance, AttendanceAdapter.AttendanceViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Attendance>() {
            override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
                // Compara los elementos por su DNI (o cualquier ID único)
                return oldItem.dni == newItem.dni
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean {
                // Compara todos los campos para ver si el contenido ha cambiado
                return oldItem == newItem
            }
        }
    }

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvHora: TextView = itemView.findViewById(R.id.tvHora)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = getItem(position)

// Asegurarse de que el timestamp se convierta a un Date antes de formatearlo
        val formattedDate = attendance.timestamp?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        } ?: "Fecha no disponible"

        val formattedTime = attendance.timestamp?.toDate()?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "Hora no disponible"

        holder.tvFecha.text = "Fecha: $formattedDate"
        holder.tvHora.text = "Hora: $formattedTime"
        holder.tvTipo.text = "Tipo: ${attendance.tipo}"
    }
}
