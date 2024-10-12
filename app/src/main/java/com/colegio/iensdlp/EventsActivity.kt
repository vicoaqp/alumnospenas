package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.zxing.integration.android.IntentIntegrator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var eventsAdapter: EventsAdapter
    private val eventsList = mutableListOf<Event>()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // Recuperar el tipo de usuario desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        isAdmin = sharedPreferences.getString("userType", "alumno") == "administrador"

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
            // Si es alumno, mostrar las opciones "Asistencias" y "Salir"
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
                // Navegar a la pantalla de asistencias (Record) si el alumno selecciona "Asistencias"
                startActivity(Intent(this,record::class.java))
            }
            R.id.action_scan -> {
                // Iniciar el escaneo de QR para ingresar la asistencia si es administrador
                scanQRCode()
            }
            R.id.action_logout -> {
                // Cerrar sesión para cualquier usuario
                logoutUser()
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
    private fun registerAttendance(dni: String) {
        val db = FirebaseFirestore.getInstance()

        // Obtener la marca de tiempo actual
        val currentTimestamp = Date()

        // Buscar el último registro de asistencia del estudiante por su DNI y fecha actual
        db.collection("attendances")
            .whereEqualTo("dni", dni)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordenar por la marca de tiempo más reciente
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                var tipoAsistencia = "Entrada" // Valor predeterminado

                // Si existe un registro de asistencia en la fecha actual, alternar el tipo
                if (!documents.isEmpty) {
                    val lastAttendance = documents.first()
                    val lastTipo = lastAttendance.getString("tipo") ?: "Salida"

                    // Alternar entre "Entrada" y "Salida"
                    tipoAsistencia = if (lastTipo == "Entrada") "Salida" else "Entrada"
                }

                // Buscar al estudiante por su DNI para obtener más detalles
                db.collection("students").whereEqualTo("dni", dni)
                    .get()
                    .addOnSuccessListener { studentDocuments ->
                        if (studentDocuments.isEmpty) {
                            Toast.makeText(this, "Estudiante no encontrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (document in studentDocuments) {
                            val student = document.toObject(Student::class.java)

                            // Crear un registro de asistencia
                            val attendance = hashMapOf(
                                "nombres" to student.nombres,
                                "dni" to student.dni,
                                "grado" to student.grado,
                                "seccion" to student.seccion,
                                "timestamp" to currentTimestamp, // Almacenar como Timestamp
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
                Log.e("FirestoreError", "Error al buscar último registro de asistencia", e)
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
}