package com.colegio.iensdlp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InfoAdapter : RecyclerView.Adapter<InfoAdapter.InfoViewHolder>() {

    private var infoList = mutableListOf<InfoCole>()

    inner class InfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_info, parent, false)
        return InfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.tvDescripcion.text = infoList[position].descripcion
    }

    override fun getItemCount(): Int = infoList.size

    fun updateData(newData: List<InfoCole>) {
        infoList = newData.toMutableList()
        notifyDataSetChanged()
    }
}