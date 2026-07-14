package com.proyectofinal

import com.proyectofinal.model.Inspeccion
import com.proyectofinal.model.TareaDetalle
import java.util.Locale

data class DetalleInspeccionProgramada(
    val detalle: TareaDetalle,
    val fechaProgramada: String
)

object InspectionSummaryUtils {

    fun combinarInspeccionesUnicas(
        detallesDeTareas: List<DetalleInspeccionProgramada>,
        inspeccionesIndependientes: List<Inspeccion>
    ): List<TareaDetalle> {
        val inspeccionesUnicas = linkedMapOf<String, TareaDetalle>()

        inspeccionesIndependientes
            .filter { it.condicion.isNotBlank() || it.notas.isNotBlank() }
            .forEach { inspeccion ->
                inspeccionesUnicas[clave(
                    inspeccion.nombre,
                    inspeccion.descripcion,
                    inspeccion.fecha
                )] = TareaDetalle(
                    id = inspeccion.id,
                    tareaId = 0,
                    tipo = "inspeccion",
                    nombre = inspeccion.nombre,
                    descripcion = inspeccion.descripcion,
                    condicion = inspeccion.condicion,
                    notas = inspeccion.notas,
                    fotos = inspeccion.fotos,
                    completada = inspeccion.completada,
                    fechaCompletada = inspeccion.fechaCompletada
                )
            }

        detallesDeTareas
            .filter {
                it.detalle.tipo == "inspeccion" &&
                    (it.detalle.condicion.isNotBlank() || it.detalle.notas.isNotBlank())
            }
            .forEach { programada ->
                val detalle = programada.detalle
                val clave = clave(detalle.nombre, detalle.descripcion, programada.fechaProgramada)
                val existente = inspeccionesUnicas[clave]
                inspeccionesUnicas[clave] = if (existente == null) {
                    detalle
                } else {
                    detalle.copy(
                        condicion = detalle.condicion.ifBlank { existente.condicion },
                        notas = detalle.notas.ifBlank { existente.notas },
                        fotos = (existente.fotos + detalle.fotos).distinct(),
                        completada = existente.completada || detalle.completada,
                        fechaCompletada = detalle.fechaCompletada ?: existente.fechaCompletada
                    )
                }
            }

        return inspeccionesUnicas.values.toList()
    }

    private fun clave(nombre: String, descripcion: String, fechaProgramada: String): String {
        return listOf(nombre, descripcion, fechaProgramada)
            .joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
