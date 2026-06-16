package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.SavingsGoal
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.components.parseHexColor
import java.time.Instant
import java.time.ZoneId

private val GOAL_COLORS = listOf(
    "#3b82f6", "#10b981", "#f59e0b", "#ef4444",
    "#8b5cf6", "#ec4899", "#06b6d4", "#64748b"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalSheet(
    goal: SavingsGoal?,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var name         by remember { mutableStateOf(goal?.name ?: "") }
    var targetAmount by remember { mutableStateOf(goal?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var savedAmount  by remember { mutableStateOf(goal?.savedAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var color        by remember { mutableStateOf(goal?.color ?: GOAL_COLORS[0]) }
    var hasDeadline  by remember { mutableStateOf(goal?.deadline?.isNotEmpty() == true) }
    var deadline     by remember { mutableStateOf(goal?.deadline ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDelete     by remember { mutableStateOf(false) }

    val isEdit = goal != null
    val target = targetAmount.toDoubleOrNull() ?: 0.0
    val saved  = savedAmount.toDoubleOrNull() ?: 0.0
    val progress = if (target > 0) (saved / target).toFloat().coerceIn(0f, 1f) else 0f

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (deadline.isNotEmpty()) {
                try {
                    java.time.LocalDate.parse(deadline).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                } catch (e: Exception) { System.currentTimeMillis() }
            } else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        deadline = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC"))
                            .toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Ziel löschen?") },
            text = { Text("\"${goal?.name}\" wird unwiderruflich gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    goal?.let { vm.deleteSavingsGoal(it.id) }
                    onDismiss()
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Abbrechen") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEdit) "Ziel bearbeiten" else "Neues Sparziel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isEdit) {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Live progress preview (only when editing or when values exist)
            if (target > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(parseHexColor(color).copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (name.isNotBlank()) name else "Ziel",
                            fontWeight = FontWeight.Bold,
                            color = parseHexColor(color),
                            fontSize = 14.sp
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            fontWeight = FontWeight.Black,
                            color = parseHexColor(color),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = parseHexColor(color),
                        trackColor = parseHexColor(color).copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(formatCurrency(saved), fontSize = 11.sp, color = parseHexColor(color).copy(0.8f))
                        Text(formatCurrency(target), fontSize = 11.sp, color = parseHexColor(color).copy(0.5f))
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Zielname") },
                placeholder = { Text("z.B. Urlaub, Neues Fahrrad…") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Amounts row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.replace(",", ".") },
                    label = { Text("Zielbetrag (€)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = savedAmount,
                    onValueChange = { savedAmount = it.replace(",", ".") },
                    label = { Text("Gespart (€)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Deadline toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zieldatum festlegen", fontSize = 14.sp)
                Switch(
                    checked = hasDeadline,
                    onCheckedChange = {
                        hasDeadline = it
                        if (!it) deadline = ""
                    }
                )
            }
            if (hasDeadline) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (deadline.isNotEmpty()) {
                                    val p = deadline.split("-")
                                    "${p[2]}.${p[1]}.${p[0]}"
                                } else "Datum wählen",
                                fontSize = 14.sp,
                                color = if (deadline.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Color picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Farbe", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GOAL_COLORS.forEach { hex ->
                        val isSelected = color == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = {
                    val t = targetAmount.toDoubleOrNull() ?: 0.0
                    val s = savedAmount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && t > 0) {
                        vm.saveSavingsGoal(
                            id = goal?.id, name = name.trim(),
                            targetAmount = t, savedAmount = s,
                            deadline = if (hasDeadline) deadline else "",
                            color = color
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(color))
            ) {
                Text(
                    if (isEdit) "Speichern" else "Ziel erstellen",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
