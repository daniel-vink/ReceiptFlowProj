package com.example.receiptflow.data

import com.example.receiptflow.data.interfaces.IReceiptRepository
import com.example.receiptflow.models.Receipt
import com.example.receiptflow.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ReceiptRepository : IReceiptRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getAssignedCustomers(accountantId: String): Result<List<User>> {
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

    override suspend fun getReceiptsForCustomer(customerId: String, year: Int, month: Int): Result<List<Receipt>> {
        return try {
            val querySnapshot = db.collection("receipts")
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val receipts = querySnapshot.toObjects(Receipt::class.java)
            Result.success(receipts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReceipt(receipt: Receipt): Result<Unit> {
        return try {
            val ref = db.collection("receipts").document()
            val finalReceipt = receipt.copy(id = ref.id)
            ref.set(finalReceipt).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReceiptStatus(receiptId: String, newStatus: String): Result<Unit> {
        return try {
            db.collection("receipts").document(receiptId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReceipt(receiptId: String): Result<Unit> {
        return try {
            db.collection("receipts").document(receiptId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
