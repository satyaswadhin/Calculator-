package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculationHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val realtimeResult by viewModel.realtimeResult.collectAsStateWithLifecycle()
    val isDegreeMode by viewModel.isDegreeMode.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scientific Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // DEG/RAD quick toggler
                    TextButton(
                        onClick = { viewModel.toggleTrigMode() },
                        modifier = Modifier
                            .testTag("trig_toggle_button")
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isDegreeMode) "DEG" else "RAD",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Show history log"
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Adaptive Layout: show a side history panel on larger tablet screens (Expanded widths)
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Calculator Grid Block (2/3 of space)
                Box(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight()
                ) {
                    CalculatorMainContent(
                        expression = expression,
                        realtimeResult = realtimeResult,
                        viewModel = viewModel
                    )
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // History Side Panel (1/3 of space)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculation History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (historyList.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearHistory() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear all database histories",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No previous calculations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historyList) { item ->
                                HistoryCard(
                                    item = item,
                                    onSelect = { viewModel.selectHistoryItem(item) },
                                    onDelete = { viewModel.deleteHistoryItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Portrait phone mode: full screen calculation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CalculatorMainContent(
                    expression = expression,
                    realtimeResult = realtimeResult,
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal dialogs for history logging on standard cellular screens
    if (showHistoryDialog && !isExpanded) {
        HistoryDialog(
            historyList = historyList,
            onClose = { showHistoryDialog = false },
            onSelect = {
                viewModel.selectHistoryItem(it)
                showHistoryDialog = false
            },
            onDelete = { viewModel.deleteHistoryItem(it) },
            onClearAll = { viewModel.clearHistory() }
        )
    }

    // Settings Modal
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onClose = { showSettingsDialog = false }
        )
    }
}

@Composable
fun CalculatorMainContent(
    expression: String,
    realtimeResult: String,
    viewModel: CalculatorViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Calculation Display (Input and Live results panel)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            // Input Expression field
            Text(
                text = expression.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (expression.length > 15) 28.sp else 38.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState(initial = Int.MAX_VALUE))
                    .testTag("expression_display"),
                overflow = TextOverflow.Clip,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time parsed solution field
            AnimatedVisibility(
                visible = realtimeResult.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = realtimeResult,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (realtimeResult.length > 15) 26.sp else 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_display")
                )
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // scientific scroll grid
        ScientificScrollPanel(onKeyClick = { viewModel.appendInput(it) })

        Spacer(modifier = Modifier.height(10.dp))

        // Basic keypad grid - 5 columns
        KeypadGrid(
            onKeyClick = { key ->
                when (key) {
                    "C" -> viewModel.clearAll()
                    "⌫" -> viewModel.deleteLast()
                    "=" -> viewModel.evaluateExpression(isFinal = true)
                    "+/-" -> viewModel.handleNegate()
                    "%" -> viewModel.handlePercent()
                    else -> viewModel.appendInput(key)
                }
            }
        )
    }
}

@Composable
fun ScientificScrollPanel(onKeyClick: (String) -> Unit) {
    val keys = listOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "π", "e", "abs")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { key ->
            Button(
                onClick = { onKeyClick(key) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("button_$key")
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
fun KeypadGrid(onKeyClick: (String) -> Unit) {
    // 5 Columns:
    // C  (  )  ⌫  ÷
    // 7  8  9  ^  ×
    // 4  5  6  √  -
    // 1  2  3  !  +
    // 0  .  mod  +/-  =
    val rows = listOf(
        listOf("C", "(", ")", "⌫", "÷"),
        listOf("7", "8", "9", "^", "×"),
        listOf("4", "5", "6", "√", "-"),
        listOf("1", "2", "3", "!", "+"),
        listOf("0", ".", "mod", "+/-", "=")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyClick(key) }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOperator = label in setOf("÷", "×", "-", "+", "=")
    val isPrimaryClear = label in setOf("C", "⌫")
    val isUtility = label in setOf("(", ")", "^", "√", "!", "mod", "%", "+/-")

    val containerColor = when {
        label == "=" -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.primaryContainer
        isPrimaryClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        isUtility -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }

    val contentColor = when {
        label == "=" -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        isPrimaryClear -> MaterialTheme.colorScheme.onErrorContainer
        isUtility -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val testTagLabel = when(label) {
        "+" -> "plus"
        "-" -> "minus"
        "×" -> "multiply"
        "÷" -> "divide"
        "=" -> "equals"
        "⌫" -> "backspace"
        "C" -> "clear"
        "." -> "decimal"
        else -> label
    }

    Box(
        modifier = modifier
            .aspectRatio(if (label == "=") 1f else 1.1f)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .testTag("button_$testTagLabel"),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (label.length > 2) 15.sp else 20.sp,
                    fontWeight = if (isOperator || isPrimaryClear) FontWeight.ExtraBold else FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun HistoryCard(
    item: CalculationHistory,
    onSelect: (CalculationHistory) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = remember(item.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect(item) }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete from history log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = item.expression,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "= ${item.result}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HistoryDialog(
    historyList: List<CalculationHistory>,
    onClose: () -> Unit,
    onSelect: (CalculationHistory) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        if (historyList.isNotEmpty()) {
                            IconButton(onClick = onClearAll) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No calculations found yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { item ->
                            HistoryCard(
                                item = item,
                                onSelect = onSelect,
                                onDelete = { onDelete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: CalculatorViewModel,
    onClose: () -> Unit
) {
    val themeMode by viewModel.themeSetting.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Settings Selection Slider
                Text(
                    text = "App Interface Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Toggles (Single Select Segmented buttons using elevated buttons)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        "system" to "System Default",
                        "light" to "Force Light Theme",
                        "dark" to "Force Dark Theme"
                    )

                    themes.forEach { (mode, label) ->
                        val isSelected = themeMode == mode
                        Button(
                            onClick = { viewModel.setThemeSetting(mode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Theme",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Information banner
                Text(
                    text = "Operator Instructions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• Parentheses structure implicit parameters: e.g. 5(2+3) yields 25.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Trig ratios accept Degrees or Radians matching current active top-bar indicator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Modulo matches local standard equation remainder formats: e.g. 10 mod 3 yields 1.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onClose) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
