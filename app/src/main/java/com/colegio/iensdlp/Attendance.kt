package com.colegio.iensdlp

import com.google.firebase.Timestamp

class Attendance(
    val nombres: String,
    val grado: String,
    val timestamp: Timestamp,
    val seccion: String,
    val dni: String,
    val tipo: String
)