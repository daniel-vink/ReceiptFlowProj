package com.example.receiptflow.data

import com.example.receiptflow.data.interfaces.IAdminRepository
import com.example.receiptflow.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository : IAdminRepository {
    private val db = FirebaseFirestore.getInstance()

    // Get all the accountant users from Firestore
    override suspend fun getAllAccountants(): Result<List<User>> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("role", "accountant")
                .get()
                .await()
            Result.success(querySnapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all the customer users from Firestore
    override suspend fun getAllCustomers(): Result<List<User>> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .await()
            Result.success(querySnapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Assign the customer to an accountant and update in the Firestore
    override suspend fun assignCustomerToAccountant(customerId: String, accountantId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            
            val userRef = db.collection("users").document(customerId)
            batch.update(userRef, "accountantId", accountantId)
            
            val receiptsSnapshot = db.collection("receipts")
                .whereEqualTo("customerId", customerId)
                .get()
                .await()
            
            for (doc in receiptsSnapshot.documents) {
                batch.update(doc.reference, "accountantId", accountantId)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete user from Firestore
    override suspend fun deleteUser(userId: String, role: String): Result<Unit> {
        return try {
            val batch = db.batch()
            val userRef = db.collection("users").document(userId)
            batch.delete(userRef)

            if (role == "customer") {
                val receiptsSnapshot = db.collection("receipts")
                    .whereEqualTo("customerId", userId)
                    .get()
                    .await()
                for (doc in receiptsSnapshot.documents) {
                    batch.delete(doc.reference)
                }
            } else if (role == "accountant") {
                val customersSnapshot = db.collection("users")
                    .whereEqualTo("accountantId", userId)
                    .get()
                    .await()
                for (doc in customersSnapshot.documents) {
                    batch.update(doc.reference, "accountantId", null)
                }
                
                val receiptsSnapshot = db.collection("receipts")
                    .whereEqualTo("accountantId", userId)
                    .get()
                    .await()
                for (doc in receiptsSnapshot.documents) {
                    batch.update(doc.reference, "accountantId", null)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
