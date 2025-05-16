package com.apisexample.pdftranslatorjetpack.uicode.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.apisexample.pdftranslatorjetpack.databinding.ActivityPdfBinding
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File

class PdfActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfBinding

    private var pdfUri: Uri? = null
    private lateinit var translator: Translator
    private var translatedText: String? = null
    private var tempPdfFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnSelectPdf.setOnClickListener { selectPdf() }
        binding.btnTranslate.setOnClickListener { pdfUri?.let { translatePdf(it) } }
        binding.btnDownload.setOnClickListener {
            translatedText?.let {
                createTempPdfAndDownload(it)
            }
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.GUJARATI)
            .build()
        translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener { binding.tvStatus.text = "Gujarati model downloaded." }
            .addOnFailureListener { binding.tvStatus.text = "Model download failed." }
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            pdfUri = data?.data
            if (pdfUri != null) {
                binding.tvStatus.text = "PDF selected."
                binding.btnTranslate.isEnabled = true
            }
        }
    }

//    private fun translatePdf(uri: Uri) {
//        binding.tvStatus.text = "Reading PDF..."
//
//        val inputStream = contentResolver.openInputStream(uri)
//        val reader = PdfReader(inputStream)
//        val pdfDoc = PdfDocument(reader)
//        val totalText = StringBuilder()
//
//        for (i in 1..pdfDoc.numberOfPages) {
//            val page = pdfDoc.getPage(i)
//            val text = PdfTextExtractor.getTextFromPage(page)
//            totalText.append(text).append("\n\n")
//        }
//
//        pdfDoc.close()
//
//        binding.tvStatus.text = "Translating..."
//
//        translator.translate(totalText.toString())
//            .addOnSuccessListener { translatedText ->
//                binding.tvStatus.text = "Translation complete. Saving PDF..."
//                saveTranslatedPdf(translatedText)
//            }
//            .addOnFailureListener {
//                binding.tvStatus.text = "Translation failed: ${it.message}"
//            }
//    }


    private fun translatePdf(uri: Uri) {
        // Same as before...
        val totalText = StringBuilder()
        translator.translate(totalText.toString())
            .addOnSuccessListener { text ->
                translatedText = text
                binding.tvStatus.text = "Translation complete. Ready to download."
                binding.btnDownload.isEnabled = true
            }
            .addOnFailureListener {
                binding.tvStatus.text = "Translation failed: ${it.message}"
            }
    }

    private fun saveTranslatedPdf(text: String) {
        try {
            val fileName = "translated_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)

            val writer = PdfWriter(file)
            val pdfDoc = PdfDocument(writer)
            val doc = com.itextpdf.layout.Document(pdfDoc)

            doc.add(Paragraph(text))
            doc.close()

            binding.tvStatus.text = "PDF saved: ${file.absolutePath}"
            Toast.makeText(this, "Saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            binding.tvStatus.text = "Error saving PDF: ${e.message}"
        }
    }

    private fun createTempPdfAndDownload(text: String) {
        try {
            val fileName = "translated_${System.currentTimeMillis()}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            val writer = PdfWriter(file)
            val pdfDoc = PdfDocument(writer)
            val doc = Document(pdfDoc)
            doc.add(Paragraph(text))
            doc.close()

            binding.tvStatus.text = "PDF saved to Downloads"
            Toast.makeText(this, "Saved to Downloads: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)

        } catch (e: Exception) {
            binding.tvStatus.text = "Failed to save: ${e.message}"
        }
    }

}