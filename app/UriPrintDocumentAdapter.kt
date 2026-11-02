package com.msnetworkprinter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.print.PageRange
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

class UriPrintDocumentAdapter(
    private val context: Context,
    private val uri: Uri,
    private val mime: String,
    private val title: String
) : PrintDocumentAdapter() {
    private var pdfFile: File? = null
    private var renderer: PdfRenderer? = null
    private var imageBytes: ByteArray? = null
    private var pageCount = 1

    override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes,
                          cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
        try {
            val input = if (uri.scheme == "file") java.io.File(uri.path!!).inputStream() else context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("ফাইল খোলা যায়নি")
            val bytes = input.use { it.readBytes() }
            if (mime == "application/pdf" || title.endsWith(".pdf", true)) {
                val f = File(context.cacheDir, "print_${System.currentTimeMillis()}.pdf")
                f.writeBytes(bytes); pdfFile = f
                renderer?.close()
                renderer = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
                pageCount = renderer!!.pageCount
            } else {
                imageBytes = bytes
                pageCount = 1
            }
            val info = PrintDocumentInfo.Builder(title).setContentType(
                if (mime == "application/pdf") PrintDocumentInfo.CONTENT_TYPE_DOCUMENT else PrintDocumentInfo.CONTENT_TYPE_PHOTO
            ).setPageCount(pageCount).build()
            callback.onLayoutFinished(info, true)
        } catch (e: Exception) {
            callback.onLayoutFailed(e.localizedMessage)
        }
    }

    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor,
                         cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
        val attrs = android.print.PrintAttributes.Builder().build()
        val doc = PrintedPdfDocument(context, attrs)
        try {
            for (i in 0 until pageCount) {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled(); doc.close(); return
                }
                val page = doc.startPage(i)
                val canvas: Canvas = page.canvas
                val pdf = renderer
                if (pdf != null) {
                    pdf.openPage(i).use { p ->
                        val scale = minOf(canvas.width.toFloat() / p.width, canvas.height.toFloat() / p.height)
                        val w = (p.width * scale).toInt(); val h = (p.height * scale).toInt()
                        val dest = Rect((canvas.width-w)/2, (canvas.height-h)/2, (canvas.width+w)/2, (canvas.height+h)/2)
                        p.render(canvas, dest, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    }
                } else {
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
                        ?: throw IllegalArgumentException("ছবিটি পড়া যাচ্ছে না")
                    val scale = minOf(canvas.width.toFloat()/bitmap.width, canvas.height.toFloat()/bitmap.height)
                    val w=(bitmap.width*scale).toInt(); val h=(bitmap.height*scale).toInt()
                    canvas.drawBitmap(bitmap, null, Rect((canvas.width-w)/2,(canvas.height-h)/2,(canvas.width+w)/2,(canvas.height+h)/2), null)
                    bitmap.recycle()
                }
                doc.finishPage(page)
            }
            FileOutputStream(destination.fileDescriptor).use { doc.writeTo(it) }
            callback.onWriteFinished(arrayOf(PageRange(0, pageCount-1)))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage)
        } finally {
            doc.close()
        }
    }

    override fun onFinish() {
        renderer?.close()
        pdfFile?.delete()
        super.onFinish()
    }
}
