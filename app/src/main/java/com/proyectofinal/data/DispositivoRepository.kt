package com.proyectofinal.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.proyectofinal.MaintenanceDateUtils
import com.proyectofinal.model.Dispositivo
import com.proyectofinal.model.Inspeccion
import com.proyectofinal.model.ItemProgramado
import com.proyectofinal.model.Mantenimiento
import com.proyectofinal.model.Tarea
import com.proyectofinal.model.TareaFoto
import com.proyectofinal.model.TareaItem
import java.io.File
import java.time.LocalDate

class DispositivoRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = DatabaseHelper(appContext)

    fun insertar(dispositivo: Dispositivo): Long {
        return dbHelper.writableDatabase.insert(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            null,
            valoresDispositivo(dispositivo)
        )
    }

    fun obtenerTodos(): List<Dispositivo> {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_NOMBRE} ASC"
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Dispositivo(
                            id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                            nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE)),
                            categoria = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORIA)),
                            marca = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MARCA)),
                            modelo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO)),
                            foto = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO)).orEmpty()
                        )
                    )
                }
            }
        }
    }

    fun actualizar(dispositivo: Dispositivo): Int {
        return dbHelper.writableDatabase.update(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            valoresDispositivo(dispositivo),
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(dispositivo.id.toString())
        )
    }

    fun eliminar(id: Long): Int {
        val fotos = obtenerFotosPorDispositivo(id).map { it.ruta }
        val filas = dbHelper.writableDatabase.delete(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        if (filas > 0) fotos.forEach(::eliminarArchivoInterno)
        return filas
    }

    private fun valoresDispositivo(dispositivo: Dispositivo) = ContentValues().apply {
        put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
        put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
        put(DatabaseHelper.COL_MARCA, dispositivo.marca)
        put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
        put(DatabaseHelper.COL_FOTO, dispositivo.foto)
    }

    fun guardarDispositivoConCalendario(
        dispositivo: Dispositivo,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>
    ): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val dispositivoId = db.insert(
                DatabaseHelper.TABLE_DISPOSITIVOS,
                null,
                valoresDispositivo(dispositivo)
            )
            check(dispositivoId > 0) { "No se pudo guardar el dispositivo" }
            guardarCalendarioInterno(db, dispositivoId, mantenimientos, inspecciones)
            db.setTransactionSuccessful()
            return dispositivoId
        } finally {
            db.endTransaction()
        }
    }

    fun guardarCalendario(
        dispositivoId: Long,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>
    ) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            guardarCalendarioInterno(db, dispositivoId, mantenimientos, inspecciones)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun guardarEdicionCalendario(
        dispositivoId: Long,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>,
        mantenimientosEliminados: Set<Long>,
        inspeccionesEliminadas: Set<Long>
    ) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            mantenimientosEliminados.forEach { id ->
                db.delete(
                    DatabaseHelper.TABLE_MANTENIMIENTOS,
                    "${DatabaseHelper.COL_MANTENIMIENTO_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            inspeccionesEliminadas.forEach { id ->
                db.delete(
                    DatabaseHelper.TABLE_INSPECCIONES,
                    "${DatabaseHelper.COL_INSPECCION_ID} = ?",
                    arrayOf(id.toString())
                )
            }

            mantenimientos.forEach { mantenimiento ->
                val guardado = mantenimiento.copy(dispositivoId = dispositivoId)
                val id = if (guardado.id > 0) {
                    actualizarMantenimiento(db, guardado)
                    guardado.id
                } else {
                    insertarMantenimiento(db, guardado)
                }
                check(id > 0) { "No se pudo guardar el mantenimiento" }
                asociarFuenteATarea(db, dispositivoId, guardado.fecha, "mantenimiento", id)
            }

            inspecciones.forEach { inspeccion ->
                val guardada = inspeccion.copy(dispositivoId = dispositivoId)
                val id = if (guardada.id > 0) {
                    actualizarInspeccion(db, guardada)
                    guardada.id
                } else {
                    insertarInspeccion(db, guardada)
                }
                check(id > 0) { "No se pudo guardar la inspeccion" }
                asociarFuenteATarea(db, dispositivoId, guardada.fecha, "inspeccion", id)
            }

            eliminarTareasVacias(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun guardarCalendarioInterno(
        db: SQLiteDatabase,
        dispositivoId: Long,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>
    ) {
        mantenimientos.forEach { mantenimiento ->
            val guardado = mantenimiento.copy(dispositivoId = dispositivoId)
            val id = insertarMantenimiento(db, guardado)
            check(id > 0) { "No se pudo guardar el mantenimiento" }
            asociarFuenteATarea(db, dispositivoId, guardado.fecha, "mantenimiento", id)
        }
        inspecciones.forEach { inspeccion ->
            val guardada = inspeccion.copy(dispositivoId = dispositivoId)
            val id = insertarInspeccion(db, guardada)
            check(id > 0) { "No se pudo guardar la inspeccion" }
            asociarFuenteATarea(db, dispositivoId, guardada.fecha, "inspeccion", id)
        }
    }

    private fun insertarMantenimiento(db: SQLiteDatabase, mantenimiento: Mantenimiento): Long {
        return db.insert(DatabaseHelper.TABLE_MANTENIMIENTOS, null, valoresMantenimiento(mantenimiento))
    }

    private fun actualizarMantenimiento(db: SQLiteDatabase, mantenimiento: Mantenimiento): Int {
        val filas = db.update(
            DatabaseHelper.TABLE_MANTENIMIENTOS,
            valoresMantenimiento(mantenimiento),
            "${DatabaseHelper.COL_MANTENIMIENTO_ID} = ?",
            arrayOf(mantenimiento.id.toString())
        )
        moverItemAFuentePendiente(db, "mantenimiento", mantenimiento.id, mantenimiento.dispositivoId, mantenimiento.fecha)
        return filas
    }

    private fun valoresMantenimiento(mantenimiento: Mantenimiento) = ContentValues().apply {
        put(DatabaseHelper.COL_MANTENIMIENTO_NOMBRE, mantenimiento.nombre)
        put(DatabaseHelper.COL_MANTENIMIENTO_DESCRIPCION, mantenimiento.descripcion)
        put(DatabaseHelper.COL_MANTENIMIENTO_FECHA, mantenimiento.fecha)
        put(DatabaseHelper.COL_MANTENIMIENTO_REPETIR, mantenimiento.repetirCada)
        putIdOrNull(DatabaseHelper.COL_MANTENIMIENTO_DISPOSITIVO_ID, mantenimiento.dispositivoId)
    }

    private fun insertarInspeccion(db: SQLiteDatabase, inspeccion: Inspeccion): Long {
        return db.insert(DatabaseHelper.TABLE_INSPECCIONES, null, valoresInspeccion(inspeccion))
    }

    private fun actualizarInspeccion(db: SQLiteDatabase, inspeccion: Inspeccion): Int {
        val filas = db.update(
            DatabaseHelper.TABLE_INSPECCIONES,
            valoresInspeccion(inspeccion),
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(inspeccion.id.toString())
        )
        moverItemAFuentePendiente(db, "inspeccion", inspeccion.id, inspeccion.dispositivoId, inspeccion.fecha)
        return filas
    }

    private fun valoresInspeccion(inspeccion: Inspeccion) = ContentValues().apply {
        put(DatabaseHelper.COL_INSPECCION_NOMBRE, inspeccion.nombre)
        put(DatabaseHelper.COL_INSPECCION_DESCRIPCION, inspeccion.descripcion)
        put(DatabaseHelper.COL_INSPECCION_FECHA, inspeccion.fecha)
        put(DatabaseHelper.COL_INSPECCION_REPETIR, inspeccion.repetirCada)
        putIdOrNull(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID, inspeccion.dispositivoId)
    }

    private fun ContentValues.putIdOrNull(columna: String, id: Long) {
        if (id > 0) put(columna, id) else putNull(columna)
    }

    private fun asociarFuenteATarea(
        db: SQLiteDatabase,
        dispositivoId: Long,
        fecha: String,
        tipo: String,
        fuenteId: Long
    ) {
        val columnaFuente = if (tipo == "mantenimiento") {
            DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID
        } else {
            DatabaseHelper.COL_ITEM_INSPECCION_ID
        }
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREA_ITEMS,
            arrayOf(DatabaseHelper.COL_ITEM_ID),
            "$columnaFuente = ?",
            arrayOf(fuenteId.toString()),
            null, null, null
        )
        val existente = cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
        val tareaId = obtenerOCrearTarea(db, dispositivoId, fecha)
        if (existente > 0) {
            db.update(
                DatabaseHelper.TABLE_TAREA_ITEMS,
                ContentValues().apply { put(DatabaseHelper.COL_ITEM_TAREA_ID, tareaId) },
                "${DatabaseHelper.COL_ITEM_ID} = ?",
                arrayOf(existente.toString())
            )
            return
        }

        db.insert(
            DatabaseHelper.TABLE_TAREA_ITEMS,
            null,
            ContentValues().apply {
                put(DatabaseHelper.COL_ITEM_TAREA_ID, tareaId)
                put(DatabaseHelper.COL_ITEM_TIPO, tipo)
                if (tipo == "mantenimiento") {
                    put(DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID, fuenteId)
                    putNull(DatabaseHelper.COL_ITEM_INSPECCION_ID)
                } else {
                    putNull(DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID)
                    put(DatabaseHelper.COL_ITEM_INSPECCION_ID, fuenteId)
                }
            }
        )
    }

    private fun moverItemAFuentePendiente(
        db: SQLiteDatabase,
        tipo: String,
        fuenteId: Long,
        dispositivoId: Long,
        fecha: String
    ) {
        val columnaFuente = if (tipo == "mantenimiento") {
            DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID
        } else {
            DatabaseHelper.COL_ITEM_INSPECCION_ID
        }
        val cursor = db.rawQuery(
            """
                SELECT ti.${DatabaseHelper.COL_ITEM_ID}
                FROM ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                INNER JOIN ${DatabaseHelper.TABLE_TAREAS} t
                    ON ti.${DatabaseHelper.COL_ITEM_TAREA_ID} = t.${DatabaseHelper.COL_TAREA_ID}
                WHERE ti.$columnaFuente = ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
            """.trimIndent(),
            arrayOf(fuenteId.toString())
        )
        val itemId = cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
        if (itemId == 0L) return
        val tareaId = obtenerOCrearTarea(db, dispositivoId, fecha)
        db.update(
            DatabaseHelper.TABLE_TAREA_ITEMS,
            ContentValues().apply { put(DatabaseHelper.COL_ITEM_TAREA_ID, tareaId) },
            "${DatabaseHelper.COL_ITEM_ID} = ?",
            arrayOf(itemId.toString())
        )
    }

    private fun obtenerOCrearTarea(db: SQLiteDatabase, dispositivoId: Long, fecha: String): Long {
        val seleccionDispositivo = if (dispositivoId > 0) {
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?"
        } else {
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} IS NULL"
        }
        val args = if (dispositivoId > 0) arrayOf(fecha, dispositivoId.toString()) else arrayOf(fecha)
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            arrayOf(DatabaseHelper.COL_TAREA_ID),
            "${DatabaseHelper.COL_TAREA_FECHA} = ? AND $seleccionDispositivo AND ${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
            args,
            null, null,
            "${DatabaseHelper.COL_TAREA_ID} ASC",
            "1"
        )
        cursor.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return db.insert(
            DatabaseHelper.TABLE_TAREAS,
            null,
            ContentValues().apply {
                putIdOrNull(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID, dispositivoId)
                put(DatabaseHelper.COL_TAREA_FECHA, fecha)
            }
        )
    }

    fun obtenerMantenimientosPorDispositivo(dispositivoId: Long): List<Mantenimiento> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT DISTINCT m.*
                FROM ${DatabaseHelper.TABLE_MANTENIMIENTOS} m
                INNER JOIN ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                    ON ti.${DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID} = m.${DatabaseHelper.COL_MANTENIMIENTO_ID}
                INNER JOIN ${DatabaseHelper.TABLE_TAREAS} t
                    ON t.${DatabaseHelper.COL_TAREA_ID} = ti.${DatabaseHelper.COL_ITEM_TAREA_ID}
                WHERE m.${DatabaseHelper.COL_MANTENIMIENTO_DISPOSITIVO_ID} = ?
                  AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
                ORDER BY m.${DatabaseHelper.COL_MANTENIMIENTO_FECHA} ASC
            """.trimIndent(),
            arrayOf(dispositivoId.toString())
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.aMantenimiento()) } }
    }

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT DISTINCT i.*
                FROM ${DatabaseHelper.TABLE_INSPECCIONES} i
                INNER JOIN ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                    ON ti.${DatabaseHelper.COL_ITEM_INSPECCION_ID} = i.${DatabaseHelper.COL_INSPECCION_ID}
                INNER JOIN ${DatabaseHelper.TABLE_TAREAS} t
                    ON t.${DatabaseHelper.COL_TAREA_ID} = ti.${DatabaseHelper.COL_ITEM_TAREA_ID}
                WHERE i.${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?
                  AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
                ORDER BY i.${DatabaseHelper.COL_INSPECCION_FECHA} ASC
            """.trimIndent(),
            arrayOf(dispositivoId.toString())
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.aInspeccion()) } }
    }

    fun obtenerTareas(): List<Tarea> {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_TAREAS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_TAREA_FECHA} ASC"
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.aTarea()) } }
    }

    fun obtenerTareaPorId(tareaId: Long): Tarea? {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.aTarea() else null }
    }

    fun obtenerItemsPorTarea(tareaId: Long): List<TareaItem> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT ti.*,
                       COALESCE(m.${DatabaseHelper.COL_MANTENIMIENTO_NOMBRE}, i.${DatabaseHelper.COL_INSPECCION_NOMBRE}, '') AS nombre,
                       COALESCE(m.${DatabaseHelper.COL_MANTENIMIENTO_DESCRIPCION}, i.${DatabaseHelper.COL_INSPECCION_DESCRIPCION}, '') AS descripcion
                FROM ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                LEFT JOIN ${DatabaseHelper.TABLE_MANTENIMIENTOS} m
                    ON ti.${DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID} = m.${DatabaseHelper.COL_MANTENIMIENTO_ID}
                LEFT JOIN ${DatabaseHelper.TABLE_INSPECCIONES} i
                    ON ti.${DatabaseHelper.COL_ITEM_INSPECCION_ID} = i.${DatabaseHelper.COL_INSPECCION_ID}
                WHERE ti.${DatabaseHelper.COL_ITEM_TAREA_ID} = ?
                ORDER BY CASE ti.${DatabaseHelper.COL_ITEM_TIPO} WHEN 'mantenimiento' THEN 0 ELSE 1 END, nombre ASC
            """.trimIndent(),
            arrayOf(tareaId.toString())
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.aTareaItem()) } }
    }

    fun obtenerFotosPorTarea(tareaId: Long): List<TareaFoto> {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_TAREA_FOTOS,
            null,
            "${DatabaseHelper.COL_TAREA_FOTO_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null,
            "${DatabaseHelper.COL_TAREA_FOTO_ID} ASC"
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        TareaFoto(
                            id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_ID)),
                            tareaId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_TAREA_ID)),
                            ruta = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_RUTA)),
                            fechaCreacion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_FECHA))
                        )
                    )
                }
            }
        }
    }

    fun completarTarea(
        tareaId: Long,
        items: List<TareaItem>,
        fotos: List<String>,
        fechaCompletada: String
    ) {
        val db = dbHelper.writableDatabase
        val tarea = obtenerTareaPorId(tareaId) ?: return
        if (tarea.completada) return
        val rutasAnteriores = obtenerFotosPorTarea(tareaId).map { it.ruta }.toSet()
        val rutasNuevas = fotos.filter { it.isNotBlank() }.distinct().toSet()

        db.beginTransaction()
        try {
            items.forEach { item ->
                db.update(
                    DatabaseHelper.TABLE_TAREA_ITEMS,
                    ContentValues().apply {
                        put(DatabaseHelper.COL_ITEM_NOTAS, item.notas)
                        put(DatabaseHelper.COL_ITEM_CONDICION, if (item.tipo == "inspeccion") item.condicion else "")
                    },
                    "${DatabaseHelper.COL_ITEM_ID} = ? AND ${DatabaseHelper.COL_ITEM_TAREA_ID} = ?",
                    arrayOf(item.id.toString(), tareaId.toString())
                )
            }

            db.delete(
                DatabaseHelper.TABLE_TAREA_FOTOS,
                "${DatabaseHelper.COL_TAREA_FOTO_TAREA_ID} = ?",
                arrayOf(tareaId.toString())
            )
            rutasNuevas.forEach { ruta ->
                db.insert(
                    DatabaseHelper.TABLE_TAREA_FOTOS,
                    null,
                    ContentValues().apply {
                        put(DatabaseHelper.COL_TAREA_FOTO_TAREA_ID, tareaId)
                        put(DatabaseHelper.COL_TAREA_FOTO_RUTA, ruta)
                        put(DatabaseHelper.COL_TAREA_FOTO_FECHA, fechaCompletada)
                    }
                )
            }

            db.update(
                DatabaseHelper.TABLE_TAREAS,
                ContentValues().apply {
                    put(DatabaseHelper.COL_TAREA_COMPLETADA, 1)
                    put(DatabaseHelper.COL_TAREA_FECHA_COMPLETADA, fechaCompletada)
                },
                "${DatabaseHelper.COL_TAREA_ID} = ?",
                arrayOf(tareaId.toString())
            )
            crearRepeticiones(db, tarea, items)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        (rutasAnteriores - rutasNuevas).forEach(::eliminarArchivoInterno)
    }

    private fun crearRepeticiones(db: SQLiteDatabase, tarea: Tarea, items: List<TareaItem>) {
        items.forEach { item ->
            if (item.tipo == "mantenimiento") {
                val actual = item.mantenimientoId?.let { obtenerMantenimientoPorId(db, it) } ?: return@forEach
                val siguienteFecha = MaintenanceDateUtils.siguienteFecha(actual.fecha, actual.repetirCada) ?: return@forEach
                val nuevoId = insertarMantenimiento(db, actual.copy(id = 0, fecha = siguienteFecha))
                asociarFuenteATarea(db, tarea.dispositivoId, siguienteFecha, "mantenimiento", nuevoId)
            } else {
                val actual = item.inspeccionId?.let { obtenerInspeccionPorId(db, it) } ?: return@forEach
                val siguienteFecha = MaintenanceDateUtils.siguienteFecha(actual.fecha, actual.repetirCada) ?: return@forEach
                val nuevoId = insertarInspeccion(db, actual.copy(id = 0, fecha = siguienteFecha))
                asociarFuenteATarea(db, tarea.dispositivoId, siguienteFecha, "inspeccion", nuevoId)
            }
        }
    }

    fun obtenerResultadosInspeccionPorDispositivo(dispositivoId: Long): List<TareaItem> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT ti.*, i.${DatabaseHelper.COL_INSPECCION_NOMBRE} AS nombre,
                       i.${DatabaseHelper.COL_INSPECCION_DESCRIPCION} AS descripcion
                FROM ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                INNER JOIN ${DatabaseHelper.TABLE_TAREAS} t
                    ON ti.${DatabaseHelper.COL_ITEM_TAREA_ID} = t.${DatabaseHelper.COL_TAREA_ID}
                INNER JOIN ${DatabaseHelper.TABLE_INSPECCIONES} i
                    ON ti.${DatabaseHelper.COL_ITEM_INSPECCION_ID} = i.${DatabaseHelper.COL_INSPECCION_ID}
                WHERE t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?
                  AND ti.${DatabaseHelper.COL_ITEM_TIPO} = 'inspeccion'
                  AND ti.${DatabaseHelper.COL_ITEM_CONDICION} <> ''
                ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} DESC
            """.trimIndent(),
            arrayOf(dispositivoId.toString())
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(c.aTareaItem()) } }
    }

    private fun ejecutarItemsQuery(whereClause: String, args: Array<String>?): List<ItemProgramado> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT t.${DatabaseHelper.COL_TAREA_ID} AS tarea_id,
                       t.${DatabaseHelper.COL_TAREA_FECHA} AS fecha,
                       COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') AS dispositivo,
                       ti.${DatabaseHelper.COL_ITEM_TIPO} AS tipo,
                       COALESCE(m.${DatabaseHelper.COL_MANTENIMIENTO_NOMBRE}, i.${DatabaseHelper.COL_INSPECCION_NOMBRE}, '') AS nombre,
                       COALESCE(m.${DatabaseHelper.COL_MANTENIMIENTO_DESCRIPCION}, i.${DatabaseHelper.COL_INSPECCION_DESCRIPCION}, '') AS descripcion
                FROM ${DatabaseHelper.TABLE_TAREAS} t
                INNER JOIN ${DatabaseHelper.TABLE_TAREA_ITEMS} ti
                    ON ti.${DatabaseHelper.COL_ITEM_TAREA_ID} = t.${DatabaseHelper.COL_TAREA_ID}
                LEFT JOIN ${DatabaseHelper.TABLE_MANTENIMIENTOS} m
                    ON ti.${DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID} = m.${DatabaseHelper.COL_MANTENIMIENTO_ID}
                LEFT JOIN ${DatabaseHelper.TABLE_INSPECCIONES} i
                    ON ti.${DatabaseHelper.COL_ITEM_INSPECCION_ID} = i.${DatabaseHelper.COL_INSPECCION_ID}
                LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d
                    ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
                WHERE $whereClause
                ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} ASC, ti.${DatabaseHelper.COL_ITEM_ID} ASC
            """.trimIndent(),
            args
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        ItemProgramado(
                            id = c.getLong(c.getColumnIndexOrThrow("tarea_id")),
                            nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                            descripcion = c.getString(c.getColumnIndexOrThrow("descripcion")),
                            fecha = c.getString(c.getColumnIndexOrThrow("fecha")),
                            nombreDispositivo = c.getString(c.getColumnIndexOrThrow("dispositivo")),
                            tipo = c.getString(c.getColumnIndexOrThrow("tipo"))
                        )
                    )
                }
            }
        }
    }

    fun obtenerItemsPasadas(): List<ItemProgramado> = ejecutarItemsQuery(
        "t.${DatabaseHelper.COL_TAREA_FECHA} < ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
        arrayOf(LocalDate.now().toString())
    )

    fun obtenerItemsProximas(): List<ItemProgramado> = ejecutarItemsQuery(
        "t.${DatabaseHelper.COL_TAREA_FECHA} >= ? AND t.${DatabaseHelper.COL_TAREA_FECHA} < ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
        arrayOf(LocalDate.now().toString(), LocalDate.now().plusDays(7).toString())
    )

    fun obtenerItemsLejanas(): List<ItemProgramado> = ejecutarItemsQuery(
        "t.${DatabaseHelper.COL_TAREA_FECHA} >= ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
        arrayOf(LocalDate.now().plusDays(7).toString())
    )

    fun obtenerItemsCompletadas(): List<ItemProgramado> = ejecutarItemsQuery(
        "t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 1",
        null
    )

    private fun obtenerMantenimientoPorId(db: SQLiteDatabase, id: Long): Mantenimiento? {
        val cursor = db.query(
            DatabaseHelper.TABLE_MANTENIMIENTOS,
            null,
            "${DatabaseHelper.COL_MANTENIMIENTO_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.aMantenimiento() else null }
    }

    private fun obtenerInspeccionPorId(db: SQLiteDatabase, id: Long): Inspeccion? {
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.aInspeccion() else null }
    }

    private fun eliminarTareasVacias(db: SQLiteDatabase) {
        db.execSQL(
            "DELETE FROM ${DatabaseHelper.TABLE_TAREAS} WHERE ${DatabaseHelper.COL_TAREA_COMPLETADA} = 0 AND ${DatabaseHelper.COL_TAREA_ID} NOT IN (SELECT ${DatabaseHelper.COL_ITEM_TAREA_ID} FROM ${DatabaseHelper.TABLE_TAREA_ITEMS})"
        )
    }

    private fun obtenerFotosPorDispositivo(dispositivoId: Long): List<TareaFoto> {
        val cursor = dbHelper.readableDatabase.rawQuery(
            """
                SELECT f.*
                FROM ${DatabaseHelper.TABLE_TAREA_FOTOS} f
                INNER JOIN ${DatabaseHelper.TABLE_TAREAS} t
                    ON f.${DatabaseHelper.COL_TAREA_FOTO_TAREA_ID} = t.${DatabaseHelper.COL_TAREA_ID}
                WHERE t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?
            """.trimIndent(),
            arrayOf(dispositivoId.toString())
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        TareaFoto(
                            id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_ID)),
                            tareaId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_TAREA_ID)),
                            ruta = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_RUTA)),
                            fechaCreacion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FOTO_FECHA))
                        )
                    )
                }
            }
        }
    }

    private fun eliminarArchivoInterno(ruta: String) {
        runCatching {
            val archivo = File(ruta)
            val carpetaFotos = File(appContext.filesDir, "tarea_fotos").canonicalFile
            if (archivo.canonicalFile.parentFile == carpetaFotos) archivo.delete()
        }
    }

    private fun Cursor.aMantenimiento() = Mantenimiento(
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_MANTENIMIENTO_ID)),
        nombre = getString(getColumnIndexOrThrow(DatabaseHelper.COL_MANTENIMIENTO_NOMBRE)),
        descripcion = getString(getColumnIndexOrThrow(DatabaseHelper.COL_MANTENIMIENTO_DESCRIPCION)),
        fecha = getString(getColumnIndexOrThrow(DatabaseHelper.COL_MANTENIMIENTO_FECHA)),
        repetirCada = getString(getColumnIndexOrThrow(DatabaseHelper.COL_MANTENIMIENTO_REPETIR)),
        dispositivoId = getLongOrZero(DatabaseHelper.COL_MANTENIMIENTO_DISPOSITIVO_ID)
    )

    private fun Cursor.aInspeccion() = Inspeccion(
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
        nombre = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
        descripcion = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
        fecha = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
        repetirCada = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
        dispositivoId = getLongOrZero(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)
    )

    private fun Cursor.aTarea() = Tarea(
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
        dispositivoId = getLongOrZero(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID),
        fecha = getString(getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
        completada = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1,
        fechaCompletada = getString(getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA_COMPLETADA))
    )

    private fun Cursor.aTareaItem() = TareaItem(
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_ITEM_ID)),
        tareaId = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_ITEM_TAREA_ID)),
        tipo = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ITEM_TIPO)),
        mantenimientoId = getNullableLong(DatabaseHelper.COL_ITEM_MANTENIMIENTO_ID),
        inspeccionId = getNullableLong(DatabaseHelper.COL_ITEM_INSPECCION_ID),
        nombre = getString(getColumnIndexOrThrow("nombre")),
        descripcion = getString(getColumnIndexOrThrow("descripcion")),
        notas = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ITEM_NOTAS)).orEmpty(),
        condicion = getString(getColumnIndexOrThrow(DatabaseHelper.COL_ITEM_CONDICION)).orEmpty()
    )

    private fun Cursor.getLongOrZero(columna: String): Long {
        val indice = getColumnIndexOrThrow(columna)
        return if (isNull(indice)) 0L else getLong(indice)
    }

    private fun Cursor.getNullableLong(columna: String): Long? {
        val indice = getColumnIndexOrThrow(columna)
        return if (isNull(indice)) null else getLong(indice)
    }
}
