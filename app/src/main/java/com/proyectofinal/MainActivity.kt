package com.proyectofinal

import com.proyectofinal.model.*
import com.proyectofinal.viewmodel.DispositivosViewModel
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var barraNavegacion: BottomNavigationView
    private lateinit var contenedorPantallas: FrameLayout
    private lateinit var viewModel: DispositivosViewModel
    private var pantallaActual = ID_INICIO

    companion object {
        private const val ID_INICIO = 1
        private const val ID_CALENDARIO = 2
        private const val ID_DISPOSITIVOS = 3
        private const val ID_AJUSTES = 4
        private const val ESTADO_PANTALLA_ACTUAL = "pantalla_actual"
        const val PREFS_NAME = "mis_preferencias"
        const val PREFS_TEMA_OSCURO = "tema_oscuro"
    }

    private val resultadoAgregarDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recargarDatosPrincipales()
    }

    private val resultadoDetalleDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recargarDatosPrincipales()
    }

    private val resultadoAgregarTarea = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recargarDatosProgramados()
    }

    private val resultadoTareaDetalle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recargarDatosProgramados()
    }

    private val solicitudPermisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            MaintenanceNotificationScheduler.scheduleAll(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaActual = savedInstanceState?.getInt(ESTADO_PANTALLA_ACTUAL, ID_INICIO) ?: ID_INICIO
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        barraNavegacion = findViewById(R.id.barra_navegacion)
        contenedorPantallas = findViewById(R.id.contenedor_pantallas)

        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]
        solicitarPermisoNotificaciones()
        lifecycleScope.launch(Dispatchers.IO) {
            MaintenanceNotificationScheduler.scheduleAll(applicationContext)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        cargarPreferencias()
        configurarBarra()
        observarDispositivos()
        observarHomeData()
        observarCalendarioData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ESTADO_PANTALLA_ACTUAL, pantallaActual)
        super.onSaveInstanceState(outState)
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permiso = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permiso != PackageManager.PERMISSION_GRANTED) {
            solicitudPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        recargarDatosPrincipales()
    }

    private fun recargarDatosPrincipales() {
        viewModel.cargarDispositivos()
        recargarDatosProgramados()
    }

    private fun recargarDatosProgramados() {
        viewModel.cargarHomeData()
        viewModel.cargarCalendarioData()
    }

    private fun observarDispositivos() {
        lifecycleScope.launch {
            viewModel.dispositivos.collect { lista ->
                val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
                if (listView != null) {
                    asignarListaDispositivos(lista)
                    actualizarResumenDispositivosIA()
                }
            }
        }
    }

    private fun observarHomeData() {
        lifecycleScope.launch {
            viewModel.tareasPasadas.collect { lista ->
                actualizarSeccionAtrasadas(lista)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasProximas.collect { lista ->
                actualizarSeccionPorHacer(lista)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasLejanas.collect { lista ->
                actualizarSeccionProximo(lista)
            }
        }
    }

    private fun observarCalendarioData() {
        lifecycleScope.launch {
            viewModel.tareaSeleccionada.collect { tab ->
                actualizarCalendarioSegunTab(tab)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasPasadas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "atrasado") {
                    actualizarListaCalendario(lista)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.calendarioProximas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "proximo") {
                    actualizarListaCalendario(lista)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.tareasCompletadas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "completado") {
                    actualizarListaCalendario(lista)
                }
            }
        }
    }

    private fun actualizarSeccionAtrasadas(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_atrasadas) ?: return
        seccion.removeAllViews()

        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = getString(R.string.atrasado)
        header.findViewById<TextView>(R.id.texto_contador).text = getString(R.string.cantidad_items, items.size)
        seccion.addView(header)

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = getString(R.string.sin_tareas_atrasadas)
                textSize = 14f
                setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
                setTextColor(resources.getColor(R.color.text_secondary, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in agruparPorFechaYDispositivo(items)) {
                agregarCardGrupoInicio(seccion, grupo, atrasada = true)
            }
        }
    }

    private fun actualizarSeccionPorHacer(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_por_hacer) ?: return
        seccion.removeAllViews()

        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = getString(R.string.por_hacer)
        header.findViewById<TextView>(R.id.texto_contador).text = getString(R.string.cantidad_items, items.size)
        seccion.addView(header)

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = getString(R.string.sin_tareas_pendientes)
                textSize = 14f
                setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
                setTextColor(resources.getColor(R.color.text_secondary, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in agruparPorFechaYDispositivo(items)) {
                agregarCardGrupoInicio(seccion, grupo, atrasada = false)
            }
        }
    }

    private fun actualizarSeccionProximo(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_proximo) ?: return
        seccion.removeAllViews()

        val grupos = agruparPorFechaYDispositivo(items)
        val gruposVisibles = grupos.take(5)
        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = getString(R.string.proximo)
        header.findViewById<TextView>(R.id.texto_contador).text = if (grupos.size > 5) {
            getString(R.string.cinco_de_tarjetas, grupos.size)
        } else {
            getString(R.string.cantidad_tarjetas, grupos.size)
        }
        seccion.addView(header)

        if (grupos.isEmpty()) {
            val vacio = TextView(this).apply {
                text = getString(R.string.sin_tareas_proximas)
                textSize = 14f
                setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
                setTextColor(resources.getColor(R.color.text_secondary, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in gruposVisibles) {
                agregarCardGrupoProximo(seccion, grupo)
            }
        }
    }

    private fun agruparPorFechaYDispositivo(items: List<ItemProgramado>): List<List<ItemProgramado>> {
        return items.groupBy { it.id }.values.toList()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun agregarCardGrupoInicio(seccion: LinearLayout, grupo: List<ItemProgramado>, atrasada: Boolean) {
        val primero = grupo.firstOrNull() ?: return
        val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_inicio, seccion, false)
        vista.setBackgroundResource(if (atrasada) R.drawable.fondo_tarjeta_atrasada else R.drawable.fondo_tarjeta)
        vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
        vista.findViewById<TextView>(R.id.texto_descripcion_tarea).text = resumenGrupo(grupo)
        vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text =
            getString(R.string.fecha_y_cantidad, primero.fecha, textoCantidadGrupo(grupo))
        vista.findViewById<Button>(R.id.boton_ver).apply {
            visibility = View.VISIBLE
            setOnClickListener { abrirDetalleGrupo(grupo, soloLectura = false) }
        }
        seccion.addView(vista)
    }

    private fun agregarCardGrupoProximo(seccion: LinearLayout, grupo: List<ItemProgramado>) {
        val primero = grupo.firstOrNull() ?: return
        val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_proximo, seccion, false)
        vista.findViewById<TextView>(R.id.texto_fecha).text = primero.fecha
        vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
        vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text = resumenGrupo(grupo)
        vista.findViewById<Button>(R.id.boton_ver).apply {
            visibility = View.VISIBLE
            setOnClickListener { abrirDetalleGrupo(grupo, soloLectura = false) }
        }
        seccion.addView(vista)
    }

    private fun resumenGrupo(grupo: List<ItemProgramado>): String {
        return itemsVisiblesGrupo(grupo).joinToString(separator = "\n") { item ->
            val tipo = if (item.tipo == "mantenimiento") "Mantenimiento" else "Inspeccion"
            "$tipo: ${item.nombre}"
        }
    }

    private fun textoCantidadGrupo(grupo: List<ItemProgramado>): String {
        val cantidad = itemsVisiblesGrupo(grupo).size
        return if (cantidad == 1) "1 item" else "$cantidad items"
    }

    private fun itemsVisiblesGrupo(grupo: List<ItemProgramado>): List<ItemProgramado> {
        return grupo.distinctBy { "${it.tipo}:${it.nombre.trim().lowercase()}:${it.fecha}" }
    }

    private fun abrirDetalleGrupo(grupo: List<ItemProgramado>, soloLectura: Boolean = false) {
        val primera = grupo.firstOrNull() ?: return
        val intent = Intent(this, TareaDetalleActivity::class.java).apply {
            putExtra(TareaDetalleActivity.EXTRA_TAREA_ID, primera.id)
            putExtra(TareaDetalleActivity.EXTRA_DISPOSITIVO_NOMBRE, primera.nombreDispositivo)
            putExtra(TareaDetalleActivity.EXTRA_TAREA_FECHA, primera.fecha)
            putExtra(TareaDetalleActivity.EXTRA_SOLO_LECTURA, soloLectura)
        }
        resultadoTareaDetalle.launch(intent)
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
        agregarOpcion(menuBarra, ID_INICIO, R.drawable.ic_inicio, getString(R.string.inicio))
        agregarOpcion(menuBarra, ID_CALENDARIO, R.drawable.ic_calendario, getString(R.string.calendario))
        agregarOpcion(menuBarra, ID_DISPOSITIVOS, R.drawable.ic_dispositivos, getString(R.string.dispositivos))
        agregarOpcion(menuBarra, ID_AJUSTES, R.drawable.ic_ajustes, getString(R.string.ajustes))
        barraNavegacion.setOnItemSelectedListener { elemento ->
            procesarSeleccion(elemento)
        }

        barraNavegacion.selectedItemId = pantallaActual
    }

    private fun agregarOpcion(menu: Menu, id: Int, icono: Int, titulo: String) {
        menu.add(Menu.NONE, id, Menu.NONE, titulo).setIcon(icono)
    }

    private fun procesarSeleccion(elemento: MenuItem): Boolean {
        pantallaActual = elemento.itemId
        return when (elemento.itemId) {
            ID_INICIO -> {
                mostrarInicio()
                viewModel.cargarHomeData()
                true
            }
            ID_DISPOSITIVOS -> {
                mostrarPantalla(R.layout.layout_dispositivos)
                configurarBotonAgregar()
                configurarClickLista()
                viewModel.cargarDispositivos()
                asignarListaDispositivos()
                actualizarResumenDispositivosIA()
                true
            }
            ID_CALENDARIO -> {
                mostrarPantalla(R.layout.layout_calendario)
                configurarCalendario()
                viewModel.cargarCalendarioData()
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

    private fun mostrarInicio() {
        mostrarPantalla(R.layout.layout_inicio)
        actualizarSeccionAtrasadas(viewModel.tareasPasadas.value)
        actualizarSeccionPorHacer(viewModel.tareasProximas.value)
        actualizarSeccionProximo(viewModel.tareasLejanas.value)
    }

    private fun asignarListaDispositivos(
        dispositivos: List<Dispositivo> = viewModel.dispositivos.value
    ) {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        if (listView != null) {
            if (listView.headerViewsCount == 0) {
                val encabezado = layoutInflater.inflate(
                    R.layout.item_header_resumen_dispositivos,
                    listView,
                    false
                )
                listView.addHeaderView(encabezado, null, false)
            }
            listView.adapter = AdaptadorDispositivos(this, dispositivos)
        }
    }

    private fun actualizarResumenDispositivosIA() {
        val textoResumen = contenedorPantallas.findViewById<TextView>(R.id.texto_resumen_dispositivos_ia) ?: return
        textoResumen.text = getString(R.string.analizando_dispositivos)
        lifecycleScope.launch {
            val resumen = withContext(Dispatchers.IO) {
                generarResumenGeneralDispositivos()
            }
            textoResumen.text = resumen
        }
    }

    private fun generarResumenGeneralDispositivos(): String {
        val dispositivos = viewModel.obtenerTodosDispositivos()
        if (dispositivos.isEmpty()) {
            return "Agrega un dispositivo para empezar a recibir resumenes del estado general."
        }

        var inspeccionesConEstado = 0
        var malas = 0
        var regulares = 0
        var buenas = 0
        val dispositivosConAlerta = mutableSetOf<String>()

        for (dispositivo in dispositivos) {
            val detalles = viewModel.obtenerResultadosInspeccionPorDispositivo(dispositivo.id)

            inspeccionesConEstado += detalles.size
            malas += detalles.count { it.condicion == "malo" }
            regulares += detalles.count { it.condicion == "regular" }
            buenas += detalles.count { it.condicion == "bueno" }

            if (detalles.any { it.condicion == "malo" || it.condicion == "regular" }) {
                dispositivosConAlerta.add(dispositivo.nombre)
            }
        }

        if (inspeccionesConEstado == 0) {
            val cantidadDispositivos = if (dispositivos.size == 1) {
                "Tienes 1 dispositivo registrado."
            } else {
                "Tienes ${dispositivos.size} dispositivos registrados."
            }
            return "$cantidadDispositivos Completa las inspecciones indicando si el estado es Bueno, Regular o Malo para conocer su condición general."
        }

        return when {
            malas > 0 -> {
                val nombres = dispositivosConAlerta.take(2).joinToString(", ")
                "Atencion: hay $malas inspeccion(es) en mal estado. Revisa primero ${nombres.ifBlank { "los dispositivos marcados" }}."
            }
            regulares > 0 -> {
                val nombres = dispositivosConAlerta.take(2).joinToString(", ")
                "Hay $regulares inspeccion(es) en estado regular. Vigila ${nombres.ifBlank { "esos dispositivos" }} y programa revision preventiva."
            }
            else -> {
                "Todo funciona correctamente segun $buenas inspeccion(es) registradas. Mantente al dia con los mantenimientos preventivos."
            }
        }
    }

    private fun configurarClickLista() {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, posicion, _ ->
            val posicionDispositivo = posicion - listView.headerViewsCount
            val dispositivo = viewModel.dispositivos.value.getOrNull(posicionDispositivo)
                ?: return@OnItemClickListener
            val intent = Intent(this, DetalleDispositivoActivity::class.java).apply {
                putExtra(DetalleDispositivoActivity.EXTRA_ID, dispositivo.id)
                putExtra(DetalleDispositivoActivity.EXTRA_NOMBRE_DISPOSITIVO, dispositivo.nombre)
                putExtra(DetalleDispositivoActivity.EXTRA_CATEGORIA, dispositivo.categoria)
                putExtra(DetalleDispositivoActivity.EXTRA_MARCA, dispositivo.marca)
                putExtra(DetalleDispositivoActivity.EXTRA_MODELO, dispositivo.modelo)
                putExtra(DetalleDispositivoActivity.EXTRA_FOTO, dispositivo.foto)
            }
            resultadoDetalleDispositivo.launch(intent)
        }
    }

    private fun configurarCalendario() {
        val botonProximo = contenedorPantallas.findViewById<Button>(R.id.boton_proximo)
        val botonAtrasado = contenedorPantallas.findViewById<Button>(R.id.boton_atrasado)
        val botonCompletado = contenedorPantallas.findViewById<Button>(R.id.boton_completado)
        val botonAgregar = contenedorPantallas.findViewById<Button>(R.id.boton_agregar_tarea_calendario)
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario)

        val actualizarSeleccion = { seleccion: String ->
            val botones = listOf(botonProximo, botonAtrasado, botonCompletado)
            for (btn in botones) {
                btn?.alpha = 0.5f
            }
            when (seleccion) {
                "proximo" -> botonProximo?.alpha = 1.0f
                "atrasado" -> botonAtrasado?.alpha = 1.0f
                "completado" -> botonCompletado?.alpha = 1.0f
            }
            viewModel.seleccionarTabCalendario(seleccion)
        }

        botonProximo?.setOnClickListener { actualizarSeleccion("proximo") }
        botonAtrasado?.setOnClickListener { actualizarSeleccion("atrasado") }
        botonCompletado?.setOnClickListener { actualizarSeleccion("completado") }

        botonAgregar?.setOnClickListener {
            val intent = Intent(this, AgregarTareaActivity::class.java)
            resultadoAgregarTarea.launch(intent)
        }

        actualizarSeleccion(viewModel.tareaSeleccionada.value)
        actualizarCalendarioSegunTab(viewModel.tareaSeleccionada.value)
    }

    private fun actualizarCalendarioSegunTab(tab: String) {
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario) ?: return
        when (tab) {
            "proximo" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.calendarioProximas.value) { grupo ->
                abrirDetalleGrupo(grupo, soloLectura = false)
            }
            "atrasado" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.tareasPasadas.value) { grupo ->
                abrirDetalleGrupo(grupo, soloLectura = false)
            }
            "completado" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.tareasCompletadas.value) { grupo ->
                abrirDetalleGrupo(grupo, soloLectura = true)
            }
        }
    }

    private fun actualizarListaCalendario(items: List<ItemProgramado>) {
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario) ?: return
        val soloLectura = viewModel.tareaSeleccionada.value == "completado"
        listaCalendario.adapter = AdaptadorTareasCalendario(this, items) { grupo ->
            abrirDetalleGrupo(grupo, soloLectura)
        }
    }

    private fun configurarAjustes() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val switchOscuro = contenedorPantallas.findViewById<SwitchCompat>(R.id.switch_modo_oscuro)

        if (switchOscuro != null) {
            switchOscuro.isChecked = prefs.getBoolean(PREFS_TEMA_OSCURO, false)
            switchOscuro.setOnCheckedChangeListener { _, activo ->
                prefs.edit { putBoolean(PREFS_TEMA_OSCURO, activo) }
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
            vista.findViewById<TextView>(R.id.texto_categoria).text = dispositivo.categoria
            vista.findViewById<TextView>(R.id.texto_marca).text = contexto.getString(
                R.string.marca_formato,
                dispositivo.marca.ifBlank { contexto.getString(R.string.sin_marca) }
            )
            vista.findViewById<TextView>(R.id.texto_modelo).text = contexto.getString(
                R.string.modelo_formato,
                dispositivo.modelo.ifBlank { contexto.getString(R.string.sin_modelo) }
            )

            val imagen = vista.findViewById<ImageView>(R.id.imagen_dispositivo)
            val placeholder = vista.findViewById<View>(R.id.texto_foto_placeholder)
            if (dispositivo.foto.isBlank()) {
                imagen.setImageDrawable(null)
                placeholder.visibility = View.VISIBLE
            } else {
                val uri = if (dispositivo.foto.startsWith("content:")) dispositivo.foto.toUri() else Uri.fromFile(File(dispositivo.foto))
                imagen.setImageURI(uri)
                placeholder.visibility = View.GONE
            }

            return vista
        }
    }

    class AdaptadorTareasCalendario(
        private val contexto: Context,
        private val items: List<ItemProgramado>,
        private val onVerClick: (List<ItemProgramado>) -> Unit
    ) : BaseAdapter() {

        private val grupos = items
            .groupBy { it.id }
            .values
            .toList()

        override fun getCount() = grupos.size
        override fun getItem(posicion: Int) = grupos[posicion]
        override fun getItemId(posicion: Int) = grupos[posicion].firstOrNull()?.id ?: posicion.toLong()

        override fun getView(posicion: Int, vistaReciclada: View?, parent: ViewGroup): View {
            val vista = vistaReciclada ?: LayoutInflater.from(contexto)
                .inflate(R.layout.item_tarea_inicio, parent, false)

            val grupo = grupos[posicion]
            val primero = grupo.first()
            val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
            vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
            vista.findViewById<TextView>(R.id.texto_descripcion_tarea).text = resumenGrupoCalendario(grupo)
            vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text = contexto.getString(
                R.string.fecha_y_cantidad,
                primero.fecha,
                textoCantidadGrupoCalendario(grupo)
            )
            vista.findViewById<Button>(R.id.boton_ver).apply {
                visibility = View.VISIBLE
                setOnClickListener { onVerClick(grupo) }
            }

            return vista
        }

        private fun resumenGrupoCalendario(grupo: List<ItemProgramado>): String {
            return itemsVisiblesGrupoCalendario(grupo).joinToString(separator = "\n") { item ->
                val tipo = if (item.tipo == "mantenimiento") "Mantenimiento" else "Inspeccion"
                "$tipo: ${item.nombre}"
            }
        }

        private fun textoCantidadGrupoCalendario(grupo: List<ItemProgramado>): String {
            val cantidad = itemsVisiblesGrupoCalendario(grupo).size
            return if (cantidad == 1) "1 item" else "$cantidad items"
        }

        private fun itemsVisiblesGrupoCalendario(grupo: List<ItemProgramado>): List<ItemProgramado> {
            return grupo.distinctBy { "${it.tipo}:${it.nombre.trim().lowercase()}:${it.fecha}" }
        }
    }
}
