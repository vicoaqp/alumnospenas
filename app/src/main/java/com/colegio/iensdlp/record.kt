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

            // Inicializar el DrawerLayout y NavigationView
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.navigation_view)

            // Configurar el Toolbar como ActionBar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)

            // Configurar el ActionBarDrawerToggle
            val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            // Configurar el listener para la opción de "Salir" en el NavigationView
            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_logout -> {
                        logoutUser()
                        true
                    }
                    R.id.nav_horario -> {
                        startActivity(Intent(this, Horarios::class.java))
                        true
                    }
                    else -> false
                }
            }

            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val dniPadre = sharedPreferences.getString("dni", null)

            if (dniPadre == null || dniPadre.isEmpty()) {
                Toast.makeText(this, "Error al obtener el DNI", Toast.LENGTH_SHORT).show()
                Log.e("RecordActivity", "DNI no encontrado en SharedPreferences")
                return
            }
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

            // Actualizar el texto del mes seleccionado por defecto (mes y año actuales)
            updateSelectedMonthText()

            // Obtener los alumnos relacionados y sus asistencias para el mes actual
            fetchAlumnosRelacionados(dniPadre!!, selectedMonth, selectedYear)

            // Configurar BottomNavigationView
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
                handleBottomNavigation(menuItem)
                true
            }

            // Listener para el botón de selección de mes
            val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)
            btnSelectMonth.setOnClickListener {
                showMonthPickerDialog(dniPadre!!)  // Mostrar diálogo para seleccionar el mes
            }

        } catch (e: Exception) {
            Log.e("RecordActivity", "Error durante la inicialización de la actividad", e)
            Toast.makeText(this, "Ocurrió un error al inicializar la pantalla: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    private fun showMonthPickerDialog(dniPadre: String) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                // Actualizar el mes y año seleccionados
                selectedMonth = month
                selectedYear = year
                // Actualizar el texto del mes seleccionado
                updateSelectedMonthText()
                // Volver a cargar los registros de asistencia del mes seleccionado
                fetchAlumnosRelacionados(dniPadre, selectedMonth, selectedYear)
            },
            selectedYear, selectedMonth, calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE // Ocultar la selección de día
        dialog.show()
    }

    private fun updateSelectedMonthText() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, selectedMonth)
        calendar.set(Calendar.YEAR, selectedYear)

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        tvSelectedMonth.text = "Mostrando asistencia de: $formattedDate"
    }


    // Función para manejar las selecciones del menú inferior
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
    // Método para obtener los alumnos relacionados con el DNI del padre
    private fun fetchAlumnosRelacionados(dniPadre: String, month: Int, year: Int) {
        db.collection("students")
            .whereEqualTo("dnipapa", dniPadre)  // Buscar alumnos relacionados con el padre
            .get()
            .addOnSuccessListener { studentDocuments ->
                val dniAlumnos = mutableListOf<String>()

                for (document in studentDocuments) {
                    val dniAlumno = document.getString("dni") ?: ""
                    dniAlumnos.add(dniAlumno)
                }

                if (dniAlumnos.isEmpty()) {
                    Toast.makeText(this, "No se encontraron alumnos relacionados", Toast.LENGTH_SHORT).show()
                } else {
                    // Ahora que tenemos los DNIs de los alumnos, buscar sus registros de asistencia
                    fetchAttendanceRecords(dniAlumnos, month, year)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al buscar alumnos relacionados: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /// Función para obtener los registros de asistencia del mes y año seleccionados
    private fun fetchAttendanceRecords(dniAlumnos: List<String>, month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time  // Primer día del mes seleccionado

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = calendar.time  // Último día del mes seleccionado

        db.collection("attendances")
            .whereIn("dni", dniAlumnos)  // Filtrar por los DNIs de los alumnos relacionados
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
                attendanceAdapter.submitList(newAttendanceList)  // Actualizar la lista del adaptador
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener los registros: ${exception.message}", Toast.LENGTH_SHORT).show()
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