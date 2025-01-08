package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.ApiClient
import com.reloading.optik_form.data.api.model.ProcessResponse
import okhttp3.MultipartBody

/**
 * Form işleme ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak form verilerini işler.
 */
class ProcessRepository {
    private val service = ApiClient.processService

    /**
     * Formu işlemek için bir resim yükler.
     *
     * @param imagePart İşlenecek formun resim dosyası.
     * @return İşlem sonucunun mesajı.
     */
    suspend fun processForm(imagePart: MultipartBody.Part): String {
        val response = service.processForm(imagePart)
        return response.message
    }

    /**
     * Cevap anahtarını çıkarmak için bir resim yükler.
     *
     * @param imagePart İşlenecek cevap anahtarı resim dosyası.
     * @return İşlem sonucunun mesajı.
     */
    suspend fun extractAnswerKey(imagePart: MultipartBody.Part): String {
        val response = service.extractAnswerKey(imagePart)
        return response.message
    }
}
