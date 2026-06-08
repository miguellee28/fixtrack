package com.proyectofinal

data class Dispositivo(
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val marca: String,
    val modelo: String
)

data class Tarea(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val repetirCada: String,
    val dispositivoId: Long = 0
)

data class Inspeccion(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val repetirCada: String,
    val dispositivoId: Long = 0
)
