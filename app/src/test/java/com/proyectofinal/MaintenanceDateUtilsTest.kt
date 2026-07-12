package com.proyectofinal

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceDateUtilsTest {

    @Test
    fun siguienteFecha_respetaCadaFrecuencia() {
        assertEquals("2026-07-19", MaintenanceDateUtils.siguienteFecha("2026-07-12", "Semana"))
        assertEquals("2026-08-12", MaintenanceDateUtils.siguienteFecha("2026-07-12", "Mes"))
        assertEquals("2027-01-12", MaintenanceDateUtils.siguienteFecha("2026-07-12", "6 meses"))
        assertEquals("2027-07-12", MaintenanceDateUtils.siguienteFecha("2026-07-12", "1 año"))
        assertNull(MaintenanceDateUtils.siguienteFecha("2026-07-12", "Una vez"))
    }

    @Test
    fun estaAtrasada_noConsideraHoyComoAtrasado() {
        val hoy = LocalDate.of(2026, 7, 12)
        assertTrue(MaintenanceDateUtils.estaAtrasada("2026-07-11", hoy))
        assertFalse(MaintenanceDateUtils.estaAtrasada("2026-07-12", hoy))
        assertFalse(MaintenanceDateUtils.estaAtrasada("2026-07-13", hoy))
    }

    @Test
    fun fechasInvalidasNoGeneranResultados() {
        assertNull(MaintenanceDateUtils.siguienteFecha("fecha-invalida", "Mes"))
        assertFalse(MaintenanceDateUtils.estaAtrasada("fecha-invalida"))
    }
}
