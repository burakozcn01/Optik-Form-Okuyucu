package com.reloading.optik_form.ui.viewmodel

import androidx.lifecycle.*
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.data.repository.CoursesRepository
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Dersler ekranı için ViewModel.
 */
class CoursesViewModel : ViewModel() {

    // Dersleri yöneten repository
    private val repository = CoursesRepository()

    // Dersler listesini tutan LiveData
    private val _courses = MutableLiveData<List<Course>>()
    val courses: LiveData<List<Course>> get() = _courses

    // Hata mesajlarını tutan LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    // Ders ekleme durumunu tutan LiveData
    private val _courseAdded = MutableLiveData<Boolean>()
    val courseAdded: LiveData<Boolean> get() = _courseAdded

    // Ders silme durumunu tutan LiveData
    private val _courseDeleted = MutableLiveData<Boolean>()
    val courseDeleted: LiveData<Boolean> get() = _courseDeleted

    init {
        // ViewModel oluşturulduğunda dersleri yükler
        fetchCourses()
    }

    /**
     * Tüm dersleri getirir ve LiveData'ya atar.
     */
    fun fetchCourses() {
        viewModelScope.launch {
            try {
                Log.d("CoursesViewModel", "Fetching courses from repository")
                val courseList = repository.getAll()
                _courses.postValue(courseList)
                Log.d("CoursesViewModel", "Fetched courses: $courseList")
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error fetching courses: ${e.message}")
                _error.postValue("Veri alınırken hata oluştu: ${e.message}")
            }
        }
    }

    /**
     * Yeni bir ders ekler.
     *
     * @param course Eklenecek ders verisi.
     */
    fun addCourse(course: Course) {
        viewModelScope.launch {
            try {
                Log.d("CoursesViewModel", "Attempting to add course: $course")
                repository.create(course)
                _courseAdded.postValue(true)
                fetchCourses() // Güncel listeyi almak için tekrar dersleri yükler
                Log.d("CoursesViewModel", "Course added successfully")
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error adding course: ${e.message}")
                _error.postValue(e.message ?: "Kurs eklenirken hata oluştu.")
            }
        }
    }

    /**
     * Belirli bir dersi siler.
     *
     * @param id Silinecek dersin ID'si.
     */
    fun deleteCourse(id: Int) {
        viewModelScope.launch {
            try {
                Log.d("CoursesViewModel", "Attempting to delete course with id: $id")
                repository.delete(id)
                _courseDeleted.postValue(true)
                fetchCourses() // Güncel listeyi almak için tekrar dersleri yükler
                Log.d("CoursesViewModel", "Course deleted successfully")
            } catch (e: Exception) {
                Log.e("CoursesViewModel", "Error deleting course: ${e.message}")
                _error.postValue("Kurs silinirken hata oluştu: ${e.message}")
            }
        }
    }
}
