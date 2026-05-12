package com.example.receiptflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.auth.AuthManager
import com.example.receiptflow.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.textView.visibility = View.GONE

        binding.button.setOnClickListener {
            performLogin()
        }

        binding.button2.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmailAddress.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            binding.textView.visibility = View.VISIBLE
            return
        }

        binding.textView.visibility = View.GONE
        binding.button.isEnabled = false

        lifecycleScope.launch {
            val result = authManager.login(email, password)
            binding.button.isEnabled = true

            result.onSuccess { user ->
                val nextActivity = when (user.role) {
                    "customer" -> CustomerActionScreen::class.java
                    "accountant" -> AccountantSelect::class.java
                    "admin" -> ManagerUsersManagement::class.java
                    else -> CustomerActionScreen::class.java
                }
                startActivity(Intent(this@MainActivity, nextActivity))
                finish()
            }.onFailure {
                binding.textView.visibility = View.VISIBLE
            }
        }
    }
}
