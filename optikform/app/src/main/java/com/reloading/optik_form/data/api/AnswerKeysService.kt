package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.AnswerKeyResponse
import com.reloading.optik_form.data.api.model.AnswerKeyRequest
import retrofit2.http.*

/**
 * Cevap Anahtarları ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface AnswerKeysService {

    /**
     * Tüm cevap anahtarlarını getirir.
     *
     * @return Cevap anahtarlarının bir listesi.
     */
    @GET("answerkeys/")
    suspend fun getAnswerKeys(): List<AnswerKeyResponse>

    /**
     * Yeni bir cevap anahtarı oluşturur.
     *
     * @param req Oluşturulacak cevap anahtarı için istek verisi.
     * @return Oluşturulan cevap anahtarının yanıtı.
     */
    @POST("answerkeys/")
    suspend fun createAnswerKey(@Body req: AnswerKeyRequest): AnswerKeyResponse

    /**
     * Belirli bir ID'ye sahip cevap anahtarını getirir.
     *
     * @param id İstenen cevap anahtarının benzersiz kimliği.
     * @return İlgili cevap anahtarının yanıtı.
     */
    @GET("answerkeys/{id}/")
    suspend fun getAnswerKey(@Path("id") id: Int): AnswerKeyResponse

    /**
     * Belirli bir ID'ye sahip cevap anahtarını günceller.
     *
     * @param id Güncellenecek cevap anahtarının benzersiz kimliği.
     * @param req Güncelleme için kullanılacak istek verisi.
     * @return Güncellenen cevap anahtarının yanıtı.
     */
    @PUT("answerkeys/{id}/")
    suspend fun updateAnswerKey(@Path("id") id: Int, @Body req: AnswerKeyRequest): AnswerKeyResponse

    /**
     * Belirli bir ID'ye sahip cevap anahtarını siler.
     *
     * @param id Silinecek cevap anahtarının benzersiz kimliği.
     */
    @DELETE("answerkeys/{id}/")
    suspend fun deleteAnswerKey(@Path("id") id: Int)
}
