package com.pedropalo.taskflow.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedropalo.taskflow.R
import com.pedropalo.taskflow.databinding.ItemTaskCardBinding
import com.pedropalo.taskflow.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onAdvanceStatus: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "HN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            // Title & Description
            binding.tvTaskTitle.text = task.title
            if (task.description.isNotBlank()) {
                binding.tvTaskDescription.text = task.description
                binding.tvTaskDescription.visibility = View.VISIBLE
            } else {
                binding.tvTaskDescription.visibility = View.GONE
            }

            // Category Styling
            binding.tvCategoryChip.text = task.category
            applyCategoryStyle(task.category)

            // Due Date & Time
            if (task.dueTimestamp != null && task.dueTimestamp!! > 0) {
                binding.layoutDueTime.visibility = View.VISIBLE
                binding.tvDueDate.text = dateFormat.format(Date(task.dueTimestamp!!))

                // Highlight if overdue and not done
                val isOverdue = task.dueTimestamp!! < System.currentTimeMillis() && task.status != "done"
                if (isOverdue) {
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.danger))
                } else {
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            } else {
                binding.layoutDueTime.visibility = View.GONE
            }

            // Status Advance Button
            when (task.status) {
                "todo" -> {
                    binding.btnAdvanceStatus.text = "Comenzar ⏳"
                    binding.btnAdvanceStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_forward)
                    binding.btnAdvanceStatus.iconTint = ColorStateList.valueOf(Color.parseColor("#1D4ED8"))
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#1D4ED8"))
                }
                "in_progress" -> {
                    binding.btnAdvanceStatus.text = "Completar ✅"
                    binding.btnAdvanceStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_check)
                    binding.btnAdvanceStatus.iconTint = ColorStateList.valueOf(Color.parseColor("#047857"))
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#047857"))
                }
                "done" -> {
                    binding.btnAdvanceStatus.text = "Reabrir ↩️"
                    binding.btnAdvanceStatus.icon = null
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#64748B"))
                }
            }

            // Click Handlers
            binding.btnAdvanceStatus.setOnClickListener {
                onAdvanceStatus(task)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(task)
            }

            binding.root.setOnClickListener {
                onItemClick(task)
            }
        }

        private fun applyCategoryStyle(category: String) {
            val (chipBg, textColor) = when {
                category.contains("Visita", ignoreCase = true) -> Pair("#F3E8FF", "#7C3AED")
                category.contains("Reporte", ignoreCase = true) -> Pair("#E0F2FE", "#0284C7")
                category.contains("Venta", ignoreCase = true) -> Pair("#DCFCE7", "#15803D")
                category.contains("Urgente", ignoreCase = true) -> Pair("#FEE2E2", "#DC2626")
                else -> Pair("#F1F5F9", "#475569")
            }
            binding.tvCategoryChip.backgroundTintList = ColorStateList.valueOf(Color.parseColor(chipBg))
            binding.tvCategoryChip.setTextColor(Color.parseColor(textColor))
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
