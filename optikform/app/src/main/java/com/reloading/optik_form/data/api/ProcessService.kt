package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.ProcessResponse
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Form işleme ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface ProcessService {

    /**
     * Formu işlemek için bir resim yükler.
     *
     * @param image İşlenecek formun resim dosyası.
     * @return İşlem sonucunun yanıtı.
     */
    @Multipart
    @POST("process/")
    suspend fun processForm(@Part image: MultipartBody.Part): ProcessResponse

    /**
     * Cevap anahtarını çıkarmak için bir resim yükler.
     *
     * @param image İşlenecek cevap anahtarı resim dosyası.
     * @return İşlem sonucunun yanıtı.
     */
    @Multipart
    @POST("extract-answer-key/")
    suspend fun extractAnswerKey(@Part image: MultipartBody.Part): ProcessResponse
}
