package com.colegio.iensdlp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class InfoColeActivity : AppCompatActivity() {

    private lateinit var imgMision: ImageView
    private lateinit var imgVision: ImageView
    private lateinit var recyclerViewMision: RecyclerView
    private lateinit var recyclerViewVision: RecyclerView
    private lateinit var misionAdapter: InfoAdapter
    private lateinit var visionAdapter: InfoAdapter
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info_cole)


        imgMision = findViewById(R.id.imgMision)
        imgVision = findViewById(R.id.imgVision)
        recyclerViewMision = findViewById(R.id.recyclerViewMision)
        recyclerViewVision = findViewById(R.id.recyclerViewVision)

        recyclerViewMision.layoutManager = LinearLayoutManager(this)
        recyclerViewVision.layoutManager = LinearLayoutManager(this)

        misionAdapter = InfoAdapter()
        visionAdapter = InfoAdapter()

        recyclerViewMision.adapter = misionAdapter
        recyclerViewVision.adapter = visionAdapter

        loadMisionData()
        loadVisionData()

    }

    private fun loadMisionData() {
        db.collection("mision").get()
            .addOnSuccessListener { documents ->
                val misionList = mutableListOf<InfoCole>()
                for (document in documents) {
                    val item = document.toObject(InfoCole::class.java)
                    misionList.add(item)
                }
                misionAdapter.updateData(misionList)
                if (misionList.isNotEmpty()) {
                    Glide.with(this).load(misionList[0].imagen).into(imgMision)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar misión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadVisionData() {
        db.collection("vision").get()
            .addOnSuccessListener { documents ->
                val visionList = mutableListOf<InfoCole>()
                for (document in documents) {
                    val item = document.toObject(InfoCole::class.java)
                    visionList.add(item)
                }
                visionAdapter.updateData(visionList)
                if (visionList.isNotEmpty()) {
                    Glide.with(this).load(visionList[0].imagen).into(imgVision)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar visión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}