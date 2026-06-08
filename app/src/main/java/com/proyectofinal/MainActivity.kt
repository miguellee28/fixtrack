package com.proyectofinal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
        const val PREFS_TEMA_OSCURO = "tema_oscuro"
    }

    private val resultadoAgregarDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarDispositivos()
    }

    private val resultadoDetalleDispositivo = registerForActivityResult(
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
            viewModel.dispositivos.collect { lista ->
                val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
                if (listView != null) {
                    listView.adapter = AdaptadorDispositivos(this@MainActivity, lista)
                }
            }
        }
    }

    private fun cargarPreferencias() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val temaOscuro = prefs.getBoolean(PREFS_TEMA_OSCURO, false)

        if (temaOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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
                configurarClickLista()
                viewModel.cargarDispositivos()
                asignarListaDispositivos()
                true
            }
            ID_CALENDARIO -> {
                mostrarPantalla(R.layout.layout_calendario)
                true
            }
            ID_AJUSTES -> {
                mostrarPantalla(R.layout.layout_ajustes)
                configurarAjustes()
                true
            }
            else -> false
        }
    }

    private fun asignarListaDispositivos() {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        if (listView != null) {
            listView.adapter = AdaptadorDispositivos(this, viewModel.dispositivos.value)
        }
    }

    private fun configurarClickLista() {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, posicion, _ ->
            val dispositivo = viewModel.dispositivos.value.getOrNull(posicion) ?: return@OnItemClickListener
            val intent = Intent(this, DetalleDispositivoActivity::class.java).apply {
                putExtra(DetalleDispositivoActivity.EXTRA_ID, dispositivo.id)
                putExtra(DetalleDispositivoActivity.EXTRA_NOMBRE_DISPOSITIVO, dispositivo.nombre)
                putExtra(DetalleDispositivoActivity.EXTRA_CATEGORIA, dispositivo.categoria)
                putExtra(DetalleDispositivoActivity.EXTRA_MARCA, dispositivo.marca)
                putExtra(DetalleDispositivoActivity.EXTRA_MODELO, dispositivo.modelo)
            }
            resultadoDetalleDispositivo.launch(intent)
        }
    }

    private fun configurarAjustes() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val switchOscuro = contenedorPantallas.findViewById<SwitchCompat>(R.id.switch_modo_oscuro)

        if (switchOscuro != null) {
            switchOscuro.isChecked = prefs.getBoolean(PREFS_TEMA_OSCURO, false)
            switchOscuro.setOnCheckedChangeListener { _, activo ->
                prefs.edit().putBoolean(PREFS_TEMA_OSCURO, activo).apply()
                if (activo) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
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
            vista.findViewById<TextView>(R.id.texto_marca).text = dispositivo.marca
            vista.findViewById<TextView>(R.id.texto_modelo).text = dispositivo.modelo

            return vista
        }
    }
}
