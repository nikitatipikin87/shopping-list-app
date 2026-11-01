package com.example.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.shoppinglist.ui.theme.ShoppingListTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: ShoppingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ShoppingRepository(this)

        setContent {
            ShoppingListTheme {
                ShoppingApp(
                    initialItems = repository.loadItems(),
                    onSaveItems = { items -> repository.saveItems(items) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingApp(
    initialItems: List<ShoppingItem>,
    onSaveItems: (List<ShoppingItem>) -> Unit
) {
    var items by remember { mutableStateOf(initialItems) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Все") }
    var searchQuery by remember { mutableStateOf("") }

    fun updateAndSave(newItems: List<ShoppingItem>) {
        items = newItems
        onSaveItems(newItems)
    }

    val categories = listOf("Все") + ShoppingItem.CATEGORIES

    val filteredItems = items.filter { item ->
        val matchesCategory = selectedCategory == "Все" || item.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }.sortedBy { it.isBought }

    val boughtCount = items.count { it.isBought }
    val totalCount = items.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Список покупок",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (totalCount > 0) {
                            Text(
                                text = "Куплено $boughtCount из $totalCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (boughtCount > 0) {
                        IconButton(onClick = {
                            updateAndSave(items.filter { !it.isBought })
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Удалить купленное",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить товар")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по списку...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (items.isEmpty()) "Список покупок пуст" else "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (items.isEmpty()) "Нажмите +, чтобы добавить первый товар" else "Попробуйте изменить поисковый запрос",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredItems,
                        key = { it.id }
                    ) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggleBought = {
                                updateAndSave(items.map {
                                    if (it.id == item.id) it.copy(isBought = !it.isBought) else it
                                })
                            },
                            onDelete = {
                                updateAndSave(items.filter { it.id != item.id })
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, quantity, category ->
                val newItem = ShoppingItem(
                    name = name,
                    quantity = quantity.ifBlank { "1 шт" },
                    category = category
                )
                updateAndSave(listOf(newItem) + items)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggleBought: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleBought() },
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isBought,
                onCheckedChange = { onToggleBought() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isBought) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isBought) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quantity tag
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.quantity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Category tag
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, quantity: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1 шт") }
    var selectedCategory by remember { mutableStateOf(ShoppingItem.CATEGORIES.first()) }
    var showError by remember { mutableStateOf(false) }

    val quickQuantities = listOf("1 шт", "2 шт", "0.5 кг", "1 кг", "2 кг", "1 л")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый товар") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Название товара *") },
                    isError = showError,
                    supportingText = {
                        if (showError) Text("Введите название товара")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Количество / объем") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick quantity chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickQuantities) { q ->
                        SuggestionChip(
                            onClick = { quantity = q },
                            label = { Text(q, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = "Категория:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onAdd(name.trim(), quantity.trim(), selectedCategory)
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
