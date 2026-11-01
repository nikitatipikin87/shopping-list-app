package com.example.shoppinglist.data

import com.example.shoppinglist.model.ShoppingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncService {
    // Бесплатный публичный ключ-хранилище для синхронизации тестового приложения
    private const val SYNC_URL = "https://kvdb.io/A95gY4FvxXF37E36sE1xYm/nikita_shopping_lists"

    suspend fun uploadLists(lists: List<ShoppingList>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            lists.forEach { jsonArray.put(it.toJson()) }

            val url = URL(SYNC_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
            val url = URL(SYNC_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val jsonArray = JSONArray(response)
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
