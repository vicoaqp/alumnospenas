package com.colegio.iensdlp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class eventosalon : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventoAdapter: EventoAdapter
    private val eventos: MutableList<Evento> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventosalon)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(eventos)
        recyclerView.adapter = eventoAdapter

        fetchEventos()

    }

    private fun fetchEventos() {
        val db = FirebaseFirestore.getInstance()
        db.collection("eventos")
            .get()
            .addOnSuccessListener { querySnapshot ->
                eventos.clear()

                for (document in querySnapshot) {
                    val evento = document.toObject(Evento::class.java)
                    // Si el timestamp es null, puedes asignar un valor por defecto (opcional)
                    if (evento.timestamp == null) {
                        evento.timestamp = Timestamp.now() // Asignando un valor por defecto
                    }
                    eventos.add(evento)
                }
                eventoAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace() // Manejo de errores
            }
    }

}