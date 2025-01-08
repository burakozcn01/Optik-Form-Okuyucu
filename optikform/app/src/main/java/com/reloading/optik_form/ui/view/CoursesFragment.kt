package com.reloading.optik_form.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.databinding.FragmentCoursesBinding
import com.reloading.optik_form.ui.adapter.CoursesAdapter
import com.reloading.optik_form.ui.viewmodel.CoursesViewModel

/**
 * Dersler ekranını yöneten Fragment.
 */
class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    // Activity scope'da ViewModel kullanmak için activityViewModels() kullanıyoruz
    private val viewModel: CoursesViewModel by activityViewModels()
    private lateinit var adapter: CoursesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView'i kurar ve ViewModel'i gözlemler
        setupRecyclerView()
        observeViewModel()

        // Yeni kurs eklemek için FAB butonuna tıklama olayını ayarlar
        binding.fabAddCourse.setOnClickListener {
            // Yeni kurs eklemek için dialog aç
            val dialog = AddCourseDialogFragment()
            dialog.show(childFragmentManager, "AddCourseDialog") // childFragmentManager kullanıldı
        }
    }

    /**
     * RecyclerView'i yapılandırır.
     */
    private fun setupRecyclerView() {
        adapter = CoursesAdapter { course ->
            // Silme işlemi için tıklama olayı
            Toast.makeText(requireContext(), "${course.name} siliniyor...", Toast.LENGTH_SHORT).show()
            viewModel.deleteCourse(course.id ?: 0)
        }
        binding.recyclerViewCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCourses.adapter = adapter
    }

    /**
     * ViewModel'den gelen verileri gözlemler ve UI'yi günceller.
     */
    private fun observeViewModel() {
        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            adapter.submitList(courses)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), "Hata: $errorMessage", Toast.LENGTH_SHORT).show()
        }

        viewModel.courseAdded.observe(viewLifecycleOwner) { added ->
            if (added) {
                Toast.makeText(requireContext(), "Kurs eklendi!", Toast.LENGTH_SHORT).show()
                // Listeyi güncellemek için ViewModel'den tekrar fetchCourses çağırır
                viewModel.fetchCourses()
            }
        }

        viewModel.courseDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                Toast.makeText(requireContext(), "Kurs silindi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
