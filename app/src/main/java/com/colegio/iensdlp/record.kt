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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
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
    private var dniPadre: String? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Referencia al TextView del mes seleccionado
    private lateinit var tvSelectedMonth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        try {
            // Configuración de UI y SharedPreferences
            initializeUI()

            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val dniPadre = sharedPreferences.getString("dni", null)

            if (dniPadre == null || dniPadre.isEmpty()) {
                Toast.makeText(this, "Error al obtener el DNI", Toast.LENGTH_SHORT).show()
                Log.e("RecordActivity", "DNI no encontrado en SharedPreferences")
                return
            }

            // Inicializar Firestore
            db = FirebaseFirestore.getInstance()

            // Obtener los alumnos relacionados y sus asistencias para el mes actual
            fetchAlumnosRelacionados(dniPadre, selectedMonth, selectedYear)

        } catch (e: Exception) {
            Log.e("RecordActivity", "Error durante la inicialización de la actividad", e)
            Toast.makeText(this, "Ocurrió un error al inicializar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeUI() {
        // Inicializar DrawerLayout, NavigationView, Toolbar y RecyclerView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        recyclerView = findViewById(R.id.rvAttendances)
        recyclerView.layoutManager = LinearLayoutManager(this)
        attendanceAdapter = AttendanceAdapter()
        recyclerView.adapter = attendanceAdapter

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        updateSelectedMonthText()

        val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)
        btnSelectMonth.setOnClickListener {
            showMonthPickerDialog()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            handleBottomNavigation(menuItem)
            true
        }
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_info -> {
                startActivity(Intent(this, InfoColeActivity::class.java))
                return true
            }
            R.id.nav_horario -> {
                startActivity(Intent(this, Horarios::class.java))
                return true
            }
            R.id.nav_docentes -> {
                startActivity(Intent(this, DocenteActivity::class.java))
                return true
            }
            R.id.nav_biblioteca -> {
                startActivity(Intent(this, biblioteca::class.java)) // Navegar a la actividad Libreria
                return true
            }
            R.id.nav_logout -> {
                logoutUser()
                return true
            }
            else -> return false
        }
    }

    private fun fetchAlumnosRelacionados(dniPadre: String, month: Int, year: Int) {
        db.collection("students")
            .whereEqualTo("dnipapa", dniPadre)
            .get()
            .addOnSuccessListener { studentDocuments ->
                val dniAlumnos = mutableListOf<String>()

                for (document in studentDocuments) {
                    val dniAlumno = document.getString("dni") ?: ""
                    dniAlumnos.add(dniAlumno)

                    // Guarda cada DNI de alumno en SharedPreferences
                    saveDniAlumnoToPreferences(dniAlumno)
                }

                if (dniAlumnos.isEmpty()) {
                    Toast.makeText(this, "No se encontraron alumnos relacionados", Toast.LENGTH_SHORT).show()
                } else {
                    fetchAttendanceRecords(dniAlumnos, month, year)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al buscar alumnos relacionados: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveDniAlumnoToPreferences(dniAlumno: String) {
        val sharedPreferences = getSharedPreferences("AlumnoPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dniAlumno", dniAlumno)
        editor.apply()
    }

    private fun fetchAttendanceRecords(dniAlumnos: List<String>, month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = calendar.time

        db.collection("attendances")
            .whereIn("dni", dniAlumnos)
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
                    val timestamp = document.getTimestamp("timestamp") ?: Timestamp.now()
                    val dni = document.getString("dni") ?: ""
                    val tipo = document.getString("tipo") ?: ""

                    val attendance = Attendance(nombres, grado, timestamp, seccion, dni, tipo)
                    newAttendanceList.add(attendance)
                }
                attendanceAdapter.submitList(newAttendanceList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener los registros: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSelectedMonthText() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, selectedMonth)
        calendar.set(Calendar.YEAR, selectedYear)

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        tvSelectedMonth.text = "Mostrando asistencia de: $formattedDate"
    }

    private fun showMonthPickerDialog() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedMonth = month
                selectedYear = year
                updateSelectedMonthText()
                fetchAlumnosRelacionados(dniPadre!!, selectedMonth, selectedYear)
            },
            selectedYear, selectedMonth, calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE
        dialog.show()
    }

    private fun handleBottomNavigation(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_events -> {
                startActivity(Intent(this, EventsActivity::class.java))
            }
            R.id.action_regist -> {
                startActivity(Intent(this, register::class.java))
            }
        }
    }

    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}