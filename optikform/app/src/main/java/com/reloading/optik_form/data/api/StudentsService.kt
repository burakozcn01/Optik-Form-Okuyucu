package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.Student
import retrofit2.http.*

/**
 * Öğrenciler ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface StudentsService {

    /**
     * Tüm öğrencileri getirir.
     *
     * @return Öğrencilerin bir listesi.
     */
    @GET("students")
    suspend fun getStudents(): List<Student>

    /**
     * Yeni bir öğrenci oluşturur.
     *
     * @param req Oluşturulacak öğrenci için istek verisi.
     * @return Oluşturulan öğrencinin yanıtı.
     */
    @POST("students")
    suspend fun createStudent(@Body req: Student): Student

    /**
     * Belirli bir ID'ye sahip öğrenciyi getirir.
     *
     * @param id İstenen öğrencinin benzersiz kimliği.
     * @return İlgili öğrencinin yanıtı.
     */
    @GET("students/{id}")
    suspend fun getStudent(@Path("id") id: Int): Student

    /**
     * Belirli bir ID'ye sahip öğrenciyi günceller.
     *
     * @param id Güncellenecek öğrencinin benzersiz kimliği.
     * @param req Güncelleme için kullanılacak istek verisi.
     * @return Güncellenen öğrencinin yanıtı.
     */
    @PUT("students/{id}")
    suspend fun updateStudent(@Path("id") id: Int, @Body req: Student): Student

    /**
     * Belirli bir ID'ye sahip öğrenciyi siler.
     *
     * @param id Silinecek öğrencinin benzersiz kimliği.
     */
    @DELETE("students/{id}")
    suspend fun deleteStudent(@Path("id") id: Int)
}
