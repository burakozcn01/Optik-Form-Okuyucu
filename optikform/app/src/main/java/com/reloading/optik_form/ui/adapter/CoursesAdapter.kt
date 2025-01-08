package com.reloading.optik_form.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.databinding.ItemCourseBinding

/**
 * Dersler için RecyclerView Adapter'ı.
 *
 * @param onDeleteClick Ders silme işlemi için tıklama geri çağırma fonksiyonu.
 */
class CoursesAdapter(
    private val onDeleteClick: (Course) -> Unit
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    private var courses = listOf<Course>()

    /**
     * Yeni ders listesini ayarlar ve RecyclerView'i günceller.
     *
     * @param newCourses Yeni ders listesi.
     */
    fun submitList(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }

    /**
     * ViewHolder sınıfı, her bir ders öğesini bağlar.
     */
    inner class CourseViewHolder(private val binding: ItemCourseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        /**
         * Ders verisini View'a bağlar.
         *
         * @param course Ders verisi.
         */
        fun bind(course: Course) {
            binding.textViewCourseName.text = course.name
            binding.textViewCourseCode.text = course.code
            binding.textViewCourseDescription.text = course.description ?: "Açıklama yok"
            binding.textViewTestGroup.text = mapTestGroup(course.testGroup)
            binding.textViewColumnNumber.text = "Sütun Numarası: ${course.columnNumber}" // Non-nullable kullanım

            binding.buttonDeleteCourse.setOnClickListener {
                onDeleteClick(course)
            }
        }

        /**
         * Test grubu numarasını harf karşılığına çevirir.
         *
         * @param testGroup Test grubu numarası.
         * @return Test grubu harf karşılığı.
         */
        private fun mapTestGroup(testGroup: Int?): String {
            return when (testGroup) {
                1 -> "A"
                2 -> "B"
                3 -> "C"
                4 -> "D"
                else -> "Bilinmiyor"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding =
            ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size
}
