package com.example.receiptflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.receiptflow.databinding.ItemReceiptBinding
import com.example.receiptflow.models.Receipt
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptAdapter(
    private var receipts: List<Receipt>,
    private val onReceiptClicked: (Receipt) -> Unit,
    private val onLongClicked: ((Receipt) -> Unit)? = null
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
        
        holder.binding.receiptLBLDate.text = "Date: $dateString"
        holder.binding.receiptLBLComment.text = receipt.comment
        holder.binding.receiptLBLStatus.text = "Status: ${receipt.status}"

        // Load small thumbnail preview
        Glide.with(holder.itemView.context)
            .load(receipt.storageUrl)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.binding.receiptIMGThumbnail)

        holder.binding.root.setOnClickListener { onReceiptClicked(receipt) }
        
        holder.binding.root.setOnLongClickListener {
            onLongClicked?.invoke(receipt)
            true
        }
    }

    override fun getItemCount(): Int = receipts.size

    fun updateData(newReceipts: List<Receipt>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }
}
