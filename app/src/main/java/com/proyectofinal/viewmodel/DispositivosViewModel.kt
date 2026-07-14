package com.proyectofinal.viewmodel

// Estado y operaciones que consumen las pantallas.
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.proyectofinal.MaintenanceNotificationScheduler
import com.proyectofinal.data.DispositivoRepository
import com.proyectofinal.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DispositivosViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DispositivoRepository(app)

    private val _dispositivos = MutableStateFlow<List<Dispositivo>>(emptyList())
    val dispositivos: StateFlow<List<Dispositivo>> = _dispositivos.asStateFlow()

    private val _tareasPasadas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasPasadas: StateFlow<List<ItemProgramado>> = _tareasPasadas.asStateFlow()

    private val _tareasProximas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasProximas: StateFlow<List<ItemProgramado>> = _tareasProximas.asStateFlow()

    private val _tareasLejanas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasLejanas: StateFlow<List<ItemProgramado>> = _tareasLejanas.asStateFlow()

    private val _calendarioProximas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val calendarioProximas: StateFlow<List<ItemProgramado>> = _calendarioProximas.asStateFlow()

    private val _tareasCompletadas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasCompletadas: StateFlow<List<ItemProgramado>> = _tareasCompletadas.asStateFlow()

    private val _tareaSeleccionada = MutableStateFlow("proximo")
    val tareaSeleccionada: StateFlow<String> = _tareaSeleccionada.asStateFlow()

    private suspend fun reprogramarNotificaciones() {
        withContext(Dispatchers.IO) {
            MaintenanceNotificationScheduler.scheduleAll(getApplication())
        }
    }

    fun cargarDispositivos() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerTodos()
            }
            _dispositivos.value = datos
        }
    }

    suspend fun actualizarDispositivo(dispositivo: Dispositivo) {
        withContext(Dispatchers.IO) {
            repository.actualizar(dispositivo)
        }
        cargarDispositivos()
    }

    fun eliminarDispositivo(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminar(id)
            }
            reprogramarNotificaciones()
            cargarDispositivos()
            cargarHomeData()
            cargarCalendarioData()
        }
    }

    suspend fun marcarTareaCompletada(id: Long) {
        withContext(Dispatchers.IO) {
            repository.marcarTareaCompletada(id)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
    }

    suspend fun completarInspeccion(
        id: Long,
        condicion: String,
        notas: String,
        fotos: List<String>,
        fechaCompletada: String
    ) {
        withContext(Dispatchers.IO) {
            repository.completarInspeccion(id, condicion, notas, fotos, fechaCompletada)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
    }

    // ==================== HOME DATA ====================

    fun cargarHomeData() {
        viewModelScope.launch {
            val pasadas = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
            val proximas = withContext(Dispatchers.IO) { repository.obtenerItemsProximas() }
            val lejanas = withContext(Dispatchers.IO) { repository.obtenerItemsLejanas() }
            _tareasPasadas.value = pasadas
            _tareasProximas.value = proximas
            _tareasLejanas.value = lejanas
        }
    }

    // ==================== CALENDARIO DATA ====================

    fun seleccionarTabCalendario(tab: String) {
        _tareaSeleccionada.value = tab
        cargarCalendarioData()
    }

    fun cargarCalendarioData() {
        viewModelScope.launch {
            when (_tareaSeleccionada.value) {
                "proximo" -> {
                    val datos = withContext(Dispatchers.IO) {
                        (repository.obtenerItemsProximas() + repository.obtenerItemsLejanas())
                            .distinctBy { "${it.tipo}:${it.id}" }
                            .sortedBy { it.fecha }
                    }
                    _calendarioProximas.value = datos
                }
                "atrasado" -> {
                    val datos = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
                    _tareasPasadas.value = datos
                }
                "completado" -> {
                    val datos = withContext(Dispatchers.IO) { repository.obtenerItemsCompletadas() }
                    _tareasCompletadas.value = datos
                }
            }
        }
    }

    // ==================== TAREA DETALLES ====================

    fun cargarDetallesPorTarea(tareaId: Long): List<TareaDetalle> {
        return repository.obtenerDetallesPorTarea(tareaId)
    }

    suspend fun actualizarTareaDetalle(detalle: TareaDetalle) {
        withContext(Dispatchers.IO) {
            repository.actualizarTareaDetalle(detalle)
        }
    }

    // ==================== GUARDAR TODO ====================

    suspend fun guardarDispositivoConCalendario(
        dispositivo: Dispositivo,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ): Long {
        val dispositivoId = withContext(Dispatchers.IO) {
            repository.guardarDispositivoConCalendario(dispositivo, tareas, inspecciones)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
        return dispositivoId
    }

    suspend fun guardarTareasEInspecciones(
        dispositivoId: Long?,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ) {
        val idDispositivo = dispositivoId ?: 0
        withContext(Dispatchers.IO) {
            repository.guardarCalendario(idDispositivo, tareas, inspecciones)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
    }

    suspend fun guardarEdicionCalendario(
        dispositivoId: Long,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>,
        tareasEliminadas: Set<Long> = emptySet(),
        inspeccionesEliminadas: Set<Long> = emptySet()
    ) {
        withContext(Dispatchers.IO) {
            repository.guardarEdicionCalendario(
                dispositivoId,
                tareas,
                inspecciones,
                tareasEliminadas,
                inspeccionesEliminadas
            )
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
    }

    fun obtenerTodosDispositivos(): List<Dispositivo> = repository.obtenerTodos()

    fun obtenerTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        return repository.obtenerTareasPorDispositivo(dispositivoId)
    }

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        return repository.obtenerInspeccionesPorDispositivo(dispositivoId)
    }

    fun obtenerTodasTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        return repository.obtenerTodasTareasPorDispositivo(dispositivoId)
    }

    fun obtenerTodasInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        return repository.obtenerTodasInspeccionesPorDispositivo(dispositivoId)
    }

    suspend fun obtenerInspeccionPorId(id: Long): Inspeccion? {
        return withContext(Dispatchers.IO) {
            repository.obtenerInspeccionPorId(id)
        }
    }
}
