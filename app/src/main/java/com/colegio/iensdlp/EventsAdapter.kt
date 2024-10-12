package com.colegio.iensdlp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EventsAdapter(private val eventsList: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventsViewHolder>() {

    class EventsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage: ImageView = itemView.findViewById(R.id.eventImage)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventDescription: TextView = itemView.findViewById(R.id.eventDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventsViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventsViewHolder, position: Int) {
        val event = eventsList[position]
        holder.eventTitle.text = event.title
        holder.eventDescription.text = event.description
        Glide.with(holder.itemView.context).load(event.imageUrl).into(holder.eventImage)
    }

    override fun getItemCount(): Int = eventsList.size
}