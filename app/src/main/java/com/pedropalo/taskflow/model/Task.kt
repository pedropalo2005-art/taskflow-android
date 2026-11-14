package com.pedropalo.taskflow.model

import org.json.JSONObject
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var description: String = "",
    var category: String = "General",
    var status: String = "todo", // "todo", "in_progress", "done"
    var dueTimestamp: Long? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var userPin: String = "1234"
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("description", description)
        obj.put("category", category)
        obj.put("status", status)
        obj.put("user_pin", userPin)
        if (dueTimestamp != null) {
            obj.put("due_timestamp", dueTimestamp)
        } else {
            obj.put("due_timestamp", JSONObject.NULL)
        }
        obj.put("created_at", createdAt)
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): Task {
            return Task(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                category = json.optString("category", "General"),
                status = json.optString("status", "todo"),
                dueTimestamp = if (json.has("due_timestamp") && !json.isNull("due_timestamp")) {
                    json.optLong("due_timestamp")
                } else null,
                createdAt = json.optLong("created_at", System.currentTimeMillis()),
                userPin = json.optString("user_pin", "1234")
            )
        }
    }
}
