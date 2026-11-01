package com.example.shoppinglist.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ShoppingList(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "Продукты",
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null,
    val recipientName: String? = null,
    val status: String = "draft", // "draft", "sent", "in_progress", "completed"
    val items: List<ShoppingItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val json = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("senderId", senderId)
            put("senderName", senderName)
            if (recipientId != null) put("recipientId", recipientId)
            if (recipientName != null) put("recipientName", recipientName)
            put("status", status)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)

            val itemsArray = JSONArray()
            items.forEach { itemsArray.put(it.toJson()) }
            put("items", itemsArray)
        }
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ShoppingList {
            val itemsList = mutableListOf<ShoppingItem>()
            val itemsArray = json.optJSONArray("items")
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    itemsList.add(ShoppingItem.fromJson(itemsArray.getJSONObject(i)))
                }
            }

            return ShoppingList(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Список покупок"),
                category = json.optString("category", "Продукты"),
                senderId = json.optString("senderId", ""),
                senderName = json.optString("senderName", "Автор"),
                recipientId = if (json.has("recipientId") && !json.isNull("recipientId")) json.getString("recipientId") else null,
                recipientName = if (json.has("recipientName") && !json.isNull("recipientName")) json.getString("recipientName") else null,
                status = json.optString("status", "draft"),
                items = itemsList,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}
