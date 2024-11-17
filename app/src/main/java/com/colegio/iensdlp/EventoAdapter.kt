package com.colegio.iensdlp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class EventoAdapter(private val eventos: List<Evento>) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventos[position]

        // Verificar si timestamp es null antes de formatearlo
        val formattedDate = if (evento.timestamp != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.format(evento.timestamp!!.toDate()) // Convierte el Timestamp a Date
        } else {
            "Fecha no disponible"
        }

        holder.timestampTextView.text = formattedDate
        holder.descripcionTextView.text = evento.descripcion
        holder.tipoEventoTextView.text = evento.tipoevento
    }

    override fun getItemCount(): Int = eventos.size

    class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val descripcionTextView: TextView = itemView.findViewById(R.id.descripcionTextView)
        val tipoEventoTextView: TextView = itemView.findViewById(R.id.tipoEventoTextView)
    }
}