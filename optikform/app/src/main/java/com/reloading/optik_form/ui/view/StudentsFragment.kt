package com.reloading.optik_form.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reloading.optik_form.R
import com.reloading.optik_form.data.api.model.Student
import com.reloading.optik_form.data.api.model.StudentAnswer
import com.reloading.optik_form.databinding.DialogStudentDetailsBinding
import com.reloading.optik_form.databinding.FragmentStudentsBinding
import com.reloading.optik_form.ui.adapter.StudentAdapter
import com.reloading.optik_form.ui.state.UiState
import com.reloading.optik_form.ui.viewmodel.StudentsViewModel
import kotlinx.coroutines.launch

/**
 * Öğrenciler ekranını yöneten Fragment.
 */
class StudentsFragment : Fragment(R.layout.fragment_students) {
    private val viewModel: StudentsViewModel by viewModels()
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!

    // Adapter'ı initialize eder ve detay tıklama olayını tanımlar
    private val adapter = StudentAdapter { student ->
        showStudentDetails(student)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStudentsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView'i kurar
        binding.studentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.studentsRecyclerView.adapter = adapter

        // ViewModel'den gelen öğrenciler listesinin durumunu gözlemler
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            binding.studentsProgressBar.visibility = View.VISIBLE
                            binding.studentsEmptyView.visibility = View.GONE
                        }
                        UiState.Empty -> {
                            binding.studentsProgressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            binding.studentsEmptyView.visibility = View.VISIBLE
                        }
                        is UiState.Success -> {
                            binding.studentsProgressBar.visibility = View.GONE
                            binding.studentsEmptyView.visibility = View.GONE
                            adapter.submitList(state.data)
                        }
                        is UiState.Error -> {
                            binding.studentsProgressBar.visibility = View.GONE
                            binding.studentsEmptyView.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        // Öğrenci verilerini yükler
        viewModel.loadStudents()
    }

    /**
     * Öğrenci detaylarını gösteren dialogu açar.
     *
     * @param student Detayları gösterilecek öğrenci verisi.
     */
    private fun showStudentDetails(student: Student) {
        val dialogBinding = DialogStudentDetailsBinding.inflate(LayoutInflater.from(requireContext()))
        val dialogView = dialogBinding.root

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Öğrenci Detayları")
            .setView(dialogView)
            .setPositiveButton("Kapat") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()

        // Öğrencinin not ve cevap bilgilerini çağırıyoruz
        viewModel.loadGrades(student.id)
        viewModel.loadAnswers(student.studentNumber)

        // 1) NOT DURUMUNU GÖZLEMLEME
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.gradesState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            dialogBinding.gradesContainer.removeAllViews()
                            val loadingText = TextView(requireContext()).apply {
                                text = "Notlar yükleniyor..."
                                textSize = 16f
                            }
                            dialogBinding.gradesContainer.addView(loadingText)
                        }
                        UiState.Empty -> {
                            dialogBinding.gradesContainer.removeAllViews()
                            val emptyText = TextView(requireContext()).apply {
                                text = "Not Bulunamadı"
                                setTextColor(resources.getColor(android.R.color.darker_gray))
                                textSize = 16f
                            }
                            dialogBinding.gradesContainer.addView(emptyText)
                        }
                        is UiState.Success -> {
                            dialogBinding.gradesContainer.removeAllViews()
                            state.data?.forEach { (dersKodu, puan) ->
                                val gradeText = TextView(requireContext()).apply {
                                    text = "$dersKodu: $puan"
                                    textSize = 16f
                                }
                                dialogBinding.gradesContainer.addView(gradeText)
                            }
                        }
                        is UiState.Error -> {
                            dialogBinding.gradesContainer.removeAllViews()
                            val errorText = TextView(requireContext()).apply {
                                text = "Notlar yüklenirken hata oluştu!"
                                setTextColor(resources.getColor(android.R.color.holo_red_dark))
                                textSize = 16f
                            }
                            dialogBinding.gradesContainer.addView(errorText)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        // 2) CEVAPLAR DURUMUNU GÖZLEMLEME – Yeni Tasarım
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.answersState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            dialogBinding.answersContainer.removeAllViews()
                            val loadingText = TextView(requireContext()).apply {
                                text = "Cevaplar yükleniyor..."
                                textSize = 16f
                            }
                            dialogBinding.answersContainer.addView(loadingText)
                        }
                        UiState.Empty -> {
                            dialogBinding.answersContainer.removeAllViews()
                            val emptyText = TextView(requireContext()).apply {
                                text = "Cevap Bulunamadı"
                                setTextColor(resources.getColor(android.R.color.darker_gray))
                                textSize = 16f
                            }
                            dialogBinding.answersContainer.addView(emptyText)
                        }
                        is UiState.Success -> {
                            dialogBinding.answersContainer.removeAllViews()

                            // Her bir "answer" için item_answer.xml'i inflate edip değerleri dolduruyoruz
                            state.data.forEach { answer ->
                                val isCorrect = answer.is_correct == true
                                val colorRes = if (isCorrect) {
                                    android.R.color.holo_green_dark
                                } else {
                                    android.R.color.holo_red_dark
                                }

                                // == ÖNEMLİ KISIM: CardView inflate ediyoruz ==
                                val answerView = layoutInflater.inflate(
                                    R.layout.item_answer,
                                    dialogBinding.answersContainer,
                                    false
                                )

                                // item_answer içindeki View'ları buluyoruz
                                val courseCodeText = answerView.findViewById<TextView>(R.id.courseCodeText)
                                val questionIdText = answerView.findViewById<TextView>(R.id.questionIdText)
                                val selectedAnswerText = answerView.findViewById<TextView>(R.id.selectedAnswerText)
                                val correctnessText = answerView.findViewById<TextView>(R.id.correctnessText)

                                // "Ders Kodu" -> answer.course_code olduğunu varsayıyorum
                                // Eğer modelde başka alan varsa (ör. answer.lessonName), orayı kullanın.
                                val dersKodu = answer.course ?: "Bilinmiyor"

                                courseCodeText.text = "Ders: $dersKodu"
                                questionIdText.text = "Soru: ${answer.question_id}"

                                val secilenYanit = answer.selected_answer ?: "Yanıt Yok"
                                selectedAnswerText.text = "Seçilen Yanıt: $secilenYanit"

                                val dogruYanitMetni = if (isCorrect) "Doğru" else "Yanlış"
                                correctnessText.text = dogruYanitMetni
                                correctnessText.setTextColor(resources.getColor(colorRes))

                                // Artık "Öğrenci: ..." bilgisi yok, istek üzerine kaldırdık
                                // Onun yerine "dersKodu" göstermiş olduk.

                                // Şimdi bu item'ı "answersContainer" içine ekleyelim
                                dialogBinding.answersContainer.addView(answerView)
                            }
                        }
                        is UiState.Error -> {
                            dialogBinding.answersContainer.removeAllViews()
                            val errorText = TextView(requireContext()).apply {
                                text = "Cevaplar yüklenirken hata oluştu!"
                                setTextColor(resources.getColor(android.R.color.holo_red_dark))
                                textSize = 16f
                            }
                            dialogBinding.answersContainer.addView(errorText)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
