package com.example.shoppinglist.model

import org.json.JSONObject
import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#2563EB"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("colorHex", colorHex)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): User {
            return User(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Пользователь"),
                colorHex = json.optString("colorHex", "#2563EB")
            )
        }

        val DEFAULT_USERS = listOf(
            User(id = "user_nikita", name = "Никита", colorHex = "#2563EB"),
            User(id = "user_maria", name = "Мария", colorHex = "#EC4899"),
            User(id = "user_alex", name = "Алексей", colorHex = "#10B981")
        )
    }
}
