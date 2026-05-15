package com.example.receiptflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptflow.databinding.ItemManagerUserBinding
import com.example.receiptflow.models.User

class ManagerUserAdapter(
    private var users: List<User>,
    private val onAssignClicked: (User) -> Unit,
    private val onDeleteClicked: (User) -> Unit
) : RecyclerView.Adapter<ManagerUserAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ItemManagerUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemManagerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.binding.textViewUserName.text = user.displayName
        
        if (user.role == "customer") {
            holder.binding.textViewUserDetail.text = "Accountant ID: ${user.accountantId ?: "None"}"
            holder.binding.buttonAssign.visibility = View.VISIBLE
        } else {
            holder.binding.textViewUserDetail.text = "Role: Accountant"
            holder.binding.buttonAssign.visibility = View.GONE
        }

        holder.binding.buttonAssign.setOnClickListener { onAssignClicked(user) }
        holder.binding.buttonDelete.setOnClickListener { onDeleteClicked(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
