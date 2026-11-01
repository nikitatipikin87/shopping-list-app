package com.example.shoppinglist.data

import android.content.Context
import android.content.SharedPreferences
import com.example.shoppinglist.model.ShoppingItem
import org.json.JSONArray

class ShoppingRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shopping_list_prefs", Context.MODE_PRIVATE)

    fun loadItems(): List<ShoppingItem> {
        val jsonString = prefs.getString("items_json", null) ?: return getDefaultItems()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ShoppingItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ShoppingItem.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultItems()
        }
    }

    fun saveItems(items: List<ShoppingItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            jsonArray.put(item.toJson())
        }
        prefs.edit().putString("items_json", jsonArray.toString()).apply()
    }

    private fun getDefaultItems(): List<ShoppingItem> {
        return listOf(
            ShoppingItem(name = "Молоко", quantity = "1 л", category = "Продукты", isBought = false),
            ShoppingItem(name = "Хлеб бородинский", quantity = "1 шт", category = "Продукты", isBought = true),
            ShoppingItem(name = "Яблоки красные", quantity = "1.5 кг", category = "Продукты", isBought = false),
            ShoppingItem(name = "Стиральный порошок", quantity = "1 уп", category = "Бытовая химия", isBought = false)
        )
    }
}
