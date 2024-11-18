package com.colegio.iensdlp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
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

        // Inicializar RecyclerView y Adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        eventoAdapter = EventoAdapter(eventos)
        recyclerView.adapter = eventoAdapter

        // Obtener dniAlumno y filtrar eventos
        fetchEventos()
    }

    private fun fetchEventos() {
        // Obtener el dniAlumno desde SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("AlumnoPreferences", Context.MODE_PRIVATE)
        val dniAlumno = sharedPreferences.getString("dniAlumno", null)

        if (dniAlumno.isNullOrEmpty()) {
            // Manejar el caso donde no hay dniAlumno
            Toast.makeText(this, "No se encontró el DNI del alumno", Toast.LENGTH_SHORT).show()
            return
        }

        // Conectar a Firestore y filtrar los eventos por dniAlumno
        val db = FirebaseFirestore.getInstance()
        db.collection("eventos")
            .whereEqualTo("dni", dniAlumno) // Aplicar el filtro
            .get()
            .addOnSuccessListener { querySnapshot ->
                eventos.clear() // Limpiar la lista actual

                for (document in querySnapshot) {
                    val evento = document.toObject(Evento::class.java)
                    // Manejar el caso de un timestamp nulo
                    if (evento.timestamp == null) {
                        evento.timestamp = Timestamp.now()
                    }
                    eventos.add(evento) // Agregar evento a la lista
                }
                eventoAdapter.notifyDataSetChanged() // Notificar cambios al adapter
            }
            .addOnFailureListener { exception ->
                // Manejo de errores
                Toast.makeText(this, "Error al cargar eventos: ${exception.message}", Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }
    }
}
