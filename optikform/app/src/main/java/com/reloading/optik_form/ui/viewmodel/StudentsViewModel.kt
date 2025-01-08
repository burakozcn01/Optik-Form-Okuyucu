package com.reloading.optik_form.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reloading.optik_form.data.api.model.Student
import com.reloading.optik_form.data.api.model.StudentAnswer
import com.reloading.optik_form.data.repository.StudentAnswersRepository
import com.reloading.optik_form.data.repository.StudentsRepository
import com.reloading.optik_form.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Öğrenciler ekranı için ViewModel.
 *
 * @param studentsRepository Öğrenci verilerini yöneten repository.
 * @param studentAnswersRepository Öğrenci cevaplarını yöneten repository.
 */
class StudentsViewModel(
    private val studentsRepository: StudentsRepository = StudentsRepository(),
    private val studentAnswersRepository: StudentAnswersRepository = StudentAnswersRepository()
) : ViewModel() {

    // Öğrenciler durumunu temsil eden StateFlow
    private val _uiState = MutableStateFlow<UiState<List<Student>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Öğrencinin not durumunu temsil eden StateFlow (Map<String, Double> -> Ders Kodu -> Puan)
    private val _gradesState = MutableStateFlow<UiState<Map<String, Double>?>>(UiState.Empty)
    val gradesState = _gradesState.asStateFlow()

    // Öğrencinin cevaplarını temsil eden StateFlow
    private val _answersState = MutableStateFlow<UiState<List<StudentAnswer>>>(UiState.Empty)
    val answersState = _answersState.asStateFlow()

    /**
     * Tüm öğrencileri yükler.
     */
    fun loadStudents() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val students = studentsRepository.getAll()
                _uiState.value = if (students.isEmpty()) UiState.Empty else UiState.Success(students)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Öğrenciler yüklenirken hata oluştu!")
            }
        }
    }

    /**
     * Yeni bir öğrenci ekler.
     *
     * @param student Eklenecek öğrenci verisi.
     */
    fun addStudent(student: Student) {
        viewModelScope.launch {
            try {
                val newStudent = studentsRepository.create(student)
                val currentData = (_uiState.value as? UiState.Success)?.data ?: emptyList()
                _uiState.value = UiState.Success(currentData + newStudent)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Öğrenci eklenirken hata oluştu!")
            }
        }
    }

    /**
     * Belirli bir öğrencinin notlarını (ders kodu->puan) yükler.
     *
     * @param studentId Öğrencinin ID'si.
     */
    fun loadGrades(studentId: Int) {
        viewModelScope.launch {
            _gradesState.value = UiState.Loading
            try {
                val grades = studentsRepository.getGradesByStudentId(studentId)
                _gradesState.value = if (grades.isNullOrEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(grades)
                }
            } catch (e: Exception) {
                _gradesState.value = UiState.Error(e.message ?: "Notlar yüklenirken hata oluştu!")
            }
        }
    }

    /**
     * Belirli bir öğrencinin cevaplarını (doğru/yanlış) yükler.
     *
     * @param studentNumber Öğrencinin numarası.
     */
    fun loadAnswers(studentNumber: String) {
        viewModelScope.launch {
            _answersState.value = UiState.Loading
            try {
                val answers = studentAnswersRepository.getAnswersByStudentId(studentNumber)
                _answersState.value = if (answers.isEmpty()) UiState.Empty else UiState.Success(answers)
            } catch (e: Exception) {
                _answersState.value = UiState.Error(e.message ?: "Cevaplar yüklenirken hata oluştu!")
            }
        }
    }
}
