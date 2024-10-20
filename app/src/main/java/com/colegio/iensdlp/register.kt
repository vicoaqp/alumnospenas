package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import java.text.SimpleDateFormat
import java.util.Locale

class register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var dniPapa: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val autoCompleteTextViewGrado: AutoCompleteTextView = findViewById(R.id.autoCompleteTextViewGrado)
        val autoCompleteTextViewSeccion: AutoCompleteTextView = findViewById(R.id.autoCompleteTextViewSeccion)

        // Lista de valores para Grado (1, 2, 3, 4, 5)
        val grados = arrayOf("1", "2", "3", "4", "5")
        val gradoAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, grados)
        autoCompleteTextViewGrado.setAdapter(gradoAdapter)

// Lista de valores para Sección (a, b, c, d)
        val secciones = arrayOf("a", "b", "c", "d")
        val seccionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, secciones)
        autoCompleteTextViewSeccion.setAdapter(seccionAdapter)

        // Asegúrate de que el dropdown se muestre al hacer clic
        autoCompleteTextViewGrado.setOnClickListener {
            autoCompleteTextViewGrado.showDropDown() // Mostrar el dropdown de grado al hacer clic
        }

        autoCompleteTextViewSeccion.setOnClickListener {
            autoCompleteTextViewSeccion.showDropDown() // Mostrar el dropdown de sección al hacer clic
        }


        // Recuperar el DNI del padre desde SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        dniPapa = sharedPreferences.getString("dni", null)

        val apellidos = findViewById<EditText>(R.id.editTextLastName)
        val dni = findViewById<EditText>(R.id.editTextDni)
        val nombres = findViewById<EditText>(R.id.editTextName)
        val btnRegistrar = findViewById<Button>(R.id.buttonRegister)

        btnRegistrar.setOnClickListener {
            val apellidosText = apellidos.text.toString().trim()
            val dniText = dni.text.toString().trim()
            val nombresText = nombres.text.toString().trim()
            val gradoText = autoCompleteTextViewGrado.text.toString().trim() // Obtener el valor seleccionado del Spinner
            val seccionText = autoCompleteTextViewSeccion.text.toString().trim() // Obtener el valor seleccionado del Spinner

            if (apellidosText.isNotEmpty() && dniText.isNotEmpty() &&
                gradoText.isNotEmpty() && nombresText.isNotEmpty() && seccionText.isNotEmpty()) {

                // Verificar si el DNI ya está registrado
                checkDniExists(dniText) { exists ->
                    if (exists) {
                        // Mostrar mensaje de error si el DNI ya está registrado
                        Toast.makeText(this, "El DNI ya está registrado", Toast.LENGTH_SHORT).show()
                    } else {
                        // Proceder con el registro si el DNI no existe
                        registerStudent(apellidosText, nombresText, dniText, gradoText, seccionText)
                    }
                }
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }


    }


    // Verificar si el DNI ya existe en la colección "students"
    private fun checkDniExists(dni: String, callback: (Boolean) -> Unit) {
        db.collection("students").document(dni)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // El documento con el DNI ya existe
                    callback(true)
                } else {
                    // El documento no existe, el DNI está disponible
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                // Manejo de error al consultar Firestore
                Toast.makeText(this, "Error al verificar el DNI: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun registerStudent(apellidos: String, nombres: String, dni: String, grado: String, seccion: String) {
        val student = hashMapOf(
            "apellidos" to apellidos,
            "nombres" to nombres,
            "dni" to dni,
            "grado" to grado,
            "seccion" to seccion,
            "dnipapa" to dniPapa // Registrar el DNI del padre
        )

        // Agregar los datos a la colección "students"
        db.collection("students").document(dni).set(student)
            .addOnSuccessListener {
                Toast.makeText(this, "Alumno registrado con éxito", Toast.LENGTH_SHORT).show()
                finish() // Cerrar la actividad después de registrar al alumno
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar al alumno: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}