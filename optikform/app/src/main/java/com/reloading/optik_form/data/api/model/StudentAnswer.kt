package com.reloading.optik_form.data.api.model

/**
 * Öğrenci cevap modeli.
 *
 * @property id Cevabın benzersiz kimliği (isteğe bağlı).
 * @property question_id İlgili sorunun kimliği.
 * @property selected_answer Öğrencinin seçtiği cevap (isteğe bağlı).
 * @property is_correct Cevabın doğru olup olmadığı (isteğe bağlı).
 * @property created_at Cevabın oluşturulma zamanı (isteğe bağlı).
 * @property updated_at Cevabın güncellenme zamanı (isteğe bağlı).
 * @property student Öğrencinin adı veya kimliği.
 * @property test_group İlgili test grubunun adı (isteğe bağlı).
 * @property course İlgili dersin adı (isteğe bağlı).
 */
data class StudentAnswer(
    val id: Int? = null,
    val question_id: Int,
    val selected_answer: String?,
    val is_correct: Boolean?,
    val created_at: String?,
    val updated_at: String?,
    val student: String,
    val test_group: String?,
    val course: String?
)
