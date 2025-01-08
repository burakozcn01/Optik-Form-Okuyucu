package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.StudentAnswer
import retrofit2.http.*

/**
 * Öğrenci Cevapları ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface StudentAnswersService {

    /**
     * Tüm öğrenci cevaplarını getirir.
     *
     * @return Öğrenci cevaplarının bir listesi.
     */
    @GET("studentanswers")
    suspend fun getList(): List<StudentAnswer>

    /**
     * Yeni bir öğrenci cevabı oluşturur.
     *
     * @param req Oluşturulacak öğrenci cevabı için istek verisi.
     * @return Oluşturulan öğrenci cevabının yanıtı.
     */
    @POST("studentanswers")
    suspend fun create(@Body req: StudentAnswer): StudentAnswer

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını getirir.
     *
     * @param id İstenen öğrenci cevabının benzersiz kimliği.
     * @return İlgili öğrenci cevabının yanıtı.
     */
    @GET("studentanswers/{id}")
    suspend fun getById(@Path("id") id: Int): StudentAnswer

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını günceller.
     *
     * @param id Güncellenecek öğrenci cevabının benzersiz kimliği.
     * @param req Güncelleme için kullanılacak istek verisi.
     * @return Güncellenen öğrenci cevabının yanıtı.
     */
    @PUT("studentanswers/{id}")
    suspend fun update(@Path("id") id: Int, @Body req: StudentAnswer): StudentAnswer

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını siler.
     *
     * @param id Silinecek öğrenci cevabının benzersiz kimliği.
     */
    @DELETE("studentanswers/{id}")
    suspend fun delete(@Path("id") id: Int)
}
