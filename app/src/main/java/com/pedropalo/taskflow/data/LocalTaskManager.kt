package com.pedropalo.taskflow.data

import android.content.Context
import com.pedropalo.taskflow.model.Task
import org.json.JSONArray

class LocalTaskManager(context: Context) {

    private val prefs = context.getSharedPreferences("taskflow_local_storage", Context.MODE_PRIVATE)

    fun getUserPin(): String {
        return prefs.getString(KEY_USER_PIN, "1234") ?: "1234"
    }

    fun setUserPin(pin: String) {
        prefs.edit().putString(KEY_USER_PIN, pin.trim()).apply()
    }

    fun getLocalTasks(pin: String = getUserPin()): MutableList<Task> {
        val jsonString = prefs.getString(KEY_TASKS + "_" + pin, null) 
            ?: prefs.getString(KEY_TASKS, null) 
            ?: return mutableListOf()

        val list = mutableListOf<Task>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val task = Task.fromJson(obj)
                if (task.userPin == pin || task.userPin.isBlank()) {
                    task.userPin = pin
                    list.add(task)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveLocalTasks(tasks: List<Task>, pin: String = getUserPin()) {
        try {
            val jsonArray = JSONArray()
            for (task in tasks) {
                jsonArray.put(task.toJson())
            }
            prefs.edit().putString(KEY_TASKS + "_" + pin, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun upsertTask(task: Task) {
        val pin = task.userPin.ifBlank { getUserPin() }
        val current = getLocalTasks(pin)
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
        } else {
            current.add(0, task)
        }
        saveLocalTasks(current, pin)
    }

    fun removeTask(taskId: String, pin: String = getUserPin()) {
        val current = getLocalTasks(pin)
        current.removeAll { it.id == taskId }
        saveLocalTasks(current, pin)
    }

    companion object {
        private const val KEY_TASKS = "cached_tasks"
        private const val KEY_USER_PIN = "active_user_pin"
    }
}
