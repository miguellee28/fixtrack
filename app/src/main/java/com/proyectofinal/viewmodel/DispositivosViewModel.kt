package com.proyectofinal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.proyectofinal.MaintenanceNotificationScheduler
import com.proyectofinal.data.DispositivoRepository
import com.proyectofinal.model.Dispositivo
import com.proyectofinal.model.Inspeccion
import com.proyectofinal.model.ItemProgramado
import com.proyectofinal.model.Mantenimiento
import com.proyectofinal.model.Tarea
import com.proyectofinal.model.TareaFoto
import com.proyectofinal.model.TareaItem
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
            _dispositivos.value = withContext(Dispatchers.IO) { repository.obtenerTodos() }
        }
    }

    suspend fun actualizarDispositivo(dispositivo: Dispositivo) {
        withContext(Dispatchers.IO) { repository.actualizar(dispositivo) }
        cargarDispositivos()
    }

    fun eliminarDispositivo(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.eliminar(id) }
            reprogramarNotificaciones()
            cargarDispositivos()
            cargarHomeData()
            cargarCalendarioData()
        }
    }

    suspend fun completarTarea(
        tareaId: Long,
        items: List<TareaItem>,
        fotos: List<String>,
        fechaCompletada: String
    ) {
        withContext(Dispatchers.IO) {
            repository.completarTarea(tareaId, items, fotos, fechaCompletada)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
    }

    fun cargarHomeData() {
        viewModelScope.launch {
            _tareasPasadas.value = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
            _tareasProximas.value = withContext(Dispatchers.IO) { repository.obtenerItemsProximas() }
            _tareasLejanas.value = withContext(Dispatchers.IO) { repository.obtenerItemsLejanas() }
        }
    }

    fun seleccionarTabCalendario(tab: String) {
        _tareaSeleccionada.value = tab
        cargarCalendarioData()
    }

    fun cargarCalendarioData() {
        viewModelScope.launch {
            when (_tareaSeleccionada.value) {
                "proximo" -> {
                    _calendarioProximas.value = withContext(Dispatchers.IO) {
                        (repository.obtenerItemsProximas() + repository.obtenerItemsLejanas())
                            .distinctBy { "${it.id}:${it.tipo}:${it.nombre}:${it.descripcion}" }
                            .sortedBy { it.fecha }
                    }
                }
                "atrasado" -> {
                    _tareasPasadas.value = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
                }
                "completado" -> {
                    _tareasCompletadas.value = withContext(Dispatchers.IO) { repository.obtenerItemsCompletadas() }
                }
            }
        }
    }

    fun obtenerTarea(tareaId: Long): Tarea? = repository.obtenerTareaPorId(tareaId)

    fun obtenerItemsPorTarea(tareaId: Long): List<TareaItem> = repository.obtenerItemsPorTarea(tareaId)

    fun obtenerFotosPorTarea(tareaId: Long): List<TareaFoto> = repository.obtenerFotosPorTarea(tareaId)

    suspend fun guardarDispositivoConCalendario(
        dispositivo: Dispositivo,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>
    ): Long {
        val dispositivoId = withContext(Dispatchers.IO) {
            repository.guardarDispositivoConCalendario(dispositivo, mantenimientos, inspecciones)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
        return dispositivoId
    }

    suspend fun guardarMantenimientosEInspecciones(
        dispositivoId: Long?,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>
    ) {
        withContext(Dispatchers.IO) {
            repository.guardarCalendario(dispositivoId ?: 0, mantenimientos, inspecciones)
        }
        actualizarDespuesDeCalendario()
    }

    suspend fun guardarEdicionCalendario(
        dispositivoId: Long,
        mantenimientos: List<Mantenimiento>,
        inspecciones: List<Inspeccion>,
        mantenimientosEliminados: Set<Long> = emptySet(),
        inspeccionesEliminadas: Set<Long> = emptySet()
    ) {
        withContext(Dispatchers.IO) {
            repository.guardarEdicionCalendario(
                dispositivoId,
                mantenimientos,
                inspecciones,
                mantenimientosEliminados,
                inspeccionesEliminadas
            )
        }
        actualizarDespuesDeCalendario()
    }

    private suspend fun actualizarDespuesDeCalendario() {
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
    }

    fun obtenerTodosDispositivos(): List<Dispositivo> = repository.obtenerTodos()

    fun obtenerMantenimientosPorDispositivo(dispositivoId: Long): List<Mantenimiento> =
        repository.obtenerMantenimientosPorDispositivo(dispositivoId)

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> =
        repository.obtenerInspeccionesPorDispositivo(dispositivoId)

    fun obtenerResultadosInspeccionPorDispositivo(dispositivoId: Long): List<TareaItem> =
        repository.obtenerResultadosInspeccionPorDispositivo(dispositivoId)
}
