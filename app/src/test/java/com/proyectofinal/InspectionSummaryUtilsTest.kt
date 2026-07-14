package com.proyectofinal

import com.proyectofinal.model.Inspeccion
import com.proyectofinal.model.TareaDetalle
import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionSummaryUtilsTest {

    @Test
    fun `no cuenta dos veces la misma inspeccion vinculada a una tarea`() {
        val detalle = TareaDetalle(
            id = 20,
            tareaId = 10,
            tipo = "inspeccion",
            nombre = "Revisar filtro",
            descripcion = "Comprobar suciedad",
            condicion = "bueno",
            completada = true,
            fechaCompletada = "2026-07-14"
        )
        val inspeccion = Inspeccion(
            id = 30,
            nombre = "Revisar filtro",
            descripcion = "Comprobar suciedad",
            fecha = "2026-07-14",
            repetirCada = "Una vez",
            completada = true,
            condicion = "bueno"
        )

        val resultado = InspectionSummaryUtils.combinarInspeccionesUnicas(
            listOf(DetalleInspeccionProgramada(detalle, "2026-07-14")),
            listOf(inspeccion)
        )

        assertEquals(1, resultado.size)
        assertEquals("bueno", resultado.single().condicion)
    }

    @Test
    fun `mantiene repeticiones realizadas en fechas diferentes`() {
        val inspecciones = listOf("2026-07-01", "2026-07-08").mapIndexed { indice, fecha ->
            Inspeccion(
                id = indice.toLong() + 1,
                nombre = "Revisar filtro",
                descripcion = "Comprobar suciedad",
                fecha = fecha,
                repetirCada = "Semanal",
                completada = true,
                condicion = "bueno"
            )
        }

        val resultado = InspectionSummaryUtils.combinarInspeccionesUnicas(emptyList(), inspecciones)

        assertEquals(2, resultado.size)
    }
}
