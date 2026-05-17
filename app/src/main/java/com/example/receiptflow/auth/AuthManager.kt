package com.example.receiptflow.auth

import com.example.receiptflow.data.interfaces.IAuthRepository
import com.example.receiptflow.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Register a new user to Firebase Auth and Firestore
    override suspend fun register(email: String, password: String, displayName: String, role: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(
                uid = authResult.user?.uid ?: throw Exception("Auth failed"),
                email = email,
                displayName = displayName,
                role = role
            )
            db.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Authenticate user on login and get the profile from Firestore
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Auth failed")
            val document = db.collection("users").document(uid).get().await()
            val user = document.toObject(User::class.java) ?: throw Exception("User profile not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out the current user
    override fun logout() {
        auth.signOut()
    }

    // Check if a user is currently logged in
    override fun isUserLoggedIn(): Boolean = auth.currentUser != null
}
