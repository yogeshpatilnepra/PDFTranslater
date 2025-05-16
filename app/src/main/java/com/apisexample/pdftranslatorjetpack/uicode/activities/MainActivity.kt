package com.apisexample.pdftranslatorjetpack.uicode.activities

//import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.text.PDFTextStripper
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.apisexample.pdftranslatorjetpack.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedPdfUri: Uri? = null
    private var extractedText: String? = null
    private val languages = listOf(
        "English" to "en",
        "Spanish" to "es",
        "French" to "fr",
        "German" to "de",
        "Chinese" to "zh",
        "Japanese" to "ja",
        "Hindi" to "hi"
    )
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allgranted = permissions.all { it.value }
            if (allgranted) {
                openPdfPicker()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkAndRequestPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.INTERNET
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // API 33
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.INTERNET
                )
            }

            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
                )
            }
        }
        val permissionsToRequest = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // Permissions already  already granted, proceed with logic
            openPdfPicker()
        }
    }

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedPdfUri = it
                Toast.makeText(this, "PDF selected", Toast.LENGTH_SHORT).show()
                extractTextFromPdf(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLanguageDropdown()
        setupButtons()
    }

    private fun setupLanguageDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            languages.map { it.first }
        )
        (binding.languageDropdown as AutoCompleteTextView).setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.uploadButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openPdfPicker()
            } else {
                checkAndRequestPermissions()
            }
        }

        binding.translateButton.setOnClickListener {
            val selectedLanguage = binding.languageDropdown.text.toString()
            val targetLangCode = languages.find { it.first == selectedLanguage }?.second
            if (selectedPdfUri == null) {
                Toast.makeText(this, "Please upload a PDF", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (targetLangCode == null) {
                Toast.makeText(this, "Please select a language", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            translateAndNavigate(targetLangCode)
        }
    }

    private fun openPdfPicker() {
        pickPdfLauncher.launch("application/pdf")
    }

    private fun extractTextFromPdf(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val pdfReader = com.itextpdf.kernel.pdf.PdfReader(inputStream)
                    val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(pdfReader)
                    try {
                        val textBuilder = StringBuilder()
                        for (pageNum in 1..pdfDocument.numberOfPages) {
                            val page = pdfDocument.getPage(pageNum)
                            val text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page)
                            textBuilder.append(text).append("\n")
                        }
                        extractedText = textBuilder.toString().ifEmpty { "No text extracted" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Text extracted", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pdfDocument.close()
                        pdfReader.close()
                    }
                } ?: throw IOException("Failed to open PDF")
            } catch (e: Exception) {
                Log.e("PDFExtractor", "Error extracting text", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error extracting text: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun translateAndNavigate(targetLangCode: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Placeholder for translation (use a free API like LibreTranslate)
                val translatedText = translateText(extractedText ?: "", targetLangCode)
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MainActivity, PreviewActivity::class.java)
                    intent.putExtra("translatedText", translatedText)
                    intent.putExtra("targetLangCode", targetLangCode)
                    startActivity(intent)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Translation failed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    //    private suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
//        if (text.isEmpty()) return@withContext ""
//        val client = OkHttpClient()
//        val json = JSONObject().apply {
//            put("q", text)
//            put("source", "auto")
//            put("target", targetLang)
//        }
//        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
//        val request = Request.Builder()
//            .url("https://libretranslate.de/translate")
//            .post(requestBody)
//            .build()
//
//        client.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) throw IOException("Translation API error: ${response.code}")
//            val responseBody = response.body?.string() ?: throw IOException("Empty response")
//            val jsonResponse = JSONObject(responseBody)
//            return@withContext jsonResponse.getString("translatedText")
//        }
//    }
    private suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isEmpty()) return@withContext ""
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val url = "https://api.mymemory.translated.net/get?q=${URLEncoder.encode(text.take(500), "UTF-8")}&langpair=auto|$targetLang"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("Translator", "API error: ${response.code} - ${response.message}")
                    throw IOException("Translation API error: ${response.code}")
                }
                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                Log.d("Translator", "Response: $responseBody")
                val jsonResponse = JSONObject(responseBody)
                val translatedText = jsonResponse.getJSONObject("responseData").getString("translatedText")
                return@withContext translatedText
            }
        } catch (e: Exception) {
            Log.e("Translator", "Translation failed", e)
            throw IOException("Translation failed: ${e.message}")
        }
    }
}