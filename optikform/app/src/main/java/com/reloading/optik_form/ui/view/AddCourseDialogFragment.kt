package com.reloading.optik_form.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.R
import com.reloading.optik_form.databinding.DialogAddCourseBinding
import com.reloading.optik_form.ui.viewmodel.CoursesViewModel
import android.util.Log

/**
 * Yeni ders eklemek için kullanılan DialogFragment.
 */
class AddCourseDialogFragment : DialogFragment() {

    private var _binding: DialogAddCourseBinding? = null
    private val binding get() = _binding!!

    // Activity scope'da ViewModel kullanmak için activityViewModels() kullanıyoruz
    private val viewModel: CoursesViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullWidthDialog) // Özel tema uygulandı
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogAddCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()

        binding.buttonAddCourse.setOnClickListener {
            addCourse()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Gözat butonunu klavye işlemi için ayarlama
        binding.editTextColumnNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCourse()
                true
            } else {
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Test grubu spinner'ını ayarlar.
     */
    private fun setupSpinner() {
        val testGroups = listOf("A", "B", "C", "D")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, testGroups)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTestGroup.adapter = adapter
    }

    /**
     * Yeni bir ders eklemek için gerekli verileri toplar ve ViewModel üzerinden ekler.
     */
    private fun addCourse() {
        val name = binding.editTextCourseName.text.toString().trim()
        val code = binding.editTextCourseCode.text.toString().trim()
        val description = binding.editTextCourseDescription.text.toString().trim()
        val columnNumberText = binding.editTextColumnNumber.text.toString().trim()
        val testGroupText = binding.spinnerTestGroup.selectedItem.toString()

        Log.d("AddCourseDialog", "Add button clicked with name: $name, code: $code")

        // Kod alanı için karakter sınırını kontrol et
        if (code.length > 10) {
            Toast.makeText(requireContext(), "Kurs Kodu en fazla 10 karakter olabilir.", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isEmpty() || code.isEmpty() || columnNumberText.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val columnNumber = columnNumberText.toIntOrNull()
        if (columnNumber == null || columnNumber < 1) {
            Toast.makeText(requireContext(), "Geçerli bir sütun numarası girin", Toast.LENGTH_SHORT).show()
            return
        }

        val testGroup = when (testGroupText) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            "D" -> 4
            else -> {
                Toast.makeText(requireContext(), "Geçerli bir test grubu seçin", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val newCourse = Course(
            name = name,
            code = code,
            description = description,
            testGroup = testGroup,
            columnNumber = columnNumber
        )

        Log.d("AddCourseDialog", "Creating course: $newCourse")

        viewModel.addCourse(newCourse)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
