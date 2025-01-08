package com.reloading.optik_form.ui.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.reloading.optik_form.R
import com.reloading.optik_form.data.api.model.AnswerKeyRequest
import com.reloading.optik_form.data.api.model.AnswerKeyResponse
import com.reloading.optik_form.data.api.model.Course
import com.reloading.optik_form.databinding.FragmentAnswerKeysBinding
import com.reloading.optik_form.ui.adapter.AnswerKeysAdapter
import com.reloading.optik_form.ui.state.UiState
import com.reloading.optik_form.ui.viewmodel.AnswerKeysViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Cevap Anahtarları ekranını yöneten Fragment.
 */
class AnswerKeysFragment : Fragment(R.layout.fragment_answer_keys) {
    // ViewModel'ı initialize eder
    private val viewModel: AnswerKeysViewModel by viewModels()
    private lateinit var adapter: AnswerKeysAdapter

    private var _binding: FragmentAnswerKeysBinding? = null
    private val binding get() = _binding!!

    // Test gruplarını temsil eden veri sınıfı
    private val testGroups = listOf(
        TestGroup(1, "A"),
        TestGroup(2, "B"),
        TestGroup(3, "C"),
        TestGroup(4, "D")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAnswerKeysBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // Adapter'ı oluşturur ve silme işlemi için geri çağırma fonksiyonunu tanımlar
        adapter = AnswerKeysAdapter { answerKey ->
            showDeleteConfirmationDialog(answerKey)
        }

        // RecyclerView'i yapılandırır
        binding.answerKeysRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.answerKeysRecyclerView.adapter = adapter

        // UI durumunu gözlemler ve günceller
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            binding.answerKeysProgressBar.visibility = View.VISIBLE
                            binding.answerKeysEmptyView.visibility = View.GONE
                        }
                        UiState.Empty -> {
                            binding.answerKeysProgressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            binding.answerKeysEmptyView.visibility = View.VISIBLE
                        }
                        is UiState.Success -> {
                            binding.answerKeysProgressBar.visibility = View.GONE
                            binding.answerKeysEmptyView.visibility = View.GONE
                            adapter.submitList(state.data)
                        }
                        is UiState.Error -> {
                            binding.answerKeysProgressBar.visibility = View.GONE
                            binding.answerKeysEmptyView.visibility = View.VISIBLE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Ders durumunu gözlemler ve hata mesajlarını gösterir
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.coursesState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            // Yükleniyor durumunda yapılacak bir şey yok
                        }
                        UiState.Empty -> {
                            Toast.makeText(requireContext(), "Ders bulunamadı.", Toast.LENGTH_SHORT).show()
                        }
                        is UiState.Success -> {
                            // Başarılı durumunda yapılacak bir şey yok
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), "Dersler yüklenemedi: ${state.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Yeni cevap anahtarı eklemek için FAB butonuna tıklama olayını ayarlar
        binding.fabAddAnswerKey.setOnClickListener {
            showCreateAnswerKeyDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Cevap anahtarı silme onayı dialogunu gösterir.
     *
     * @param answerKey Silinecek cevap anahtarı verisi.
     */
    private fun showDeleteConfirmationDialog(answerKey: AnswerKeyResponse) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Silmek İstediğine Emin misin?")
            .setMessage("Bu cevap anahtarını silmek istediğinden emin misin?")
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Sil") { dialog, _ ->
                answerKey.id?.let { viewModel.deleteAnswerKey(it) }
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Yeni cevap anahtarı oluşturmak için dialogu gösterir.
     */
    private fun showCreateAnswerKeyDialog() {
        val coursesState = viewModel.coursesState.value
        if (coursesState !is UiState.Success) {
            Toast.makeText(requireContext(), "Dersler yükleniyor veya yüklenemedi.", Toast.LENGTH_SHORT).show()
            return
        }

        val courses = coursesState.data

        // Dialog görünümünü inflate eder
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_answer_key, null)
        val spinnerTestGroup = dialogView.findViewById<Spinner>(R.id.spinnerTestGroup)
        val spinnerCourse = dialogView.findViewById<Spinner>(R.id.spinnerCourse)
        val editQuestionId = dialogView.findViewById<EditText>(R.id.editQuestionId)
        val editCorrectAnswer = dialogView.findViewById<EditText>(R.id.editCorrectAnswer)

        // Test grubu spinner'ını ayarlar
        val testGroupNames = testGroups.map { it.name }
        val testGroupAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, testGroupNames)
        testGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTestGroup.adapter = testGroupAdapter

        // Ders spinner'ını ayarlar
        val courseNames = courses.map { it.name }
        val courseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseNames)
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = courseAdapter

        // Dialogu oluşturur ve gösterir
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yeni Cevap Anahtarı Ekle")
            .setView(dialogView)
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Ekle") { dialog, _ ->
                val selectedTestGroupPosition = spinnerTestGroup.selectedItemPosition
                val selectedTestGroup = if (selectedTestGroupPosition >= 0 && selectedTestGroupPosition < testGroups.size) {
                    testGroups[selectedTestGroupPosition].id
                } else {
                    null
                }

                val selectedCoursePosition = spinnerCourse.selectedItemPosition
                val selectedCourse = if (selectedCoursePosition >= 0 && selectedCoursePosition < courses.size) {
                    courses[selectedCoursePosition].id
                } else {
                    null
                }

                val questionId = editQuestionId.text.toString().toIntOrNull()
                val correctAnswer = editCorrectAnswer.text.toString().uppercase()

                // Girdi doğrulaması yapar
                if (selectedTestGroup != null && selectedCourse != null && questionId != null && correctAnswer in listOf("A", "B", "C", "D", "E")) {
                    val newAnswerKeyRequest = AnswerKeyRequest(
                        question_id = questionId,
                        correct_answer = correctAnswer,
                        test_group = selectedTestGroup,
                        course = selectedCourse
                    )
                    viewModel.createAnswerKey(newAnswerKeyRequest)
                } else {
                    Toast.makeText(requireContext(), "Geçerli veriler giriniz.", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .show()
    }

    /**
     * Test grubu verisini temsil eden veri sınıfı.
     *
     * @param id Test grubu kimliği.
     * @param name Test grubu adı.
     */
    data class TestGroup(val id: Int, val name: String)
}
