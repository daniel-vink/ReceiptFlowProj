package com.example.receiptflow.data.interfaces

import com.example.receiptflow.models.User

interface IAuthRepository {
    suspend fun register(email: String, password: String, displayName: String, role: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<User>
    fun logout()
    fun isUserLoggedIn(): Boolean
}
