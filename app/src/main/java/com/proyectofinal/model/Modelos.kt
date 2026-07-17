package com.proyectofinal.model

data class Dispositivo(
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val foto: String = ""
)

data class Mantenimiento(
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

data class Tarea(
    val id: Long = 0,
    val dispositivoId: Long = 0,
    val fecha: String,
    val completada: Boolean = false,
    val fechaCompletada: String? = null
)

data class TareaItem(
    val id: Long = 0,
    val tareaId: Long,
    val tipo: String,
    val mantenimientoId: Long? = null,
    val inspeccionId: Long? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val notas: String = "",
    val condicion: String = ""
)

data class TareaFoto(
    val id: Long = 0,
    val tareaId: Long,
    val ruta: String,
    val fechaCreacion: String
)

data class ItemProgramado(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val nombreDispositivo: String,
    val tipo: String
)
