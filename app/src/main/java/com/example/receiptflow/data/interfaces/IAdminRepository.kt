package com.example.receiptflow.data.interfaces

import com.example.receiptflow.models.User

interface IAdminRepository {
    suspend fun getAllAccountants(): Result<List<User>>
    suspend fun getAllCustomers(): Result<List<User>>
    suspend fun assignCustomerToAccountant(customerId: String, accountantId: String): Result<Unit>
    suspend fun deleteUser(userId: String, role: String): Result<Unit>
}
