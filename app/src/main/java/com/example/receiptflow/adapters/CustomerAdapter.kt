package com.example.receiptflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptflow.databinding.ItemCustomerBinding
import com.example.receiptflow.models.User

class CustomerAdapter(
    private var customers: List<User>,
    private val onCustomerSelected: (User) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customers[position]
        holder.binding.textViewCustomerName.text = customer.displayName
        holder.binding.root.setOnClickListener { onCustomerSelected(customer) }
    }

    override fun getItemCount(): Int = customers.size

    fun updateData(newCustomers: List<User>) {
        customers = newCustomers
        notifyDataSetChanged()
    }
}
