package com.colegio.iensdlp

import android.app.AlertDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class Horarios : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var cursosAdapter: CursosAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var tvSelectDia: TextView
    private lateinit var tvTitleDia: TextView
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // Día actual
    private var dniPadre: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_horarios)


        recyclerView = findViewById(R.id.rvCursos)
        tvSelectDia = findViewById(R.id.tvSelectDia)
        tvTitleDia = findViewById(R.id.tvTitleDia)

        recyclerView.layoutManager = LinearLayoutManager(this)
        cursosAdapter = CursosAdapter(emptyList())
        recyclerView.adapter = cursosAdapter

        // Obtener el DNI del padre desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        dniPadre = sharedPreferences.getString("dni", null)

        // Mostrar el día actual por defecto
        updateDayText(selectedDay)

        // Configurar el click en el texto para seleccionar el día
        tvSelectDia.setOnClickListener {
            showDayPickerDialog()
        }

        // Cargar los cursos del día seleccionado
        fetchCursosByDay(selectedDay)

    }

    // Mostrar diálogo para elegir el día de la semana
    private fun showDayPickerDialog() {
        val days = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar día")
            .setItems(days) { _, which ->
                // El día es el índice más 2, porque Calendar.MONDAY = 2
                selectedDay = which + 2
                updateDayText(selectedDay)
                fetchCursosByDay(selectedDay)
            }
            .show()
    }

    // Actualizar el texto que muestra el día
    private fun updateDayText(dayOfWeek: Int) {
        val dayName = when (dayOfWeek) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            else -> "Día no válido"
        }
        tvTitleDia.text = "Cursos para el día $dayName"
    }

    // Función para obtener los cursos del día seleccionado
    private fun fetchCursosByDay(dayOfWeek: Int) {
        if (dniPadre == null) return

        // Consultar los alumnos relacionados con el padre
        db = FirebaseFirestore.getInstance()
        db.collection("students")
            .whereEqualTo("dnipapa", dniPadre)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("CursosAdapter", "No se encontraron estudiantes para este padre")
                    Toast.makeText(this, "No se encontraron estudiantes", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                for (document in documents) {
                    val grado = document.getString("grado") ?: ""
                    val seccion = document.getString("seccion") ?: ""

                    Toast.makeText(this, "grado"+grado, Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "seccion"+seccion, Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "dia"+ dayOfWeek, Toast.LENGTH_SHORT).show()
                    // Ahora que tenemos grado y sección, consultar los cursos del día
                    db.collection("cursos")
                        .whereEqualTo("grado", grado)
                        .whereEqualTo("seccion", seccion)
                        .whereEqualTo("dia", dayOfWeek)  // Filtrar por día de la semana
                        .get()
                        .addOnSuccessListener { cursosSnapshot ->
                            val cursosList = mutableListOf<Curso>()
                            for (cursoDocument in cursosSnapshot) {
                                val nombreCurso = cursoDocument.getString("nombre") ?: ""
                                val profesor = cursoDocument.getString("profesor") ?: ""
                                val horaInicio = cursoDocument.getString("horaInicio") ?: ""
                                val horaFin = cursoDocument.getString("horaFin") ?: ""

                                Toast.makeText(this, "seccion"+ nombreCurso, Toast.LENGTH_SHORT).show()

                                val curso = Curso(nombreCurso, profesor, horaInicio, horaFin)
                                cursosList.add(curso)
                            }

                            //recyclerView.layoutManager = LinearLayoutManager(this)
                            //cursosAdapter = CursosAdapter(emptyList())
                            //recyclerView.adapter = cursosAdapter

                            cursosAdapter.updateCursos(cursosList)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener cursos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


}