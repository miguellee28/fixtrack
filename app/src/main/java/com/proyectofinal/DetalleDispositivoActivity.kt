package com.proyectofinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
        const val EXTRA_ID = "id"
        const val EXTRA_NOMBRE_DISPOSITIVO = "nombre_dispositivo"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_MARCA = "marca"
        const val EXTRA_MODELO = "modelo"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var campoNombre: EditText
    private lateinit var campoMarca: EditText
    private lateinit var campoModelo: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var botonGuardar: Button

    private var dispositivoId: Long = 0

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
        spinnerCategoria = findViewById(R.id.spinner_categoria)
        botonGuardar = findViewById(R.id.boton_guardar)

        dispositivoId = intent.getLongExtra(EXTRA_ID, 0)
        campoNombre.setText(intent.getStringExtra(EXTRA_NOMBRE_DISPOSITIVO) ?: "")
        campoMarca.setText(intent.getStringExtra(EXTRA_MARCA) ?: "")
        campoModelo.setText(intent.getStringExtra(EXTRA_MODELO) ?: "")

        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""
        val categorias = resources.getStringArray(R.array.opciones_categoria)
        val indiceCategoria = categorias.indexOf(categoria)
        if (indiceCategoria >= 0) {
            spinnerCategoria.setSelection(indiceCategoria)
        }

        botonGuardar.setOnClickListener {
            val nombre = campoNombre.text.toString().trim()
            val marca = campoMarca.text.toString().trim()
            val modelo = campoModelo.text.toString().trim()
            val categoriaSel = spinnerCategoria.selectedItem.toString()

            if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dispositivo = Dispositivo(
                id = dispositivoId,
                nombre = nombre,
                categoria = categoriaSel,
                marca = marca,
                modelo = modelo
            )

            lifecycleScope.launch {
                viewModel.actualizarDispositivo(dispositivo)
                Toast.makeText(this@DetalleDispositivoActivity, "Dispositivo actualizado", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }
}
