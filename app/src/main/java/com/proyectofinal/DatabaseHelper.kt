package com.proyectofinal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "proyecto_final.db"
        const val DATABASE_VERSION = 1

        // Tabla Dispositivos
        const val TABLE_DISPOSITIVOS = "dispositivos"
        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_CATEGORIA = "categoria"
        const val COL_MARCA = "marca"
        const val COL_MODELO = "modelo"

        // Tabla Tareas
        const val TABLE_TAREAS = "tareas"
        const val COL_TAREA_ID = "id"
        const val COL_TAREA_NOMBRE = "nombre"
        const val COL_TAREA_DESCRIPCION = "descripcion"
        const val COL_TAREA_FECHA = "fecha"
        const val COL_TAREA_REPETIR = "repetir_cada"
        const val COL_TAREA_DISPOSITIVO_ID = "dispositivo_id"

        // Tabla Inspecciones
        const val TABLE_INSPECCIONES = "inspecciones"
        const val COL_INSPECCION_ID = "id"
        const val COL_INSPECCION_NOMBRE = "nombre"
        const val COL_INSPECCION_DESCRIPCION = "descripcion"
        const val COL_INSPECCION_FECHA = "fecha"
        const val COL_INSPECCION_REPETIR = "repetir_cada"
        const val COL_INSPECCION_DISPOSITIVO_ID = "dispositivo_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla Dispositivos
        db.execSQL("""
            CREATE TABLE $TABLE_DISPOSITIVOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CATEGORIA TEXT NOT NULL,
                $COL_MARCA TEXT NOT NULL,
                $COL_MODELO TEXT NOT NULL
            )
        """)

        // Crear tabla Tareas
        db.execSQL("""
            CREATE TABLE $TABLE_TAREAS (
                $COL_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAREA_NOMBRE TEXT NOT NULL,
                $COL_TAREA_DESCRIPCION TEXT NOT NULL,
                $COL_TAREA_FECHA TEXT NOT NULL,
                $COL_TAREA_REPETIR TEXT NOT NULL,
                $COL_TAREA_DISPOSITIVO_ID INTEGER,
                FOREIGN KEY ($COL_TAREA_DISPOSITIVO_ID) REFERENCES $TABLE_DISPOSITIVOS($COL_ID)
            )
        """)

        // Crear tabla Inspecciones
        db.execSQL("""
            CREATE TABLE $TABLE_INSPECCIONES (
                $COL_INSPECCION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INSPECCION_NOMBRE TEXT NOT NULL,
                $COL_INSPECCION_DESCRIPCION TEXT NOT NULL,
                $COL_INSPECCION_FECHA TEXT NOT NULL,
                $COL_INSPECCION_REPETIR TEXT NOT NULL,
                $COL_INSPECCION_DISPOSITIVO_ID INTEGER,
                FOREIGN KEY ($COL_INSPECCION_DISPOSITIVO_ID) REFERENCES $TABLE_DISPOSITIVOS($COL_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INSPECCIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DISPOSITIVOS")
        onCreate(db)
    }
}
