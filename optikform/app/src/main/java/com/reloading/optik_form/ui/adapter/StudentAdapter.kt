package com.reloading.optik_form.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.reloading.optik_form.data.api.model.CourseResult
import com.reloading.optik_form.data.api.model.Student
import com.reloading.optik_form.databinding.ItemStudentBinding

/**
 * Öğrenciler için RecyclerView Adapter'ı.
 *
 * @param onDetailClick Öğrenci detayına tıklama geri çağırma fonksiyonu.
 */
class StudentAdapter(
    private val onDetailClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var students: List<Student> = emptyList()

    /**
     * Yeni öğrenci listesini ayarlar ve RecyclerView'i günceller.
     *
     * @param list Yeni öğrenci listesi.
     */
    fun submitList(list: List<Student>) {
        students = list
        notifyDataSetChanged()
    }

    /**
     * ViewHolder sınıfı, her bir öğrenci öğesini bağlar.
     */
    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Öğrenci verisini View'a bağlar.
         *
         * @param student Öğrenci verisi.
         */
        fun bind(student: Student) {
            // Öğrenci Numarası
            binding.studentNumberTextView.text = student.studentNumber

            // "results" -> ders kodu ve puan bilgisini göstermek için
            val gradesText = student.results?.map { (dersKodu, courseResult: CourseResult) ->
                "$dersKodu: ${courseResult.overall.score}"
            }?.joinToString(", ") ?: "Not Bulunamadı"

            binding.studentGradesTextView.text = gradesText

            // Detay butonuna tıklayınca, ilgili öğrenci nesnesini geriye döndürelim
            binding.detailButton.setOnClickListener {
                onDetailClick(student)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size
}
