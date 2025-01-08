package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.CoursesService
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.data.api.ApiClient
import retrofit2.HttpException
import android.util.Log

/**
 * Dersler ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak veri alır ve gönderir.
 * Hataları güvenli bir şekilde yakalar ve loglar.
 */
class CoursesRepository {
    private val service: CoursesService = ApiClient.coursesService

    /**
     * Tüm dersleri getirir.
     *
     * @return Derslerin bir listesi.
     */
    suspend fun getAll(): List<Course> = safeApiCall { service.getCourses() }

    /**
     * Yeni bir ders oluşturur.
     *
     * @param course Oluşturulacak ders verisi.
     * @return Oluşturulan dersin yanıtı.
     */
    suspend fun create(course: Course): Course = safeApiCall { service.createCourse(course) }

    /**
     * Belirli bir ID'ye sahip dersi siler.
     *
     * @param id Silinecek dersin benzersiz kimliği.
     */
    suspend fun delete(id: Int) = safeApiCall { service.deleteCourse(id) }

    /**
     * API çağrılarını güvenli bir şekilde yapar ve hataları yönetir.
     *
     * @param action Yapılacak API çağrısı.
     * @return API çağrısının sonucu.
     * @throws Exception API çağrısı sırasında oluşan hatalar.
     */
    private inline fun <T> safeApiCall(action: () -> T): T {
        return try {
            action()
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = parseErrorMessage(errorBody)
            Log.e("CoursesRepository", "HTTP Exception: $errorMessage")
            throw Exception(errorMessage)
        } catch (e: Exception) {
            Log.e("CoursesRepository", "General Exception: ${e.message}")
            throw Exception("API Hatası: ${e.message}")
        }
    }

    /**
     * Hata mesajını ayrıştırır.
     *
     * @param errorBody Hata yanıtının gövdesi.
     * @return Ayrıştırılmış hata mesajı.
     */
    private fun parseErrorMessage(errorBody: String?): String {
        return errorBody ?: "Bilinmeyen bir hata oluştu."
    }
}
