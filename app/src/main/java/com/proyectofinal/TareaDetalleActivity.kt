package com.proyectofinal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.proyectofinal.model.TareaItem
import com.proyectofinal.viewmodel.DispositivosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaDetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAREA_ID = "tarea_id"
        const val EXTRA_DISPOSITIVO_NOMBRE = "dispositivo_nombre"
        const val EXTRA_TAREA_FECHA = "tarea_fecha"
        const val EXTRA_SOLO_LECTURA = "solo_lectura"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var contenedorItems: LinearLayout
    private lateinit var contenedorFotos: LinearLayout
    private lateinit var botonAgregarFoto: View
    private lateinit var botonGuardar: Button

    private var tareaId: Long = 0
    private var soloLectura = false
    private val itemsExistentes = mutableListOf<TareaItem>()
    private val fotosTemporales = mutableSetOf<String>()
    private val fotosSeleccionadas = mutableListOf<String>()

    private val resultadoCamara = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val ruta = fotosTemporales.lastOrNull() ?: return@registerForActivityResult
        if (resultado.resultCode == RESULT_OK && File(ruta).exists()) {
            agregarFotoPreview(ruta)
        } else {
            fotosTemporales.remove(ruta)
            File(ruta).delete()
        }
    }

    private val resultadoGaleria = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            val rutas = withContext(Dispatchers.IO) {
                uris.mapNotNull(::guardarFotoGaleria)
            }
            rutas.forEach(::agregarFotoPreview)
            if (rutas.size != uris.size) {
                Toast.makeText(this@TareaDetalleActivity, "Algunas fotos no se pudieron guardar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tarea_detalle)
        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        contenedorItems = findViewById(R.id.contenedor_detalles)
        contenedorFotos = findViewById(R.id.contenedor_fotos)
        botonAgregarFoto = findViewById(R.id.boton_agregar_foto)
        botonGuardar = findViewById(R.id.boton_guardar)

        tareaId = intent.getLongExtra(EXTRA_TAREA_ID, 0)
        soloLectura = intent.getBooleanExtra(EXTRA_SOLO_LECTURA, false)
        val dispositivo = intent.getStringExtra(EXTRA_DISPOSITIVO_NOMBRE).orEmpty().ifBlank { "Sin dispositivo" }
        val fecha = intent.getStringExtra(EXTRA_TAREA_FECHA).orEmpty()

        findViewById<TextView>(R.id.texto_nombre_tarea).text = "Tarea"
        findViewById<TextView>(R.id.texto_nombre_dispositivo).text = dispositivo
        findViewById<TextView>(R.id.texto_fecha).text = fecha

        configurarBotones()
        cargarTarea()
    }

    private fun cargarTarea() {
        if (tareaId <= 0) {
            Toast.makeText(this, "No se encontro la tarea", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val (items, fotos) = withContext(Dispatchers.IO) {
                Pair(
                    viewModel.obtenerItemsPorTarea(tareaId),
                    viewModel.obtenerFotosPorTarea(tareaId)
                )
            }
            itemsExistentes.clear()
            itemsExistentes.addAll(items)
            contenedorItems.removeAllViews()
            items.forEach { item ->
                if (item.tipo == "mantenimiento") agregarMantenimiento(item) else agregarInspeccion(item)
            }
            if (items.isEmpty()) {
                contenedorItems.addView(TextView(this@TareaDetalleActivity).apply {
                    text = getString(R.string.sin_detalles)
                    setPadding(0, 16.dp(), 0, 16.dp())
                })
            }

            contenedorFotos.removeAllViews()
            fotosSeleccionadas.clear()
            fotos.forEach { agregarFotoPreview(it.ruta) }
        }
    }

    private fun agregarMantenimiento(item: TareaItem) {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_mantenimiento, contenedorItems, false)
        vista.findViewById<TextView>(R.id.texto_nombre_mantenimiento).text = item.nombre
        vista.findViewById<TextView>(R.id.texto_descripcion_mantenimiento).text = item.descripcion
        vista.findViewById<EditText>(R.id.campo_notas_mantenimiento).apply {
            setText(item.notas)
            isEnabled = !soloLectura
            if (soloLectura) hint = ""
        }
        vista.tag = item.id
        contenedorItems.addView(vista)
    }

    private fun agregarInspeccion(item: TareaItem) {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_inspeccion_detalle, contenedorItems, false)
        vista.findViewById<TextView>(R.id.texto_nombre_inspeccion).text = item.nombre
        vista.findViewById<TextView>(R.id.texto_descripcion_inspeccion).text = item.descripcion
        vista.findViewById<EditText>(R.id.campo_notas_inspeccion).apply {
            setText(item.notas)
            isEnabled = !soloLectura
            if (soloLectura) hint = ""
        }

        val bueno = vista.findViewById<Button>(R.id.boton_bueno)
        val regular = vista.findViewById<Button>(R.id.boton_regular)
        val malo = vista.findViewById<Button>(R.id.boton_malo)
        listOf(bueno, regular, malo).forEach { it.alpha = 0.5f }
        when (item.condicion) {
            "bueno" -> seleccionarCondicion(bueno, regular, malo)
            "regular" -> seleccionarCondicion(regular, bueno, malo)
            "malo" -> seleccionarCondicion(malo, bueno, regular)
        }
        if (soloLectura) {
            listOf(bueno, regular, malo).forEach { it.isEnabled = false }
        } else {
            bueno.setOnClickListener { seleccionarCondicion(bueno, regular, malo) }
            regular.setOnClickListener { seleccionarCondicion(regular, bueno, malo) }
            malo.setOnClickListener { seleccionarCondicion(malo, bueno, regular) }
        }
        vista.tag = item.id
        contenedorItems.addView(vista)
    }

    private fun seleccionarCondicion(seleccionado: Button, otro1: Button, otro2: Button) {
        seleccionado.alpha = 1f
        otro1.alpha = 0.5f
        otro2.alpha = 0.5f
    }

    private fun configurarBotones() {
        if (soloLectura) {
            botonAgregarFoto.visibility = View.GONE
            botonGuardar.text = getString(R.string.cerrar_boton)
            botonGuardar.setOnClickListener { finish() }
            return
        }
        botonAgregarFoto.setOnClickListener { mostrarOpcionesFoto() }
        botonGuardar.setOnClickListener { completarTarea() }
    }

    private fun mostrarOpcionesFoto() {
        AlertDialog.Builder(this)
            .setTitle("Agregar fotos")
            .setItems(arrayOf("Camara", "Galeria")) { _, opcion ->
                if (opcion == 0) abrirCamara() else resultadoGaleria.launch("image/*")
            }
            .show()
    }

    private fun abrirCamara() {
        val carpeta = File(filesDir, "tarea_fotos").apply { mkdirs() }
        val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        fotosTemporales.add(archivo.absolutePath)
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivo)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            resultadoCamara.launch(intent)
        } catch (_: ActivityNotFoundException) {
            fotosTemporales.remove(archivo.absolutePath)
            archivo.delete()
            Toast.makeText(this, "No hay una aplicacion de camara disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarFotoGaleria(uri: Uri): String? {
        return runCatching {
            val carpeta = File(filesDir, "tarea_fotos").apply { mkdirs() }
            val archivo = File(carpeta, "foto_${System.nanoTime()}.jpg")
            contentResolver.openInputStream(uri)?.use { entrada ->
                archivo.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: return null
            fotosTemporales.add(archivo.absolutePath)
            archivo.absolutePath
        }.getOrNull()
    }

    private fun agregarFotoPreview(ruta: String) {
        if (ruta in fotosSeleccionadas) return
        fotosSeleccionadas.add(ruta)
        val vista = LayoutInflater.from(this).inflate(R.layout.item_foto_preview, contenedorFotos, false)
        val imagen = vista.findViewById<ImageView>(R.id.imagen_preview)
        val eliminar = vista.findViewById<ImageButton>(R.id.boton_eliminar_foto)
        runCatching {
            val archivo = File(ruta)
            if (archivo.exists()) imagen.setImageBitmap(cargarBitmapReducido(ruta)) else imagen.setImageURI(ruta.toUri())
        }.onFailure { imagen.setImageResource(android.R.drawable.ic_menu_gallery) }

        if (soloLectura) {
            eliminar.visibility = View.GONE
        } else {
            eliminar.setOnClickListener {
                contenedorFotos.removeView(vista)
                fotosSeleccionadas.remove(ruta)
                if (fotosTemporales.remove(ruta)) File(ruta).delete()
            }
        }
        contenedorFotos.addView(vista)
    }

    private fun completarTarea() {
        val actualizados = mutableListOf<TareaItem>()
        for (i in 0 until contenedorItems.childCount) {
            val vista = contenedorItems.getChildAt(i)
            val itemId = vista.tag as? Long ?: continue
            val item = itemsExistentes.firstOrNull { it.id == itemId } ?: continue
            if (item.tipo == "mantenimiento") {
                val notas = vista.findViewById<EditText>(R.id.campo_notas_mantenimiento)?.text.toString().trim()
                actualizados.add(item.copy(notas = notas))
            } else {
                val condicion = condicionSeleccionada(vista)
                if (condicion.isBlank()) {
                    Toast.makeText(this, "Selecciona Bueno, Regular o Malo en cada inspeccion", Toast.LENGTH_SHORT).show()
                    return
                }
                val notas = vista.findViewById<EditText>(R.id.campo_notas_inspeccion)?.text.toString().trim()
                actualizados.add(item.copy(notas = notas, condicion = condicion))
            }
        }
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            viewModel.completarTarea(tareaId, actualizados, fotosSeleccionadas, fecha)
            fotosTemporales.clear()
            Toast.makeText(this@TareaDetalleActivity, "Tarea completada", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun condicionSeleccionada(vista: View): String {
        return when {
            vista.findViewById<Button>(R.id.boton_bueno)?.alpha == 1f -> "bueno"
            vista.findViewById<Button>(R.id.boton_regular)?.alpha == 1f -> "regular"
            vista.findViewById<Button>(R.id.boton_malo)?.alpha == 1f -> "malo"
            else -> ""
        }
    }

    private fun cargarBitmapReducido(ruta: String, maximo: Int = 512): android.graphics.Bitmap? {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, opciones)
        var escala = 1
        while (opciones.outWidth / escala > maximo || opciones.outHeight / escala > maximo) escala *= 2
        opciones.inJustDecodeBounds = false
        opciones.inSampleSize = escala
        return BitmapFactory.decodeFile(ruta, opciones)
    }

    override fun onDestroy() {
        if (isFinishing) {
            fotosTemporales.forEach { ruta -> File(ruta).delete() }
            fotosTemporales.clear()
        }
        super.onDestroy()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
