package com.example.receiptflow.data

import com.example.receiptflow.models.Receipt
import com.example.receiptflow.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReceiptRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAssignedCustomers(accountantId: String): Result<List<User>> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("role", "customer")
                .whereEqualTo("accountantId", accountantId)
                .get()
                .await()
            val customers = querySnapshot.toObjects(User::class.java)
            Result.success(customers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReceiptsForCustomer(customerId: String, year: Int, month: Int): Result<List<Receipt>> {
        return try {
            val querySnapshot = db.collection("receipts")
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .get()
                .await()
            val receipts = querySnapshot.toObjects(Receipt::class.java)
            Result.success(receipts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
