package com.example.receiptflow.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.receiptflow.models.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.OutputStream

class PdfGenerator(private val context: Context) {

    companion object {
        private const val MAX_IMAGE_WIDTH = 515
        private const val MAX_IMAGE_HEIGHT = 600
    }

    // Generate the PDF of the Receipts
    suspend fun generateReceiptsPdf(receipts: List<Receipt>, fileName: String): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val paint = Paint()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for ((index, receipt) in receipts.withIndex()) {
                    // Create a page (A4 size roughly 595x842)
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    // Use high-quality flags for the paint
                    paint.isAntiAlias = true
                    paint.isFilterBitmap = true
                    paint.isDither = true

                    // Draw text title
                    paint.textSize = 18f
                    paint.isFakeBoldText = true
                    canvas.drawText("Receipt Report", 40f, 50f, paint)

                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    val dateStr = receipt.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                    canvas.drawText("Date: $dateStr", 40f, 80f, paint)
                    canvas.drawText("Status: ${receipt.status}", 40f, 100f, paint)

                    // Download and draw image
                    val bitmap = downloadBitmap(receipt.storageUrl)
                    if (bitmap != null) {
                        val destRect = calculateDestRect(bitmap, 40f, 130f, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                        canvas.drawBitmap(bitmap, null, destRect, paint)
                        
                        // Draw comment below image
                        val commentY = destRect.bottom + 30f
                        canvas.drawText("Comment:", 40f, commentY, paint)
                        paint.textSize = 14f
                        canvas.drawText(receipt.comment.ifEmpty { "No comment provided." }, 40f, commentY + 20f, paint)
                    } else {
                        canvas.drawText("[Image could not be loaded]", 40f, 130f, paint)
                    }

                    pdfDocument.finishPage(page)
                }

                val uri = saveToPublicDownloads(pdfDocument, fileName)
                pdfDocument.close()

                if(uri != null) {
                    Result.success(uri)
                }
                else {
                    Result.failure(Exception("Could not create PDF in Download"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Save the PDF to the public downloads folder
    private fun saveToPublicDownloads(pdfDocument: PdfDocument, fileName: String): Uri? {
        var uri: Uri? = null
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        }

        uri?.let { uri ->
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            outputStream?.use { stream ->
                pdfDocument.writeTo(stream)
            }
        }
        return uri
    }

    // Connects to the URL and download the bitmap, needed to download the receipts images for the PDF
    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    // Calculates the size of image to fit into the PDF file
    private fun calculateDestRect(bitmap: Bitmap, x: Float, y: Float, maxWidth: Int, maxHeight: Int): RectF {
        val width = bitmap.width
        val height = bitmap.height

        val widthRatio = (maxWidth - 4).toFloat() / width
        val heightRatio = (maxHeight - 4).toFloat() / height
        val ratio = minOf(widthRatio, heightRatio)

        val destWidth = width * ratio
        val destHeight = height * ratio

        return RectF(x, y, x + destWidth, y + destHeight)
    }
}
