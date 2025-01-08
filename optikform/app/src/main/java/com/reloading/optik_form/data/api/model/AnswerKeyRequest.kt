package com.reloading.optik_form.data.api.model

/**
 * İstemci tarafından gönderilen cevap anahtarı talebi.
 *
 * @property question_id Soruya ait benzersiz kimlik.
 * @property correct_answer Doğru cevap metni.
 * @property test_group Test grubunun numarası.
 * @property course İlgili dersin kimliği.
 */
data class AnswerKeyRequest(
    val question_id: Int,
    val correct_answer: String,
    val test_group: Int,
    val course: Int
)
