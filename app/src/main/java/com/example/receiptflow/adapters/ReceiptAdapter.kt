package com.example.receiptflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptflow.databinding.ItemReceiptBinding
import com.example.receiptflow.models.Receipt
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptAdapter(
    private var receipts: List<Receipt>
) : RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

    class ReceiptViewHolder(val binding: ItemReceiptBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceiptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val receipt = receipts[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = receipt.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        
        holder.binding.textViewReceiptDate.text = "Date: $dateString"
        holder.binding.textViewReceiptComment.text = receipt.comment
        holder.binding.textViewReceiptStatus.text = "Status: ${receipt.status}"
    }

    override fun getItemCount(): Int = receipts.size

    fun updateData(newReceipts: List<Receipt>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }
}
