package com.msnetworkprinter

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var ipInput: EditText
    private var selectedUri: Uri? = null
    private var selectedName: String = "document"

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedUri = uri
            selectedName = queryName(uri)
            status.text = "নির্বাচিত ফাইল: $selectedName"
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val out = File(cacheDir, "camera_scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            selectedUri = Uri.fromFile(out)
            selectedName = out.name
            status.text = "ক্যামেরার ছবি প্রস্তুত: $selectedName"
        } else {
            status.text = "ক্যামেরা থেকে ছবি নেওয়া হয়নি।"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
        }
        fun label(s: String) = TextView(this).apply { text = s; textSize = 16f; setPadding(0, 12, 0, 12) }
        ipInput = EditText(this).apply {
            hint = "Printer IP (যেমন 192.168.1.50)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        status = label("এখনো কোনো ফাইল নির্বাচন করা হয়নি।")
        layout.addView(label("MS Network Printer"))
        layout.addView(label("প্রিন্টারের IP (তথ্য হিসেবে সংরক্ষণ):"))
        layout.addView(ipInput)
        layout.addView(Button(this).apply {
            text = "PDF / ছবি / ডকুমেন্ট নির্বাচন"
            setOnClickListener { pickFile.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/*")) }
        })
        layout.addView(Button(this).apply {
            text = "ক্যামেরা দিয়ে ছবি নিন"
            setOnClickListener { takePhoto.launch(null) }
        })
        layout.addView(Button(this).apply {
            text = "Android Print দিয়ে প্রিন্ট"
            setOnClickListener { startPrint() }
        })
        layout.addView(status)
        layout.addView(label("নোট: IP লিখলেই প্রিন্টার ড্রাইভার তৈরি হয় না। Android-এর Print Service থেকে Toshiba/নেটওয়ার্ক প্রিন্টার সেটআপ থাকতে হবে।"))
        setContentView(layout)
    }

    private fun startPrint() {
        val uri = selectedUri
        if (uri == null) {
            Toast.makeText(this, "আগে একটি PDF বা ছবি নির্বাচন করুন।", Toast.LENGTH_LONG).show()
            return
        }
        val mime = contentResolver.getType(uri) ?: when (selectedName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        try {
            val manager = getSystemService(PRINT_SERVICE) as PrintManager
            manager.print(
                selectedName,
                UriPrintDocumentAdapter(this, uri, mime, selectedName),
                PrintAttributes.Builder().build()
            )
            status.text = "Android Print dialog খোলা হয়েছে। সেখান থেকে প্রিন্টার নির্বাচন করুন।"
        } catch (e: Exception) {
            status.text = "প্রিন্ট শুরু করা যায়নি: ${e.localizedMessage}"
        }
    }

    private fun queryName(uri: Uri): String {
        return contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else "document" } ?: "document"
    }
}
