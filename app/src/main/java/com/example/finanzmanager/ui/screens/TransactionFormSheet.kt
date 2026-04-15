package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.*
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.today
import com.example.finanzmanager.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    initialTx: Transaction?,
    isStandingOrder: Boolean = false,
    initialOrder: StandingOrder? = null,
    state: UiState,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var isOrder by remember { mutableStateOf(isStandingOrder || initialOrder != null) }
    var txType by remember { mutableStateOf(initialTx?.type ?: initialOrder?.type ?: "expense") }
    var amount by remember { mutableStateOf((initialTx?.amount ?: initialOrder?.amount ?: 0.0).let { if (it == 0.0) "" else it.toString() }) }
    var description by remember { mutableStateOf(initialTx?.description ?: initialOrder?.description ?: "") }
    var categoryId by remember { mutableStateOf(initialTx?.categoryId ?: initialOrder?.categoryId ?: state.categories.firstOrNull()?.id ?: "") }
    var accountId by remember { mutableStateOf(initialTx?.accountId ?: initialOrder?.accountId ?: state.accounts.firstOrNull()?.id ?: "") }
    var toAccountId by remember { mutableStateOf(initialTx?.toAccountId ?: initialOrder?.toAccountId ?: "") }
    var isSplit by remember { mutableStateOf(initialTx?.isSplit ?: initialOrder?.isSplit ?: false) }
    var isSettlement by remember { mutableStateOf(initialTx?.isSettlement ?: false) }
    var splitMode by remember { mutableStateOf(initialTx?.splitMode ?: initialOrder?.splitMode ?: "half") }
    var date by remember { mutableStateOf(initialTx?.date ?: today()) }
    var interval by remember { mutableStateOf(initialOrder?.interval ?: "monthly") }
    var nextRun by remember { mutableStateOf(initialOrder?.nextRun ?: today()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Buchung", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (initialTx != null || initialOrder != null) {
                    IconButton(onClick = {
                        initialTx?.id?.let { vm.deleteTransaction(it) }
                        initialOrder?.id?.let { vm.deleteStandingOrder(it) }
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // One-time vs Standing Order toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(false to "Einmalig", true to "Dauerauftrag").forEach { (isOrderVal, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOrder == isOrderVal) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isOrder = isOrderVal }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (isOrder == isOrderVal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Transaction type selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("expense" to "Ausgabe", "income" to "Einnahme", "transfer" to "Transfer").forEach { (type, label) ->
                    val bg = when {
                        txType == type && type == "income" -> Color(0xFF059669)
                        txType == type && type == "transfer" -> Color(0xFF1D4ED8)
                        txType == type -> Color(0xFFDC2626)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { txType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = if (txType == type) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                placeholder = { Text("0,00 €", textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            // Live parsed value preview - helps user see if comma/dot was accepted
            val parsedAmt = amount.parseLocalDouble()
            if (amount.isNotBlank()) {
                Text(
                    text = if (parsedAmt != null)
                        "✓ ${formatCurrency(parsedAmt)}"
                    else
                        "⚠ Ungültiger Betrag – bitte Komma oder Punkt verwenden",
                    fontSize = 11.sp,
                    color = if (parsedAmt != null)
                        androidx.compose.ui.graphics.Color(0xFF10B981)
                    else
                        androidx.compose.ui.graphics.Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Account & Category selectors
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Account
                ExposedDropdownSelector(
                    label = "Konto",
                    options = state.accounts.map { it.id to it.name },
                    selected = accountId,
                    onSelect = { accountId = it },
                    modifier = Modifier.weight(1f)
                )
                // Category or To-Account
                if (txType == "transfer") {
                    ExposedDropdownSelector(
                        label = "Zielkonto",
                        options = state.accounts.filter { it.id != accountId }.map { it.id to it.name },
                        selected = toAccountId,
                        onSelect = { toAccountId = it },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val filteredCats = state.categories.filter {
                        it.type == if (txType == "income") "income" else "expense"
                    }
                    ExposedDropdownSelector(
                        label = "Kategorie",
                        options = filteredCats.map { it.id to it.name },
                        selected = categoryId,
                        onSelect = { categoryId = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Date / Interval
            if (isOrder) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownSelector(
                        label = "Intervall",
                        options = listOf("weekly" to "Wöchentlich", "monthly" to "Monatlich", "yearly" to "Jährlich"),
                        selected = interval,
                        onSelect = { interval = it },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerButton(
                        label = "Erste Ausführung",
                        value = nextRun,
                        onDateSelected = { nextRun = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                DatePickerButton(
                    label = "Datum",
                    value = date,
                    onDateSelected = { date = it }
                )
            }

            // Split toggle (only for non-transfer)
            if (txType != "transfer" && !isOrder) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSplit = !isSplit },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSplit) Color(0xFF5B21B6).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CallSplit, contentDescription = null,
                                tint = if (isSplit) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Split-Topf", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Checkbox(
                            checked = isSplit,
                            onCheckedChange = { isSplit = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C3AED))
                        )
                    }
                }

                if (isSplit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("half" to "50% Split", "partner" to "100% Partner").forEach { (mode, label) ->
                            OutlinedButton(
                                onClick = { splitMode = mode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (splitMode == mode) Color(0xFF5B21B6) else Color.Transparent,
                                    contentColor = if (splitMode == mode) Color.White else Color(0xFF7C3AED)
                                ),
                                border = null
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Partnerausgleich toggle (expense only, not for standing orders)
            if (txType == "expense" && !isOrder) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSettlement = !isSettlement },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSettlement)
                            androidx.compose.ui.graphics.Color(0xFF1D4ED8).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Handshake,
                                contentDescription = null,
                                tint = if (isSettlement)
                                    androidx.compose.ui.graphics.Color(0xFF3B82F6)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Partnerausgleich",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Text(
                                    "Hebt Split-Topf Reservierung auf",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = isSettlement,
                            onCheckedChange = { isSettlement = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)
                            )
                        )
                    }
                }
            }

            // Save button
            Button(
                onClick = {
                    val amt = amount.parseLocalDouble() ?: return@Button
                    if (isOrder) {
                        vm.saveStandingOrder(
                            id = initialOrder?.id, type = txType, amount = amt,
                            description = description, categoryId = categoryId,
                            accountId = accountId, toAccountId = toAccountId.ifBlank { null },
                            isSplit = isSplit, splitMode = splitMode, interval = interval, nextRun = nextRun
                        )
                    } else {
                        vm.saveTransaction(
                            id = initialTx?.id, type = txType, amount = amt,
                            description = description, categoryId = categoryId,
                            accountId = accountId, toAccountId = toAccountId.ifBlank { null },
                            isSplit = isSplit, splitMode = splitMode, date = date,
                            isSettlement = isSettlement
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Speichern", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}


// Parses "1.500,00" (DE), "1,500.00" (EN), "1500,5" and "1500.5" all correctly
fun String.parseLocalDouble(): Double? {
    val cleaned = this.trim()
        .replace(" ", "")
        .replace(" ", "") // non-breaking space
    // Detect format: if both . and , exist, the last one is decimal separator
    val lastDot   = cleaned.lastIndexOf('.')
    val lastComma = cleaned.lastIndexOf(',')
    val normalized = when {
        lastComma > lastDot  -> cleaned.replace(".", "").replace(",", ".")
        lastDot  > lastComma -> cleaned.replace(",", "")
        else                 -> cleaned.replace(",", ".")
    }
    return normalized.toDoubleOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Convert stored "yyyy-MM-dd" string to millis for the picker
    val initialMillis = remember(value) {
        try {
            java.time.LocalDate.parse(value, fmt)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline
        )
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showDialog) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(date.format(fmt))
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: options.firstOrNull()?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, fontSize = 13.sp) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
