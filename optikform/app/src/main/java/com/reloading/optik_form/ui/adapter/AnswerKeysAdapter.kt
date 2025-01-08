package com.reloading.optik_form.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reloading.optik_form.data.api.model.AnswerKeyResponse
import com.reloading.optik_form.databinding.ItemAnswerKeyBinding

/**
 * Cevap Anahtarları için RecyclerView Adapter'ı.
 *
 * @param onDeleteClick Cevap anahtarı silme işlemi için tıklama geri çağırma fonksiyonu.
 */
class AnswerKeysAdapter(
    private val onDeleteClick: (AnswerKeyResponse) -> Unit
) : ListAdapter<AnswerKeyResponse, AnswerKeysAdapter.AnswerKeyVH>(diffUtil) {

    companion object {
        /**
         * DiffUtil.ItemCallback implementasyonu, liste öğelerinin değişikliklerini optimize eder.
         */
        val diffUtil = object : DiffUtil.ItemCallback<AnswerKeyResponse>() {
            override fun areItemsTheSame(oldItem: AnswerKeyResponse, newItem: AnswerKeyResponse): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AnswerKeyResponse, newItem: AnswerKeyResponse): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerKeyVH {
        val binding =
            ItemAnswerKeyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnswerKeyVH(binding)
    }

    override fun onBindViewHolder(holder: AnswerKeyVH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder sınıfı, her bir cevap anahtarı öğesini bağlar.
     */
    inner class AnswerKeyVH(private val binding: ItemAnswerKeyBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Öğeyi View'a bağlar ve silme butonuna tıklama olayını ayarlar.
         *
         * @param item Cevap anahtarı verisi.
         */
        fun bind(item: AnswerKeyResponse) {
            binding.courseName.text = "Ders: ${item.course_name}"
            binding.questionId.text = "Soru ID: ${item.question_id}"
            binding.testGroup.text = "Grup: ${item.test_group_name}"
            binding.correctAnswerChip.text = "Cevap: ${item.correct_answer}"

            binding.buttonDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

}
