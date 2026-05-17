package com.example.receiptflow.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.example.receiptflow.databinding.DialogImagePreviewBinding

object ImageUtils {
    fun showFullImage(context: Context, url: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogImagePreviewBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        Glide.with(context)
            .load(url)
            .placeholder(android.R.drawable.progress_indeterminate_horizontal)
            .into(dialogBinding.previewIMGFull)

        dialogBinding.previewBTNClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
