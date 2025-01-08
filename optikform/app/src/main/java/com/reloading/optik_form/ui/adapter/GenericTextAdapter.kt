package com.reloading.optik_form.ui.adapter

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Generic Text RecyclerView Adapter'ı.
 *
 * @param T Veri türü.
 * @param textMapper Veriyi String'e dönüştüren fonksiyon.
 */
class GenericTextAdapter<T : Any>(private val textMapper: (T) -> String) :
    ListAdapter<T, GenericTextVH>(object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericTextVH {
        val tv = TextView(parent.context).apply {
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        return GenericTextVH(tv)
    }

    override fun onBindViewHolder(holder: GenericTextVH, position: Int) {
        holder.bind(textMapper(getItem(position)))
    }
}

/**
 * Generic Text ViewHolder'ı.
 *
 * @param textView Metni göstermek için kullanılan TextView.
 */
class GenericTextVH(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
    /**
     * Metni View'a bağlar.
     *
     * @param text Gösterilecek metin.
     */
    fun bind(text: String) {
        textView.text = text
    }
}
