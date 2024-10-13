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

class ParentRegisterActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_register)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val nombres = findViewById<EditText>(R.id.editTextName)
        val apellidos = findViewById<EditText>(R.id.editTextLastName)
        val dniPadre = findViewById<EditText>(R.id.editTextDni)
        val direccion = findViewById<EditText>(R.id.editTextDireccion)
        val correo = findViewById<EditText>(R.id.editTextEmail)
        val celular = findViewById<EditText>(R.id.editTextCelular)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val btnRegistrar = findViewById<Button>(R.id.buttonRegister)

        btnRegistrar.setOnClickListener {
            val nombresText = nombres.text.toString().trim()
            val apellidosText = apellidos.text.toString().trim()
            val dniText = dniPadre.text.toString().trim()
            val direccionText = direccion.text.toString().trim()
            val correoText = correo.text.toString().trim()
            val celularText = celular.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (nombresText.isNotEmpty() && apellidosText.isNotEmpty() && dniText.isNotEmpty() &&
                direccionText.isNotEmpty() && correoText.isNotEmpty() && celularText.isNotEmpty() && passwordText.isNotEmpty()) {

                val padre = hashMapOf(
                    "nombres" to nombresText,
                    "apellidos" to apellidosText,
                    "dnipadre" to dniText,
                    "direccion" to direccionText,
                    "correo" to correoText,
                    "celular" to celularText,
                    "contraseña" to passwordText,
                    "tipo" to "padre"
                )

                // Registrar en la colección "padres"
                db.collection("padres").document(dniText)
                    .set(padre)
                    .addOnSuccessListener {
                        // Guardar estado de sesión y redirigir
                        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("dni", dniText)
                        editor.putString("userType", "padre")
                        editor.apply()

                        Toast.makeText(this, "Padre registrado exitosamente", Toast.LENGTH_SHORT).show()

                        // Redirigir a pantalla de registro de asistencias
                        val intent = Intent(this, record::class.java)
                        intent.putExtra("dni", dniText)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }


    }