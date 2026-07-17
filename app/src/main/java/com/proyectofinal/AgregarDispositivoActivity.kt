package com.proyectofinal

import com.proyectofinal.model.*
import com.proyectofinal.viewmodel.DispositivosViewModel
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgregarDispositivoActivity : AppCompatActivity() {

    private lateinit var viewModel: DispositivosViewModel

    private lateinit var botonManual: Button
    private lateinit var botonIA: Button
    private lateinit var contenedorTarjetas: LinearLayout
    private lateinit var contenedorBotonesAgregar: LinearLayout
    private lateinit var botonAceptar: Button

    private var modoIA = false
    private var fotoIaUri: Uri? = null
    private var fotoIaArchivo: File? = null
    private var textoEstadoIA: TextView? = null
    private var textoFuentesIA: TextView? = null
    private var botonBuscarIA: Button? = null
    private var imagenDispositivoIA: ImageView? = null
    private val aiService by lazy {
        MaintenanceAiService(
            GoogleGenAiClient(BuildConfig.GOOGLE_GENAI_API_KEY),
            BraveSearchClient(BuildConfig.BRAVE_SEARCH_API_KEY)
        )
    }

    private val resultadoGaleriaIA = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            resultado.data?.data?.let { uri ->
                lifecycleScope.launch {
                    fotoIaUri = withContext(Dispatchers.IO) { guardarFotoDispositivo(uri) }
                    if (fotoIaUri != null) {
                        imagenDispositivoIA?.setImageURI(fotoIaUri)
                        contenedorTarjetas.findViewById<View>(R.id.texto_foto_placeholder)?.visibility = View.GONE
                    } else {
                        Toast.makeText(this@AgregarDispositivoActivity, "No se pudo guardar la foto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val resultadoCamaraIA = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            fotoIaArchivo?.takeIf { it.exists() }?.let { archivo ->
                fotoIaUri = Uri.fromFile(archivo)
                imagenDispositivoIA?.setImageURI(fotoIaUri)
                contenedorTarjetas.findViewById<View>(R.id.texto_foto_placeholder)?.visibility = View.GONE
            }
        } else {
            fotoIaArchivo?.delete()
            fotoIaArchivo = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_dispositivo)

        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        botonManual = findViewById(R.id.opcion_manual)
        botonIA = findViewById(R.id.opcion_ia)
        contenedorTarjetas = findViewById(R.id.contenedor_tarjetas)
        contenedorBotonesAgregar = findViewById(R.id.contenedor_botones_agregar)
        botonAceptar = findViewById(R.id.boton_aceptar)

        configurarToggle()
        configurarBotones()
        mostrarTarjetas()
    }

    private fun configurarToggle() {
        botonManual.setOnClickListener {
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.white, theme))
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.text_primary, theme))
            modoIA = false
            fotoIaUri = null
            mostrarTarjetas()
        }

        botonIA.setOnClickListener {
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.white, theme))
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.text_primary, theme))
            modoIA = true
            mostrarDetalleDispositivoParaIA()
        }
    }

    private fun configurarBotones() {
        botonAceptar.setOnClickListener {
            val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre)?.text.toString().trim()
            val marca = contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.text.toString().trim()
            val modelo = contenedorTarjetas.findViewById<EditText>(R.id.campo_modelo)?.text.toString().trim()
            val categoria = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_categoria)?.selectedItem?.toString() ?: ""

            if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                Toast.makeText(this, "Complete nombre, marca y modelo del dispositivo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dispositivo = Dispositivo(nombre = nombre, categoria = categoria, marca = marca, modelo = modelo, foto = rutaFotoDispositivo())
            val mantenimientos = construirMantenimientos()
            val inspecciones = construirInspecciones()

            lifecycleScope.launch {
                runCatching {
                    viewModel.guardarDispositivoConCalendario(dispositivo, mantenimientos, inspecciones)
                }.onSuccess {
                    Toast.makeText(this@AgregarDispositivoActivity, "Dispositivo guardado", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }.onFailure {
                    Toast.makeText(this@AgregarDispositivoActivity, "No se pudo guardar el dispositivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun construirMantenimientos(): List<Mantenimiento> {
        val mantenimientos = mutableListOf<Mantenimiento>()

        for (i in 0 until contenedorTarjetas.childCount) {
            val vista = contenedorTarjetas.getChildAt(i)
            val nombreCampo = vista.findViewById<EditText>(R.id.campo_nombre_tarea)
            if (nombreCampo != null) {
                val nombre = nombreCampo.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_tarea)?.text.toString()
                    mantenimientos.add(Mantenimiento(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir))
                }
            }
        }

        return mantenimientos
    }

    private fun construirInspecciones(): List<Inspeccion> {
        val inspecciones = mutableListOf<Inspeccion>()

        for (i in 0 until contenedorTarjetas.childCount) {
            val vista = contenedorTarjetas.getChildAt(i)
            val nombreCampo = vista.findViewById<EditText>(R.id.campo_nombre_inspeccion)
            if (nombreCampo != null) {
                val nombre = nombreCampo.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir_inspeccion)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_inspeccion)?.text.toString()
                    inspecciones.add(Inspeccion(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir))
                }
            }
        }

        return inspecciones
    }


    private fun configurarCalendarioPlegable(
        vista: View,
        textoFechaId: Int
    ) {
        val textoFecha = vista.findViewById<TextView>(textoFechaId) ?: return
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaSeleccionada = Calendar.getInstance()

        textoFecha.text = formato.format(fechaSeleccionada.time)

        textoFecha.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    fechaSeleccionada.set(year, month, dayOfMonth)
                    textoFecha.text = formato.format(fechaSeleccionada.time)
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun configurarCalendarioTarea(vista: View) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_tarea
        )
    }

    private fun configurarCalendarioInspeccion(vista: View) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_inspeccion
        )
    }

    private fun mostrarTarjetas() {
        contenedorTarjetas.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.layout_detalle_dispositivo, contenedorTarjetas, true)
        configurarFotoDispositivoIA()

        val tarea = LayoutInflater.from(this).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
        configurarCalendarioTarea(tarea)
        configurarBotonEliminar(tarea)
        contenedorTarjetas.addView(tarea)

        val inspeccion = LayoutInflater.from(this).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
        configurarCalendarioInspeccion(inspeccion)
        configurarBotonEliminar(inspeccion)
        contenedorTarjetas.addView(inspeccion)

        agregarBotonesDinamicos()
    }

    private fun mostrarDetalleDispositivoParaIA() {
        contenedorTarjetas.removeAllViews()
        contenedorBotonesAgregar.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.layout_detalle_dispositivo, contenedorTarjetas, true)
        configurarFotoDispositivoIA()
        agregarControlesIA()
    }

    private fun configurarFotoDispositivoIA() {
        imagenDispositivoIA = contenedorTarjetas.findViewById(R.id.foto_dispositivo)
        imagenDispositivoIA?.setOnClickListener {
            mostrarOpcionesFotoIA()
        }
    }

    private fun agregarControlesIA() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.fondo_tarjeta)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4.dp(), 0, 4.dp(), 16.dp())
            }
        }

        panel.addView(TextView(this).apply {
            text = getString(R.string.asistente_ia)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8.dp())
        })

        panel.addView(TextView(this).apply {
            text = getString(R.string.foto_opcional_ia)
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            setPadding(0, 0, 0, 10.dp())
        })

        botonBuscarIA = Button(this).apply {
            text = getString(R.string.buscar_mantenimiento_ia)
            setAllCaps(false)
            textSize = 14f
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.fondo_boton_primario)
            minHeight = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp()
            )
            setOnClickListener { buscarMantenimientoConIA() }
        }
        panel.addView(botonBuscarIA)

        textoEstadoIA = TextView(this).apply {
            textSize = 13f
            setPadding(0, 10.dp(), 0, 4.dp())
            setTextColor(resources.getColor(R.color.text_secondary, theme))
        }
        panel.addView(textoEstadoIA)

        textoFuentesIA = TextView(this).apply {
            textSize = 12f
            setTextColor(resources.getColor(R.color.text_secondary, theme))
        }
        panel.addView(textoFuentesIA)
        contenedorTarjetas.addView(panel)
    }

    private fun buscarMantenimientoConIA() {
        val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre)?.text.toString().trim()
        val marca = contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.text.toString().trim()
        val modelo = contenedorTarjetas.findViewById<EditText>(R.id.campo_modelo)?.text.toString().trim()
        val categoria = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_categoria)?.selectedItem?.toString() ?: ""

        if (fotoIaUri == null && marca.isBlank() && modelo.isBlank()) {
            Toast.makeText(this, "Agrega una foto o escribe marca/modelo", Toast.LENGTH_SHORT).show()
            return
        }

        if (fotoIaUri != null && marca.isBlank() && modelo.isBlank()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Buscar solo con imagen")
                .setMessage("La identificacion usando solo una foto puede ser imprecisa. Para mejores resultados escribe al menos la marca o el modelo.")
                .setPositiveButton("Continuar con imagen") { _, _ -> ejecutarBusquedaMantenimientoConIA(nombre, categoria, marca, modelo) }
                .setNegativeButton("Llenar marca/modelo") { _, _ ->
                    contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.requestFocus()
                }
                .show()
            return
        }

        ejecutarBusquedaMantenimientoConIA(nombre, categoria, marca, modelo)
    }

    private fun ejecutarBusquedaMantenimientoConIA(
        nombre: String,
        categoria: String,
        marca: String,
        modelo: String
    ) {
        botonBuscarIA?.isEnabled = false
        textoEstadoIA?.text = getString(R.string.buscando_fuentes)
        textoFuentesIA?.text = ""

        lifecycleScope.launch {
            val resultado = runCatching {
                actualizarEstadoIA(getString(R.string.preparando_datos))
                val imageBytes = withContext(Dispatchers.IO) { leerFotoIA() }
                withContext(Dispatchers.IO) {
                    aiService.generateSchedule(nombre, categoria, marca, modelo, imageBytes) { mensaje ->
                        runOnUiThread { actualizarEstadoIA(mensaje) }
                    }
                }
            }

            botonBuscarIA?.isEnabled = true
            resultado
                .onSuccess {
                    actualizarEstadoIA(getString(R.string.aplicando_calendario))
                    aplicarResultadoIA(it)
                }
                .onFailure { error ->
                    val mensaje = when (error) {
                        is SocketTimeoutException -> "La solicitud tardo demasiado. Intenta con marca/modelo escritos o una foto mas clara."
                        else -> error.message ?: "Error desconocido"
                    }
                    textoEstadoIA?.text = getString(R.string.error_generar_calendario, mensaje)
                    Toast.makeText(this@AgregarDispositivoActivity, "No se pudo generar el calendario", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun actualizarEstadoIA(mensaje: String) {
        textoEstadoIA?.text = mensaje
    }

    private fun aplicarResultadoIA(resultado: AiMaintenanceResult) {
        val dispositivo = resultado.dispositivo
        contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre)?.setText(dispositivo.nombre)
        contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.setText(dispositivo.marca)
        contenedorTarjetas.findViewById<EditText>(R.id.campo_modelo)?.setText(dispositivo.modelo)
        seleccionarSpinnerPorTexto(contenedorTarjetas.findViewById(R.id.spinner_categoria), dispositivo.categoria)

        eliminarTarjetasProgramadas()
        resultado.mantenimientos.forEach { agregarTarjetaTareaDesdeIA(it) }
        resultado.inspecciones.forEach { agregarTarjetaInspeccionDesdeIA(it) }

        textoEstadoIA?.text = getString(R.string.calendario_generado)
        textoFuentesIA?.text = if (resultado.fuentes.isEmpty()) {
            ""
        } else {
            getString(R.string.fuentes_formato, resultado.fuentes.joinToString("\n") { fuente ->
                "- ${fuente.titulo.ifBlank { fuente.url }}\n  ${fuente.url}"
            })
        }
    }

    private fun eliminarTarjetasProgramadas() {
        var i = contenedorTarjetas.childCount - 1
        while (i >= 0) {
            val vista = contenedorTarjetas.getChildAt(i)
            val esTarea = vista.findViewById<EditText>(R.id.campo_nombre_tarea) != null
            val esInspeccion = vista.findViewById<EditText>(R.id.campo_nombre_inspeccion) != null
            if (esTarea || esInspeccion) {
                contenedorTarjetas.removeViewAt(i)
            }
            i--
        }
    }

    private fun agregarTarjetaTareaDesdeIA(tarea: Mantenimiento) {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
        configurarCalendarioTarea(vista, tarea.fecha)
        vista.findViewById<EditText>(R.id.campo_nombre_tarea)?.setText(tarea.nombre)
        vista.findViewById<EditText>(R.id.campo_descripcion)?.setText(tarea.descripcion)
        seleccionarSpinnerPorTexto(vista.findViewById(R.id.spinner_repetir), tarea.repetirCada)
        configurarBotonEliminar(vista)
        contenedorTarjetas.addView(vista)
    }

    private fun agregarTarjetaInspeccionDesdeIA(inspeccion: Inspeccion) {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
        configurarCalendarioInspeccion(vista, inspeccion.fecha)
        vista.findViewById<EditText>(R.id.campo_nombre_inspeccion)?.setText(inspeccion.nombre)
        vista.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.setText(inspeccion.descripcion)
        seleccionarSpinnerPorTexto(vista.findViewById(R.id.spinner_repetir_inspeccion), inspeccion.repetirCada)
        configurarBotonEliminar(vista)
        contenedorTarjetas.addView(vista)
    }

    private fun configurarCalendarioTarea(vista: View, fechaInicial: String?) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_tarea,
            fechaInicial
        )
    }

    private fun configurarCalendarioInspeccion(vista: View, fechaInicial: String?) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_inspeccion,
            fechaInicial
        )
    }

    private fun configurarCalendarioPlegable(
        vista: View,
        textoFechaId: Int,
        fechaInicial: String?
    ) {
        val textoFecha = vista.findViewById<TextView>(textoFechaId) ?: return
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaSeleccionada = Calendar.getInstance().apply {
            val fechaParseada = fechaInicial?.let { runCatching { formato.parse(it) }.getOrNull() }
            if (fechaParseada != null) {
                time = fechaParseada
            }
        }

        textoFecha.text = formato.format(fechaSeleccionada.time)

        textoFecha.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    fechaSeleccionada.set(year, month, dayOfMonth)
                    textoFecha.text = formato.format(fechaSeleccionada.time)
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun seleccionarSpinnerPorTexto(spinner: Spinner?, texto: String) {
        if (spinner == null || texto.isBlank()) return
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(texto, ignoreCase = true)) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun leerFotoIA(): ByteArray? {
        val uri = fotoIaUri ?: return null
        val bytes = if (uri.scheme == "file") {
            File(uri.path ?: return null).readBytes()
        } else {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
        return comprimirFotoIA(bytes ?: return null)
    }

    private fun rutaFotoDispositivo(): String {
        val uri = fotoIaUri ?: return ""
        return if (uri.scheme == "file") uri.path.orEmpty() else uri.toString()
    }

    private fun guardarFotoDispositivo(uri: Uri): Uri? {
        return runCatching {
            val carpetaFotos = File(filesDir, "dispositivo_fotos").apply { mkdirs() }
            val archivoFoto = File(carpetaFotos, "dispositivo_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { entrada ->
                archivoFoto.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: return null
            Uri.fromFile(archivoFoto)
        }.getOrNull()
    }

    private fun comprimirFotoIA(bytes: ByteArray): ByteArray {
        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)

        opciones.inSampleSize = calcularEscalaImagen(opciones.outWidth, opciones.outHeight, 1024)
        opciones.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) ?: return bytes
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 72, salida)
        bitmap.recycle()
        return salida.toByteArray()
    }

    private fun calcularEscalaImagen(ancho: Int, alto: Int, maximo: Int): Int {
        var escala = 1
        var anchoReducido = ancho
        var altoReducido = alto
        while (anchoReducido > maximo || altoReducido > maximo) {
            escala *= 2
            anchoReducido /= 2
            altoReducido /= 2
        }
        return escala
    }

    private fun mostrarOpcionesFotoIA() {
        val opciones = arrayOf("Camara", "Galeria")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Foto del dispositivo")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamaraIA()
                    1 -> abrirGaleriaIA()
                }
            }
            .show()
    }

    private fun abrirCamaraIA() {
        val carpetaFotos = File(filesDir, "dispositivo_ia").apply { mkdirs() }
        val archivoFoto = File(carpetaFotos, "dispositivo_${System.currentTimeMillis()}.jpg")
        fotoIaArchivo = archivoFoto
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivoFoto)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            resultadoCamaraIA.launch(intent)
        } catch (_: ActivityNotFoundException) {
            archivoFoto.delete()
            fotoIaArchivo = null
            Toast.makeText(this, "No hay una aplicación de cámara disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirGaleriaIA() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultadoGaleriaIA.launch(intent)
    }

    private fun agregarBotonesDinamicos() {
        contenedorBotonesAgregar.removeAllViews()

        val botonTarea = Button(this).apply {
            text = getString(R.string.agregar_mantenimiento)
            setAllCaps(false)
            textSize = 13f
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.fondo_boton_primario)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp()
            )
            params.topMargin = 8.dp()
            params.bottomMargin = 6.dp()
            layoutParams = params
            minHeight = 0
        }

        botonTarea.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_tarea, "Completa la tarea actual antes de agregar otra")) {
                return@setOnClickListener
            }
            val tarea = LayoutInflater.from(this@AgregarDispositivoActivity).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
            configurarCalendarioTarea(tarea)
            configurarBotonEliminar(tarea)
            contenedorTarjetas.addView(tarea)
        }

        val botonInspeccion = Button(this).apply {
            text = getString(R.string.agregar_inspeccion)
            setAllCaps(false)
            textSize = 13f
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.fondo_boton_primario)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp()
            )
            params.topMargin = 0
            params.bottomMargin = 6.dp()
            layoutParams = params
            minHeight = 0
        }

        botonInspeccion.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_inspeccion, "Completa la inspeccion actual antes de agregar otra")) {
                return@setOnClickListener
            }
            val inspeccion = LayoutInflater.from(this@AgregarDispositivoActivity).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
            configurarCalendarioInspeccion(inspeccion)
            configurarBotonEliminar(inspeccion)
            contenedorTarjetas.addView(inspeccion)
        }

        contenedorBotonesAgregar.addView(botonTarea)
        contenedorBotonesAgregar.addView(botonInspeccion)
    }

    private fun puedeAgregarTarjeta(campoNombreId: Int, mensaje: String): Boolean {
        for (i in 0 until contenedorTarjetas.childCount) {
            val campo = contenedorTarjetas.getChildAt(i).findViewById<EditText>(campoNombreId)
            if (campo != null && campo.text.toString().trim().isEmpty()) {
                campo.requestFocus()
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun configurarBotonEliminar(vista: View) {
        val boton = vista.findViewById<TextView>(R.id.boton_eliminar) ?: return
        boton.setOnClickListener {
            contenedorTarjetas.removeView(vista)
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

}
