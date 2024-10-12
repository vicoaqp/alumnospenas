package com.colegio.iensdlp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
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
import java.util.Calendar
import java.util.Locale

class record : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private var isAdmin = false
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    // Referencia al TextView del mes seleccionado
    private lateinit var tvSelectedMonth: TextView
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


            // Obtener el TextView que muestra el mes seleccionado
            tvSelectedMonth = findViewById(R.id.tvSelectedMonth)

            // Obtener el DNI del alumno logueado desde SharedPreferences
            val dni = sharedPreferences.getString("dni", null)

            if (dni == null || dni.isEmpty()) {
                Toast.makeText(this, "Error al obtener el DNI", Toast.LENGTH_SHORT).show()
                Log.e("RecordActivity", "DNI no encontrado en SharedPreferences")


            } else {
                Toast.makeText(this, "El DNI es $dni", Toast.LENGTH_SHORT).show()
                // Actualizar el texto del mes seleccionado por defecto (mes y año actuales)
                updateSelectedMonthText()
                fetchAttendanceRecords(dni, selectedMonth, selectedYear)
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
            // Agregar listener al botón para seleccionar otro mes
            val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)
            btnSelectMonth.setOnClickListener {
                if (dni != null) {
                    showMonthPickerDialog(dni) // Mostrar el diálogo de selección de mes
                }
            }

        } catch (e: Exception) {
            Log.e("RecordActivity", "Error durante la inicialización de la actividad", e)
            Toast.makeText(this, "Ocurrió un error al inicializar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    private fun showMonthPickerDialog(dni: String) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                // Actualizar el mes y año seleccionado
                selectedMonth = month
                selectedYear = year
                // Actualizar el texto del mes seleccionado
                updateSelectedMonthText()

                fetchAttendanceRecords(dni, selectedMonth, selectedYear)
            },
            selectedYear,
            selectedMonth,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE // Ocultar selección de días
        dialog.show()
    }

    private fun updateSelectedMonthText() {
        // Convertir el mes y año seleccionados a un formato legible
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, selectedMonth)
        calendar.set(Calendar.YEAR, selectedYear)

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        // Actualizar el TextView con el mes seleccionado
        tvSelectedMonth.text = "Mostrando asistencia de: $formattedDate"
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


    /// Función para obtener los registros de asistencia del mes y año seleccionados
    private fun fetchAttendanceRecords(dni: String, month: Int, year: Int) {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startDate = calendar.time // Primer día del mes seleccionado

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endDate = calendar.time // Último día del mes seleccionado

            db.collection("attendances")
                .whereEqualTo("dni", dni)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
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