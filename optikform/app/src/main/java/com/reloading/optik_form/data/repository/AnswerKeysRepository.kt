package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.ApiClient
import com.reloading.optik_form.data.api.model.AnswerKeyRequest
import com.reloading.optik_form.data.api.model.AnswerKeyResponse

/**
 * Cevap Anahtarları ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak veri alır ve gönderir.
 */
class AnswerKeysRepository {
    private val service = ApiClient.answerKeysService

    /**
     * Tüm cevap anahtarlarını getirir.
     *
     * @return Cevap anahtarlarının bir listesi.
     */
    suspend fun getAll(): List<AnswerKeyResponse> = service.getAnswerKeys()

    /**
     * Yeni bir cevap anahtarı oluşturur.
     *
     * @param key Oluşturulacak cevap anahtarı verisi.
     * @return Oluşturulan cevap anahtarının yanıtı.
     */
    suspend fun create(key: AnswerKeyRequest): AnswerKeyResponse = service.createAnswerKey(key)

    /**
     * Belirli bir ID'ye sahip cevap anahtarını getirir.
     *
     * @param id İstenen cevap anahtarının benzersiz kimliği.
     * @return İlgili cevap anahtarının yanıtı.
     */
    suspend fun get(id: Int): AnswerKeyResponse = service.getAnswerKey(id)

    /**
     * Belirli bir ID'ye sahip cevap anahtarını günceller.
     *
     * @param id Güncellenecek cevap anahtarının benzersiz kimliği.
     * @param key Güncelleme için kullanılacak cevap anahtarı verisi.
     * @return Güncellenen cevap anahtarının yanıtı.
     */
    suspend fun update(id: Int, key: AnswerKeyRequest): AnswerKeyResponse = service.updateAnswerKey(id, key)

    /**
     * Belirli bir ID'ye sahip cevap anahtarını siler.
     *
     * @param id Silinecek cevap anahtarının benzersiz kimliği.
     */
    suspend fun delete(id: Int) = service.deleteAnswerKey(id)
}
