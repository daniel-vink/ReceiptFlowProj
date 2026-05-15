package com.example.receiptflow.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        
        // 1. Scale down if too large (Max width/height 1600px)
        val maxSize = 1600
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
        
        // 2. Compress quality
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        
        return outputStream.toByteArray()
    }
}
