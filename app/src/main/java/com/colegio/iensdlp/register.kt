package com.colegio.iensdlp

import android.content.Intent
import android.content.SharedPreferences
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

        // Establecer valores por defecto
        autoCompleteTextViewGrado.setText("1", false) // Valor predeterminado de grado
        autoCompleteTextViewSeccion.setText("a", false) // Valor predeterminado de sección

        // Asegúrate de que el dropdown se muestre al hacer clic
        autoCompleteTextViewGrado.setOnClickListener {
            autoCompleteTextViewGrado.showDropDown()
        }

        autoCompleteTextViewSeccion.setOnClickListener {
            autoCompleteTextViewSeccion.showDropDown()
        }

        // Recuperar el DNI del padre desde SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        dniPapa = sharedPreferences.getString("dni", null)

        val apellidos = findViewById<EditText>(R.id.editTextLastName)
        val dni = findViewById<EditText>(R.id.editTextDni)
        val nombres = findViewById<EditText>(R.id.editTextName)
        val btnRegistrar = findViewById<Button>(R.id.buttonRegister)

        btnRegistrar.setOnClickListener {
            var apellidosText = apellidos.text.toString().trim()
            var dniText = dni.text.toString().trim()
            var nombresText = nombres.text.toString().trim()
            val gradoText = autoCompleteTextViewGrado.text.toString().trim() // Obtener el valor seleccionado del Spinner
            var seccionText = autoCompleteTextViewSeccion.text.toString().trim() // Obtener el valor seleccionado del Spinner

            // Convertir los nombres, apellidos y sección a minúsculas
            apellidosText = apellidosText.lowercase()
            nombresText = nombresText.lowercase()
            seccionText = seccionText.lowercase()

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

    private fun checkDniExists(dni: String, callback: (Boolean) -> Unit) {
        db.collection("students")
            .whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.size() > 0)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar el DNI", Toast.LENGTH_SHORT).show()
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
            "dniPapa" to dniPapa
        )

        db.collection("students")
            .add(student)
            .addOnSuccessListener {
                Toast.makeText(this, "Alumno registrado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar al alumno", Toast.LENGTH_SHORT).show()
            }
    }
}