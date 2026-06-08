package com.proyectofinal

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DispositivosViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DispositivosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DispositivosViewModel(application) as T
        }
        throw IllegalArgumentException("ViewModel no reconocido")
    }
}
