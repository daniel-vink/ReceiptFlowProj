package com.example.receiptflow

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.adapters.CustomerAdapter
import com.example.receiptflow.adapters.ReceiptAdapter
import com.example.receiptflow.data.ReceiptRepository
import com.example.receiptflow.databinding.ActivityAccountantSelectBinding
import com.example.receiptflow.models.Receipt
import com.example.receiptflow.models.User
import com.example.receiptflow.utils.PdfGenerator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class AccountantSelect : AppCompatActivity() {
    private lateinit var binding: ActivityAccountantSelectBinding
    private val repository = ReceiptRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var pdfGenerator: PdfGenerator
    
    private var selectedCustomer: User? = null
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-based

    private lateinit var customerAdapter: CustomerAdapter
    private lateinit var receiptAdapter: ReceiptAdapter
    private var currentReceipts: List<Receipt> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountantSelectBinding.inflate(layoutInflater)
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
    }

    private fun setupDownloadButton() {
        binding.buttonDownloadPdf.setOnClickListener {
            val customerName = selectedCustomer?.displayName ?: "Customer"
            val fileName = "Receipts_${customerName}_${selectedYear}_${selectedMonth}"
            
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = pdfGenerator.generateReceiptsPdf(currentReceipts, fileName)
                binding.progressBar.visibility = View.GONE
                result.onSuccess { file ->
                    Toast.makeText(this@AccountantSelect, "PDF saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(this@AccountantSelect, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        customerAdapter = CustomerAdapter(emptyList()) { customer ->
            selectedCustomer = customer
            fetchReceipts()
        }
        binding.recyclerViewCustomers.adapter = customerAdapter

        receiptAdapter = ReceiptAdapter(emptyList())
        binding.recyclerViewReceipts.adapter = receiptAdapter
    }

    private fun setupSpinners() {
        val years = (2020..2030).map { it.toString() }
        val months = (1..12).map { it.toString() }

        binding.spinnerYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        binding.spinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)

        binding.spinnerYear.setSelection(years.indexOf(selectedYear.toString()))
        binding.spinnerMonth.setSelection(selectedMonth - 1)

        val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = binding.spinnerYear.selectedItem.toString().toInt()
                selectedMonth = binding.spinnerMonth.selectedItem.toString().toInt()
                fetchReceipts()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerYear.onItemSelectedListener = onItemSelectedListener
        binding.spinnerMonth.onItemSelectedListener = onItemSelectedListener
    }

    private fun fetchCustomers() {
        val currentUserId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getAssignedCustomers(currentUserId)
            binding.progressBar.visibility = View.GONE
            result.onSuccess { customers ->
                customerAdapter.updateData(customers)
            }.onFailure { e ->
                Toast.makeText(this@AccountantSelect, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchReceipts() {
        val customerId = selectedCustomer?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getReceiptsForCustomer(customerId, selectedYear, selectedMonth)
            binding.progressBar.visibility = View.GONE
            result.onSuccess { receipts ->
                currentReceipts = receipts
                receiptAdapter.updateData(receipts)
                binding.buttonDownloadPdf.visibility = if (receipts.isNotEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Toast.makeText(this@AccountantSelect, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
