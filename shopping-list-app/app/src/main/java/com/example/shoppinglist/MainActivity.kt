package com.example.shoppinglist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shoppinglist.data.ShoppingRepository
import com.example.shoppinglist.model.ShoppingItem
import com.example.shoppinglist.model.ShoppingList
import com.example.shoppinglist.model.User
import com.example.shoppinglist.ui.theme.ShoppingListTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: ShoppingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ShoppingRepository(this)

        setContent {
            ShoppingListTheme {
                MainApp(repository)
            }
        }
    }
}

enum class ActiveTab(val title: String) {
    MY_LISTS("Мои списки"),
    INBOX("Входящие"),
    SENT("Отправленные")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(repository: ShoppingRepository) {
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf(repository.loadUsers()) }
    var currentUserId by remember { mutableStateOf(repository.getCurrentUserId()) }
    val currentUser = users.find { it.id == currentUserId } ?: users.first()

    var allLists by remember { mutableStateOf(repository.loadLists()) }
    var activeTab by remember { mutableStateOf(ActiveTab.MY_LISTS) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Диалоги
    var showUserSwitchDialog by remember { mutableStateOf(false) }
    var showNewUserDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }

    val selectedList = allLists.find { it.id == selectedListId }

    fun saveAndBroadcast(updated: List<ShoppingList>) {
        allLists = updated
        repository.saveLists(updated)
    }

    // Фоновая синхронизация с облаком при старте
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val remote = repository.syncFromCloud()
            if (remote != null && remote.isNotEmpty()) {
                allLists = remote
            }
        }
    }

    // Фильтрация списков по активной вкладке
    val displayedLists = remember(allLists, activeTab, currentUser) {
        when (activeTab) {
            ActiveTab.MY_LISTS -> allLists.filter { it.senderId == currentUser.id && it.recipientId == null }
            ActiveTab.INBOX -> allLists.filter { it.recipientId == currentUser.id }
            ActiveTab.SENT -> allLists.filter { it.senderId == currentUser.id && it.recipientId != null }
        }
    }

    val inboxUnfinishedCount = allLists.count { it.recipientId == currentUser.id && it.status != "completed" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedList != null) {
                        Column {
                            Text(
                                text = selectedList.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedList.recipientId != null) {
                                    if (selectedList.recipientId == currentUser.id) "От: ${selectedList.senderName}"
                                    else "Кому: ${selectedList.recipientName}"
                                } else "Личный список",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showUserSwitchDialog = true }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(android.graphics.Color.parseColor(currentUser.colorHex)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser.name.take(1),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUser.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Сменить пользователя",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Нажмите для смены",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selectedList != null) {
                        IconButton(onClick = { selectedListId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (selectedList != null) {
                        // Кнопка направить, если мы создатель списка
                        if (selectedList.senderId == currentUser.id) {
                            FilledTonalButton(
                                onClick = { showSendDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedList.recipientId != null) "Перенаправить" else "Направить",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // Кнопка синхронизации с облаком
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                val remote = repository.syncFromCloud()
                                if (remote != null) {
                                    allLists = remote
                                }
                                isSyncing = false
                            }
                        }) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Default.SyncDisabled else Icons.Default.Sync,
                                contentDescription = "Синхронизировать",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            if (selectedList != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Товар") }
                )
            } else if (activeTab == ActiveTab.MY_LISTS) {
                FloatingActionButton(onClick = { showCreateListDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Создать список")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedList != null) {
                // ЭКРАН ДЕТАЛЬНОГО ПРОСМОТРА СПИСКА
                ListDetailScreen(
                    list = selectedList,
                    currentUser = currentUser,
                    onToggleItem = { itemId ->
                        val updatedItems = selectedList.items.map { item ->
                            if (item.id == itemId) {
                                val newBought = !item.isBought
                                item.copy(
                                    isBought = newBought,
                                    boughtBy = if (newBought) currentUser.name else null
                                )
                            } else item
                        }
                        val allBought = updatedItems.isNotEmpty() && updatedItems.all { it.isBought }
                        val anyBought = updatedItems.any { it.isBought }
                        val newStatus = if (allBought) "completed" else if (anyBought) "in_progress" else if (selectedList.recipientId != null) "sent" else "draft"

                        val updatedList = selectedList.copy(
                            items = updatedItems,
                            status = newStatus,
                            updatedAt = System.currentTimeMillis()
                        )
                        saveAndBroadcast(allLists.map { if (it.id == updatedList.id) updatedList else it })
                    },
                    onDeleteItem = { itemId ->
                        val updatedItems = selectedList.items.filter { it.id != itemId }
                        val updatedList = selectedList.copy(
                            items = updatedItems,
                            updatedAt = System.currentTimeMillis()
                        )
                        saveAndBroadcast(allLists.map { if (it.id == updatedList.id) updatedList else it })
                    }
                )
            } else {
                // ВКЛАДКИ
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    ActiveTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tab.title)
                                    if (tab == ActiveTab.INBOX && inboxUnfinishedCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge {
                                            Text(inboxUnfinishedCount.toString())
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // СПИСОК КАРТОЧЕК
                if (displayedLists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (activeTab) {
                                    ActiveTab.MY_LISTS -> Icons.Default.PlaylistAdd
                                    ActiveTab.INBOX -> Icons.Default.MailOutline
                                    ActiveTab.SENT -> Icons.Default.Send
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (activeTab) {
                                    ActiveTab.MY_LISTS -> "Нет личных списков"
                                    ActiveTab.INBOX -> "Входящих списков нет"
                                    ActiveTab.SENT -> "Нет отправленных списков"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (activeTab) {
                                    ActiveTab.MY_LISTS -> "Нажмите +, чтобы создать первый список"
                                    ActiveTab.INBOX -> "Когда другой пользователь направит вам список, он сразу появится здесь"
                                    ActiveTab.SENT -> "Откройте список в «Мои списки» и нажмите «Направить»"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedLists, key = { it.id }) { item ->
                            ShoppingListCard(
                                list = item,
                                activeTab = activeTab,
                                onClick = { selectedListId = item.id },
                                onDelete = {
                                    saveAndBroadcast(allLists.filter { it.id != item.id })
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ДИАЛОГ: СМЕНА ПОЛЬЗОВАТЕЛЯ
    if (showUserSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showUserSwitchDialog = false },
            title = { Text("Выберите профиль") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Переключайтесь между пользователями прямо в приложении, чтобы тестировать обмен списками:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    users.forEach { u ->
                        val isCurrent = u.id == currentUser.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentUserId = u.id
                                    repository.setCurrentUserId(u.id)
                                    selectedListId = null
                                    showUserSwitchDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(android.graphics.Color.parseColor(u.colorHex)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(u.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = u.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            showUserSwitchDialog = false
                            showNewUserDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Добавить другого пользователя")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserSwitchDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // ДИАЛОГ: ДОБАВИТЬ НОВОГО ПОЛЬЗОВАТЕЛЯ
    if (showNewUserDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewUserDialog = false },
            title = { Text("Новый пользователь") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Имя (например, Иван, Мама)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        val colors = listOf("#8B5CF6", "#F59E0B", "#EF4444", "#14B8A6", "#6366F1")
                        val newUser = User(
                            name = newName.trim(),
                            colorHex = colors.random()
                        )
                        val updatedUsers = users + newUser
                        users = updatedUsers
                        repository.saveUsers(updatedUsers)
                        currentUserId = newUser.id
                        repository.setCurrentUserId(newUser.id)
                        showNewUserDialog = false
                    }
                }) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewUserDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ДИАЛОГ: СОЗДАТЬ СПИСОК
    if (showCreateListDialog) {
        var title by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(ShoppingItem.CATEGORIES.first()) }

        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Новый список покупок") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название списка *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Категория:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ShoppingItem.CATEGORIES) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val newList = ShoppingList(
                            title = title.trim(),
                            category = selectedCategory,
                            senderId = currentUser.id,
                            senderName = currentUser.name,
                            items = emptyList()
                        )
                        saveAndBroadcast(listOf(newList) + allLists)
                        selectedListId = newList.id
                        showCreateListDialog = false
                    }
                }) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ДИАЛОГ: НАПРАВИТЬ СПИСОК ДРУГОМУ ПОЛЬЗОВАТЕЛЮ
    if (showSendDialog && selectedList != null) {
        val targetUsers = users.filter { it.id != currentUser.id }
        AlertDialog(
            onDismissRequest = { showSendDialog = false },
            title = { Text("Кому направить список?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Список мгновенно появится во вкладке «Входящие» выбранного пользователя:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (targetUsers.isEmpty()) {
                        Text(
                            text = "Нет других пользователей. Сначала добавьте нового пользователя через меню профиля.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        targetUsers.forEach { targetUser ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updatedList = selectedList.copy(
                                            recipientId = targetUser.id,
                                            recipientName = targetUser.name,
                                            status = "sent",
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        saveAndBroadcast(allLists.map { if (it.id == updatedList.id) updatedList else it })
                                        showSendDialog = false
                                        selectedListId = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(android.graphics.Color.parseColor(targetUser.colorHex)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(targetUser.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(targetUser.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Направить в приложение", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSendDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ДИАЛОГ: ДОБАВИТЬ ТОВАР
    if (showAddItemDialog && selectedList != null) {
        var itemName by remember { mutableStateOf("") }
        var itemQuantity by remember { mutableStateOf("1 шт") }
        var itemCategory by remember { mutableStateOf(ShoppingItem.CATEGORIES.first()) }
        val quickQuantities = listOf("1 шт", "2 шт", "0.5 кг", "1 кг", "2 кг", "1 л")

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Добавить покупку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Название товара *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = itemQuantity,
                        onValueChange = { itemQuantity = it },
                        label = { Text("Количество / объём") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickQuantities) { q ->
                            SuggestionChip(
                                onClick = { itemQuantity = q },
                                label = { Text(q, fontSize = 12.sp) }
                            )
                        }
                    }

                    Text("Категория:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ShoppingItem.CATEGORIES) { cat ->
                            FilterChip(
                                selected = itemCategory == cat,
                                onClick = { itemCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemName.isNotBlank()) {
                        val newItem = ShoppingItem(
                            name = itemName.trim(),
                            quantity = itemQuantity.trim().ifBlank { "1 шт" },
                            category = itemCategory
                        )
                        val updatedList = selectedList.copy(
                            items = selectedList.items + newItem,
                            updatedAt = System.currentTimeMillis()
                        )
                        saveAndBroadcast(allLists.map { if (it.id == updatedList.id) updatedList else it })
                        showAddItemDialog = false
                    }
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    list: ShoppingList,
    activeTab: ActiveTab,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val boughtCount = list.items.count { it.isBought }
    val totalCount = list.items.size
    val isComplete = totalCount > 0 && boughtCount == totalCount

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = list.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (activeTab) {
                            ActiveTab.INBOX -> "От: ${list.senderName}"
                            ActiveTab.SENT -> "Кому: ${list.recipientName ?: "—"}"
                            ActiveTab.MY_LISTS -> list.category
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isComplete -> MaterialTheme.colorScheme.primaryContainer
                        list.recipientId != null -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            isComplete -> "Куплено"
                            list.recipientId != null -> "Направлен"
                            else -> "Черновик"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
                            list.recipientId != null -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (totalCount == 0) "Список пуст" else "Куплено $boughtCount из $totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ListDetailScreen(
    list: ShoppingList,
    currentUser: User,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    if (list.items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "В списке пока нет товаров",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Нажмите «+ Товар» в правом нижнем углу",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list.items, key = { it.id }) { item ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleItem(item.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (item.isBought)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isBought,
                            onCheckedChange = { onToggleItem(item.id) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.isBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (item.isBought) FontWeight.Normal else FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.quantity,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (item.boughtBy != null) {
                                    Text(
                                        text = "Купил(а): ${item.boughtBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
