package com.reloading.optik_form.data.api.model

import com.squareup.moshi.Json

/**
 * Her dersin sonuçlarını tutan veri sınıfı.
 *
 * Örnek JSON:
 * "results": {
 *   "MAT101": {
 *     "overall": {
 *       "score": 50.0,
 *       "correct": 5,
 *       "incorrect": 5
 *     }
 *   },
 *   "TR101": {
 *     "overall": {
 *       "score": 60.0,
 *       "correct": 6,
 *       "incorrect": 4
 *     }
 *   }
 * }
 */
data class Overall(
    val score: Double,
    val correct: Int,
    val incorrect: Int
)

data class CourseResult(
    val overall: Overall
)

/**
 * Öğrenci modeli.
 *
 * @property id Öğrencinin benzersiz kimliği.
 * @property studentNumber Öğrenci numarası.
 * @property name Öğrencinin adı.
 * @property results Ders bazlı sonuçları tutan bir Map.
 */
data class Student(
    val id: Int,
    @Json(name = "student_number")
    val studentNumber: String,
    val results: Map<String, CourseResult>?
)
