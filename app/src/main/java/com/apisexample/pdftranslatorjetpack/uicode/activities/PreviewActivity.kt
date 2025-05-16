package com.apisexample.pdftranslatorjetpack.uicode.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.apisexample.pdftranslatorjetpack.databinding.ActivityPreviewBinding
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

class PreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviewBinding
    private var translatedText: String? = null
    private var targetLangCode: String? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                downloadPdf()
            } else {
                Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        translatedText = intent.getStringExtra("translatedText")
        targetLangCode = intent.getStringExtra("targetLangCode")

        binding.previewText.text = translatedText ?: "No text available"

        binding.downloadButton.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // API 34+
                arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // API 33
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }

            else -> { // API < 33
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        } else {
            downloadPdf()
        }
    }

    private fun downloadPdf() {
        translatedText?.let { text ->
            try {
                val document = PDDocument()
                val page = PDPage()
                document.addPage(page)

                val contentStream = PDPageContentStream(document, page)
                contentStream.setFont(PDType1Font.HELVETICA, 12f)
                contentStream.beginText()
                contentStream.newLineAtOffset(50f, 700f)

                // Split text into lines to fit page
                text.lines().forEach { line ->
                    if (line.length > 80) { // Basic line wrapping
                        line.chunked(80).forEach { chunk ->
                            contentStream.showText(chunk)
                            contentStream.newLineAtOffset(0f, -15f)
                        }
                    } else {
                        contentStream.showText(line)
                        contentStream.newLineAtOffset(0f, -15f)
                    }
                }

                contentStream.endText()
                contentStream.close()

                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Translated_${System.currentTimeMillis()}.pdf"
                )
                document.save(file)
                document.close()

                Toast.makeText(this, "PDF downloaded to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("PDFGenerator", "Error downloading PDF", e)
                Toast.makeText(this, "Error downloading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "No text to download", Toast.LENGTH_SHORT).show()
    }
}