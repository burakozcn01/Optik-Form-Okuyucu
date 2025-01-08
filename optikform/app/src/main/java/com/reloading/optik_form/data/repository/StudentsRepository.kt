package com.reloading.optik_form.data.repository

import com.reloading.optik_form.data.api.ApiClient
import com.reloading.optik_form.data.api.model.CourseResult
import com.reloading.optik_form.data.api.model.Student

/**
 * Öğrenciler ile ilgili veri işlemlerini yöneten repository sınıfı.
 * API servislerini kullanarak veri alır ve gönderir.
 */
class StudentsRepository {
    private val service = ApiClient.studentsService

    /**
     * Tüm öğrencileri getirir.
     *
     * @return Öğrencilerin bir listesi.
     */
    suspend fun getAll(): List<Student> = service.getStudents()

    /**
     * Yeni bir öğrenci oluşturur.
     *
     * @param student Oluşturulacak öğrenci verisi.
     * @return Oluşturulan öğrencinin yanıtı.
     */
    suspend fun create(student: Student): Student = service.createStudent(student)

    /**
     * Belirli bir ID'ye sahip öğrenciyi getirir.
     *
     * @param id İstenen öğrencinin benzersiz kimliği.
     * @return İlgili öğrencinin yanıtı.
     */
    suspend fun get(id: Int): Student = service.getStudent(id)

    /**
     * Belirli bir ID'ye sahip öğrenciyi günceller.
     *
     * @param id Güncellenecek öğrencinin benzersiz kimliği.
     * @param student Güncelleme için kullanılacak öğrenci verisi.
     * @return Güncellenen öğrencinin yanıtı.
     */
    suspend fun update(id: Int, student: Student): Student = service.updateStudent(id, student)

    /**
     * Belirli bir ID'ye sahip öğrenciyi siler.
     *
     * @param id Silinecek öğrencinin benzersiz kimliği.
     */
    suspend fun delete(id: Int) = service.deleteStudent(id)

    /**
     * Belirli bir öğrenci ID'sine sahip öğrencinin ders bazlı sonuçlarını getirir.
     * Örn: {"MAT101": CourseResult(overall=Overall(...)), "TR101": ...}
     */
    suspend fun getResultsByStudentId(studentId: Int): Map<String, CourseResult>? {
        return get(studentId).results
    }

    /**
     * Belirli bir öğrenci ID'sine sahip öğrencinin sadece 'score' (puan) değerlerini getirir.
     * Örn: {"MAT101": 50.0, "TR101": 60.0}
     */
    suspend fun getGradesByStudentId(studentId: Int): Map<String, Double>? {
        val results = get(studentId).results ?: return null
        return results.mapValues { (_, courseResult) ->
            courseResult.overall.score
        }
    }
}
