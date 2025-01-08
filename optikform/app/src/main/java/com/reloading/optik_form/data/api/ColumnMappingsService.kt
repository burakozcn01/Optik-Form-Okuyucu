package com.reloading.optik_form.data.api

import com.reloading.optik_form.data.api.model.ColumnMapping
import retrofit2.http.*

/**
 * Sütun Eşlemeleri ile ilgili API çağrılarını yöneten servis arayüzü.
 */
interface ColumnMappingsService {

    /**
     * Tüm sütun eşlemelerini getirir.
     *
     * @return Sütun eşlemelerinin bir listesi.
     */
    @GET("columnmappings/")
    suspend fun getList(): List<ColumnMapping>

    /**
     * Yeni bir sütun eşlemesi oluşturur.
     *
     * @param req Oluşturulacak sütun eşlemesi için istek verisi.
     * @return Oluşturulan sütun eşlemesinin yanıtı.
     */
    @POST("columnmappings/")
    suspend fun create(@Body req: ColumnMapping): ColumnMapping

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini getirir.
     *
     * @param id İstenen sütun eşlemesinin benzersiz kimliği.
     * @return İlgili sütun eşlemesinin yanıtı.
     */
    @GET("columnmappings/{id}/")
    suspend fun getById(@Path("id") id: Int): ColumnMapping

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini günceller.
     *
     * @param id Güncellenecek sütun eşlemesinin benzersiz kimliği.
     * @param req Güncelleme için kullanılacak istek verisi.
     * @return Güncellenen sütun eşlemesinin yanıtı.
     */
    @PUT("columnmappings/{id}/")
    suspend fun update(@Path("id") id: Int, @Body req: ColumnMapping): ColumnMapping

    /**
     * Belirli bir ID'ye sahip sütun eşlemesini siler.
     *
     * @param id Silinecek sütun eşlemesinin benzersiz kimliği.
     */
    @DELETE("columnmappings/{id}/")
    suspend fun delete(@Path("id") id: Int)
}
