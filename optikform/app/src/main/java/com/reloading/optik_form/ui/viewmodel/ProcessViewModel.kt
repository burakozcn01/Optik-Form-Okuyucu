package com.reloading.optik_form.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reloading.optik_form.data.di.AppModule
import com.reloading.optik_form.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * Form işleme ekranı için ViewModel.
 */
class ProcessViewModel : ViewModel() {
    // Form işleme repository'si
    private val repository = AppModule.processRepository

    // Form işleme durumunu temsil eden StateFlow
    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    /**
     * Resmi işlemek için API çağrısı yapar (processForm).
     *
     * @param imagePart İşlenecek resmin MultipartBody.Part formatındaki parçası.
     */
    fun processImage(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val message = repository.processForm(imagePart)
                _uiState.value = UiState.Success(message)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error processing image")
            }
        }
    }

    /**
     * Cevap anahtarını çıkarmak için API çağrısı yapar (extractAnswerKey).
     *
     * @param imagePart İşlenecek resmin MultipartBody.Part formatındaki parçası.
     */
    fun extractAnswerKey(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val message = repository.extractAnswerKey(imagePart)
                _uiState.value = UiState.Success(message)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error extracting answer key")
            }
        }
    }
}
