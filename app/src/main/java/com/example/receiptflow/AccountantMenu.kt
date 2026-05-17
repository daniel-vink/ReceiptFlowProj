package com.example.receiptflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.adapters.ReceiptAdapter
import com.example.receiptflow.data.ReceiptRepository
import com.example.receiptflow.data.interfaces.IReceiptRepository
import com.example.receiptflow.databinding.ActivityAccountantMenuBinding
import com.example.receiptflow.models.Receipt
import com.example.receiptflow.models.User
import com.example.receiptflow.utils.PdfGenerator
import com.example.receiptflow.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class AccountantMenu : AppCompatActivity() {
    private lateinit var binding: ActivityAccountantMenuBinding
    private val repository: IReceiptRepository = ReceiptRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var pdfGenerator: PdfGenerator
    
    private var selectedCustomer: User? = null
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-based

    private lateinit var customerAdapter: ArrayAdapter<String>
    private var customerList: List<User> = emptyList()
    private lateinit var receiptAdapter: ReceiptAdapter
    private var currentReceipts: List<Receipt> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountantMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        pdfGenerator = PdfGenerator(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerViews()
        setupSpinners()
        setupDownloadButton()
        fetchCustomers()

        binding.accountantBTNLogout.setOnClickListener {
            performLogout()
        }
    }

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

    private fun setupDownloadButton() {
        binding.accountantBTNDownloadPdf.setOnClickListener {
            val customerName = selectedCustomer?.displayName ?: "Customer"
            val fileName = "Receipts_${customerName}_${selectedYear}_${selectedMonth}"
            
            binding.accountantProgressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = pdfGenerator.generateReceiptsPdf(currentReceipts, fileName)
                binding.accountantProgressBar.visibility = View.GONE
                result.onSuccess { file ->
                    Toast.makeText(this@AccountantMenu, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(this@AccountantMenu, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Customer Spinner setup
        customerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
        binding.accountantSPNRCustomer.adapter = customerAdapter

        binding.accountantSPNRCustomer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (customerList.isNotEmpty()) {
                    selectedCustomer = customerList[position]
                    fetchReceipts()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Receipt RecyclerView setup
        receiptAdapter = ReceiptAdapter(
            receipts = emptyList(),
            onReceiptClicked = { receipt -> ImageUtils.showFullImage(this, receipt.storageUrl) },
            onLongClicked = { receipt -> showStatusUpdateDialog(receipt) }
        )
        binding.accountantRVReceipts.adapter = receiptAdapter
    }

    private fun showStatusUpdateDialog(receipt: Receipt) {
        val statuses = arrayOf("Image unclear", "Not relevant", "Approved")
        AlertDialog.Builder(this)
            .setTitle("Update Status")
            .setItems(statuses) { _, which ->
                val newStatus = statuses[which]
                updateStatus(receipt.id, newStatus)
            }
            .show()
    }

    private fun updateStatus(receiptId: String, newStatus: String) {
        binding.accountantProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.updateReceiptStatus(receiptId, newStatus)
            binding.accountantProgressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@AccountantMenu, "Status updated to: $newStatus", Toast.LENGTH_SHORT).show()
                fetchReceipts() // Refresh the list
            }.onFailure { e ->
                Toast.makeText(this@AccountantMenu, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinners() {
        val years = (2020..2030).map { it.toString() }
        val months = (1..12).map { it.toString() }

        binding.accountantSPNRYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        binding.accountantSPNRMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)

        binding.accountantSPNRYear.setSelection(years.indexOf(selectedYear.toString()))
        binding.accountantSPNRMonth.setSelection(selectedMonth - 1)

        val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = binding.accountantSPNRYear.selectedItem.toString().toInt()
                selectedMonth = binding.accountantSPNRMonth.selectedItem.toString().toInt()
                fetchReceipts()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.accountantSPNRYear.onItemSelectedListener = onItemSelectedListener
        binding.accountantSPNRMonth.onItemSelectedListener = onItemSelectedListener
    }

    private fun fetchCustomers() {
        val currentUserId = auth.currentUser?.uid ?: return
        binding.accountantProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getAssignedCustomers(currentUserId)
            binding.accountantProgressBar.visibility = View.GONE
            result.onSuccess { customers ->
                customerList = customers
                customerAdapter.clear()
                customerAdapter.addAll(customers.map { it.displayName })
                customerAdapter.notifyDataSetChanged()
                
                if (customers.isNotEmpty()) {
                    selectedCustomer = customers[0]
                    fetchReceipts()
                }
            }.onFailure { e ->
                Toast.makeText(this@AccountantMenu, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchReceipts() {
        val customerId = selectedCustomer?.uid ?: return
        binding.accountantProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getReceiptsForCustomer(customerId, selectedYear, selectedMonth)
            binding.accountantProgressBar.visibility = View.GONE
            result.onSuccess { receipts ->
                currentReceipts = receipts
                receiptAdapter.updateData(receipts)
                binding.accountantBTNDownloadPdf.visibility = if (receipts.isNotEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Toast.makeText(this@AccountantMenu, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
