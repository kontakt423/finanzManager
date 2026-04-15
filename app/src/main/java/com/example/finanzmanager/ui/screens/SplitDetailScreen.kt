package com.example.finanzmanager.ui.screens

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.Transaction
import com.example.finanzmanager.ui.FilterType
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.components.FilterBar
import com.example.finanzmanager.ui.components.TransactionItem
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.components.parseHexColor
import com.example.finanzmanager.ui.components.formatDateDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitDetailSheet(
    state: UiState,
    vm: FinanzViewModel,
    onEditTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    // Local filter state — independent of the dashboard filter
    var localFilter by remember { mutableStateOf(FilterType.MONTH) }
    var localCustomFrom by remember { mutableStateOf(state.customFrom) }
    var localCustomTo   by remember { mutableStateOf(state.customTo) }

    // Compute filtered split transactions using local filter
    val (rangeFrom, rangeTo) = remember(localFilter, localCustomFrom, localCustomTo) {
        vm.effectiveRangeFor(localFilter, localCustomFrom, localCustomTo)
    }

    val allSplitTxs = remember(state.transactions, rangeFrom, rangeTo) {
        state.transactions
            .filter { it.isSplit && it.date in rangeFrom..rangeTo }
            .sortedByDescending { it.date }
    }

    val splitExpenses = remember(allSplitTxs) {
        allSplitTxs.filter { it.type == "expense" }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else t.amount / 2
        }
    }
    val splitIncome = remember(allSplitTxs) {
        allSplitTxs.filter { it.type == "income" }.sumOf { t ->
            if (t.splitMode == "partner") t.amount else t.amount / 2
        }
    }
    val net = splitExpenses - splitIncome

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF5B21B6))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "SPLIT-TOPF",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    // Net badge
                    val netColor = if (net >= 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${if (net > 0) "+" else ""}${formatCurrency(net)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = netColor
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "GUTHABEN",
                            fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            color = Color.White.copy(0.6f), letterSpacing = 1.sp
                        )
                        Text(
                            "(Partner zahlt dir)",
                            fontSize = 8.sp, color = Color.White.copy(0.45f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            formatCurrency(splitExpenses),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF86EFAC), fontSize = 18.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "ABZUG",
                            fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            color = Color.White.copy(0.6f), letterSpacing = 1.sp
                        )
                        Text(
                            "(du zahlst Partner)",
                            fontSize = 8.sp, color = Color.White.copy(0.45f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            formatCurrency(splitIncome),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF9A8D4), fontSize = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Filter bar (white on purple)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterType.entries.forEach { type ->
                        val label = when (type) {
                            FilterType.TODAY  -> "Heute"
                            FilterType.WEEK   -> "Woche"
                            FilterType.MONTH  -> "Monat"
                            FilterType.CUSTOM -> "Indiv."
                        }
                        val isSelected = localFilter == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color.White
                                    else Color.Transparent
                                )
                                .clickable { localFilter = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF5B21B6)
                                        else Color.White.copy(0.7f)
                            )
                        }
                    }
                }

                // Custom date range
                if (localFilter == FilterType.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SplitDateButton(
                            label = "Von",
                            value = localCustomFrom,
                            onDateSelected = { localCustomFrom = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", color = Color.White.copy(0.5f), fontSize = 14.sp)
                        SplitDateButton(
                            label = "Bis",
                            value = localCustomTo,
                            onDateSelected = { localCustomTo = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Transaction list ─────────────────────────────────────────────
            if (allSplitTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Keine Split-Buchungen im gewählten Zeitraum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Section headers + items
                val expTxs = allSplitTxs.filter { it.type == "expense" }
                val incTxs = allSplitTxs.filter { it.type == "income" }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (expTxs.isNotEmpty()) {
                        item {
                            SplitSectionHeader(
                                title = "Guthaben — Partner zahlt dir",
                                total = splitExpenses,
                                color = Color(0xFF10B981)
                            )
                        }
                        items(expTxs) { tx ->
                            val cat = state.categories.find { it.id == tx.categoryId }
                            SplitTransactionRow(
                                tx = tx,
                                category = cat,
                                onClick = { onEditTransaction(tx); onDismiss() }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    if (incTxs.isNotEmpty()) {
                        item {
                            SplitSectionHeader(
                                title = "Abzug — du zahlst Partner",
                                total = splitIncome,
                                color = Color(0xFFEC4899)
                            )
                        }
                        items(incTxs) { tx ->
                            val cat = state.categories.find { it.id == tx.categoryId }
                            SplitTransactionRow(
                                tx = tx,
                                category = cat,
                                onClick = { onEditTransaction(tx); onDismiss() }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }
}



// White-themed date picker button for the purple Split header
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitDateButton(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val initialMillis = remember(value) {
        try {
            LocalDate.parse(value, fmt)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color.White.copy(0.8f),
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    label,
                    fontSize = 8.sp,
                    color = Color.White.copy(0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    if (showDialog) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(picked.format(fmt))
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// Compact two-column row: [icon + name + date] | [100%] | [50%]
@Composable
private fun SplitTransactionRow(
    tx: com.example.finanzmanager.domain.Transaction,
    category: com.example.finanzmanager.domain.Category?,
    onClick: () -> Unit
) {
    val isExpense = tx.type == "expense"
    val amountColor = if (isExpense)
        androidx.compose.ui.graphics.Color(0xFFEF4444)
    else
        androidx.compose.ui.graphics.Color(0xFF10B981)
    val amount100 = tx.amount
    val amount50  = tx.amount / 2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category dot
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    category?.let { parseHexColor(it.color) }
                        ?: androidx.compose.ui.graphics.Color.Gray
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpense)
                    Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(10.dp))

        // Description + date — takes remaining space
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { category?.name ?: "-" },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = formatDateDisplay(tx.date),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(6.dp))

        // 100% column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = "100%",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (isExpense) "-" else "+"}${formatCurrency(amount100)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(6.dp))

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
        Spacer(Modifier.width(6.dp))

        // 50% column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = "50%",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (isExpense) "-" else "+"}${formatCurrency(amount50)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor.copy(alpha = 0.65f),
                maxLines = 1
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SplitSectionHeader(title: String, total: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.sp
        )
        Text(
            formatCurrency(total),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
    HorizontalDivider(color = color.copy(alpha = 0.2f), thickness = 1.dp)
}
