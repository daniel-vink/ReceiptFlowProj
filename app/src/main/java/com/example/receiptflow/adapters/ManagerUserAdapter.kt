package com.example.receiptflow.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptflow.databinding.ItemManagerUserBinding
import com.example.receiptflow.models.User

class ManagerUserAdapter(
    private var users: List<User>,
    private var accountantNames: Map<String, String> = emptyMap(),
    private val onAssignClicked: (User) -> Unit,
    private val onDeleteClicked: (User) -> Unit
) : RecyclerView.Adapter<ManagerUserAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ItemManagerUserBinding) : RecyclerView.ViewHolder(binding.root)
    // Create new container for RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemManagerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }
    // Bind the data to the RecyclerView
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.binding.userLBLUserName.text = user.displayName
        holder.binding.userLBLEmail.text = user.email
        
        if (user.role == "customer") {
            val accName = user.accountantId?.let { accountantNames[it] } ?: "None"
            holder.binding.userLBLDetails.text = "Accountant: $accName"
            holder.binding.userLBLDetails.visibility = View.VISIBLE
            holder.binding.userBTNAssign.visibility = View.VISIBLE
        } else {
            holder.binding.userLBLDetails.visibility = View.GONE
            holder.binding.userBTNAssign.visibility = View.GONE
        }

        holder.binding.userBTNAssign.setOnClickListener { onAssignClicked(user) }
        holder.binding.userBTNDelete.setOnClickListener { onDeleteClicked(user) }
    }
    // Return the number of users
    override fun getItemCount(): Int = users.size

    // Update the list of users and the accountant the assign to name
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newUsers: List<User>, newAccountantNames: Map<String, String>) {
        users = newUsers
        accountantNames = newAccountantNames
        notifyDataSetChanged()
    }
}
