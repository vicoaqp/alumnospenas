package com.colegio.iensdlp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DocenteActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var docenteAdapter: DocenteAdapter
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docente)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerViewDocentes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        docenteAdapter = DocenteAdapter(emptyList(), this)
        recyclerView.adapter = docenteAdapter

        // Cargar la lista de docentes desde Firestore
        fetchDocentesFromFirebase()




    }

    private fun fetchDocentesFromFirebase() {
        db.collection("docentes")
            .get()
            .addOnSuccessListener { documents ->
                val docentesList = mutableListOf<Docente>()
                for (document in documents) {
                    val docente = document.toObject(Docente::class.java)
                    docentesList.add(docente)
                }
                docenteAdapter.updateDocentes(docentesList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar docentes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}