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
import com.example.receiptflow.data.interfaces.IAuthRepository
import com.example.receiptflow.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authManager: IAuthRepository = AuthManager()

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

        binding.mainLBLError.visibility = View.GONE

        binding.mainBTNLogin.setOnClickListener {
            performLogin()
        }

        binding.mainBTNCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = binding.mainETEmailAddress.text.toString().trim()
        val password = binding.mainETPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            binding.mainLBLError.visibility = View.VISIBLE
            return
        }

        binding.mainLBLError.visibility = View.GONE
        binding.mainBTNLogin.isEnabled = false

        lifecycleScope.launch {
            val result = authManager.login(email, password)
            binding.mainBTNLogin.isEnabled = true

            result.onSuccess { user ->
                val nextActivity = when (user.role) {
                    "customer" -> CustomerMenu::class.java
                    "accountant" -> AccountantMenu::class.java
                    "admin" -> ManagerMenu::class.java
                    else -> CustomerMenu::class.java
                }
                startActivity(Intent(this@MainActivity, nextActivity))
                finish()
            }.onFailure {
                binding.mainLBLError.visibility = View.VISIBLE
            }
        }
    }
}
