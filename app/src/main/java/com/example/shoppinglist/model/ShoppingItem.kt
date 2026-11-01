package com.example.shoppinglist.model

import org.json.JSONObject
import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String = "1 шт",
    val category: String = "Продукты",
    val isBought: Boolean = false,
    val boughtBy: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("quantity", quantity)
            put("category", category)
            put("isBought", isBought)
            if (boughtBy != null) put("boughtBy", boughtBy)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ShoppingItem {
            return ShoppingItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                quantity = json.optString("quantity", "1 шт"),
                category = json.optString("category", "Продукты"),
                isBought = json.optBoolean("isBought", false),
                boughtBy = if (json.has("boughtBy")) json.optString("boughtBy") else null,
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }

        val CATEGORIES = listOf(
            "Продукты",
            "Бытовая химия",
            "Аптека",
            "Для дома",
            "Напитки",
            "Другое"
        )
    }
}
