package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.ApiClient
import com.reloading.optik_form.data.api.model.ColumnMapping

/**
 * Sütun Eşlemeleri ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak veri alır ve gönderir.
 */
class ColumnMappingsRepository {
    private val service = ApiClient.columnMappingsService

    /**
     * Tüm sütun eşlemelerini getirir.
     *
     * @return Sütun eşlemelerinin bir listesi.
     */
    suspend fun getAll(): List<ColumnMapping> = service.getList()

    /**
     * Yeni bir sütun eşlemesi oluşturur.
     *
     * @param mapping Oluşturulacak sütun eşlemesi verisi.
     * @return Oluşturulan sütun eşlemesinin yanıtı.
     */
    suspend fun create(mapping: ColumnMapping): ColumnMapping = service.create(mapping)

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini getirir.
     *
     * @param id İstenen sütun eşlemesinin benzersiz kimliği.
     * @return İlgili sütun eşlemesinin yanıtı.
     */
    suspend fun get(id: Int): ColumnMapping = service.getById(id)

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini günceller.
     *
     * @param id Güncellenecek sütun eşlemesinin benzersiz kimliği.
     * @param mapping Güncelleme için kullanılacak sütun eşlemesi verisi.
     * @return Güncellenen sütun eşlemesinin yanıtı.
     */
    suspend fun update(id: Int, mapping: ColumnMapping): ColumnMapping = service.update(id, mapping)

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini siler.
     *
     * @param id Silinecek sütun eşlemesinin benzersiz kimliği.
     */
    suspend fun delete(id: Int) = service.delete(id)
}
