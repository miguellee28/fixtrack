package com.proyectofinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DetalleDispositivoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE_DISPOSITIVO = "nombre_dispositivo"
        const val EXTRA_CATEGORIA = "categoria"
    }

    private lateinit var viewModel: DispositivosViewModel

    private lateinit var campoNombre: EditText
    private lateinit var campoMarca: EditText
    private lateinit var campoModelo: EditText
    private lateinit var botonGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        campoNombre = findViewById(R.id.campo_nombre)
        campoMarca = findViewById(R.id.campo_marca)
        campoModelo = findViewById(R.id.campo_modelo)
        botonGuardar = findViewById(R.id.boton_guardar)

        // Recibe datos del Intent
        val nombre = intent.getStringExtra(EXTRA_NOMBRE_DISPOSITIVO) ?: ""
        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""

        campoNombre.setText(nombre)

        observarViewModel()

        botonGuardar.setOnClickListener {
            val nombreDispositivo = campoNombre.text.toString()
            val marca = campoMarca.text.toString()
            val modelo = campoModelo.text.toString()

            if (nombreDispositivo.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dispositivo = Dispositivo(
                nombre = nombreDispositivo,
                categoria = categoria,
                marca = marca,
                modelo = modelo
            )

            viewModel.insertarDispositivo(dispositivo)
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.mensaje.collect { msg ->
                msg?.let {
                    Toast.makeText(this@DetalleDispositivoActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                    finish()
                }
            }
        }
    }
}
