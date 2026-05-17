package com.example.receiptflow

import android.content.Intent
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
import com.example.receiptflow.data.interfaces.IAdminRepository
import com.example.receiptflow.databinding.ActivityManagerMenuBinding
import com.example.receiptflow.databinding.DialogAssignAccountantBinding
import com.example.receiptflow.models.User
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ManagerMenu : AppCompatActivity() {
    private lateinit var binding: ActivityManagerMenuBinding
    private val repository: IAdminRepository = AdminRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ManagerUserAdapter
    
    private var allAccountants: List<User> = emptyList()
    private var currentTab = 0 // 0 for Customers, 1 for Accountants

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerMenuBinding.inflate(layoutInflater)
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

        binding.managerBTNLogout.setOnClickListener {
            performLogout()
        }
    }

    // Log out of the manager
    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // set up recyclerView container
    private fun setupRecyclerView() {
        adapter = ManagerUserAdapter(
            users = emptyList(),
            accountantNames = emptyMap(),
            onAssignClicked = { customer -> showAssignDialog(customer) },
            onDeleteClicked = { user -> showDeleteConfirmation(user) }
        )
        binding.managerRVUsers.adapter = adapter
    }

    // set up the tab layout to switch between customer and accountant
    private fun setupTabs() {
        binding.managerTABLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                fetchData()
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    // Fetch the data of the accountant and customers
    private fun fetchData() {
        binding.managerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getAllAccountants().onSuccess { accountants ->
                allAccountants = accountants
                val accMap = accountants.associate { it.uid to it.displayName }

                if (currentTab == 0) {
                    repository.getAllCustomers().onSuccess { customers ->
                        adapter.updateData(customers, accMap)
                    }.onFailure { showToast("Failed to fetch customers") }
                } else {
                    adapter.updateData(accountants, accMap)
                }
            }.onFailure { showToast("Failed to fetch accountants") }

            binding.managerProgressBar.visibility = View.GONE
        }
    }

    // Get all accountants and show the assignment dialog for a specific customer
    private fun showAssignDialog(customer: User) {
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

    // Display the assign alerts dialog
    private fun displayAssignDialog(customer: User) {
        val dialogBinding = DialogAssignAccountantBinding.inflate(LayoutInflater.from(this))
        val accountantNames = allAccountants.map { it.displayName }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accountantNames)
        dialogBinding.assignSPNRCustomer.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Assign") { _, _ ->
                val selectedIndex = dialogBinding.assignSPNRCustomer.selectedItemPosition
                val selectedAccountant = allAccountants[selectedIndex]
                assignAccountant(customer.uid, selectedAccountant.uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Assign a customer to an accountant and update the UI
    private fun assignAccountant(customerId: String, accountantId: String) {
        binding.managerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.assignCustomerToAccountant(customerId, accountantId).onSuccess {
                showToast("Assignment successful")
                fetchData()
            }.onFailure { showToast("Assignment failed") }
            binding.managerProgressBar.visibility = View.GONE
        }
    }

    // Show alert dialog for deletion confirmation
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

    // Delete the user from Firestore
    private fun deleteUser(user: User) {
        binding.managerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.deleteUser(user.uid, user.role).onSuccess {
                showToast("User deleted")
                fetchData()
            }.onFailure { showToast("Failed to delete user") }
            binding.managerProgressBar.visibility = View.GONE
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
