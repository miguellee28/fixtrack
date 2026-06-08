package com.proyectofinal

import android.content.ContentValues
import android.content.Context

class DispositivoRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // ==================== DISPOSITIVOS ====================

    fun insertar(dispositivo: Dispositivo): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
        }
        val id = db.insert(DatabaseHelper.TABLE_DISPOSITIVOS, null, values)
        db.close()
        return id
    }

    fun obtenerTodos(): List<Dispositivo> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Dispositivo>()
        val cursor = db.query(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Dispositivo(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE)),
                        categoria = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORIA)),
                        marca = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MARCA)),
                        modelo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO))
                    )
                )
            }
        }
        db.close()
        return lista
    }

    fun actualizar(dispositivo: Dispositivo): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            values,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(dispositivo.id.toString())
        )
        db.close()
        return filas
    }

    fun eliminar(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return filas
    }

    // ==================== TAREAS ====================

    fun insertarTarea(tarea: Tarea): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TAREA_NOMBRE, tarea.nombre)
            put(DatabaseHelper.COL_TAREA_DESCRIPCION, tarea.descripcion)
            put(DatabaseHelper.COL_TAREA_FECHA, tarea.fecha)
            put(DatabaseHelper.COL_TAREA_REPETIR, tarea.repetirCada)
            put(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID, tarea.dispositivoId)
        }
        val id = db.insert(DatabaseHelper.TABLE_TAREAS, null, values)
        db.close()
        return id
    }

    fun obtenerTareas(): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_TAREA_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Tarea(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID))
                    )
                )
            }
        }
        db.close()
        return lista
    }

    fun eliminarTarea(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_TAREAS,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return filas
    }

    // ==================== INSPECCIONES ====================

    fun insertarInspeccion(inspeccion: Inspeccion): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_INSPECCION_NOMBRE, inspeccion.nombre)
            put(DatabaseHelper.COL_INSPECCION_DESCRIPCION, inspeccion.descripcion)
            put(DatabaseHelper.COL_INSPECCION_FECHA, inspeccion.fecha)
            put(DatabaseHelper.COL_INSPECCION_REPETIR, inspeccion.repetirCada)
            put(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID, inspeccion.dispositivoId)
        }
        val id = db.insert(DatabaseHelper.TABLE_INSPECCIONES, null, values)
        db.close()
        return id
    }

    fun obtenerInspecciones(): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_INSPECCION_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Inspeccion(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID))
                    )
                )
            }
        }
        db.close()
        return lista
    }

    fun eliminarInspeccion(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_INSPECCIONES,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return filas
    }
}
