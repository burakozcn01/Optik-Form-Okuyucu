package com.reloading.optik_form.ui.state

/**
 * UI Durumlarını temsil eden sealed class.
 *
 * @param T Veri türü.
 */
sealed class UiState<out T> {
    /**
     * Yükleme durumu.
     */
    object Loading : UiState<Nothing>()

    /**
     * Boş durum.
     */
    object Empty : UiState<Nothing>()

    /**
     * Başarılı durum ve veri.
     *
     * @param data Başarılı durumda alınan veri.
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Hata durumu ve mesajı.
     *
     * @param message Hata mesajı.
     */
    data class Error(val message: String) : UiState<Nothing>()
}
