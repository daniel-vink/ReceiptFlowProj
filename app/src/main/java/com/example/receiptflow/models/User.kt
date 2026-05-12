package com.example.receiptflow.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "customer",
    val accountantId: String? = null
)
