package com.example.receiptflow.data.interfaces

import android.net.Uri

interface IStorageRepository {
    suspend fun uploadReceiptImage(uri: Uri, customerId: String, year: Int, month: Int): Result<String>
    suspend fun deleteImage(url: String): Result<Unit>
}
