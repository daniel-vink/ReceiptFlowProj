package com.example.receiptflow.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class StorageRepository(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadReceiptImage(uri: Uri, customerId: String, year: Int, month: Int): Result<String> {
        return try {
            val compressedData = compressImage(uri) ?: throw Exception("Compression failed")
            
            val fileName = "${UUID.randomUUID()}.jpg"
            val path = "receipts/$customerId/$year/$month/$fileName"
            val ref = storage.reference.child(path)
            
            ref.putBytes(compressedData).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
        var originalBitmap = BitmapFactory.decodeStream(inputStream)
        
        // Fix rotation based on EXIF
        originalBitmap = rotateImageIfRequired(originalBitmap, uri)
        
        // 1. Scale down if too large (Optimized for balance between detail and storage)
        val maxSize = 2000
        var width = originalBitmap.width
        var height = originalBitmap.height
        
        if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            if (width > height) {
                width = maxSize
                height = (maxSize / ratio).toInt()
            } else {
                height = maxSize
                width = (maxSize * ratio).toInt()
            }
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        
        // 2. Compress quality (85% is the 'sweet spot' for storage vs clarity)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        
        return outputStream.toByteArray()
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val ei = ExifInterface(inputStream)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    suspend fun deleteImage(url: String): Result<Unit> {
        return try {
            storage.getReferenceFromUrl(url).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
