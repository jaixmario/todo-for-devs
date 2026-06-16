package com.mario.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mario.ui.ui.theme.UITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevTaskScreen(viewModel: DevViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<DevCategory?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filteredTasks = if (selectedFilter == null) {
        uiState.tasks
    } else {
        uiState.tasks.filter { it.category == selectedFilter }
    }

    val completedCount = uiState.tasks.count { it.isCompleted }
    val totalCount = uiState.tasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "DEV_DASHBOARD",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, "New Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            DevProgressCard(completedCount, totalCount, progress)

            CategoryFilterRow(
                categories = DevCategory.entries,
                selectedCategory = selectedFilter,
                onCategorySelected = { selectedFilter = it }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    DevTaskItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddDevTaskDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, category, priority, time, timer ->
                    viewModel.addTask(title, category, priority, time, timer)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DevProgressCard(completed: Int, total: Int, progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Sprint Progress",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Done: $completed / Total: $total",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<DevCategory>,
    selectedCategory: DevCategory?,
    onCategorySelected: (DevCategory?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        item {
            SuggestionChip(
                onClick = { onCategorySelected(null) },
                label = { Text("all_tasks", fontFamily = FontFamily.Monospace) },
                border = if (selectedCategory == null) SuggestionChipDefaults.suggestionChipBorder(true, borderColor = MaterialTheme.colorScheme.primary) else SuggestionChipDefaults.suggestionChipBorder(true)
            )
        }
        items(categories) { category ->
            SuggestionChip(
                onClick = { onCategorySelected(category) },
                label = { Text(category.name.lowercase(), fontFamily = FontFamily.Monospace) },
                icon = {
                    Icon(
                        imageVector = getDevIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                border = if (selectedCategory == category) SuggestionChipDefaults.suggestionChipBorder(true, borderColor = MaterialTheme.colorScheme.primary) else SuggestionChipDefaults.suggestionChipBorder(true)
            )
        }
    }
}

@Composable
fun DevTaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "#${task.category.name.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    PriorityBadge(task.priority)
                    
                    if (task.scheduledTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(task.scheduledTime, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    
                    if (task.timerDurationMinutes != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${task.timerDurationMinutes}m", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    val color = when(priority) {
        Priority.Critical -> Color(0xFFE57373)
        Priority.High -> Color(0xFFFFB74D)
        Priority.Medium -> Color(0xFF81C784)
        Priority.Low -> Color(0xFF90A4AE)
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.name.lowercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color
        )
    }
}

fun getDevIcon(category: DevCategory): ImageVector {
    return when (category) {
        DevCategory.Feature -> Icons.Outlined.Code
        DevCategory.Bug -> Icons.Outlined.BugReport
        DevCategory.Review -> Icons.Outlined.RateReview
        DevCategory.Docs -> Icons.Outlined.Description
        DevCategory.Refactor -> Icons.Outlined.History
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevTaskDialog(onDismiss: () -> Unit, onAdd: (String, DevCategory, Priority, String?, Int?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DevCategory.Feature) }
    var selectedPriority by remember { mutableStateOf(Priority.Medium) }
    var scheduledTime by remember { mutableStateOf("") }
    var timerMinutes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CREATE_NEW_TASK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task description...", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        placeholder = { Text("Time (e.g. 8:00 PM)", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        label = { Text("SCHEDULE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    TextField(
                        value = timerMinutes,
                        onValueChange = { if (it.all { c -> c.isDigit() }) timerMinutes = it },
                        placeholder = { Text("Mins", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.width(80.dp),
                        label = { Text("TIMER", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }

                Column {
                    Text("CATEGORY", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DevCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clickable { selectedCategory = category }.padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) { Icon(getDevIcon(category), null, modifier = Modifier.size(16.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }

                Column {
                    Text("PRIORITY", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Priority.entries.forEach { priority ->
                            val isSelected = selectedPriority == priority
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clickable { selectedPriority = priority }.padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(priority.name.substring(0, 1), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, selectedCategory, selectedPriority, if (scheduledTime.isBlank()) null else scheduledTime, timerMinutes.toIntOrNull()) },
                enabled = title.isNotBlank()
            ) { Text("EXECUTE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ABORT", fontFamily = FontFamily.Monospace) } },
        shape = RoundedCornerShape(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun DevTaskPreview() {
    UITheme { DevTaskScreen() }
}
