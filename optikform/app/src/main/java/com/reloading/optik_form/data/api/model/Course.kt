package com.reloading.optik_form.data.api.model

import com.squareup.moshi.Json

/**
 * Ders modeli.
 *
 * @property id Dersin benzersiz kimliği (isteğe bağlı).
 * @property name Dersin adı.
 * @property code Dersin kodu.
 * @property description Dersin açıklaması (isteğe bağlı).
 * @property columnNumber İlgili sütun numarası.
 * @property testGroup Test grubunun kimliği (isteğe bağlı).
 */
data class Course(
    val id: Int? = null,
    val name: String,
    val code: String,
    val description: String?,
    @Json(name = "column_number")
    val columnNumber: Int,
    @Json(name = "test_group")
    val testGroup: Int? = null
)
