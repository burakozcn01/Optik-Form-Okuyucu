package com.reloading.optik_form.data.api.model

/**
 * Cevap anahtarı yanıt modeli.
 *
 * @property id Cevap anahtarının benzersiz kimliği (isteğe bağlı).
 * @property question_id İlgili sorunun kimliği.
 * @property correct_answer Doğru cevap metni.
 * @property course_name Dersin adı.
 * @property test_group_name Test grubunun adı.
 */
data class AnswerKeyResponse(
    val id: Int?,
    val question_id: Int,
    val correct_answer: String,
    val course_name: String,
    val test_group_name: String
)
