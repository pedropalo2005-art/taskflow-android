package com.pedropalo.taskflow.data

import com.pedropalo.taskflow.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private const val SUPABASE_URL = "https://cdqdpnntmiirvhycufcp.supabase.co"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNkcWRwbm50bWlpcnZoeWN1ZmNwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ1ODA2OTAsImV4cCI6MjExMDE1NjY5MH0.TfU_T25peO81chK6rGxjWltkgp2Od7oKwuqYNfL6dXs"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Obtiene las tareas aisladas por PIN de usuario desde Supabase.
     */
    suspend fun getTasks(userPin: String = "1234"): Result<List<Task>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/tasks?user_pin=eq.$userPin&order=created_at.desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val jsonArray = JSONArray(body)
            val list = mutableListOf<Task>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Task.fromJson(obj))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserta o actualiza una tarea en Supabase.
     */
    suspend fun insertTask(task: Task): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = task.toJson().toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/tasks")
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $ANON_KEY")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 201) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el estado Kanban (todo -> in_progress -> done).
     */
    suspend fun updateTaskStatus(taskId: String, newStatus: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val updateObj = JSONObject().apply {
                    put("status", newStatus)
                }
                val body = updateObj.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/tasks?id=eq.$taskId")
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer $ANON_KEY")
                    .patch(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Elimina una tarea de Supabase.
     */
    suspend fun deleteTask(taskId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/tasks?id=eq.$taskId")
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $ANON_KEY")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
