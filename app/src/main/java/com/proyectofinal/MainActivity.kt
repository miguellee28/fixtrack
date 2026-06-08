package com.proyectofinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
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

        configurarBarra()
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
            mostrarPantalla(R.layout.layout_agregar_dispositivo)
            configurarToggleAgregar()
            configurarBotonesAgregar()
            mostrarDetalleEnLista()
        }
    }

    private fun configurarToggleAgregar() {
        val botonManual = contenedorPantallas.findViewById<Button>(R.id.opcion_manual)
        val botonIA = contenedorPantallas.findViewById<Button>(R.id.opcion_ia)

        if (botonManual == null || botonIA == null) return

        botonManual.setOnClickListener {
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.white, theme))
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.black, theme))
            mostrarDetalleEnLista()
        }

        botonIA.setOnClickListener {
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.white, theme))
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.black, theme))
            ocultarDetalleEnLista()
        }
    }

    private fun configurarBotonesAgregar() {
        val botonTarea = contenedorPantallas.findViewById<Button>(R.id.boton_agregar_tarea)
        val botonInspeccion = contenedorPantallas.findViewById<Button>(R.id.boton_agregar_inspeccion)
        val botonAceptar = contenedorPantallas.findViewById<Button>(R.id.boton_aceptar)

        botonTarea?.setOnClickListener {
            Toast.makeText(this, "Agregar Tarea", Toast.LENGTH_SHORT).show()
        }

        botonInspeccion?.setOnClickListener {
            Toast.makeText(this, "Agregar Inspección", Toast.LENGTH_SHORT).show()
        }

        botonAceptar?.setOnClickListener {
            Toast.makeText(this, "Aceptar", Toast.LENGTH_SHORT).show()
        }
    }

    // Muestra las tres tarjetas dentro del ListView
    private fun mostrarDetalleEnLista() {
        val listaAcciones = contenedorPantallas.findViewById<ListView>(R.id.lista_acciones) ?: return

        val layouts = listOf(
            R.layout.layout_detalle_dispositivo,
            R.layout.layout_tarea,
            R.layout.layout_inspeccion
        )

        val adaptador = object : BaseAdapter() {
            override fun getCount() = layouts.size
            override fun getItem(posicion: Int) = null
            override fun getItemId(posicion: Int) = 0L

            override fun getView(posicion: Int, vistaReciclada: View?, parent: ViewGroup): View {
                val vista = vistaReciclada ?: LayoutInflater.from(this@MainActivity)
                    .inflate(layouts[posicion], parent, false)
                return vista
            }
        }

        listaAcciones.adapter = adaptador
    }

    // Oculta la tarjeta de detalle del ListView
    private fun ocultarDetalleEnLista() {
        val listaAcciones = contenedorPantallas.findViewById<ListView>(R.id.lista_acciones) ?: return
        listaAcciones.adapter = null
    }
}
