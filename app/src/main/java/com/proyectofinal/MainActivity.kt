package com.proyectofinal

import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var barraNavegacion: BottomNavigationView
    private lateinit var contenedorPantallas: FrameLayout
    private lateinit var viewModel: DispositivosViewModel

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

    private val resultadoAgregarDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarDispositivos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        barraNavegacion = findViewById(R.id.barra_navegacion)
        contenedorPantallas = findViewById(R.id.contenedor_pantallas)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        cargarPreferencias()
        configurarBarra()
        observarDispositivos()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarDispositivos()
    }

    private fun observarDispositivos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dispositivos.collect { lista ->
                    val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
                    listView?.adapter = AdaptadorDispositivos(this@MainActivity, lista)
                }
            }
        }
    }

    private fun cargarPreferencias() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString(PREFS_USUARIO, "Usuario")
        Toast.makeText(this, "Bienvenido: $nombreUsuario", Toast.LENGTH_SHORT).show()
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
                viewModel.cargarDispositivos()
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
            resultadoAgregarDispositivo.launch(intent)
        }
    }

    class AdaptadorDispositivos(
        private val contexto: Context,
        private val dispositivos: List<Dispositivo>
    ) : BaseAdapter() {

        override fun getCount() = dispositivos.size
        override fun getItem(posicion: Int) = dispositivos[posicion]
        override fun getItemId(posicion: Int) = dispositivos[posicion].id

        override fun getView(posicion: Int, vistaReciclada: View?, parent: ViewGroup): View {
            val vista = vistaReciclada ?: LayoutInflater.from(contexto)
                .inflate(R.layout.item_dispositivo_lista, parent, false)

            val dispositivo = dispositivos[posicion]
            vista.findViewById<TextView>(R.id.texto_nombre).text = dispositivo.nombre

            return vista
        }
    }
}
