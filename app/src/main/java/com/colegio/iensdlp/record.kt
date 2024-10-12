package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class record : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        try {
            // Recuperar el tipo de usuario desde SharedPreferences
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            isAdmin = sharedPreferences.getString("userType", "alumno") == "administrador"

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Inicializar RecyclerView
            recyclerView = findViewById(R.id.rvAttendances)
            recyclerView.layoutManager = LinearLayoutManager(this)

            // Configurar el adaptador
            attendanceAdapter = AttendanceAdapter()
            recyclerView.adapter = attendanceAdapter

            // Obtener el DNI del alumno logueado desde SharedPreferences
            val dni = sharedPreferences.getString("dni", null)
            if (dni == null || dni.isEmpty()) {
                Toast.makeText(this, "Error al obtener el DNI", Toast.LENGTH_SHORT).show()
                Log.e("RecordActivity", "DNI no encontrado en SharedPreferences")
            } else {
                Toast.makeText(this, "El DNI es $dni", Toast.LENGTH_SHORT).show()
                fetchAttendanceRecords(dni)
            }

            // Configurar BottomNavigationView
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
                handleBottomNavigation(menuItem)
                true
            }

            if (!isAdmin) {
                bottomNavigationView.menu.findItem(R.id.action_scan)?.isVisible = false
            }

        } catch (e: Exception) {
            Log.e("RecordActivity", "Error durante la inicialización de la actividad", e)
            Toast.makeText(this, "Ocurrió un error al inicializar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    // Función para manejar las selecciones del menú inferior
    private fun handleBottomNavigation(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_events -> {
                startActivity(Intent(this, EventsActivity::class.java))
            }
            R.id.action_logout -> {
                logoutUser()
            }
        }
    }


    // Función para obtener los registros de asistencia del alumno
    private fun fetchAttendanceRecords(dni: String) {
        try {
            db.collection("attendances")
                .whereEqualTo("dni", dni)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val newAttendanceList = mutableListOf<Attendance>()
                    for (document in documents) {
                        val nombres = document.getString("nombres") ?: ""
                        val grado = document.getString("grado") ?: ""
                        val seccion = document.getString("seccion") ?: ""
                        val timestamp = document.getTimestamp("timestamp")
                        val dni = document.getString("dni") ?: ""
                        val tipo = document.getString("tipo") ?: ""

                        val attendance = Attendance(nombres, grado, timestamp ?: Timestamp.now(), seccion, dni, tipo)
                        newAttendanceList.add(attendance)
                    }
                    attendanceAdapter.submitList(newAttendanceList)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error al obtener los registros: ${exception.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreError", "Error al obtener los registros de asistencia", exception)
                }
        } catch (e: Exception) {
            Log.e("RecordActivity", "Error al buscar registros de asistencia", e)
            Toast.makeText(this, "Ocurrió un error al buscar registros de asistencia: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun logoutUser() {
        // Borrar el estado de inicio de sesión en SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Redirigir al Login Activity
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

}