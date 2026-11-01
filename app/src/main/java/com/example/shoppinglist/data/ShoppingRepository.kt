package com.example.shoppinglist.data

import android.content.Context
import android.content.SharedPreferences
import com.example.shoppinglist.model.ShoppingItem
import com.example.shoppinglist.model.ShoppingList
import com.example.shoppinglist.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class ShoppingRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shopping_v2_prefs", Context.MODE_PRIVATE)

    // Пользователи
    fun loadUsers(): List<User> {
        val jsonString = prefs.getString("users_json", null) ?: return User.DEFAULT_USERS
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<User>()
            for (i in 0 until array.length()) {
                list.add(User.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) User.DEFAULT_USERS else list
        } catch (e: Exception) {
            User.DEFAULT_USERS
        }
    }

    fun saveUsers(users: List<User>) {
        val array = JSONArray()
        users.forEach { array.put(it.toJson()) }
        prefs.edit().putString("users_json", array.toString()).apply()
    }

    fun getCurrentUserId(): String {
        return prefs.getString("current_user_id", "user_nikita") ?: "user_nikita"
    }

    fun setCurrentUserId(id: String) {
        prefs.edit().putString("current_user_id", id).apply()
    }

    // Списки покупок
    fun loadLists(): List<ShoppingList> {
        val jsonString = prefs.getString("lists_json", null) ?: return getDefaultLists()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<ShoppingList>()
            for (i in 0 until array.length()) {
                list.add(ShoppingList.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) getDefaultLists() else list
        } catch (e: Exception) {
            getDefaultLists()
        }
    }

    fun saveLists(lists: List<ShoppingList>, syncToCloud: Boolean = true) {
        val array = JSONArray()
        lists.forEach { array.put(it.toJson()) }
        prefs.edit().putString("lists_json", array.toString()).apply()

        if (syncToCloud) {
            CoroutineScope(Dispatchers.IO).launch {
                CloudSyncService.uploadLists(lists)
            }
        }
    }

    suspend fun syncFromCloud(): List<ShoppingList>? {
        val cloudLists = CloudSyncService.fetchLists() ?: return null
        if (cloudLists.isNotEmpty()) {
            saveLists(cloudLists, syncToCloud = false)
        }
        return cloudLists
    }

    private fun getDefaultLists(): List<ShoppingList> {
        return listOf(
            ShoppingList(
                id = "demo_list_1",
                title = "Супермаркет на выходные",
                category = "Продукты",
                senderId = "user_nikita",
                senderName = "Никита",
                recipientId = null,
                recipientName = null,
                status = "draft",
                items = listOf(
                    ShoppingItem(name = "Молоко 3.2%", quantity = "2 л", category = "Продукты", isBought = false),
                    ShoppingItem(name = "Хлеб зерновой", quantity = "1 шт", category = "Продукты", isBought = true),
                    ShoppingItem(name = "Яблоки красные", quantity = "1.5 кг", category = "Продукты", isBought = false)
                )
            ),
            ShoppingList(
                id = "demo_list_2",
                title = "Купить в аптеке",
                category = "Аптека",
                senderId = "user_maria",
                senderName = "Мария",
                recipientId = "user_nikita",
                recipientName = "Никита",
                status = "in_progress",
                items = listOf(
                    ShoppingItem(name = "Витамин C 1000мг", quantity = "1 уп", category = "Аптека", isBought = true, boughtBy = "Никита"),
                    ShoppingItem(name = "Пластырь бактерицидный", quantity = "1 уп", category = "Аптека", isBought = false),
                    ShoppingItem(name = "Капли для глаз", quantity = "1 шт", category = "Аптека", isBought = false)
                )
            )
        )
    }
}
