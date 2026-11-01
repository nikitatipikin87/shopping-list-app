package com.example.shoppinglist.data

import com.example.shoppinglist.model.ShoppingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncService {
    // Бесплатный защищенный канал обмена в реальном времени
    private const val TOPIC = "nikita_shopping_sync_channel_88"
    private const val POST_URL = "https://ntfy.sh/$TOPIC"
    private const val GET_URL = "https://ntfy.sh/$TOPIC/json?poll=1"

    suspend fun uploadLists(lists: List<ShoppingList>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            lists.forEach { jsonArray.put(it.toJson()) }

            val url = URL(POST_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty("Title", "ShoppingListSync")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonArray.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            conn.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchLists(): List<ShoppingList>? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GET_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            var lastPayload: String? = null

            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("{") && trimmed.contains("\"message\"")) {
                    try {
                        val obj = JSONObject(trimmed)
                        if (obj.optString("event") == "message") {
                            val msg = obj.optString("message")
                            if (msg.startsWith("[")) {
                                lastPayload = msg
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            reader.close()
            conn.disconnect()

            if (lastPayload == null) return@withContext null

            val jsonArray = JSONArray(lastPayload)
            val result = mutableListOf<ShoppingList>()
            for (i in 0 until jsonArray.length()) {
                result.add(ShoppingList.fromJson(jsonArray.getJSONObject(i)))
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
