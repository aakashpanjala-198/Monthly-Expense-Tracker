package com.easy.monthlyexpensetracker.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.easy.monthlyexpensetracker.data.CycleDigest
import com.easy.monthlyexpensetracker.data.ExpenseEntry
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailScreen(
    digest: CycleDigest?,
    onUpPressed: () -> Unit,
    onAdjustIncome: (Double) -> Unit,
    onUpdateCycle: (String, Int, Int) -> Unit,
    onAddExpense: (String, Double, String, Long) -> Unit,
    onUpdateExpense: (ExpenseEntry) -> Unit,
    onDeleteExpense: (ExpenseEntry) -> Unit,
    onOpenChart: () -> Unit
) {
    var showIncome by rememberSaveable { mutableStateOf(false) }
    var showMeta by rememberSaveable { mutableStateOf(false) }
    var editingExpense by rememberSaveable { mutableStateOf<ExpenseEntry?>(null) }
    var deletingExpense by rememberSaveable { mutableStateOf<ExpenseEntry?>(null) }
    var creatingExpense by rememberSaveable { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = digest?.record?.title?.ifBlank {
                            digest?.record?.let { monthLabel(it.month) } ?: "Cycle"
                        } ?: "Cycle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onUpPressed) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Navigate up"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMeta = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onOpenChart) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "Chart")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creatingExpense = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        if (digest == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("This cycle no longer exists.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                BalanceCapsule(
                    income = digest.record.income,
                    spent = digest.totalSpent,
                    balance = digest.balance,
                    onAdjustIncome = { showIncome = true }
                )
            }
            item {
                MetricsRow(digest = digest)
            }
            items(digest.expenses, key = { it.id }) { entry ->
                ExpenseCard(
                    entry = entry,
                    onEdit = { editingExpense = entry },
                    onDelete = { deletingExpense = entry }
                )
            }
            if (digest.expenses.isEmpty()) {
                item {
                    EmptyExpensesHint()
                }
            }
        }
    }

    if (showIncome) {
        IncomeDialog(
            currentValue = digest?.record?.income ?: 0.0,
            onDismiss = { showIncome = false },
            onConfirm = {
                onAdjustIncome(it)
                showIncome = false
            }
        )
    }

    if (showMeta && digest != null) {
        CycleMetaSheet(
            title = digest.record.title,
            year = digest.record.year,
            month = digest.record.month,
            onDismiss = { showMeta = false },
            onConfirm = { t, y, m ->
                onUpdateCycle(t, y, m)
                showMeta = false
            }
        )
    }

    if (creatingExpense) {
        ExpenseSheet(
            title = "Log outgoing",
            onDismiss = { creatingExpense = false },
            onConfirm = { name, amount, category, millis ->
                onAddExpense(name, amount, category, millis)
                creatingExpense = false
            }
        )
    }

    editingExpense?.let { entry ->
        ExpenseSheet(
            title = "Edit outgoing",
            initialTitle = entry.title,
            initialAmount = entry.amount,
            initialCategory = entry.category,
            initialMillis = entry.spentAt,
            onDismiss = { editingExpense = null },
            onConfirm = { name, amount, category, millis ->
                onUpdateExpense(
                    entry.copy(
                        title = name,
                        amount = amount,
                        category = category,
                        spentAt = millis
                    )
                )
                editingExpense = null
            }
        )
    }

    deletingExpense?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete outgoing") },
            text = { Text("Remove ${entry.title}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense(entry)
                    deletingExpense = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BalanceCapsule(
    income: Double,
    spent: Double,
    balance: Double,
    onAdjustIncome: () -> Unit
) {
    val gradient = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF7F5AF0),
                Color(0xFF2CB1BC)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Balance outlook",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(alpha = 0.85f))
            )
            Text(
                text = currencyFormatter.format(balance),
                style = MaterialTheme.typography.displaySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricPill(label = "Income", value = currencyFormatter.format(income))
                MetricPill(label = "Spent", value = currencyFormatter.format(spent))
            }
            SuggestionChip(
                onClick = onAdjustIncome,
                label = { Text("Adjust income", color = MaterialTheme.colorScheme.onPrimary) },
                icon = {
                    Icon(Icons.Default.Savings, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
    }
}

@Composable
private fun MetricsRow(digest: CycleDigest) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DataTile(
            modifier = Modifier.weight(1f),
            label = "Outgoings",
            value = digest.expenses.size.toString()
        )
        DataTile(
            modifier = Modifier.weight(1f),
            label = "Category spread",
            value = digest.expenses.map { it.category.ifBlank { "General" } }.distinct().size.toString()
        )
        val average = if (digest.expenses.isEmpty()) 0.0 else digest.totalSpent / digest.expenses.size
        DataTile(
            modifier = Modifier.weight(1f),
            label = "Average spend",
            value = currencyFormatter.format(average)
        )
    }
}

@Composable
private fun DataTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExpenseCard(
    entry: ExpenseEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = remember(entry.category) {
        when ((entry.category.hashCode() and 0xFFFFFF) % 4) {
            0 -> Brush.horizontalGradient(listOf(Color(0xFF9B5DE5), Color(0xFFF15BB5)))
            1 -> Brush.horizontalGradient(listOf(Color(0xFF00BBF9), Color(0xFF3A86FF)))
            2 -> Brush.horizontalGradient(listOf(Color(0xFF06D6A0), Color(0xFF118AB2)))
            else -> Brush.horizontalGradient(listOf(Color(0xFFFFA822), Color(0xFFEF476F)))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    currencyFormatter.format(entry.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    expenseDate(entry.spentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.category.isNotBlank()) {
                Text(
                    entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun EmptyExpensesHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("No outgoings logged", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap the floating action button to record your first outgoing. We'll keep the timeline clean and minimal.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun IncomeDialog(
    currentValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(currentValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust income goal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Income") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toDoubleOrNull()?.let {
                    onConfirm(it)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleMetaSheet(
    title: String,
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(title) }
    var yearState by rememberSaveable { mutableIntStateOf(year) }
    var monthState by rememberSaveable { mutableIntStateOf(month) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cycle details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = monthState.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.takeIf { v -> v in 1..12 }?.let { monthState = it }
                    },
                    label = { Text("Month") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = yearState.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.takeIf { v -> v in 2000..2100 }?.let { yearState = it }
                    },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.size(12.dp))
                Button(onClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onConfirm(name, yearState, monthState)
                    }
                }) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    title: String,
    initialTitle: String = "",
    initialAmount: Double = 0.0,
    initialCategory: String = "",
    initialMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialTitle) }
    var amount by rememberSaveable { mutableStateOf(initialAmount.toString()) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var millis by rememberSaveable { mutableStateOf(initialMillis) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                singleLine = true
            )
            SuggestionChip(
                onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            millis = calendar.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                label = { Text("Date: ${expenseDate(millis)}") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.size(12.dp))
                Button(onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed != null) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onConfirm(name, parsed, category, millis)
                        }
                    }
                }) { Text("Save") }
            }
        }
    }
}

private val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance()

private fun expenseDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(millis)
}
