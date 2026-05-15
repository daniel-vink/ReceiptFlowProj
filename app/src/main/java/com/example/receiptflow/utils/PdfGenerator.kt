package com.example.receiptflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.receiptflow.models.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.scale

class PdfGenerator(private val context: Context) {

    suspend fun generateReceiptsPdf(receipts: List<Receipt>, fileName: String): Result<File> {
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

                    // 1. Draw Text Header
                    paint.textSize = 18f
                    paint.isFakeBoldText = true
                    canvas.drawText("Receipt Report", 40f, 50f, paint)

                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    val dateStr = receipt.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                    canvas.drawText("Date: $dateStr", 40f, 80f, paint)
                    canvas.drawText("Status: ${receipt.status}", 40f, 100f, paint)

                    // 2. Download and Draw Image
                    val bitmap = downloadBitmap(receipt.storageUrl)
                    if (bitmap != null) {
                        val scaledBitmap = scaleBitmapToWidth(bitmap) // Leave 40px margins
                        canvas.drawBitmap(scaledBitmap, 40f, 130f, paint)
                        
                        // 3. Draw Comment below image
                        val commentY = 130f + scaledBitmap.height + 30f
                        canvas.drawText("Comment:", 40f, commentY, paint)
                        paint.textSize = 14f
                        canvas.drawText(receipt.comment.ifEmpty { "No comment provided." }, 40f, commentY + 20f, paint)
                    } else {
                        canvas.drawText("[Image could not be loaded]", 40f, 130f, paint)
                    }

                    pdfDocument.finishPage(page)
                }

                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$fileName.pdf")
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()

                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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

    private fun scaleBitmapToWidth(bitmap: Bitmap): Bitmap {
        val targetWidth = 515
        val ratio = targetWidth.toFloat() / bitmap.width.toFloat()
        val targetHeight = (bitmap.height * ratio).toInt()
        return bitmap.scale(targetWidth, targetHeight)
    }
}
