package com.colegio.iensdlp

import com.google.firebase.Timestamp


data class Evento(
    val descripcion: String = "",
    val tipoevento: String = "",
    val curso: String = "",
    val docente: String = "",
    var timestamp: Timestamp? = null
)
