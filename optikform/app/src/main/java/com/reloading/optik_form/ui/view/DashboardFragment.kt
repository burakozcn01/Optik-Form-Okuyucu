package com.reloading.optik_form.ui.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.reloading.optik_form.R
import com.reloading.optik_form.databinding.FragmentDashboardBinding

/**
 * Dashboard ekranını yöneten Fragment.
 */
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDashboardBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // Answer Keys Kartına Tıklama
        binding.cardAnswerKeys.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AnswerKeysFragment())
                .addToBackStack(null)
                .commit()
        }

        // Students Kartına Tıklama
        binding.cardStudents.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, StudentsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Process Kartına Tıklama
        binding.cardProcess.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ProcessFragment())
                .addToBackStack(null)
                .commit()
        }

        // Courses Kartına Tıklama
        binding.cardCourses.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, CoursesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
