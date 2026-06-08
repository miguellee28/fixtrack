package com.proyectofinal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var barraNavegacion: BottomNavigationView
    private lateinit var contenedorPantallas: FrameLayout

    companion object {
        private const val ID_INICIO = 1
        private const val ID_CALENDARIO = 2
        private const val ID_DISPOSITIVOS = 3
        private const val ID_AJUSTES = 4
        const val PREFS_NAME = "mis_preferencias"
        const val PREFS_USUARIO = "nombre_usuario"
        const val PREFS_TEMA_OSCURO = "tema_oscuro"
        const val PREFS_ORDEN = "orden_lista"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        barraNavegacion = findViewById(R.id.barra_navegacion)
        contenedorPantallas = findViewById(R.id.contenedor_pantallas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        cargarPreferencias()
        configurarBarra()
    }

    // Cargar preferencias al iniciar la app
    private fun cargarPreferencias() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString(PREFS_USUARIO, "Usuario")
        val temaOscuro = prefs.getBoolean(PREFS_TEMA_OSCURO, false)
        val orden = prefs.getInt(PREFS_ORDEN, 0)

        Toast.makeText(this, "Bienvenido: $nombreUsuario", Toast.LENGTH_SHORT).show()
    }

    // Guardar preferencias
    private fun guardarPreferencias(nombre: String, temaOscuro: Boolean, orden: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREFS_USUARIO, nombre)
            .putBoolean(PREFS_TEMA_OSCURO, temaOscuro)
            .putInt(PREFS_ORDEN, orden)
            .apply()
    }

    private fun configurarBarra() {
        barraNavegacion.isItemActiveIndicatorEnabled = false

        val menuBarra = barraNavegacion.menu
        agregarOpcion(menuBarra, ID_INICIO, R.drawable.ic_inicio, "Inicio")
        agregarOpcion(menuBarra, ID_CALENDARIO, R.drawable.ic_calendario, "Calendario")
        agregarOpcion(menuBarra, ID_DISPOSITIVOS, R.drawable.ic_dispositivos, "Dispositivos")
        agregarOpcion(menuBarra, ID_AJUSTES, R.drawable.ic_ajustes, "Ajustes")
        barraNavegacion.selectedItemId = ID_INICIO

        barraNavegacion.setOnItemSelectedListener { elemento ->
            procesarSeleccion(elemento)
        }
    }

    private fun agregarOpcion(menu: Menu, id: Int, icono: Int, titulo: String) {
        menu.add(Menu.NONE, id, Menu.NONE, titulo).setIcon(icono)
    }

    private fun procesarSeleccion(elemento: MenuItem): Boolean {
        return when (elemento.itemId) {
            ID_INICIO -> {
                mostrarPantalla(R.layout.layout_inicio)
                true
            }
            ID_DISPOSITIVOS -> {
                mostrarPantalla(R.layout.layout_dispositivos)
                configurarBotonAgregar()
                true
            }
            ID_CALENDARIO -> {
                mostrarPantalla(R.layout.layout_calendario)
                true
            }
            ID_AJUSTES -> {
                mostrarPantalla(R.layout.layout_ajustes)
                true
            }
            else -> false
        }
    }

    private fun mostrarPantalla(layout: Int) {
        contenedorPantallas.removeAllViews()
        LayoutInflater.from(this).inflate(layout, contenedorPantallas, true)
    }

    private fun configurarBotonAgregar() {
        val boton = contenedorPantallas.findViewById<Button>(R.id.boton_agregar)
        boton?.setOnClickListener {
            val intent = Intent(this, AgregarDispositivoActivity::class.java)
            intent.putExtra(AgregarDispositivoActivity.EXTRA_NOMBRE_DISPOSITIVO, "")
            intent.putExtra(AgregarDispositivoActivity.EXTRA_CATEGORIA, "")
            startActivity(intent)
        }
    }
}
