package com.example.receiptflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.auth.AuthManager
import com.example.receiptflow.data.interfaces.IAuthRepository
import com.example.receiptflow.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val authManager: IAuthRepository = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.registerBTNSend.setOnClickListener {
            performRegistration()
        }
    }

    // Create the new user
    private fun performRegistration() {
        val name = binding.registerETFullName.text.toString().trim()
        val email = binding.registerETEmail.text.toString().trim()
        val password = binding.registerETPassword.text.toString().trim()
        val role = if (binding.registerRadioCustomer.isChecked) "customer" else "accountant"

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerProgressBar.visibility = View.VISIBLE
        binding.registerBTNSend.isEnabled = false

        lifecycleScope.launch {
            val result = authManager.register(email, password, name, role)
            binding.registerProgressBar.visibility = View.GONE
            binding.registerBTNSend.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "Success!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()
            }.onFailure { e ->
                Toast.makeText(this@RegisterActivity, e.message ?: getString(R.string.registration_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
