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

            // Dynamic Step / Status Visual Theme (Distinct Colors for each Kanban step)
            applyStatusVisualTheme(task.status)

            // Due Date & Time
            if (task.dueTimestamp != null && task.dueTimestamp!! > 0) {
                binding.layoutDueTime.visibility = View.VISIBLE
                binding.tvDueDate.text = dateFormat.format(Date(task.dueTimestamp!!))

                val isOverdue = task.dueTimestamp!! < System.currentTimeMillis() && task.status != "done"
                if (isOverdue) {
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.danger))
                } else {
                    binding.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            } else {
                binding.layoutDueTime.visibility = View.GONE
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

        private fun applyStatusVisualTheme(status: String) {
            val context = binding.root.context
            when (status) {
                "todo" -> {
                    // Warm Amber/Yellow theme for "Por Hacer"
                    binding.viewStatusAccent.setBackgroundColor(Color.parseColor("#F59E0B"))
                    binding.cardTask.strokeColor = Color.parseColor("#FDE68A")
                    binding.tvStatusBadge.text = "📋 Por Hacer"
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#B45309"))

                    binding.btnAdvanceStatus.text = "Comenzar ⏳"
                    binding.btnAdvanceStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_forward)
                    binding.btnAdvanceStatus.iconTint = ColorStateList.valueOf(Color.parseColor("#1D4ED8"))
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#1D4ED8"))
                    binding.btnAdvanceStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EFF6FF"))
                }
                "in_progress" -> {
                    // High-energy Electric Blue theme for "En Curso"
                    binding.viewStatusAccent.setBackgroundColor(Color.parseColor("#2563EB"))
                    binding.cardTask.strokeColor = Color.parseColor("#BFDBFE")
                    binding.tvStatusBadge.text = "⏳ En Curso"
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#1D4ED8"))

                    binding.btnAdvanceStatus.text = "Completar ✅"
                    binding.btnAdvanceStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_check)
                    binding.btnAdvanceStatus.iconTint = ColorStateList.valueOf(Color.parseColor("#047857"))
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#047857"))
                    binding.btnAdvanceStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
                }
                "done" -> {
                    // Emerald Green theme for "Hecho"
                    binding.viewStatusAccent.setBackgroundColor(Color.parseColor("#10B981"))
                    binding.cardTask.strokeColor = Color.parseColor("#A7F3D0")
                    binding.tvStatusBadge.text = "✅ Completado"
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D1FAE5"))
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#047857"))

                    binding.btnAdvanceStatus.text = "Reabrir ↩️"
                    binding.btnAdvanceStatus.icon = null
                    binding.btnAdvanceStatus.setTextColor(Color.parseColor("#64748B"))
                    binding.btnAdvanceStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                }
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
            return oldItem.status == newItem.status &&
                   oldItem.title == newItem.title &&
                   oldItem.description == newItem.description &&
                   oldItem.category == newItem.category &&
                   oldItem.dueTimestamp == newItem.dueTimestamp &&
                   oldItem.userPin == newItem.userPin
        }
    }
}
