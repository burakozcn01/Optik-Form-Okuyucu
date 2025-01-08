package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.ApiClient
import com.reloading.optik_form.data.api.model.StudentAnswer

/**
 * Öğrenci Cevapları ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak veri alır ve gönderir.
 */
class StudentAnswersRepository {
    private val service = ApiClient.studentAnswersService

    /**
     * Tüm öğrenci cevaplarını getirir.
     *
     * @return Öğrenci cevaplarının bir listesi.
     */
    suspend fun getAll(): List<StudentAnswer> = service.getList()

    /**
     * Yeni bir öğrenci cevabı oluşturur.
     *
     * @param answer Oluşturulacak öğrenci cevabı verisi.
     * @return Oluşturulan öğrenci cevabının yanıtı.
     */
    suspend fun create(answer: StudentAnswer): StudentAnswer = service.create(answer)

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını getirir.
     *
     * @param id İstenen öğrenci cevabının benzersiz kimliği.
     * @return İlgili öğrenci cevabının yanıtı.
     */
    suspend fun get(id: Int): StudentAnswer = service.getById(id)

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını günceller.
     *
     * @param id Güncellenecek öğrenci cevabının benzersiz kimliği.
     * @param answer Güncelleme için kullanılacak öğrenci cevabı verisi.
     * @return Güncellenen öğrenci cevabının yanıtı.
     */
    suspend fun update(id: Int, answer: StudentAnswer): StudentAnswer = service.update(id, answer)

    /**
     * Belirli bir ID'ye sahip öğrenci cevabını siler.
     *
     * @param id Silinecek öğrenci cevabının benzersiz kimliği.
     */
    suspend fun delete(id: Int) = service.delete(id)

    /**
     * Belirli bir öğrenci numarasına sahip tüm cevapları getirir.
     *
     * @param studentNumber Öğrenci numarası.
     * @return İlgili öğrencinin cevaplarının bir listesi.
     */
    suspend fun getAnswersByStudentId(studentNumber: String): List<StudentAnswer> {
        return service.getList().filter { it.student == studentNumber }
    }
}
