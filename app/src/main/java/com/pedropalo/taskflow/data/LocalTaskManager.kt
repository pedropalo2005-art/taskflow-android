package com.pedropalo.taskflow.data

import android.content.Context
import com.pedropalo.taskflow.model.Task
import org.json.JSONArray

class LocalTaskManager(context: Context) {

    private val prefs = context.getSharedPreferences("taskflow_local_storage", Context.MODE_PRIVATE)

    fun getLocalTasks(): MutableList<Task> {
        val jsonString = prefs.getString(KEY_TASKS, null) ?: return mutableListOf()
        val list = mutableListOf<Task>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Task.fromJson(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveLocalTasks(tasks: List<Task>) {
        try {
            val jsonArray = JSONArray()
            for (task in tasks) {
                jsonArray.put(task.toJson())
            }
            prefs.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun upsertTask(task: Task) {
        val current = getLocalTasks()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
        } else {
            current.add(0, task)
        }
        saveLocalTasks(current)
    }

    fun removeTask(taskId: String) {
        val current = getLocalTasks()
        current.removeAll { it.id == taskId }
        saveLocalTasks(current)
    }

    companion object {
        private const val KEY_TASKS = "cached_tasks"
    }
}
