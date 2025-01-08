package com.reloading.optik_form.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reloading.optik_form.data.api.model.AnswerKeyRequest
import com.reloading.optik_form.data.api.model.AnswerKeyResponse
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.data.di.AppModule
import com.reloading.optik_form.data.repository.AnswerKeysRepository
import com.reloading.optik_form.data.repository.CoursesRepository
import com.reloading.optik_form.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Cevap Anahtarları ekranı için ViewModel.
 *
 * @param answerKeysRepository Cevap anahtarları verilerini yöneten repository.
 * @param coursesRepository Ders verilerini yöneten repository.
 */
class AnswerKeysViewModel(
    private val answerKeysRepository: AnswerKeysRepository = AppModule.answerKeysRepository,
    private val coursesRepository: CoursesRepository = AppModule.coursesRepository
) : ViewModel() {

    // Cevap anahtarları durumunu temsil eden StateFlow
    private val _uiState = MutableStateFlow<UiState<List<AnswerKeyResponse>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Dersler durumunu temsil eden StateFlow
    private val _coursesState = MutableStateFlow<UiState<List<Course>>>(UiState.Loading)
    val coursesState = _coursesState.asStateFlow()

    /**
     * Tüm cevap anahtarlarını yükler.
     */
    fun loadAnswerKeys() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val keys = answerKeysRepository.getAll()
                _uiState.value = if (keys.isEmpty()) UiState.Empty else UiState.Success(keys)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    /**
     * Tüm dersleri yükler.
     */
    fun loadCourses() {
        viewModelScope.launch {
            _coursesState.value = UiState.Loading
            try {
                val courses = coursesRepository.getAll()
                _coursesState.value = if (courses.isEmpty()) UiState.Empty else UiState.Success(courses)
            } catch (e: Exception) {
                _coursesState.value = UiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    /**
     * Yeni bir cevap anahtarı oluşturur.
     *
     * @param key Oluşturulacak cevap anahtarı verisi.
     */
    fun createAnswerKey(key: AnswerKeyRequest) {
        viewModelScope.launch {
            try {
                val newKey = answerKeysRepository.create(key)
                val currentData = (_uiState.value as? UiState.Success)?.data ?: emptyList()
                _uiState.value = UiState.Success(currentData + listOf(newKey))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error Creating AnswerKey")
            }
        }
    }

    /**
     * Belirli bir cevap anahtarını siler.
     *
     * @param id Silinecek cevap anahtarının ID'si.
     */
    fun deleteAnswerKey(id: Int) {
        viewModelScope.launch {
            try {
                answerKeysRepository.delete(id)
                loadAnswerKeys()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error Deleting AnswerKey")
            }
        }
    }

    init {
        // ViewModel oluşturulduğunda cevap anahtarlarını ve dersleri yükler
        loadAnswerKeys()
        loadCourses()
    }
}
