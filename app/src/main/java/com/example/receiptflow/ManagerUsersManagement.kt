package com.example.receiptflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.adapters.ManagerUserAdapter
import com.example.receiptflow.data.AdminRepository
import com.example.receiptflow.databinding.ActivityManagerUsersManagementBinding
import com.example.receiptflow.databinding.DialogAssignAccountantBinding
import com.example.receiptflow.models.User
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ManagerUsersManagement : AppCompatActivity() {
    private lateinit var binding: ActivityManagerUsersManagementBinding
    private val repository = AdminRepository()
    private lateinit var adapter: ManagerUserAdapter
    
    private var allAccountants: List<User> = emptyList()
    private var currentTab = 0 // 0 for Customers, 1 for Accountants

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerUsersManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupTabs()
        fetchData()
    }

    private fun setupRecyclerView() {
        adapter = ManagerUserAdapter(
            users = emptyList(),
            onAssignClicked = { customer -> showAssignDialog(customer) },
            onDeleteClicked = { user -> showDeleteConfirmation(user) }
        )
        binding.recyclerViewUsers.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                fetchData()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            if (currentTab == 0) {
                repository.getAllCustomers().onSuccess { customers ->
                    adapter.updateData(customers)
                }.onFailure { showToast("Failed to fetch customers") }
            } else {
                repository.getAllAccountants().onSuccess { accountants ->
                    adapter.updateData(accountants)
                    allAccountants = accountants
                }.onFailure { showToast("Failed to fetch accountants") }
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showAssignDialog(customer: User) {
        // We need all accountants to populate the spinner
        if (allAccountants.isEmpty()) {
            lifecycleScope.launch {
                repository.getAllAccountants().onSuccess { accountants ->
                    allAccountants = accountants
                    displayAssignDialog(customer)
                }.onFailure { showToast("Failed to fetch accountants for assignment") }
            }
        } else {
            displayAssignDialog(customer)
        }
    }

    private fun displayAssignDialog(customer: User) {
        val dialogBinding = DialogAssignAccountantBinding.inflate(LayoutInflater.from(this))
        val accountantNames = allAccountants.map { it.displayName }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accountantNames)
        dialogBinding.spinnerAccountants.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Assign") { _, _ ->
                val selectedIndex = dialogBinding.spinnerAccountants.selectedItemPosition
                val selectedAccountant = allAccountants[selectedIndex]
                assignAccountant(customer.uid, selectedAccountant.uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun assignAccountant(customerId: String, accountantId: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.assignCustomerToAccountant(customerId, accountantId).onSuccess {
                showToast("Assignment successful")
                fetchData()
            }.onFailure { showToast("Assignment failed") }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.displayName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(user: User) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.deleteUser(user.uid, user.role).onSuccess {
                showToast("User deleted")
                fetchData()
            }.onFailure { showToast("Failed to delete user") }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
