package com.example.finanzmanager.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
import com.example.finanzmanager.ocr.ScannedReceipt
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.today
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.theme.AccentGreen
import com.example.finanzmanager.ui.theme.AccentRed
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    initialTx: Transaction?,
    isStandingOrder: Boolean = false,
    initialOrder: StandingOrder? = null,
    scanned: ScannedReceipt? = null,
    onScanReceipt: (() -> Unit)? = null,
    state: UiState,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var isOrder      by remember { mutableStateOf(isStandingOrder || initialOrder != null) }
    var txType       by remember { mutableStateOf(initialTx?.type ?: initialOrder?.type ?: "expense") }
    var amount       by remember { mutableStateOf((initialTx?.amount ?: initialOrder?.amount ?: scanned?.amount ?: 0.0).let { if (it == 0.0) "" else it.toString() }) }
    var description  by remember { mutableStateOf(initialTx?.description ?: initialOrder?.description ?: scanned?.merchant ?: "") }
    var categoryId   by remember { mutableStateOf(initialTx?.categoryId ?: initialOrder?.categoryId ?: state.categories.firstOrNull()?.id ?: "") }
    var accountId    by remember { mutableStateOf(initialTx?.accountId ?: initialOrder?.accountId ?: state.accounts.find { it.name == "Girokonto" }?.id ?: state.accounts.firstOrNull()?.id ?: "") }
    var toAccountId  by remember { mutableStateOf(initialTx?.toAccountId ?: initialOrder?.toAccountId ?: "") }
    var isSplit      by remember { mutableStateOf(initialTx?.isSplit ?: initialOrder?.isSplit ?: false) }
    var isSettlement by remember { mutableStateOf(initialTx?.isSettlement ?: false) }
    var splitMode    by remember { mutableStateOf(initialTx?.splitMode ?: initialOrder?.splitMode ?: "half") }
    var date         by remember { mutableStateOf(initialTx?.date ?: scanned?.date ?: today()) }
    var interval     by remember { mutableStateOf(initialOrder?.interval ?: "monthly") }
    var nextRun      by remember { mutableStateOf(initialOrder?.nextRun ?: today()) }

    val typeColor = when (txType) {
        "income"   -> Color(0xFF059669)
        "transfer" -> Color(0xFF1D4ED8)
        else       -> Color(0xFFDC2626)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()   // prevents keyboard from pushing content off-screen
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isOrder) "Dauerauftrag" else "Buchung",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        if (initialTx != null || initialOrder != null) "Bearbeiten" else "Neu anlegen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (initialTx != null || initialOrder != null) {
                    IconButton(onClick = {
                        initialTx?.id?.let { vm.deleteTransaction(it) }
                        initialOrder?.id?.let { vm.deleteStandingOrder(it) }
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Einmalig / Dauerauftrag ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(false to "Einmalig", true to "Dauerauftrag").forEach { (isOrderVal, label) ->
                    val bg by animateColorAsState(
                        if (isOrder == isOrderVal) MaterialTheme.colorScheme.primary else Color.Transparent,
                        tween(200), label = "orderBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { isOrder = isOrderVal }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isOrder == isOrderVal) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Typ: Ausgabe / Einnahme / Transfer ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(
                    "expense"  to "Ausgabe",
                    "income"   to "Einnahme",
                    "transfer" to "Transfer"
                ).forEach { (type, label) ->
                    val color = when (type) {
                        "income"   -> Color(0xFF059669)
                        "transfer" -> Color(0xFF1D4ED8)
                        else       -> Color(0xFFDC2626)
                    }
                    val bg by animateColorAsState(
                        if (txType == type) color else Color.Transparent,
                        tween(200), label = "typeBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { txType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (txType == type) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Beleg scannen (nur bei neuer Einmal-Buchung)
            if (onScanReceipt != null && initialTx == null && !isOrder) {
                OutlinedButton(
                    onClick = onScanReceipt,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Beleg scannen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Hinweis, dass Werte aus einem Beleg übernommen wurden
            if (scanned != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0EA5E9).copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null,
                        tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Aus Beleg übernommen – bitte prüfen und ggf. korrigieren.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Betrag ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(typeColor.copy(alpha = 0.07f))
                    .border(1.5.dp, typeColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = typeColor
                    ),
                    placeholder = {
                        Text(
                            "0,00 €",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = typeColor.copy(alpha = 0.3f)
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
            val parsedAmt = amount.parseLocalDouble()
            if (amount.isNotBlank()) {
                Text(
                    text = if (parsedAmt != null) "✓ ${formatCurrency(parsedAmt)}"
                           else "⚠ Ungültiger Betrag – Komma oder Punkt verwenden",
                    fontSize = 11.sp,
                    color = if (parsedAmt != null) AccentGreen else AccentRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // ── Beschreibung ─────────────────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // ── Konto & Kategorie ─────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownSelector(
                    label = "Konto",
                    options = state.accounts.map { it.id to it.name },
                    selected = accountId,
                    onSelect = { accountId = it },
                    modifier = Modifier.weight(1f)
                )
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

            // ── Datum / Intervall ─────────────────────────────────────────
            if (isOrder) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownSelector(
                        label = "Intervall",
                        options = listOf(
                            "weekly"  to "Wöchentlich",
                            "monthly" to "Monatlich",
                            "yearly"  to "Jährlich"
                        ),
                        selected = interval,
                        onSelect = { interval = it },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerButton(
                        label = "Start",
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

            // ── Split-Topf ────────────────────────────────────────────────
            if (txType != "transfer" && !isOrder) {
                SwitchRow(
                    icon = Icons.Default.CallSplit,
                    title = "Split-Topf",
                    checked = isSplit,
                    activeColor = Color(0xFF7C3AED),
                    onToggle = { isSplit = it }
                )
                if (isSplit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("half" to "50% Split", "partner" to "100% Partner").forEach { (mode, label) ->
                            val selected = splitMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Color(0xFF5B21B6) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { splitMode = mode }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }
            }

            // ── Partnerausgleich ──────────────────────────────────────────
            if (txType == "expense" && !isOrder) {
                SwitchRow(
                    icon = Icons.Default.Handshake,
                    title = "Partnerausgleich",
                    subtitle = "Hebt Split-Topf Reservierung auf",
                    checked = isSettlement,
                    activeColor = Color(0xFF3B82F6),
                    onToggle = { isSettlement = it }
                )
            }

            // ── Speichern ─────────────────────────────────────────────────
            Button(
                onClick = {
                    val amt = amount.parseLocalDouble() ?: return@Button
                    if (isOrder) {
                        vm.saveStandingOrder(
                            id = initialOrder?.id, type = txType, amount = amt,
                            description = description, categoryId = categoryId,
                            accountId = accountId, toAccountId = toAccountId.ifBlank { null },
                            isSplit = isSplit, splitMode = splitMode,
                            interval = interval, nextRun = nextRun
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = typeColor)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Speichern", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Compact toggle row used for Split-Topf and Partnerausgleich */
@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    activeColor: Color,
    onToggle: (Boolean) -> Unit
) {
    val bg by animateColorAsState(
        if (checked) activeColor.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "switchBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon, contentDescription = null,
                tint = if (checked) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = activeColor)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun String.parseLocalDouble(): Double? {
    val cleaned = this.trim().replace(" ", "").replace(" ", "")
    val lastDot   = cleaned.lastIndexOf('.')
    val lastComma = cleaned.lastIndexOf(',')
    val normalized = when {
        lastComma > lastDot  -> cleaned.replace(".", "").replace(",", ".")
        lastDot  > lastComma -> cleaned.replace(",", "")
        else                 -> cleaned.replace(",", ".")
    }
    return normalized.toDoubleOrNull()
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        val parts = isoDate.split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } catch (e: Exception) { isoDate }
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

    val initialMillis = remember(value) {
        try {
            java.time.LocalDate.parse(value, fmt)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 10.sp)
            Text(
                formatDisplayDate(value),   // shows "14.06.2026" instead of "2026-06-14"
                fontSize = 13.sp,
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
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateSelected(picked.format(fmt))
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = pickerState) }
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
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}
