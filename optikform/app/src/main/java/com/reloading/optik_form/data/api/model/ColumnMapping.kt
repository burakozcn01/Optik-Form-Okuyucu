package com.reloading.optik_form.data.api.model

/**
 * Sütun eşlemesi modeli.
 *
 * @property id Eşlemenin benzersiz kimliği (isteğe bağlı).
 * @property column_number Sütun numarası.
 * @property test_group Test grubunun adı.
 * @property course İlgili dersin adı (isteğe bağlı).
 */
data class ColumnMapping(
    val id: Int? = null,
    val column_number: Int,
    val test_group: String,
    val course: String?
)
