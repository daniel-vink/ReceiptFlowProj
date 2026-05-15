package com.example.receiptflow.models

import com.google.firebase.Timestamp

data class Receipt(
    val id: String = "",
    val customerId: String = "",
    val accountantId: String = "",
    val timestamp: Timestamp? = null,
    val year: Int = 0,
    val month: Int = 0,
    val storageUrl: String = "",
    val comment: String = "",
    val status: String = "pending"
)
