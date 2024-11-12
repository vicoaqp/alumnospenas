package com.colegio.iensdlp

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query

class biblioteca : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var libreriaAdapter: LibreriaAdapter
    private lateinit var db: FirebaseFirestore
    private var dniAlumno: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_biblioteca)

        // Obtén el dniAlumno desde SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("AlumnoPreferences", Context.MODE_PRIVATE)
        dniAlumno = sharedPreferences.getString("dniAlumno", null)

        if (dniAlumno == null) {
            Toast.makeText(this, "Error: No se encontró el DNI del alumno", Toast.LENGTH_SHORT).show()
            finish() // Salir de la actividad si no hay un DNI
            return
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLibreria)
        recyclerView.layoutManager = LinearLayoutManager(this)
        libreriaAdapter = LibreriaAdapter()
        recyclerView.adapter = libreriaAdapter

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Cargar datos desde Firebase
        fetchLibreriaData()

    }

    private fun fetchLibreriaData() {
        // Filtrar documentos donde el campo "dni" coincida con el dniAlumno
        db.collection("biblioteca")
            .whereEqualTo("dni", dniAlumno)
            .get()
            .addOnSuccessListener { documents ->
                val itemList = mutableListOf<LibreriaItem>()
                for (document in documents) {
                    val cantidad = document.getLong("cantidad")?.toInt() ?: 0
                    val dni = document.getString("dni") ?: ""
                    val estado = document.getString("estado") ?: ""
                    val estadodos = document.getString("estadodos") ?: ""
                    val fecha = document.getString("fecha") ?: ""
                    val grado = document.getString("grado") ?: ""
                    val nameestud = document.getString("nameestud") ?: ""
                    val nametext = document.getString("nametext") ?: ""
                    val seccion = document.getString("seccion") ?: ""
                    val tipoTexto = document.getString("tipoTexto") ?: ""

                    val item = LibreriaItem(cantidad, dni, estado, estadodos, fecha, grado, nameestud, nametext, seccion, tipoTexto)
                    itemList.add(item)
                }
                // Pasar la lista al adaptador para mostrar en el RecyclerView
                libreriaAdapter.submitList(itemList)
            }
            .addOnFailureListener { exception ->
                Log.e("LibreriaActivity", "Error al obtener datos de biblioteca", exception)
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

}