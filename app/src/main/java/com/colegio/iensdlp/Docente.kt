package com.colegio.iensdlp

data class Docente(
    val nombres: String = "",
    val apellidos: String = "",
    val especialidad: String = "",
    val celular: String = "",
    val grados: List<String> = emptyList(),
    val secciones: List<String> = emptyList()
)