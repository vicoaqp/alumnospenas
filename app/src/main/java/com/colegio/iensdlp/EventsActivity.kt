package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.integration.android.IntentIntegrator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var eventsAdapter: EventsAdapter
    private val eventsList = mutableListOf<Event>()
    private var isAdmin = false
    private var dniPadre: String? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // Inicializar el DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Inicializar el DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Configurar el botón de la barra de herramientas (tres puntos) para abrir el Navigation Drawer
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        // Configurar la opción de "Salir" en el NavigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoColeActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    logoutUser()
                    true
                }
                R.id.nav_horario -> {
                    startActivity(Intent(this, Horarios::class.java))
                    true
                }
                R.id.nav_docentes -> {
                    startActivity(Intent(this, DocenteActivity::class.java)) // Agrega la navegación a DocenteActivity
                    true
                }
                else -> false
            }
        }
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Recuperar el tipo de usuario desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val userType = sharedPreferences.getString("userType", "padre")
        isAdmin = userType == "administrador"
        dniPadre = sharedPreferences.getString("dni", null)

        // Inicializar Firestore y RecyclerView
        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        eventsAdapter = EventsAdapter(eventsList)
        recyclerView.adapter = eventsAdapter

        // Cargar los eventos desde Firestore
        fetchEventsFromFirebase()

        // Configurar el BottomNavigationView basado en el rol del usuario
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        setupBottomNavigationView(bottomNavigationView)



    }

    private fun setupBottomNavigationView(bottomNavigationView: BottomNavigationView) {
        // Configurar el menú del BottomNavigationView según el rol del usuario
        bottomNavigationView.menu.clear() // Limpiar el menú existente

        if (isAdmin) {
            // Si es administrador, mostrar las opciones "Escanear" y "Salir"
            bottomNavigationView.inflateMenu(R.menu.menu_admin)
        } else {
            // Si es padre, mostrar las opciones "Asistencias" y "Salir"
            bottomNavigationView.inflateMenu(R.menu.menu_student)
        }

        // Configurar el listener para manejar las selecciones del menú
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            handleBottomNavigation(menuItem)
            true
        }
    }

    private fun fetchEventsFromFirebase() {
        db.collection("events")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""

                    val event = Event(title, description, imageUrl)
                    eventsList.add(event)
                }
                eventsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar eventos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun handleBottomNavigation(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_assistances -> {
                // Navegar a la pantalla de asistencias (Record) si es padre
                if (!isAdmin) {
                    startActivity(Intent(this, record::class.java))
                }
            }
            R.id.action_scan -> {
                // Iniciar el escaneo de QR para ingresar la asistencia si es administrador
                if (isAdmin) {
                    scanQRCode()
                }
            }

        }
        return true
    }

    private fun scanQRCode() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanear código QR")
        integrator.setCameraId(0) // Cámara trasera
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val dni = result.contents  // DNI extraído del QR
                registerAttendance(dni)
                //Toast.makeText(this, dni, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    private fun registerAttendance(dniAlumno: String) {

        val currentTimestamp = Date()

        // Establecer la fecha de inicio y fin del día actual para buscar las asistencias del estudiante en ese día
        val calendar = Calendar.getInstance()
        calendar.time = currentTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.time

        // Consultar las asistencias de ese día
        db.collection("attendances")
            .whereEqualTo("dni", dniAlumno) // Primer filtro por DNI
            .get() // Obtener todas las asistencias del estudiante
            .addOnSuccessListener { documents ->
                var hasEntrada = false
                var hasSalida = false

                // Filtrar las asistencias del día actual
                for (document in documents) {
                    val timestamp = document.getTimestamp("timestamp")?.toDate()

                    // Verificar si la asistencia está dentro del rango del día actual
                    if (timestamp != null && timestamp.after(startOfDay) && timestamp.before(endOfDay)) {
                        val tipo = document.getString("tipo")
                        when (tipo) {
                            "Entrada" -> hasEntrada = true
                            "Salida" -> hasSalida = true
                        }
                    }
                }

                // Si ya tiene Entrada y Salida, mostrar mensaje de error y no registrar más asistencias
                if (hasEntrada && hasSalida) {
                    Toast.makeText(this, "Ya tiene registradas la Entrada y Salida para el día de hoy", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Determinar el tipo de asistencia (Entrada o Salida)
                val tipoAsistencia = if (!hasEntrada) "Entrada" else "Salida"

                // Buscar al estudiante por su DNI para obtener detalles y el DNI del padre
                db.collection("students").whereEqualTo("dni", dniAlumno)
                    .get()
                    .addOnSuccessListener { studentDocuments ->
                        if (studentDocuments.isEmpty) {
                            Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (document in studentDocuments) {
                            val student = document.toObject(Student::class.java)
                            val dnipadre = student.dnipapa  // Obtener el DNI del padre

                            // Crear un registro de asistencia
                            val attendance = hashMapOf(
                                "nombres" to student.nombres,
                                "dni" to student.dni,
                                "dnipadre" to dnipadre,
                                "grado" to student.grado,
                                "seccion" to student.seccion,
                                "timestamp" to currentTimestamp,
                                "tipo" to tipoAsistencia
                            )

                            // Registrar la asistencia en la colección "attendances"
                            db.collection("attendances")
                                .add(attendance)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Asistencia registrada como $tipoAsistencia correctamente", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al registrar la asistencia", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error al buscar estudiante", e)
                        Toast.makeText(this, "Error al buscar estudiante: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error al verificar el último registro de asistencia", e)
                Toast.makeText(this, "Error al verificar el último registro de asistencia: ${e.message}", Toast.LENGTH_SHORT).show()
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
    // Manejar el comportamiento del botón "atrás" cuando el Navigation Drawer está abierto
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

