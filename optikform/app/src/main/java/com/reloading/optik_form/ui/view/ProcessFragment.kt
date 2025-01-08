package com.reloading.optik_form.ui.view

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.reloading.optik_form.R
import com.reloading.optik_form.databinding.FragmentProcessBinding
import com.reloading.optik_form.ui.state.UiState
import com.reloading.optik_form.ui.viewmodel.ProcessViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream

/**
 * Form işleme ekranını yöneten Fragment.
 */
class ProcessFragment : Fragment(R.layout.fragment_process) {
    private val viewModel: ProcessViewModel by viewModels()

    private var _binding: FragmentProcessBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    // Resim seçme işlemi için launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.selectedImageTextView.text = it.lastPathSegment
        }
    }

    private lateinit var cameraImageUri: Uri

    // Fotoğraf çekme işlemi için launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri = cameraImageUri
                binding.selectedImageTextView.text = cameraImageUri.lastPathSegment
            } else {
                Toast.makeText(requireContext(), "Fotoğraf çekilemedi.", Toast.LENGTH_SHORT).show()
            }
        }

    // Kamera izni talep etmek için launcher
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Kamera izni gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProcessBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        // Resim seçme butonuna tıklama olayını ayarlar
        binding.pickImageButton.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Fotoğraf çekme butonuna tıklama olayını ayarlar
        binding.takePhotoButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    launchCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    // İzin gerekliyse kullanıcıya açıklama yapar
                    AlertDialog.Builder(requireContext())
                        .setTitle("Kamera İzni Gerekli")
                        .setMessage("Fotoğraf çekmek için kamera iznine ihtiyacımız var.")
                        .setPositiveButton("Tamam") { _, _ ->
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                }
                else -> {
                    // İzin istemediyse direkt olarak talep eder
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        // İşleme (process) butonuna tıklama
        binding.processButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                val part = createImagePart(uri)
                viewModel.processImage(part)
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    "İşlemek için bir resim seçin veya çekin.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // **Cevap Anahtarını Çıkar** butonuna tıklama
        binding.extractAnswerKeyButton.setOnClickListener {
            // Önce uyarı diyalogu gösteriyoruz
            AlertDialog.Builder(requireContext())
                .setTitle("Uyarı")
                .setMessage("Ders ve Sütun eşleştirmesi yapılmadan bu aşamaya lütfen geçmeyin.")
                .setPositiveButton("Onayla") { _, _ ->
                    // Onayladıysa resmi yükleyip işleme devam et
                    selectedImageUri?.let { uri ->
                        val part = createImagePart(uri)
                        viewModel.extractAnswerKey(part)
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            "Cevap anahtarı çıkarmak için bir resim seçin veya çekin.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        // UI durumunu gözlemler ve günceller
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        UiState.Loading -> {
                            binding.bottomProgressBar.visibility = View.VISIBLE
                            binding.messageCard.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            binding.bottomProgressBar.visibility = View.GONE
                            binding.messageCard.visibility = View.VISIBLE
                            binding.messageTextView.text = state.data
                        }
                        is UiState.Error -> {
                            binding.bottomProgressBar.visibility = View.GONE
                            binding.messageCard.visibility = View.VISIBLE
                            binding.messageTextView.text = "Hata: ${state.message}"
                        }
                        UiState.Empty -> {
                            binding.bottomProgressBar.visibility = View.GONE
                            binding.messageCard.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    /**
     * Kamera uygulamasını başlatır.
     */
    private fun launchCamera() {
        cameraImageUri = createImageUri()
        takePictureLauncher.launch(cameraImageUri)
    }

    /**
     * Fotoğraf için URI oluşturur.
     *
     * @return Oluşturulan fotoğraf URI'si.
     */
    private fun createImageUri(): Uri {
        val imageFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "camera_image_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }

    /**
     * Seçilen veya çekilen resmi MultipartBody.Part formatına dönüştürür.
     *
     * @param uri Resmin URI'si.
     * @return MultipartBody.Part formatındaki resim parçası.
     */
    private fun createImagePart(uri: Uri): MultipartBody.Part {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: ByteArray(0)
        val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", "form.jpg", reqFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
