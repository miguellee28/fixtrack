package com.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AgregarDispositivoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE_DISPOSITIVO = "nombre_dispositivo"
        const val EXTRA_CATEGORIA = "categoria"
    }

    private lateinit var viewModel: DispositivosViewModel

    private lateinit var botonManual: Button
    private lateinit var botonIA: Button
    private lateinit var listaAcciones: ListView
    private lateinit var botonAgregarTarea: Button
    private lateinit var botonAgregarInspeccion: Button
    private lateinit var botonAceptar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        botonManual = findViewById(R.id.opcion_manual)
        botonIA = findViewById(R.id.opcion_ia)
        listaAcciones = findViewById(R.id.lista_acciones)
        botonAgregarTarea = findViewById(R.id.boton_agregar_tarea)
        botonAgregarInspeccion = findViewById(R.id.boton_agregar_inspeccion)
        botonAceptar = findViewById(R.id.boton_aceptar)

        configurarToggle()
        configurarBotones()
        observarViewModel()
        mostrarDetalleEnLista()
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.mensaje.collect { msg ->
                msg?.let {
                    Toast.makeText(this@AgregarDispositivoActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                }
            }
        }
    }

    private fun configurarToggle() {
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

    private fun configurarBotones() {
        botonAgregarTarea.setOnClickListener {
            Toast.makeText(this, "Agregar Tarea", Toast.LENGTH_SHORT).show()
        }

        botonAgregarInspeccion.setOnClickListener {
            Toast.makeText(this, "Agregar Inspección", Toast.LENGTH_SHORT).show()
        }

        botonAceptar.setOnClickListener {
            val intent = Intent(this, DetalleDispositivoActivity::class.java)
            intent.putExtra(EXTRA_NOMBRE_DISPOSITIVO, "Nuevo Dispositivo")
            intent.putExtra(EXTRA_CATEGORIA, "Sin categoría")
            startActivity(intent)
        }
    }

    private fun mostrarDetalleEnLista() {
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
                val vista = vistaReciclada ?: LayoutInflater.from(this@AgregarDispositivoActivity)
                    .inflate(layouts[posicion], parent, false)
                return vista
            }
        }

        listaAcciones.adapter = adaptador
    }

    private fun ocultarDetalleEnLista() {
        listaAcciones.adapter = null
    }
}
