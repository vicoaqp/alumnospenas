package com.colegio.iensdlp

import com.google.firebase.Timestamp


data class Evento(
    val descripcion: String = "",
    val tipoevento: String = "",
    var timestamp: Timestamp? = null
)
