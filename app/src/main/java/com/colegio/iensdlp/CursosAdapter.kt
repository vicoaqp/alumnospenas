package com.colegio.iensdlp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CursosAdapter(private var cursosList: List<Curso>) : RecyclerView.Adapter<CursosAdapter.CursosViewHolder>() {

    class CursosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombreCurso: TextView = itemView.findViewById(R.id.tvNombreCurso)
        val tvProfesor: TextView = itemView.findViewById(R.id.tvProfesor)
        val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CursosViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_curso, parent, false)
        return CursosViewHolder(view)
    }

    override fun onBindViewHolder(holder: CursosViewHolder, position: Int) {
        val curso = cursosList[position]
        holder.tvNombreCurso.text = curso.nombre
        holder.tvProfesor.text = curso.profesor
        holder.tvHorario.text = "${curso.horaInicio} - ${curso.horaFin}"
    }

    override fun getItemCount(): Int = cursosList.size

    fun updateCursos(newCursosList: List<Curso>) {
        cursosList = newCursosList
        notifyDataSetChanged()
    }
}