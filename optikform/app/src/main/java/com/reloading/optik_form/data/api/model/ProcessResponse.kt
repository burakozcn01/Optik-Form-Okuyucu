package com.reloading.optik_form.data.api.model

import com.squareup.moshi.Json

/**
 * İşlem yanıt modeli.
 *
 * @property message İşlem sonucu mesajı.
 */
data class ProcessResponse(
    @Json(name = "mesaj")
    val message: String
)
