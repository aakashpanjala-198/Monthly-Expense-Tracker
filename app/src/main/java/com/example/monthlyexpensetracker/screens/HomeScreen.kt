package com.example.monthlyexpensetracker.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.example.monthlyexpensetracker.data.CycleRecord
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HomeUiState(
    val cycles: List<CycleRecord>,
    val highlightIncome: Double,
    val latestLabel: String,
    val empty: Boolean
)

fun buildHomeUiState(cycles: List<CycleRecord>): HomeUiState {
    val totalIncome = cycles.sumOf { it.income }
    val latestLabel = cycles.firstOrNull()?.let { record ->
        "${monthLabel(record.month)} ${record.year}"
    } ?: "No cycles yet"
    return HomeUiState(
        cycles = cycles,
        highlightIncome = totalIncome,
        latestLabel = latestLabel,
        empty = cycles.isEmpty()
    )
}

@Composable
private fun DefaultDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )
    }
}


@OptIn(
   ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun AuroraHomeScreen(
    state: HomeUiState,
    onCreateQuick: (title: String, income: Double) -> Unit,
    onCreateAdvanced: (title: String, year: Int, month: Int, income: Double) -> Unit,
    onOpenCycle: (Long) -> Unit,
    onEditCycle: (CycleRecord) -> Unit,
    onDeleteCycle: (CycleRecord) -> Unit,
    onOpenChart: () -> Unit
) {
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by rememberSaveable { mutableStateOf<CycleRecord?>(null) }
    var editingTarget by rememberSaveable { mutableStateOf<CycleRecord?>(null) }

    val carouselState = rememberLazyListState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Orbit Ledger") },
                actions = {
                    IconButton(onClick = onOpenChart) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "Open trends")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showComposer = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add cycle")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    AuroraHeroCard(
                        totalIncome = state.highlightIncome,
                        latestLabel = state.latestLabel,
                        onCreateAdvanced = { showAdvanced = true }
                    )
                }
                if (state.empty) {
                    item {
                        EmptyAuroraState(onCreate = { showComposer = true })
                    }
                } else {
                    stickyHeader {
                        Text(
                            text = "Cycles in orbit",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            state = carouselState,
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = carouselState),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(items = state.cycles, key = { it.id }) { cycle ->
                                AuroraCycleCard(
                                    record = cycle,
                                    onOpen = { onOpenCycle(cycle.id) },
                                    onEdit = { editingTarget = cycle },
                                    onDelete = { deleteTarget = cycle }
                                )
                            }
                        }
                    }


                    items(state.cycles, key = { it.id }) { cycle ->
                        OrbitListRow(
                            record = cycle,
                            onOpen = { onOpenCycle(cycle.id) },
                            onEdit = { editingTarget = cycle },
                            onDelete = { deleteTarget = cycle }
                        )
                    }
                }
            }
        }
    }

    if (showComposer) {
        QuickCycleComposer(
            onDismiss = { showComposer = false },
            onCreate = { title, income ->
                onCreateQuick(title, income)
                showComposer = false
            },
            onNeedAdvanced = {
                showComposer = false
                showAdvanced = true
            }
        )
    }

    if (showAdvanced) {
        CycleEditorSheet(
            title = "Create New Cycle",
            initialTitle = monthLabel(Calendar.getInstance().get(Calendar.MONTH) + 1),
            initialYear = Calendar.getInstance().get(Calendar.YEAR),
            initialMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
            initialIncome = 0.0,
            onDismiss = { showAdvanced = false },
            onConfirm = { title, year, month, income ->
                onCreateAdvanced(title, year, month, income)
                showAdvanced = false
            }
        )
    }

    editingTarget?.let { record ->
        CycleEditorSheet(
            title = "Edit Cycle",
            initialTitle = record.title,
            initialYear = record.year,
            initialMonth = record.month,
            initialIncome = record.income,
            onDismiss = { editingTarget = null },
            onConfirm = { title, year, month, income ->
                onEditCycle(
                    record.copy(
                        title = title,
                        year = year,
                        month = month,
                        income = income
                    )
                )
                editingTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeletionDialog(
            label = target.title.ifBlank { monthLabel(target.month) },
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteCycle(target)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun AuroraHeroCard(
    totalIncome: Double,
    latestLabel: String,
    onCreateAdvanced: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total income in orbit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = currencyFormatter.format(totalIncome),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = onCreateAdvanced,
                    label = { Text("Plan a cycle") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    }
                )
                Text(
                    text = "Last viewed: $latestLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptyAuroraState(onCreate: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Design your first orbit",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create a cycle, define your income goal, and track every outgoing with our prism layout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Button(onClick = onCreate) {
                Icon(Icons.Default.NewLabel, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Start tracking")
            }
        }
    }
}

@Composable
private fun AuroraCycleCard(
    record: CycleRecord,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gradient = remember(record.id) {
        Brush.verticalGradient(
            when ((record.month % 4)) {
                0 -> listOf(Color(0xFF7C6CFF), Color(0xFF49C8FF))
                1 -> listOf(Color(0xFFFF8FAB), Color(0xFFFFC36D))
                2 -> listOf(Color(0xFF4ADEDE), Color(0xFF9C6CFF))
                else -> listOf(Color(0xFF39D98A), Color(0xFF38A1F2))
            }
        )
    }

    ElevatedCard(
        onClick = onOpen,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .size(width = 240.dp, height = 320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = monthLabel(record.month),
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                    )
                    Text(
                        text = record.year.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = record.title.ifBlank { "Untitled orbit" },
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currencyFormatter.format(record.income),
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text("Edit") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    )
                    AssistChip(
                        onClick = onDelete,
                        label = { Text("Remove") }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbitListRow(
    record: CycleRecord,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = record.title.ifBlank { "Untitled orbit" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${monthLabel(record.month)} ${record.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Income goal ${currencyFormatter.format(record.income)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
private fun QuickCycleComposer(
    onDismiss: () -> Unit,
    onCreate: (title: String, income: Double) -> Unit,
    onNeedAdvanced: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Launch an orbit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Orbit name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = income,
                    onValueChange = { income = it },
                    label = { Text("Income goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Text(
                    text = "Want to pick a month or adjust income later? Use the advanced composer.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedIncome = income.toDoubleOrNull()
                    if (parsedIncome != null) {
                        onCreate(title.ifBlank { "Untitled orbit" }, parsedIncome)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onNeedAdvanced) {
                Text("Advanced")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleEditorSheet(
    title: String,
    initialTitle: String,
    initialYear: Int,
    initialMonth: Int,
    initialIncome: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Double) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initialTitle) }
    var year by rememberSaveable { mutableIntStateOf(initialYear) }
    var month by rememberSaveable { mutableIntStateOf(initialMonth) }
    var income by rememberSaveable { mutableStateOf(initialIncome.toString()) }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { DefaultDragHandle() } // <-- use our custom handle
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
                label = { Text("Orbit name") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = month.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 1..12) month = value
                    },
                    label = { Text("Month (1-12)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = year.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 2000..2100) year = value
                    },
                    label = { Text("Year") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
            OutlinedTextField(
                value = income,
                onValueChange = { income = it },
                label = { Text("Income goal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(onClick = {
                    val parsedIncome = income.toDoubleOrNull() ?: 0.0
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onConfirm(name, year, month, parsedIncome)
                    }
                }) {
                    Text("Save")
                }
            }
        }
    }
}


@Composable
private fun ConfirmDeletionDialog(
    label: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove orbit") },
        text = { Text("Are you sure you want to delete \"$label\"? All entries within will vanish.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance()

fun monthLabel(month: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month - 1)
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

