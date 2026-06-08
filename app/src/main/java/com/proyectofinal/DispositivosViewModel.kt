package com.proyectofinal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DispositivosViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DispositivoRepository(app)

    // Estado observable con StateFlow
    private val _dispositivos = MutableStateFlow<List<Dispositivo>>(emptyList())
    val dispositivos: StateFlow<List<Dispositivo>> = _dispositivos.asStateFlow()

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _inspecciones = MutableStateFlow<List<Inspeccion>>(emptyList())
    val inspecciones: StateFlow<List<Inspeccion>> = _inspecciones.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()


    fun cargarDispositivos() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerTodos()
            }
            _dispositivos.value = datos
        }
    }

    fun insertarDispositivo(dispositivo: Dispositivo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertar(dispositivo)
            }
            _mensaje.value = "Dispositivo guardado"
            cargarDispositivos()
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
            _mensaje.value = "Dispositivo eliminado"
            cargarDispositivos()
        }
    }

    // ==================== TAREAS ====================

    fun cargarTareas() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerTareas()
            }
            _tareas.value = datos
        }
    }

    fun insertarTarea(tarea: Tarea) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertarTarea(tarea)
            }
            _mensaje.value = "Tarea guardada"
            cargarTareas()
        }
    }

    fun eliminarTarea(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminarTarea(id)
            }
            _mensaje.value = "Tarea eliminada"
            cargarTareas()
        }
    }

    // ==================== INSPECCIONES ====================

    fun cargarInspecciones() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerInspecciones()
            }
            _inspecciones.value = datos
        }
    }

    fun insertarInspeccion(inspeccion: Inspeccion) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertarInspeccion(inspeccion)
            }
            _mensaje.value = "Inspección guardada"
            cargarInspecciones()
        }
    }

    fun eliminarInspeccion(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminarInspeccion(id)
            }
            _mensaje.value = "Inspección eliminada"
            cargarInspecciones()
        }
    }

    // ==================== GUARDAR TODO ====================

    suspend fun guardarDispositivoConTareaEInspeccion(
        dispositivo: Dispositivo,
        tarea: Tarea?,
        inspeccion: Inspeccion?
    ) {
        val id = withContext(Dispatchers.IO) {
            repository.insertar(dispositivo)
        }
        if (tarea != null) {
            withContext(Dispatchers.IO) {
                repository.insertarTarea(tarea.copy(dispositivoId = id))
            }
        }
        if (inspeccion != null) {
            withContext(Dispatchers.IO) {
                repository.insertarInspeccion(inspeccion.copy(dispositivoId = id))
            }
        }
        cargarDispositivos()
    }

    // ==================== MENSAJE ====================

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}
