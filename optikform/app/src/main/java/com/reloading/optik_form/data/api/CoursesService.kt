package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.Course
import retrofit2.http.*

/**
 * Dersler ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface CoursesService {

    /**
     * Tüm dersleri getirir.
     *
     * @return Derslerin bir listesi.
     */
    @GET("courses/")
    suspend fun getCourses(): List<Course>

    /**
     * Yeni bir ders oluşturur.
     *
     * @param course Oluşturulacak ders için istek verisi.
     * @return Oluşturulan dersin yanıtı.
     */
    @POST("courses/")
    suspend fun createCourse(@Body course: Course): Course

    /**
     * Belirli bir ID'ye sahip dersi siler.
     *
     * @param id Silinecek dersin benzersiz kimliği.
     */
    @DELETE("courses/{id}/")
    suspend fun deleteCourse(@Path("id") id: Int): Unit
}
