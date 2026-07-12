package com.proyectofinal.data

// Fuente única de acceso a los datos de dispositivos y tareas.
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.proyectofinal.MaintenanceDateUtils
import com.proyectofinal.model.*
import java.time.LocalDate

class DispositivoRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // ==================== DISPOSITIVOS ====================

    fun insertar(dispositivo: Dispositivo): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
            put(DatabaseHelper.COL_FOTO, dispositivo.foto)
        }
        val id = db.insert(DatabaseHelper.TABLE_DISPOSITIVOS, null, values)
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
                        modelo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO)),
                        foto = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_FOTO)) ?: ""
                    )
                )
            }
        }
        return lista
    }

    fun actualizar(dispositivo: Dispositivo): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
            put(DatabaseHelper.COL_FOTO, dispositivo.foto)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            values,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(dispositivo.id.toString())
        )
        return filas
    }

    fun eliminar(id: Long): Int {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                DatabaseHelper.TABLE_TAREA_DETALLES,
                "${DatabaseHelper.COL_DETALLE_TAREA_ID} IN (SELECT ${DatabaseHelper.COL_TAREA_ID} FROM ${DatabaseHelper.TABLE_TAREAS} WHERE ${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?)",
                arrayOf(id.toString())
            )
            db.delete(
                DatabaseHelper.TABLE_TAREAS,
                "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?",
                arrayOf(id.toString())
            )
            db.delete(
                DatabaseHelper.TABLE_INSPECCIONES,
                "${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
                arrayOf(id.toString())
            )
            val filas = db.delete(
                DatabaseHelper.TABLE_DISPOSITIVOS,
                "${DatabaseHelper.COL_ID} = ?",
                arrayOf(id.toString())
            )
            db.setTransactionSuccessful()
            return filas
        } finally {
            db.endTransaction()
        }
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
            put(DatabaseHelper.COL_TAREA_COMPLETADA, if (tarea.completada) 1 else 0)
        }
        val id = db.insert(DatabaseHelper.TABLE_TAREAS, null, values)
        if (id > 0) {
            insertarDetalleBaseDeTarea(id, tarea.nombre, tarea.descripcion)
        }
        return id
    }

    private fun insertarDetalleBaseDeTarea(tareaId: Long, nombre: String, descripcion: String): Long {
        return insertarTareaDetalleSiNoExiste(
            TareaDetalle(
                tareaId = tareaId,
                tipo = "mantenimiento",
                nombre = nombre,
                descripcion = descripcion
            )
        )
    }

    private fun insertarDetalleInspeccionParaTarea(tareaId: Long, inspeccion: Inspeccion): Long {
        return insertarTareaDetalleSiNoExiste(
            TareaDetalle(
                tareaId = tareaId,
                tipo = "inspeccion",
                nombre = inspeccion.nombre,
                descripcion = inspeccion.descripcion
            )
        )
    }

    private fun insertarTareaDetalleSiNoExiste(detalle: TareaDetalle): Long {
        val db = dbHelper.writableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            arrayOf(DatabaseHelper.COL_DETALLE_ID),
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ? AND ${DatabaseHelper.COL_DETALLE_TIPO} = ? AND ${DatabaseHelper.COL_DETALLE_NOMBRE} = ? AND ${DatabaseHelper.COL_DETALLE_DESCRIPCION} = ?",
            arrayOf(detalle.tareaId.toString(), detalle.tipo, detalle.nombre, detalle.descripcion),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_ID))
            }
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_DETALLE_TAREA_ID, detalle.tareaId)
            put(DatabaseHelper.COL_DETALLE_TIPO, detalle.tipo)
            put(DatabaseHelper.COL_DETALLE_NOMBRE, detalle.nombre)
            put(DatabaseHelper.COL_DETALLE_DESCRIPCION, detalle.descripcion)
            put(DatabaseHelper.COL_DETALLE_CONDICION, detalle.condicion)
            put(DatabaseHelper.COL_DETALLE_NOTAS, detalle.notas)
            put(DatabaseHelper.COL_DETALLE_FOTOS, detalle.fotos.joinToString(","))
            put(DatabaseHelper.COL_DETALLE_COMPLETADA, if (detalle.completada) 1 else 0)
            put(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA, detalle.fechaCompletada)
        }
        return db.insert(DatabaseHelper.TABLE_TAREA_DETALLES, null, values)
    }

    fun vincularInspeccionesATarea(tareaId: Long, inspecciones: List<Inspeccion>) {
        for (inspeccion in inspecciones) {
            insertarDetalleInspeccionParaTarea(tareaId, inspeccion)
        }
    }

    fun sincronizarDetallesDeTarea(tarea: Tarea, inspecciones: List<Inspeccion>) {
        sincronizarDetalleMantenimiento(tarea)
        reemplazarDetallesInspeccion(tarea.id, inspecciones)
    }

    private fun sincronizarDetalleMantenimiento(tarea: Tarea) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_DETALLE_NOMBRE, tarea.nombre)
            put(DatabaseHelper.COL_DETALLE_DESCRIPCION, tarea.descripcion)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            values,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ? AND ${DatabaseHelper.COL_DETALLE_TIPO} = ?",
            arrayOf(tarea.id.toString(), "mantenimiento")
        )
        if (filas == 0) {
            insertarDetalleBaseDeTarea(tarea.id, tarea.nombre, tarea.descripcion)
        }
    }

    private fun reemplazarDetallesInspeccion(tareaId: Long, inspecciones: List<Inspeccion>) {
        val db = dbHelper.writableDatabase
        db.delete(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ? AND ${DatabaseHelper.COL_DETALLE_TIPO} = ?",
            arrayOf(tareaId.toString(), "inspeccion")
        )
        for (inspeccion in inspecciones) {
            insertarDetalleInspeccionParaTarea(tareaId, inspeccion)
        }
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
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ? AND ${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_TAREA_FECHA} ASC"
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
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTodasTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_TAREA_FECHA} ASC"
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
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun eliminarTarea(id: Long): Int {
        val db = dbHelper.writableDatabase
        db.delete(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ?",
            arrayOf(id.toString())
        )
        val filas = db.delete(
            DatabaseHelper.TABLE_TAREAS,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(id.toString())
        )
        return filas
    }

    fun actualizarTarea(tarea: Tarea): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TAREA_NOMBRE, tarea.nombre)
            put(DatabaseHelper.COL_TAREA_DESCRIPCION, tarea.descripcion)
            put(DatabaseHelper.COL_TAREA_FECHA, tarea.fecha)
            put(DatabaseHelper.COL_TAREA_REPETIR, tarea.repetirCada)
            put(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID, tarea.dispositivoId)
            put(DatabaseHelper.COL_TAREA_COMPLETADA, if (tarea.completada) 1 else 0)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_TAREAS,
            values,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(tarea.id.toString())
        )
        if (filas > 0) {
            sincronizarDetalleMantenimiento(tarea)
        }
        return filas
    }

    fun marcarTareaCompletada(id: Long) {
        val db = dbHelper.writableDatabase
        val tarea = obtenerTareaPorId(id)
        if (tarea == null || tarea.completada) return
        val inspeccionesRelacionadas = obtenerInspeccionesRelacionadas(tarea)
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_TAREA_COMPLETADA, 1)
            }
            db.update(DatabaseHelper.TABLE_TAREAS, values, "${DatabaseHelper.COL_TAREA_ID} = ?", arrayOf(id.toString()))

            val valoresInspeccion = ContentValues().apply {
                put(DatabaseHelper.COL_INSPECCION_COMPLETADA, 1)
            }
            db.update(
                DatabaseHelper.TABLE_INSPECCIONES,
                valoresInspeccion,
                "${DatabaseHelper.COL_INSPECCION_FECHA} = ? AND ${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
                arrayOf(tarea.fecha, tarea.dispositivoId.toString())
            )
            crearSiguienteRepeticion(tarea, inspeccionesRelacionadas)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun crearSiguienteRepeticion(tarea: Tarea, inspecciones: List<Inspeccion>) {
        val siguienteFechaTarea = calcularSiguienteFecha(tarea.fecha, tarea.repetirCada)
        val nuevaTareaId = if (siguienteFechaTarea != null) {
            insertarTarea(
                tarea.copy(
                    id = 0,
                    fecha = siguienteFechaTarea,
                    completada = false
                )
            )
        } else {
            0L
        }

        val nuevasInspecciones = inspecciones
            .mapNotNull { inspeccion ->
                val siguienteFechaInspeccion = calcularSiguienteFecha(inspeccion.fecha, inspeccion.repetirCada)
                    ?: return@mapNotNull null
                inspeccion.copy(
                    id = 0,
                    fecha = siguienteFechaInspeccion,
                    completada = false
                )
            }

        for (inspeccion in nuevasInspecciones) {
            insertarInspeccion(inspeccion)
        }

        if (nuevaTareaId > 0 && siguienteFechaTarea != null) {
            vincularInspeccionesATarea(
                nuevaTareaId,
                nuevasInspecciones.filter { it.fecha == siguienteFechaTarea }
            )
        }
    }

    private fun calcularSiguienteFecha(fecha: String, repetirCada: String): String? {
        return MaintenanceDateUtils.siguienteFecha(fecha, repetirCada)
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
            put(DatabaseHelper.COL_INSPECCION_COMPLETADA, if (inspeccion.completada) 1 else 0)
            put(DatabaseHelper.COL_INSPECCION_CONDICION, inspeccion.condicion)
            put(DatabaseHelper.COL_INSPECCION_NOTAS, inspeccion.notas)
            put(DatabaseHelper.COL_INSPECCION_FOTOS, inspeccion.fotos.joinToString(","))
            put(DatabaseHelper.COL_INSPECCION_FECHA_COMPLETADA, inspeccion.fechaCompletada)
        }
        val id = db.insert(DatabaseHelper.TABLE_INSPECCIONES, null, values)
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
                lista.add(c.convertirAInspeccion())
            }
        }
        return lista
    }

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ? AND ${DatabaseHelper.COL_INSPECCION_COMPLETADA} = 0",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(c.convertirAInspeccion())
            }
        }
        return lista
    }

    fun obtenerTodasInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(c.convertirAInspeccion())
            }
        }
        return lista
    }

    fun eliminarInspeccion(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_INSPECCIONES,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString())
        )
        return filas
    }

    fun actualizarInspeccion(inspeccion: Inspeccion): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_INSPECCION_NOMBRE, inspeccion.nombre)
            put(DatabaseHelper.COL_INSPECCION_DESCRIPCION, inspeccion.descripcion)
            put(DatabaseHelper.COL_INSPECCION_FECHA, inspeccion.fecha)
            put(DatabaseHelper.COL_INSPECCION_REPETIR, inspeccion.repetirCada)
            put(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID, inspeccion.dispositivoId)
            put(DatabaseHelper.COL_INSPECCION_COMPLETADA, if (inspeccion.completada) 1 else 0)
            put(DatabaseHelper.COL_INSPECCION_CONDICION, inspeccion.condicion)
            put(DatabaseHelper.COL_INSPECCION_NOTAS, inspeccion.notas)
            put(DatabaseHelper.COL_INSPECCION_FOTOS, inspeccion.fotos.joinToString(","))
            put(DatabaseHelper.COL_INSPECCION_FECHA_COMPLETADA, inspeccion.fechaCompletada)
        }
        return db.update(
            DatabaseHelper.TABLE_INSPECCIONES,
            values,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(inspeccion.id.toString())
        )
    }

    fun completarInspeccion(
        id: Long,
        condicion: String,
        notas: String,
        fotos: List<String>,
        fechaCompletada: String
    ): Int {
        val inspeccion = obtenerInspeccionPorId(id) ?: return 0
        if (inspeccion.completada) return 0
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_INSPECCION_COMPLETADA, 1)
            put(DatabaseHelper.COL_INSPECCION_CONDICION, condicion)
            put(DatabaseHelper.COL_INSPECCION_NOTAS, notas)
            put(DatabaseHelper.COL_INSPECCION_FOTOS, fotos.joinToString(","))
            put(DatabaseHelper.COL_INSPECCION_FECHA_COMPLETADA, fechaCompletada)
        }
        val filas = dbHelper.writableDatabase.update(
            DatabaseHelper.TABLE_INSPECCIONES,
            values,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString())
        )
        if (filas > 0) {
            calcularSiguienteFecha(inspeccion.fecha, inspeccion.repetirCada)?.let { siguienteFecha ->
                insertarInspeccion(
                    inspeccion.copy(
                        id = 0,
                        fecha = siguienteFecha,
                        completada = false,
                        condicion = "",
                        notas = "",
                        fotos = emptyList(),
                        fechaCompletada = null
                    )
                )
            }
        }
        return filas
    }

    fun guardarDispositivoConCalendario(
        dispositivo: Dispositivo,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val dispositivoId = insertar(dispositivo)
            check(dispositivoId > 0) { "No se pudo guardar el dispositivo" }
            guardarCalendarioInterno(dispositivoId, tareas, inspecciones)
            db.setTransactionSuccessful()
            return dispositivoId
        } finally {
            db.endTransaction()
        }
    }

    fun guardarCalendario(
        dispositivoId: Long,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            guardarCalendarioInterno(dispositivoId, tareas, inspecciones)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun guardarCalendarioInterno(
        dispositivoId: Long,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ) {
        val inspeccionesGuardadas = inspecciones.map { it.copy(dispositivoId = dispositivoId) }
        for (inspeccion in inspeccionesGuardadas) {
            insertarInspeccion(inspeccion)
        }
        for (tarea in tareas) {
            val tareaGuardada = tarea.copy(dispositivoId = dispositivoId)
            val tareaId = insertarTarea(tareaGuardada)
            check(tareaId > 0) { "No se pudo guardar el mantenimiento" }
            val inspeccionesParaTarea = inspeccionesGuardadas.filter { it.fecha == tareaGuardada.fecha }
                .ifEmpty { if (tareas.size == 1) inspeccionesGuardadas else emptyList() }
            vincularInspeccionesATarea(tareaId, inspeccionesParaTarea)
        }
    }

    fun guardarEdicionCalendario(
        dispositivoId: Long,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>,
        tareasEliminadas: Set<Long>,
        inspeccionesEliminadas: Set<Long>
    ) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            tareasEliminadas.forEach(::eliminarTarea)
            inspeccionesEliminadas.forEach(::eliminarInspeccion)

            val inspeccionesGuardadas = inspecciones.map { it.copy(dispositivoId = dispositivoId) }
            for (inspeccion in inspeccionesGuardadas) {
                if (inspeccion.id > 0) actualizarInspeccion(inspeccion) else insertarInspeccion(inspeccion)
            }

            val tareasGuardadas = tareas.map { it.copy(dispositivoId = dispositivoId) }
            for (tarea in tareasGuardadas) {
                val tareaGuardada = if (tarea.id > 0) {
                    actualizarTarea(tarea)
                    tarea
                } else {
                    tarea.copy(id = insertarTarea(tarea))
                }
                check(tareaGuardada.id > 0) { "No se pudo guardar el mantenimiento" }
                val inspeccionesParaTarea = inspeccionesGuardadas.filter { it.fecha == tareaGuardada.fecha }
                    .ifEmpty { if (tareasGuardadas.size == 1) inspeccionesGuardadas else emptyList() }
                sincronizarDetallesDeTarea(tareaGuardada, inspeccionesParaTarea)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun obtenerInspeccionPorId(id: Long): Inspeccion? {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor.use { c ->
            return if (c.moveToFirst()) c.convertirAInspeccion() else null
        }
    }

    private fun Cursor.convertirAInspeccion(): Inspeccion {
        val fotosGuardadas = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FOTOS)).orEmpty()
        return Inspeccion(
            id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
            nombre = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
            descripcion = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
            fecha = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
            repetirCada = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
            dispositivoId = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)),
            completada = getInt(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_COMPLETADA)) == 1,
            condicion = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_CONDICION)).orEmpty(),
            notas = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOTAS)).orEmpty(),
            fotos = fotosGuardadas.takeIf { it.isNotBlank() }?.split(",").orEmpty(),
            fechaCompletada = getString(getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA_COMPLETADA))
        )
    }

    // ==================== TAREA DETALLES ====================

    fun obtenerDetallesPorTarea(tareaId: Long): List<TareaDetalle> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaDetalle>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            null,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null, null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val fotosStr = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_FOTOS)) ?: ""
                val fotos = if (fotosStr.isNotEmpty()) fotosStr.split(",") else emptyList()
                lista.add(
                    TareaDetalle(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_ID)),
                        tareaId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_TAREA_ID)),
                        tipo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_TIPO)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_DESCRIPCION)),
                        condicion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_CONDICION)),
                        notas = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_NOTAS)),
                        fotos = fotos,
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_COMPLETADA)) == 1,
                        fechaCompletada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA))
                    )
                )
            }
        }

        val tareaParaDetalle = obtenerTareaPorId(tareaId)
        if (lista.none { it.tipo == "mantenimiento" } && tareaParaDetalle != null) {
            val detalleId = insertarDetalleBaseDeTarea(tareaId, tareaParaDetalle.nombre, tareaParaDetalle.descripcion)
            if (detalleId > 0) {
                lista.add(
                    TareaDetalle(
                        id = detalleId,
                        tareaId = tareaId,
                        tipo = "mantenimiento",
                        nombre = tareaParaDetalle.nombre,
                        descripcion = tareaParaDetalle.descripcion
                    )
                )
            }
        }

        if (tareaParaDetalle != null) {
            val inspeccionesRelacionadas = obtenerInspeccionesRelacionadas(tareaParaDetalle)
            for (inspeccion in inspeccionesRelacionadas) {
                val yaExiste = lista.any {
                    it.tipo == "inspeccion" &&
                        it.nombre == inspeccion.nombre &&
                        it.descripcion == inspeccion.descripcion
                }
                if (!yaExiste) {
                    val detalleId = insertarDetalleInspeccionParaTarea(tareaId, inspeccion)
                    if (detalleId > 0) {
                        lista.add(
                            TareaDetalle(
                                id = detalleId,
                                tareaId = tareaId,
                                tipo = "inspeccion",
                                nombre = inspeccion.nombre,
                                descripcion = inspeccion.descripcion
                            )
                        )
                    }
                }
            }
        }
        return lista
    }

    private fun obtenerTareaPorId(tareaId: Long): Tarea? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
            }
        }
        return null
    }

    private fun obtenerInspeccionesRelacionadas(tarea: Tarea): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} = ? AND ${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ? AND ${DatabaseHelper.COL_INSPECCION_COMPLETADA} = 0",
            arrayOf(tarea.fecha, tarea.dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_NOMBRE} ASC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(c.convertirAInspeccion())
            }
        }
        return lista
    }

    fun actualizarTareaDetalle(detalle: TareaDetalle): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_DETALLE_CONDICION, detalle.condicion)
            put(DatabaseHelper.COL_DETALLE_NOTAS, detalle.notas)
            put(DatabaseHelper.COL_DETALLE_FOTOS, detalle.fotos.joinToString(","))
            put(DatabaseHelper.COL_DETALLE_COMPLETADA, if (detalle.completada) 1 else 0)
            put(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA, detalle.fechaCompletada)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            values,
            "${DatabaseHelper.COL_DETALLE_ID} = ?",
            arrayOf(detalle.id.toString())
        )
        return filas
    }

    // ==================== COMBINADO (TAREAS + INSPECCIONES) ====================

    private fun ejecutarUnionQuery(whereClause: String, whereArgs: Array<String>?): List<ItemProgramado> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<ItemProgramado>()
        val baseTareas = """
            SELECT t.${DatabaseHelper.COL_TAREA_ID} as id,
                   t.${DatabaseHelper.COL_TAREA_NOMBRE} as nombre,
                   t.${DatabaseHelper.COL_TAREA_DESCRIPCION} as descripcion,
                   t.${DatabaseHelper.COL_TAREA_FECHA} as fecha,
                   COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo,
                   'tarea' as tipo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d
                ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
        """
        val baseInspecciones = """
            SELECT i.${DatabaseHelper.COL_INSPECCION_ID} as id,
                   i.${DatabaseHelper.COL_INSPECCION_NOMBRE} as nombre,
                   i.${DatabaseHelper.COL_INSPECCION_DESCRIPCION} as descripcion,
                   i.${DatabaseHelper.COL_INSPECCION_FECHA} as fecha,
                   COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo,
                   'inspeccion' as tipo
            FROM ${DatabaseHelper.TABLE_INSPECCIONES} i
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d
                ON i.${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
        """
        val query = """
            $baseTareas
            WHERE $whereClause
            UNION ALL
            $baseInspecciones
            WHERE $whereClause
            ORDER BY fecha ASC
        """
        val argsDuplicados = whereArgs?.let { it + it }
        val cursor = db.rawQuery(query, argsDuplicados)
        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    ItemProgramado(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                        descripcion = c.getString(c.getColumnIndexOrThrow("descripcion")),
                        fecha = c.getString(c.getColumnIndexOrThrow("fecha")),
                        nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo")),
                        tipo = c.getString(c.getColumnIndexOrThrow("tipo"))
                    )
                )
            }
        }
        return lista.distinctBy { "${it.tipo}:${it.id}" }
    }

    fun obtenerItemsPasadas(): List<ItemProgramado> {
        val hoy = LocalDate.now().toString()
        return ejecutarUnionQuery(
            "fecha < ? AND completada = 0",
            arrayOf(hoy)
        )
    }

    fun obtenerItemsProximas(): List<ItemProgramado> {
        val hoy = LocalDate.now().toString()
        val en7 = LocalDate.now().plusDays(7).toString()
        return ejecutarUnionQuery(
            "fecha >= ? AND fecha < ? AND completada = 0",
            arrayOf(hoy, en7)
        )
    }

    fun obtenerItemsLejanas(): List<ItemProgramado> {
        val en7 = LocalDate.now().plusDays(7).toString()
        return ejecutarUnionQuery(
            "fecha >= ? AND completada = 0",
            arrayOf(en7)
        )
    }

    fun obtenerItemsCompletadas(): List<ItemProgramado> {
        return ejecutarUnionQuery(
            "completada = 1",
            null
        )
    }
}
