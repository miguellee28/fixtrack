package com.proyectofinal

import java.time.LocalDate
import java.util.Locale

object MaintenanceDateUtils {

    fun siguienteFecha(fecha: String, repeticion: String): String? {
        val fechaBase = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return null
        val repeticionNormalizada = repeticion.lowercase(Locale.getDefault())

        return when {
            repeticionNormalizada.contains("una vez") -> null
            repeticionNormalizada.contains("semana") -> fechaBase.plusWeeks(1).toString()
            repeticionNormalizada.contains("6") && repeticionNormalizada.contains("mes") ->
                fechaBase.plusMonths(6).toString()
            repeticionNormalizada.contains("mes") -> fechaBase.plusMonths(1).toString()
            repeticionNormalizada.contains("año") || repeticionNormalizada.contains("ano") ->
                fechaBase.plusYears(1).toString()
            else -> null
        }
    }

    fun estaAtrasada(fecha: String, hoy: LocalDate = LocalDate.now()): Boolean {
        val fechaProgramada = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return false
        return fechaProgramada.isBefore(hoy)
    }
}
