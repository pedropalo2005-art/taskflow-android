package com.pedropalo.taskflow

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.pedropalo.taskflow.adapter.TaskAdapter
import com.pedropalo.taskflow.data.LocalTaskManager
import com.pedropalo.taskflow.data.SupabaseClient
import com.pedropalo.taskflow.databinding.ActivityMainBinding
import com.pedropalo.taskflow.databinding.DialogAddTaskBinding
import com.pedropalo.taskflow.model.Task
import com.pedropalo.taskflow.receiver.TaskReminderReceiver
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var localManager: LocalTaskManager
    private lateinit var taskAdapter: TaskAdapter

    private val allTasks = mutableListOf<Task>()
    private var currentFilterStatus = "todo" // "todo", "in_progress", "done"
    private var currentPin = "1234"

    private var activeDialogBinding: DialogAddTaskBinding? = null

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: return@registerForActivityResult

            if (activeDialogBinding != null) {
                activeDialogBinding?.etTaskTitle?.setText(spokenText)
                autoDetectCategory(spokenText, activeDialogBinding!!)
            } else {
                showAddTaskDialog(initialTitle = spokenText)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso de notificaciones denegado. No sonarán las alarmas.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localManager = LocalTaskManager(this)
        currentPin = localManager.getUserPin()

        checkPermissions()
        setupTopBarPin()
        setupTabs()
        setupRecyclerView()
        setupActions()

        loadTasksFromLocal()
        syncTasksFromSupabase()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupTopBarPin() {
        binding.tvUserPinBadge.text = "🔒 PIN: $currentPin"
        binding.tvUserPinBadge.setOnClickListener {
            showChangePinDialog()
        }
    }

    private fun showChangePinDialog() {
        val input = EditText(this).apply {
            hint = "Ej: 1234 (4 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentPin)
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Cambiar PIN / Perfil")
            .setMessage("Cada PIN tiene su propio tablero privado independiente. Si compartes la app, cada persona usa su propio PIN.")
            .setView(input)
            .setPositiveButton("Cambiar") { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.isNotBlank()) {
                    currentPin = newPin
                    localManager.setUserPin(currentPin)
                    binding.tvUserPinBadge.text = "🔒 PIN: $currentPin"
                    Toast.makeText(this, "Cambiado a perfil PIN $currentPin", Toast.LENGTH_SHORT).show()
                    loadTasksFromLocal()
                    syncTasksFromSupabase()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupTabs() {
        binding.tabLayoutKanban.addTab(binding.tabLayoutKanban.newTab().setText("Por Hacer (0)"))
        binding.tabLayoutKanban.addTab(binding.tabLayoutKanban.newTab().setText("En Curso (0)"))
        binding.tabLayoutKanban.addTab(binding.tabLayoutKanban.newTab().setText("Hecho (0)"))

        // Set distinctive tab indicator colors according to selected tab
        binding.tabLayoutKanban.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentFilterStatus = "todo"
                        binding.tabLayoutKanban.setSelectedTabIndicatorColor(Color.parseColor("#F59E0B"))
                    }
                    1 -> {
                        currentFilterStatus = "in_progress"
                        binding.tabLayoutKanban.setSelectedTabIndicatorColor(Color.parseColor("#2563EB"))
                    }
                    2 -> {
                        currentFilterStatus = "done"
                        binding.tabLayoutKanban.setSelectedTabIndicatorColor(Color.parseColor("#10B981"))
                    }
                }
                filterTasksByStatus()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onAdvanceStatus = { task -> advanceTaskStatus(task) },
            onDelete = { task -> confirmDeleteTask(task) },
            onItemClick = { task -> showEditTaskDialog(task) }
        )
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTasks.adapter = taskAdapter
    }

    private fun setupActions() {
        binding.btnQuickVoice.setOnClickListener {
            startVoiceRecognition()
        }

        binding.fabMic.setOnClickListener {
            startVoiceRecognition()
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            syncTasksFromSupabase()
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu tarea (ej. 'Visitar tienda Blv. Morazán')")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tu dispositivo no soporta dictado por voz de Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTasksFromLocal() {
        val cached = localManager.getLocalTasks(currentPin)
        allTasks.clear()
        allTasks.addAll(cached)
        updateTabBadges()
        filterTasksByStatus()
    }

    private fun syncTasksFromSupabase() {
        binding.swipeRefresh.isRefreshing = true
        binding.tvSyncStatus.text = "🔄 Sincronizando..."

        lifecycleScope.launch {
            val result = SupabaseClient.getTasks(currentPin)
            binding.swipeRefresh.isRefreshing = false

            result.onSuccess { remoteTasks ->
                binding.tvSyncStatus.text = "☁️ Supabase Conectado"
                allTasks.clear()
                allTasks.addAll(remoteTasks)
                localManager.saveLocalTasks(remoteTasks, currentPin)
                updateTabBadges()
                filterTasksByStatus()

                for (t in remoteTasks) {
                    if (t.status != "done" && t.dueTimestamp != null && t.dueTimestamp!! > System.currentTimeMillis()) {
                        TaskReminderReceiver.scheduleAlarm(
                            this@MainActivity,
                            t.id,
                            t.title,
                            t.category,
                            t.dueTimestamp!!
                        )
                    }
                }
            }.onFailure {
                binding.tvSyncStatus.text = "📱 Modo Local"
            }
        }
    }

    private fun filterTasksByStatus() {
        val filtered = allTasks.filter { it.status == currentFilterStatus }.toList()
        taskAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            when (currentFilterStatus) {
                "todo" -> {
                    binding.tvEmptyIcon.text = "📋"
                    binding.tvEmptyTitle.text = "No hay tareas por hacer"
                    binding.tvEmptySubtitle.text = "Toca el botón + o el micrófono para crear una tarea"
                }
                "in_progress" -> {
                    binding.tvEmptyIcon.text = "⏳"
                    binding.tvEmptyTitle.text = "No hay tareas en curso"
                    binding.tvEmptySubtitle.text = "Pasa una tarea de 'Por Hacer' a 'En Curso' con el botón Comenzar"
                }
                "done" -> {
                    binding.tvEmptyIcon.text = "🎉"
                    binding.tvEmptyTitle.text = "No has completado tareas aún"
                    binding.tvEmptySubtitle.text = "Cuando finalices una tarea en curso, márcala como hecha"
                }
            }
        } else {
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateTabBadges() {
        val todoCount = allTasks.count { it.status == "todo" }
        val inProgressCount = allTasks.count { it.status == "in_progress" }
        val doneCount = allTasks.count { it.status == "done" }

        binding.tabLayoutKanban.getTabAt(0)?.text = "Por Hacer ($todoCount)"
        binding.tabLayoutKanban.getTabAt(1)?.text = "En Curso ($inProgressCount)"
        binding.tabLayoutKanban.getTabAt(2)?.text = "Hecho ($doneCount)"
    }

    /**
     * Mueve instantáneamente la tarea de columna y cambia de pestaña para que el usuario la vea de inmediato.
     */
    private fun advanceTaskStatus(task: Task) {
        val newStatus = when (task.status) {
            "todo" -> "in_progress"
            "in_progress" -> "done"
            "done" -> "todo"
            else -> "todo"
        }

        // 1. Crear copia con nuevo estado para DiffUtil
        val updatedTask = task.copy(status = newStatus)

        // 2. Actualizar en allTasks
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index] = updatedTask
        } else {
            allTasks.add(updatedTask)
        }

        // 3. Guardar localmente
        localManager.saveLocalTasks(allTasks, currentPin)
        updateTabBadges()

        // 4. Cambiar automáticamente a la pestaña correspondiente para feedback visual inmediato
        when (newStatus) {
            "in_progress" -> {
                binding.tabLayoutKanban.getTabAt(1)?.select()
                Toast.makeText(this, "Tarea movida a 'En Curso' ⏳", Toast.LENGTH_SHORT).show()
            }
            "done" -> {
                TaskReminderReceiver.cancelAlarm(this, task.id)
                binding.tabLayoutKanban.getTabAt(2)?.select()
                Toast.makeText(this, "¡Tarea completada! 🎉", Toast.LENGTH_SHORT).show()
            }
            "todo" -> {
                binding.tabLayoutKanban.getTabAt(0)?.select()
                Toast.makeText(this, "Tarea reabierta en 'Por Hacer' 📋", Toast.LENGTH_SHORT).show()
            }
        }

        filterTasksByStatus()

        // 5. Sincronizar en segundo plano con Supabase
        lifecycleScope.launch {
            SupabaseClient.updateTaskStatus(task.id, newStatus)
        }
    }

    private fun confirmDeleteTask(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Eliminar tarea?")
            .setMessage("¿Deseas eliminar '${task.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                TaskReminderReceiver.cancelAlarm(this, task.id)
                allTasks.removeAll { it.id == task.id }
                localManager.saveLocalTasks(allTasks, currentPin)
                updateTabBadges()
                filterTasksByStatus()

                lifecycleScope.launch {
                    SupabaseClient.deleteTask(task.id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddTaskDialog(initialTitle: String = "") {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        activeDialogBinding = dialogBinding

        if (initialTitle.isNotBlank()) {
            dialogBinding.etTaskTitle.setText(initialTitle)
            autoDetectCategory(initialTitle, dialogBinding)
        }

        var selectedTimestamp: Long? = null
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "HN"))

        dialogBinding.btnPickDateTime.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)

                            selectedTimestamp = calendar.timeInMillis
                            dialogBinding.tvSelectedDateTime.text = "🔔 Alarma: " + format.format(calendar.time)
                            dialogBinding.tvSelectedDateTime.visibility = View.VISIBLE
                            dialogBinding.btnClearDateTime.visibility = View.VISIBLE
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnClearDateTime.setOnClickListener {
            selectedTimestamp = null
            dialogBinding.tvSelectedDateTime.visibility = View.GONE
            dialogBinding.btnClearDateTime.visibility = View.GONE
        }

        dialogBinding.btnDialogMic.setOnClickListener {
            startVoiceRecognition()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text?.toString()?.trim() ?: ""
            if (title.isBlank()) {
                dialogBinding.etTaskTitle.error = "Ingresa el título de la tarea"
                return@setOnClickListener
            }

            val desc = dialogBinding.etTaskDescription.text?.toString()?.trim() ?: ""
            val selectedCategory = getSelectedCategoryFromChips(dialogBinding)

            val newTask = Task(
                title = title,
                description = desc,
                category = selectedCategory,
                status = "todo",
                dueTimestamp = selectedTimestamp,
                userPin = currentPin
            )

            allTasks.add(0, newTask)
            localManager.saveLocalTasks(allTasks, currentPin)
            updateTabBadges()

            // Asegurarse de que esté en la pestaña Por Hacer para ver la nueva tarea creada
            binding.tabLayoutKanban.getTabAt(0)?.select()
            filterTasksByStatus()

            if (selectedTimestamp != null && selectedTimestamp!! > System.currentTimeMillis()) {
                TaskReminderReceiver.scheduleAlarm(
                    this,
                    newTask.id,
                    newTask.title,
                    newTask.category,
                    selectedTimestamp!!
                )
                Toast.makeText(this, "Tarea guardada con alarma ⏰", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
            }

            lifecycleScope.launch {
                SupabaseClient.insertTask(newTask)
            }

            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            activeDialogBinding = null
        }

        dialog.show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Editar Tarea"
        dialogBinding.etTaskTitle.setText(task.title)
        dialogBinding.etTaskDescription.setText(task.description)

        when {
            task.category.contains("Visita", ignoreCase = true) -> dialogBinding.chipVisita.isChecked = true
            task.category.contains("Reporte", ignoreCase = true) -> dialogBinding.chipReporte.isChecked = true
            task.category.contains("Venta", ignoreCase = true) -> dialogBinding.chipVentas.isChecked = true
            task.category.contains("Urgente", ignoreCase = true) -> dialogBinding.chipUrgente.isChecked = true
            else -> dialogBinding.chipGeneral.isChecked = true
        }

        var selectedTimestamp = task.dueTimestamp
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "HN"))

        if (selectedTimestamp != null && selectedTimestamp!! > 0) {
            calendar.timeInMillis = selectedTimestamp!!
            dialogBinding.tvSelectedDateTime.text = "🔔 Alarma: " + format.format(calendar.time)
            dialogBinding.tvSelectedDateTime.visibility = View.VISIBLE
            dialogBinding.btnClearDateTime.visibility = View.VISIBLE
        }

        dialogBinding.btnPickDateTime.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)

                            selectedTimestamp = calendar.timeInMillis
                            dialogBinding.tvSelectedDateTime.text = "🔔 Alarma: " + format.format(calendar.time)
                            dialogBinding.tvSelectedDateTime.visibility = View.VISIBLE
                            dialogBinding.btnClearDateTime.visibility = View.VISIBLE
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnClearDateTime.setOnClickListener {
            selectedTimestamp = null
            dialogBinding.tvSelectedDateTime.visibility = View.GONE
            dialogBinding.btnClearDateTime.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text?.toString()?.trim() ?: ""
            if (title.isBlank()) {
                dialogBinding.etTaskTitle.error = "Ingresa el título"
                return@setOnClickListener
            }

            val updatedTask = task.copy(
                title = title,
                description = dialogBinding.etTaskDescription.text?.toString()?.trim() ?: "",
                category = getSelectedCategoryFromChips(dialogBinding),
                dueTimestamp = selectedTimestamp,
                userPin = currentPin
            )

            val index = allTasks.indexOfFirst { it.id == task.id }
            if (index != -1) {
                allTasks[index] = updatedTask
            }

            localManager.saveLocalTasks(allTasks, currentPin)
            updateTabBadges()
            filterTasksByStatus()

            if (selectedTimestamp != null && selectedTimestamp!! > System.currentTimeMillis()) {
                TaskReminderReceiver.scheduleAlarm(this, updatedTask.id, updatedTask.title, updatedTask.category, selectedTimestamp!!)
            } else {
                TaskReminderReceiver.cancelAlarm(this, updatedTask.id)
            }

            lifecycleScope.launch {
                SupabaseClient.insertTask(updatedTask)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getSelectedCategoryFromChips(binding: DialogAddTaskBinding): String {
        val checkedId = binding.chipGroupCategory.checkedChipId
        if (checkedId == View.NO_ID) return "General"
        val chip = binding.chipGroupCategory.findViewById<Chip>(checkedId)
        return chip?.text?.toString() ?: "General"
    }

    private fun autoDetectCategory(text: String, binding: DialogAddTaskBinding) {
        val lower = text.lowercase(Locale.ROOT)
        when {
            lower.contains("tienda") || lower.contains("visitar") || lower.contains("sucursal") || lower.contains("morazan") -> {
                binding.chipVisita.isChecked = true
            }
            lower.contains("reporte") || lower.contains("pedidos ya") || lower.contains("pedido") -> {
                binding.chipReporte.isChecked = true
            }
            lower.contains("venta") || lower.contains("caja") || lower.contains("cierre") || lower.contains("fin de semana") -> {
                binding.chipVentas.isChecked = true
            }
            lower.contains("urgente") || lower.contains("inmediato") || lower.contains("ya") -> {
                binding.chipUrgente.isChecked = true
            }
        }
    }
}
