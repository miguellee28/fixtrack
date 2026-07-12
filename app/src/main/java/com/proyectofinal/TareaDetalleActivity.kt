package com.proyectofinal

import com.proyectofinal.model.*
import com.proyectofinal.viewmodel.DispositivosViewModel
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
        const val EXTRA_TAREA_IDS = "tarea_ids"
        const val EXTRA_DISPOSITIVO_NOMBRE = "dispositivo_nombre"
        const val EXTRA_TAREA_FECHA = "tarea_fecha"
        const val EXTRA_SOLO_LECTURA = "solo_lectura"
        const val EXTRA_INSPECCION_NOMBRE = "inspeccion_nombre"
        const val EXTRA_INSPECCION_DESCRIPCION = "inspeccion_descripcion"
        const val EXTRA_INSPECCION_ID = "inspeccion_id"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var contenedorDetalles: LinearLayout
    private lateinit var contenedorFotos: LinearLayout
    private lateinit var botonAgregarFoto: View
    private lateinit var botonGuardar: Button

    private var tareaId: Long = 0
    private var tareaIds: List<Long> = emptyList()
    private val fotosTemporales = mutableListOf<String>()
    private val fotosSeleccionadas = mutableListOf<String>()
    private val detallesExistentes = mutableListOf<TareaDetalle>()
    private var soloLectura: Boolean = false
    private var inspeccionDirecta: TareaDetalle? = null
    private var inspeccionDirectaId: Long = 0

    private val resultadoCamara = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val uri = resultado.data?.data
            if (uri != null) {
                agregarFotoPreview(uri.toString())
            } else {
                val archivo = File(fotosTemporales.lastOrNull() ?: return@registerForActivityResult)
                if (archivo.exists()) {
                    agregarFotoPreview(archivo.absolutePath)
                }
            }
        } else {
            fotosTemporales.removeLastOrNull()?.let { ruta -> File(ruta).delete() }
        }
    }

    private val resultadoGaleria = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val uri = resultado.data?.data
            uri?.let {
                lifecycleScope.launch {
                    val ruta = withContext(Dispatchers.IO) { guardarFotoGaleria(it) }
                    if (ruta != null) {
                        agregarFotoPreview(ruta)
                    } else {
                        Toast.makeText(this@TareaDetalleActivity, "No se pudo guardar la foto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tarea_detalle)

        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        contenedorDetalles = findViewById(R.id.contenedor_detalles)
        contenedorFotos = findViewById(R.id.contenedor_fotos)
        botonAgregarFoto = findViewById(R.id.boton_agregar_foto)
        botonGuardar = findViewById(R.id.boton_guardar)

        tareaId = intent.getLongExtra(EXTRA_TAREA_ID, 0)
        tareaIds = intent.getLongArrayExtra(EXTRA_TAREA_IDS)?.toList()
            ?.filter { it > 0 }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(tareaId).filter { it > 0 }
        soloLectura = intent.getBooleanExtra(EXTRA_SOLO_LECTURA, false)
        val dispositivoNombre = intent.getStringExtra(EXTRA_DISPOSITIVO_NOMBRE) ?: ""
        val tareaFecha = intent.getStringExtra(EXTRA_TAREA_FECHA) ?: ""
        val nombreInspeccion = intent.getStringExtra(EXTRA_INSPECCION_NOMBRE).orEmpty()
        val descripcionInspeccion = intent.getStringExtra(EXTRA_INSPECCION_DESCRIPCION).orEmpty()
        inspeccionDirectaId = intent.getLongExtra(EXTRA_INSPECCION_ID, 0)

        if (nombreInspeccion.isNotBlank()) {
            inspeccionDirecta = TareaDetalle(
                tareaId = 0,
                tipo = "inspeccion",
                nombre = nombreInspeccion,
                descripcion = descripcionInspeccion
            )
        }

        findViewById<TextView>(R.id.texto_nombre_tarea).text = if (inspeccionDirecta != null) {
            "Inspección"
        } else {
            dispositivoNombre.ifBlank { "Sin dispositivo" }
        }
        findViewById<TextView>(R.id.texto_nombre_dispositivo).apply {
            visibility = if (inspeccionDirecta != null) View.VISIBLE else View.GONE
            text = dispositivoNombre.ifBlank { "Sin dispositivo" }
        }
        findViewById<TextView>(R.id.texto_fecha).text = tareaFecha

        cargarDetalles()
        configurarBotones()
    }

    private fun cargarDetalles() {
        lifecycleScope.launch {
            val detalles = if (inspeccionDirectaId > 0) {
                val inspeccion = viewModel.obtenerInspeccionPorId(inspeccionDirectaId)
                inspeccion?.let {
                    listOf(
                        TareaDetalle(
                            id = it.id,
                            tareaId = 0,
                            tipo = "inspeccion",
                            nombre = it.nombre,
                            descripcion = it.descripcion,
                            condicion = it.condicion,
                            notas = it.notas,
                            fotos = it.fotos,
                            completada = it.completada,
                            fechaCompletada = it.fechaCompletada
                        )
                    )
                } ?: inspeccionDirecta?.let { listOf(it) }.orEmpty()
            } else {
                withContext(Dispatchers.IO) {
                    tareaIds.flatMap { id -> viewModel.cargarDetallesPorTarea(id) }
                }
            }
            val detallesParaMostrar = detalles
                .distinctBy { claveVisualDetalle(it) }
                .sortedWith(compareBy<TareaDetalle> { ordenTipoDetalle(it.tipo) }.thenBy { it.nombre })
            detallesExistentes.clear()
            detallesExistentes.addAll(detalles)
            contenedorDetalles.removeAllViews()
            contenedorFotos.removeAllViews()
            fotosSeleccionadas.clear()

            for (detalle in detallesParaMostrar) {
                when (detalle.tipo) {
                    "mantenimiento" -> agregarTarjetaMantenimiento(detalle)
                    "inspeccion" -> agregarTarjetaInspeccion(detalle)
                }
            }

            detallesParaMostrar.flatMap { it.fotos }.distinct().forEach { ruta ->
                agregarFotoPreview(ruta)
            }

            if (detallesParaMostrar.isEmpty()) {
                val textoVacio = TextView(this@TareaDetalleActivity).apply {
                    text = getString(R.string.sin_detalles)
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    setPadding(0, 16.dp(), 0, 16.dp())
                }
                contenedorDetalles.addView(textoVacio)
            }
        }
    }

    private fun ordenTipoDetalle(tipo: String): Int {
        return when (tipo) {
            "mantenimiento" -> 0
            "inspeccion" -> 1
            else -> 2
        }
    }

    private fun claveVisualDetalle(detalle: TareaDetalle): String {
        return if (detalle.tipo == "inspeccion") {
            "${detalle.tipo}|${detalle.nombre}|${detalle.descripcion}"
        } else {
            "${detalle.tipo}|${detalle.tareaId}|${detalle.id}"
        }
    }

    private fun agregarTarjetaMantenimiento(detalle: TareaDetalle) {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_mantenimiento, contenedorDetalles, false)
        vista.findViewById<TextView>(R.id.texto_nombre_mantenimiento).text = detalle.nombre
        vista.findViewById<TextView>(R.id.texto_descripcion_mantenimiento).text = detalle.descripcion
        vista.findViewById<EditText>(R.id.campo_notas_mantenimiento).apply {
            setText(detalle.notas)
            if (soloLectura) hint = ""
            isEnabled = !soloLectura
        }
        vista.tag = detalle.id
        contenedorDetalles.addView(vista)
    }

    private fun agregarTarjetaInspeccion(detalle: TareaDetalle) {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_inspeccion_detalle, contenedorDetalles, false)
        vista.findViewById<TextView>(R.id.texto_nombre_inspeccion).text = detalle.nombre
        vista.findViewById<TextView>(R.id.texto_descripcion_inspeccion).text = detalle.descripcion
        vista.findViewById<EditText>(R.id.campo_notas_inspeccion).apply {
            setText(detalle.notas)
            if (soloLectura) hint = ""
            isEnabled = !soloLectura
        }

        val botonBueno = vista.findViewById<Button>(R.id.boton_bueno)
        val botonRegular = vista.findViewById<Button>(R.id.boton_regular)
        val botonMalo = vista.findViewById<Button>(R.id.boton_malo)

        botonBueno.alpha = 0.5f
        botonRegular.alpha = 0.5f
        botonMalo.alpha = 0.5f

        when (detalle.condicion) {
            "bueno" -> seleccionarCondicion(botonBueno, botonRegular, botonMalo)
            "regular" -> seleccionarCondicion(botonRegular, botonBueno, botonMalo)
            "malo" -> seleccionarCondicion(botonMalo, botonBueno, botonRegular)
        }

        if (soloLectura) {
            botonBueno.isEnabled = false
            botonRegular.isEnabled = false
            botonMalo.isEnabled = false
        } else {
            botonBueno.setOnClickListener { seleccionarCondicion(botonBueno, botonRegular, botonMalo) }
            botonRegular.setOnClickListener { seleccionarCondicion(botonRegular, botonBueno, botonMalo) }
            botonMalo.setOnClickListener { seleccionarCondicion(botonMalo, botonBueno, botonRegular) }
        }

        vista.tag = detalle.id
        contenedorDetalles.addView(vista)
    }

    private fun seleccionarCondicion(seleccionado: Button, otro1: Button, otro2: Button) {
        seleccionado.alpha = 1.0f
        otro1.alpha = 0.5f
        otro2.alpha = 0.5f
    }

    private fun configurarBotones() {
        if (soloLectura) {
            botonAgregarFoto.visibility = View.GONE
            botonGuardar.text = getString(R.string.cerrar_boton)
            botonGuardar.setOnClickListener {
                setResult(RESULT_OK)
                finish()
            }
            return
        }

        botonAgregarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        botonGuardar.setOnClickListener {
            guardarDetalles()
        }
    }

    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("Camara", "Galeria")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Agregar Foto")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> abrirGaleria()
                }
            }
            .show()
    }

    private fun abrirCamara() {
        val carpetaFotos = File(filesDir, "detalle_fotos").apply { mkdirs() }
        val archivoFoto = File(carpetaFotos, "foto_${System.currentTimeMillis()}.jpg")
        fotosTemporales.add(archivoFoto.absolutePath)

        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivoFoto)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            resultadoCamara.launch(intent)
        } catch (_: ActivityNotFoundException) {
            fotosTemporales.remove(archivoFoto.absolutePath)
            archivoFoto.delete()
            Toast.makeText(this, "No hay una aplicación de cámara disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultadoGaleria.launch(intent)
    }

    private fun guardarFotoGaleria(uri: Uri): String? {
        return runCatching {
            val carpetaFotos = File(filesDir, "detalle_fotos").apply { mkdirs() }
            val archivoFoto = File(carpetaFotos, "foto_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { entrada ->
                archivoFoto.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: return null
            archivoFoto.absolutePath
        }.getOrNull()
    }

    private fun agregarFotoPreview(ruta: String) {
        if (!fotosSeleccionadas.contains(ruta)) {
            fotosSeleccionadas.add(ruta)
        }
        val vista = LayoutInflater.from(this).inflate(R.layout.item_foto_preview, contenedorFotos, false)
        val imagen = vista.findViewById<ImageView>(R.id.imagen_preview)
        val botonEliminar = vista.findViewById<ImageButton>(R.id.boton_eliminar_foto)

        try {
            val file = File(ruta)
            if (file.exists()) {
                imagen.setImageBitmap(cargarBitmapReducido(file.absolutePath))
            } else {
                val uri = ruta.toUri()
                imagen.setImageURI(uri)
            }
        } catch (e: Exception) {
            imagen.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (soloLectura) {
            botonEliminar.visibility = View.GONE
        } else {
            botonEliminar.setOnClickListener {
                contenedorFotos.removeView(vista)
                fotosTemporales.remove(ruta)
                fotosSeleccionadas.remove(ruta)
            }
        }

        contenedorFotos.addView(vista)
    }

    private fun cargarBitmapReducido(ruta: String, maximo: Int = 512): android.graphics.Bitmap? {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, opciones)
        var escala = 1
        while (opciones.outWidth / escala > maximo || opciones.outHeight / escala > maximo) {
            escala *= 2
        }
        opciones.inJustDecodeBounds = false
        opciones.inSampleSize = escala
        return BitmapFactory.decodeFile(ruta, opciones)
    }

    private fun guardarDetalles() {
        lifecycleScope.launch {
            if (inspeccionDirectaId > 0) {
                val vista = (0 until contenedorDetalles.childCount)
                    .map { contenedorDetalles.getChildAt(it) }
                    .firstOrNull { it.findViewById<TextView>(R.id.texto_nombre_inspeccion) != null }
                if (vista == null) {
                    Toast.makeText(this@TareaDetalleActivity, "No se encontró la inspección", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val condicion = condicionSeleccionada(vista)
                if (condicion.isBlank()) {
                    Toast.makeText(this@TareaDetalleActivity, "Selecciona Bueno, Regular o Malo", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val notas = vista.findViewById<EditText>(R.id.campo_notas_inspeccion)?.text.toString().trim()
                val fechaCompletada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                viewModel.completarInspeccion(
                    inspeccionDirectaId,
                    condicion,
                    notas,
                    fotosSeleccionadas.distinct(),
                    fechaCompletada
                )
                Toast.makeText(this@TareaDetalleActivity, "Inspección completada", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
                return@launch
            }

            for (i in 0 until contenedorDetalles.childCount) {
                val vista = contenedorDetalles.getChildAt(i)
                val detalleId = vista.tag as? Long ?: continue
                val detalleExistente = detallesExistentes.find { it.id == detalleId } ?: continue

                val nombreMantenimiento = vista.findViewById<TextView>(R.id.texto_nombre_mantenimiento)
                if (nombreMantenimiento != null) {
                    val notas = vista.findViewById<EditText>(R.id.campo_notas_mantenimiento)?.text.toString()
                    val fotos = (detalleExistente.fotos + fotosSeleccionadas).distinct()
                    val actualizado = detalleExistente.copy(notas = notas, fotos = fotos, completada = true, fechaCompletada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                    viewModel.actualizarTareaDetalle(actualizado)
                }

                val nombreInspeccion = vista.findViewById<TextView>(R.id.texto_nombre_inspeccion)
                if (nombreInspeccion != null) {
                    val notas = vista.findViewById<EditText>(R.id.campo_notas_inspeccion)?.text.toString()
                    val condicion = condicionSeleccionada(vista)
                    val fechaCompletada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val detallesDuplicados = detallesExistentes.filter {
                        it.tipo == "inspeccion" &&
                            it.nombre == detalleExistente.nombre &&
                            it.descripcion == detalleExistente.descripcion
                    }
                    for (detalleDuplicado in detallesDuplicados) {
                        val fotos = (detalleDuplicado.fotos + fotosSeleccionadas).distinct()
                        val actualizado = detalleDuplicado.copy(
                            notas = notas,
                            condicion = condicion,
                            fotos = fotos,
                            completada = true,
                            fechaCompletada = fechaCompletada
                        )
                        viewModel.actualizarTareaDetalle(actualizado)
                    }
                }
            }

            for (id in tareaIds) {
                viewModel.marcarTareaCompletada(id)
            }
            Toast.makeText(this@TareaDetalleActivity, "Tarea completada", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun condicionSeleccionada(vista: View): String {
        return when {
            vista.findViewById<Button>(R.id.boton_bueno)?.alpha == 1.0f -> "bueno"
            vista.findViewById<Button>(R.id.boton_regular)?.alpha == 1.0f -> "regular"
            vista.findViewById<Button>(R.id.boton_malo)?.alpha == 1.0f -> "malo"
            else -> ""
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
