package com.example.receiptflow.adapters

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemManagerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.binding.textViewUserName.text = user.displayName
        holder.binding.textViewUserEmail.text = user.email
        
        if (user.role == "customer") {
            val accName = user.accountantId?.let { accountantNames[it] } ?: "None"
            holder.binding.textViewUserDetail.text = "Accountant: $accName"
            holder.binding.textViewUserDetail.visibility = View.VISIBLE
            holder.binding.buttonAssign.visibility = View.VISIBLE
        } else {
            holder.binding.textViewUserDetail.visibility = View.GONE
            holder.binding.buttonAssign.visibility = View.GONE
        }

        holder.binding.buttonAssign.setOnClickListener { onAssignClicked(user) }
        holder.binding.buttonDelete.setOnClickListener { onDeleteClicked(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>, newAccountantNames: Map<String, String>) {
        users = newUsers
        accountantNames = newAccountantNames
        notifyDataSetChanged()
    }
}
