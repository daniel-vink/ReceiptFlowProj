package com.example.receiptflow

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.receiptflow.adapters.ReceiptAdapter
import com.example.receiptflow.data.ReceiptRepository
import com.example.receiptflow.data.StorageRepository
import com.example.receiptflow.databinding.ActivityCustomerMenuBinding
import com.example.receiptflow.databinding.DialogUploadReceiptBinding
import com.example.receiptflow.models.Receipt
import com.example.receiptflow.utils.ImageUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Calendar

class CustomerMenu : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerMenuBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val receiptRepository = ReceiptRepository()
    private lateinit var storageRepository: StorageRepository
    private lateinit var adapter: ReceiptAdapter

    private var tempImageUri: Uri? = null
    private var accountantId: String? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempImageUri?.let { showUploadDialog(it) }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showUploadDialog(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        storageRepository = StorageRepository(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        fetchAccountantIdAndReceipts()

        binding.fabAddReceipt.setOnClickListener {
            showImageSourceOptions()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReceiptAdapter(
            receipts = emptyList(),
            onReceiptClicked = { receipt -> ImageUtils.showFullImage(this, receipt.storageUrl) },
            onLongClicked = { receipt -> showDeleteConfirmation(receipt) }
        )
        binding.recyclerViewMyReceipts.adapter = adapter
    }

    private fun showDeleteConfirmation(receipt: Receipt) {
        AlertDialog.Builder(this)
            .setTitle("Delete Receipt")
            .setMessage("Are you sure you want to delete this receipt?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReceipt(receipt)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReceipt(receipt: Receipt) {
        binding.customerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            // 1. Delete from Storage
            storageRepository.deleteImage(receipt.storageUrl)
            
            // 2. Delete from Firestore
            val result = receiptRepository.deleteReceipt(receipt.id)
            
            binding.customerProgressBar.visibility = View.GONE
            
            result.onSuccess {
                Toast.makeText(this@CustomerMenu, "Receipt deleted", Toast.LENGTH_SHORT).show()
                fetchMyReceipts()
            }.onFailure { e ->
                Toast.makeText(this@CustomerMenu, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAccountantIdAndReceipts() {
        val uid = auth.currentUser?.uid ?: return
        binding.customerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                accountantId = doc.getString("accountantId")
                fetchMyReceipts()
            } catch (e: Exception) {
                binding.customerProgressBar.visibility = View.GONE
                Toast.makeText(this@CustomerMenu, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMyReceipts() {
        val uid = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        lifecycleScope.launch {
            val result = receiptRepository.getReceiptsForCustomer(uid, year, month)
            binding.customerProgressBar.visibility = View.GONE
            result.onSuccess { receipts ->
                adapter.updateData(receipts)
            }.onFailure { e ->
                Toast.makeText(this@CustomerMenu, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }.show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_image_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        tempImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun showUploadDialog(uri: Uri) {
        val dialogBinding = DialogUploadReceiptBinding.inflate(LayoutInflater.from(this))
        dialogBinding.imageViewPreview.setImageURI(uri)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Upload") { _, _ ->
                val comment = dialogBinding.editTextComment.text.toString()
                uploadReceipt(uri, comment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadReceipt(uri: Uri, comment: String) {
        val uid = auth.currentUser?.uid ?: return
        val accId = accountantId ?: "" // In a real app, maybe block upload if no accountant is assigned
        
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        binding.customerProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val uploadResult = storageRepository.uploadReceiptImage(uri, uid, year, month)
            uploadResult.onSuccess { url ->
                val receipt = Receipt(
                    customerId = uid,
                    accountantId = accId,
                    timestamp = Timestamp.now(),
                    year = year,
                    month = month,
                    storageUrl = url,
                    comment = comment
                )
                val saveResult = receiptRepository.saveReceipt(receipt)
                binding.customerProgressBar.visibility = View.GONE
                saveResult.onSuccess {
                    Toast.makeText(this@CustomerMenu, "Upload successful!", Toast.LENGTH_SHORT).show()
                    fetchMyReceipts()
                }.onFailure { e ->
                    Toast.makeText(this@CustomerMenu, "Failed to save metadata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                binding.customerProgressBar.visibility = View.GONE
                Toast.makeText(this@CustomerMenu, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
