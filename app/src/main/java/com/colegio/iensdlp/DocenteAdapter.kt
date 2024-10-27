package com.colegio.iensdlp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class DocenteAdapter(private var docentesList: List<Docente>, private val context: Context) :
    RecyclerView.Adapter<DocenteAdapter.DocenteViewHolder>() {

    class DocenteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombres: TextView = itemView.findViewById(R.id.tvNombres)
        val tvApellidos: TextView = itemView.findViewById(R.id.tvApellidos)
        val tvEspecialidad: TextView = itemView.findViewById(R.id.tvEspecialidad)
        val tvCelular: TextView = itemView.findViewById(R.id.tvCelular)
        val tvGrados: TextView = itemView.findViewById(R.id.tvGrados)
        val tvSecciones: TextView = itemView.findViewById(R.id.tvSecciones)
        val ivCall: ImageView = itemView.findViewById(R.id.ivCall)
        val ivWhatsApp: ImageView = itemView.findViewById(R.id.ivWhatsApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocenteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_docente, parent, false)
        return DocenteViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocenteViewHolder, position: Int) {
        val docente = docentesList[position]
        holder.tvNombres.text = "Nombres: ${docente.nombres}"
        holder.tvApellidos.text = "Apellidos: ${docente.apellidos}"
        holder.tvEspecialidad.text = "Especialidad: ${docente.especialidad}"
        holder.tvCelular.text = "Celular: ${docente.celular}"
        holder.tvGrados.text = "Grados: ${docente.grados.joinToString(", ")}"
        holder.tvSecciones.text = "Secciones: ${docente.secciones.joinToString(", ")}"

        // Acción de llamada
        holder.ivCall.setOnClickListener {
            val phoneUri = Uri.parse("tel:${docente.celular}")
            val callIntent = Intent(Intent.ACTION_DIAL, phoneUri)
            context.startActivity(callIntent)
        }

        // Acción de WhatsApp
        holder.ivWhatsApp.setOnClickListener {
            val phoneNumber = docente.celular.replace(" ", "").replace("+", "")
            val whatsappUri = Uri.parse("https://wa.me/$phoneNumber")
            val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri)
            whatsappIntent.setPackage("com.whatsapp")

            // Verificar si la app de WhatsApp está instalada
            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = docentesList.size

    fun updateDocentes(newDocentesList: List<Docente>) {
        docentesList = newDocentesList
        notifyDataSetChanged()
    }
}