package com.colegio.iensdlp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class Login : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        // Comprobar si el usuario ya ha iniciado sesión
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)


        if (isLoggedIn) {
            val dni = sharedPreferences.getString("dni", "")
            val userType = sharedPreferences.getString("userType", "padre")

            // Redirigir según el tipo de usuario
            if (userType != null) {
                redirectToActivity(userType, dni)
            }
            return
        }


        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        val correo = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val btnRegister = findViewById<Button>(R.id.buttonRegister)

        btnLogin.setOnClickListener {
            val correoText = correo.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (correoText.isNotEmpty() && passwordText.isNotEmpty()) {
                loginUser(correoText, passwordText)
            } else {
                Toast.makeText(this, "Por favor ingrese el correo y la contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, ParentRegisterActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loginUser(correo: String, password: String) {
        // Verificar en la colección "padres"
        db.collection("padres")
            .whereEqualTo("correo", correo)
            .whereEqualTo("contraseña", password)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val dni = document.getString("dnipadre") ?: ""
                        val tipoUsuario = document.getString("tipo") ?: "padre" // Obtener el campo tipo (padre o administrador)

                        // Guardar el estado de inicio de sesión en SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("dni", dni)
                        editor.putString("userType", tipoUsuario)
                        editor.apply()

                        // Redirigir a la actividad correspondiente según el tipo de usuario
                        redirectToActivity(tipoUsuario, dni)
                    }
                } else {
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al iniciar sesión: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToActivity(userType: String, dni: String?) {
        // Redirigir a la actividad correspondiente según el tipo de usuario
        val intent = if (userType == "administrador") {
            Intent(this, EventsActivity::class.java) // Administrador a EventsActivity
        } else {
            Intent(this, record::class.java) // Padre a RecordActivity
        }
        intent.putExtra("dni", dni)
        startActivity(intent)
        finish()

    }

}