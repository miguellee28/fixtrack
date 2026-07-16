package com.proyectofinal.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "proyecto_final.db"
        const val DATABASE_VERSION = 6

        const val TABLE_DISPOSITIVOS = "dispositivos"
        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_CATEGORIA = "categoria"
        const val COL_MARCA = "marca"
        const val COL_MODELO = "modelo"
        const val COL_FOTO = "foto"

        const val TABLE_MANTENIMIENTOS = "mantenimientos"
        const val COL_MANTENIMIENTO_ID = "id"
        const val COL_MANTENIMIENTO_NOMBRE = "nombre"
        const val COL_MANTENIMIENTO_DESCRIPCION = "descripcion"
        const val COL_MANTENIMIENTO_FECHA = "fecha"
        const val COL_MANTENIMIENTO_REPETIR = "repetir_cada"
        const val COL_MANTENIMIENTO_DISPOSITIVO_ID = "dispositivo_id"

        const val TABLE_INSPECCIONES = "inspecciones"
        const val COL_INSPECCION_ID = "id"
        const val COL_INSPECCION_NOMBRE = "nombre"
        const val COL_INSPECCION_DESCRIPCION = "descripcion"
        const val COL_INSPECCION_FECHA = "fecha"
        const val COL_INSPECCION_REPETIR = "repetir_cada"
        const val COL_INSPECCION_DISPOSITIVO_ID = "dispositivo_id"

        const val TABLE_TAREAS = "tareas"
        const val COL_TAREA_ID = "id"
        const val COL_TAREA_DISPOSITIVO_ID = "dispositivo_id"
        const val COL_TAREA_FECHA = "fecha"
        const val COL_TAREA_COMPLETADA = "completada"
        const val COL_TAREA_FECHA_COMPLETADA = "fecha_completada"

        const val TABLE_TAREA_ITEMS = "tarea_items"
        const val COL_ITEM_ID = "id"
        const val COL_ITEM_TAREA_ID = "tarea_id"
        const val COL_ITEM_TIPO = "tipo"
        const val COL_ITEM_MANTENIMIENTO_ID = "mantenimiento_id"
        const val COL_ITEM_INSPECCION_ID = "inspeccion_id"
        const val COL_ITEM_NOTAS = "notas"
        const val COL_ITEM_CONDICION = "condicion"

        const val TABLE_TAREA_FOTOS = "tarea_fotos"
        const val COL_TAREA_FOTO_ID = "id"
        const val COL_TAREA_FOTO_TAREA_ID = "tarea_id"
        const val COL_TAREA_FOTO_RUTA = "ruta"
        const val COL_TAREA_FOTO_FECHA = "fecha_creacion"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_DISPOSITIVOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CATEGORIA TEXT NOT NULL,
                $COL_MARCA TEXT NOT NULL,
                $COL_MODELO TEXT NOT NULL,
                $COL_FOTO TEXT NOT NULL DEFAULT ''
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_MANTENIMIENTOS (
                $COL_MANTENIMIENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MANTENIMIENTO_NOMBRE TEXT NOT NULL,
                $COL_MANTENIMIENTO_DESCRIPCION TEXT NOT NULL,
                $COL_MANTENIMIENTO_FECHA TEXT NOT NULL,
                $COL_MANTENIMIENTO_REPETIR TEXT NOT NULL,
                $COL_MANTENIMIENTO_DISPOSITIVO_ID INTEGER,
                FOREIGN KEY ($COL_MANTENIMIENTO_DISPOSITIVO_ID)
                    REFERENCES $TABLE_DISPOSITIVOS($COL_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INSPECCIONES (
                $COL_INSPECCION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INSPECCION_NOMBRE TEXT NOT NULL,
                $COL_INSPECCION_DESCRIPCION TEXT NOT NULL,
                $COL_INSPECCION_FECHA TEXT NOT NULL,
                $COL_INSPECCION_REPETIR TEXT NOT NULL,
                $COL_INSPECCION_DISPOSITIVO_ID INTEGER,
                FOREIGN KEY ($COL_INSPECCION_DISPOSITIVO_ID)
                    REFERENCES $TABLE_DISPOSITIVOS($COL_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_TAREAS (
                $COL_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAREA_DISPOSITIVO_ID INTEGER,
                $COL_TAREA_FECHA TEXT NOT NULL,
                $COL_TAREA_COMPLETADA INTEGER NOT NULL DEFAULT 0,
                $COL_TAREA_FECHA_COMPLETADA TEXT,
                FOREIGN KEY ($COL_TAREA_DISPOSITIVO_ID)
                    REFERENCES $TABLE_DISPOSITIVOS($COL_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_TAREA_ITEMS (
                $COL_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ITEM_TAREA_ID INTEGER NOT NULL,
                $COL_ITEM_TIPO TEXT NOT NULL CHECK ($COL_ITEM_TIPO IN ('mantenimiento', 'inspeccion')),
                $COL_ITEM_MANTENIMIENTO_ID INTEGER,
                $COL_ITEM_INSPECCION_ID INTEGER,
                $COL_ITEM_NOTAS TEXT NOT NULL DEFAULT '',
                $COL_ITEM_CONDICION TEXT NOT NULL DEFAULT '',
                CHECK (
                    ($COL_ITEM_TIPO = 'mantenimiento' AND $COL_ITEM_MANTENIMIENTO_ID IS NOT NULL AND $COL_ITEM_INSPECCION_ID IS NULL)
                    OR
                    ($COL_ITEM_TIPO = 'inspeccion' AND $COL_ITEM_INSPECCION_ID IS NOT NULL AND $COL_ITEM_MANTENIMIENTO_ID IS NULL)
                ),
                FOREIGN KEY ($COL_ITEM_TAREA_ID)
                    REFERENCES $TABLE_TAREAS($COL_TAREA_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_ITEM_MANTENIMIENTO_ID)
                    REFERENCES $TABLE_MANTENIMIENTOS($COL_MANTENIMIENTO_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_ITEM_INSPECCION_ID)
                    REFERENCES $TABLE_INSPECCIONES($COL_INSPECCION_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_TAREA_FOTOS (
                $COL_TAREA_FOTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAREA_FOTO_TAREA_ID INTEGER NOT NULL,
                $COL_TAREA_FOTO_RUTA TEXT NOT NULL,
                $COL_TAREA_FOTO_FECHA TEXT NOT NULL,
                UNIQUE ($COL_TAREA_FOTO_TAREA_ID, $COL_TAREA_FOTO_RUTA),
                FOREIGN KEY ($COL_TAREA_FOTO_TAREA_ID)
                    REFERENCES $TABLE_TAREAS($COL_TAREA_ID) ON DELETE CASCADE
            )
        """)

        db.execSQL("CREATE INDEX idx_mantenimientos_dispositivo_fecha ON $TABLE_MANTENIMIENTOS($COL_MANTENIMIENTO_DISPOSITIVO_ID, $COL_MANTENIMIENTO_FECHA)")
        db.execSQL("CREATE INDEX idx_inspecciones_dispositivo_fecha ON $TABLE_INSPECCIONES($COL_INSPECCION_DISPOSITIVO_ID, $COL_INSPECCION_FECHA)")
        db.execSQL("CREATE INDEX idx_tareas_dispositivo_fecha ON $TABLE_TAREAS($COL_TAREA_DISPOSITIVO_ID, $COL_TAREA_FECHA)")
        db.execSQL("CREATE INDEX idx_tarea_items_tarea ON $TABLE_TAREA_ITEMS($COL_ITEM_TAREA_ID)")
        db.execSQL("CREATE INDEX idx_tarea_fotos_tarea ON $TABLE_TAREA_FOTOS($COL_TAREA_FOTO_TAREA_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREA_FOTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREA_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MANTENIMIENTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INSPECCIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DISPOSITIVOS")
        onCreate(db)
    }
}
