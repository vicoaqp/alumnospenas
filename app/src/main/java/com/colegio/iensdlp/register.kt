package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val apellidos = findViewById<EditText>(R.id.editTextLastName)
        val correo = findViewById<EditText>(R.id.editTextEmail)
        val dni = findViewById<EditText>(R.id.editTextDni)
        val grado = findViewById<EditText>(R.id.editTextGrado)
        val nombres = findViewById<EditText>(R.id.editTextName)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val seccion = findViewById<EditText>(R.id.editTextLastSeccion)
        val btnRegistrar = findViewById<Button>(R.id.buttonRegister)


        btnRegistrar.setOnClickListener {
            val apellidosText = apellidos.text.toString()
            val correoText = correo.text.toString()
            val dniText = dni.text.toString()
            val gradoText = grado.text.toString()
            val nombresText = nombres.text.toString()
            val passwordText = password.text.toString()
            val seccionText = seccion.text.toString()

            if (apellidosText.isNotEmpty() && correoText.isNotEmpty() && dniText.isNotEmpty() &&
                gradoText.isNotEmpty() && nombresText.isNotEmpty() && passwordText.isNotEmpty() && seccionText.isNotEmpty()) {

                // Registrar el alumno en Firestore con el tipo predeterminado "alumno"
                registerUser(apellidosText, correoText, dniText, gradoText, nombresText, passwordText, seccionText)
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun registerUser(apellidos: String, correo: String, dni: String, grado: String, nombres: String, password: String, seccion: String) {
        val student = hashMapOf(
            "apellidos" to apellidos,
            "correo" to correo,
            "dni" to dni,
            "grado" to grado,
            "nombres" to nombres,
            "password" to password,
            "seccion" to seccion,
            "tipo" to "alumno" // Tipo predeterminado al registrarse
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