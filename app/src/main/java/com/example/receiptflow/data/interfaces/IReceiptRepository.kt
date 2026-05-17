package com.example.receiptflow.data.interfaces

import com.example.receiptflow.models.Receipt
import com.example.receiptflow.models.User

interface IReceiptRepository {
    suspend fun getAssignedCustomers(accountantId: String): Result<List<User>>
    suspend fun getReceiptsForCustomer(customerId: String, year: Int, month: Int): Result<List<Receipt>>
    suspend fun saveReceipt(receipt: Receipt): Result<Unit>
    suspend fun updateReceiptStatus(receiptId: String, newStatus: String): Result<Unit>
    suspend fun deleteReceipt(receiptId: String): Result<Unit>
}
