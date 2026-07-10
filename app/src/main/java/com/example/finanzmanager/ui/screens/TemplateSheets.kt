package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.Template
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.components.formatCurrency

/** Liste aller Schnellbuchungs-Vorlagen mit Bearbeiten/Löschen und Anlegen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagerSheet(
    state: UiState,
    vm: FinanzViewModel,
    onAdd: () -> Unit,
    onEdit: (Template) -> Unit,
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Schnellbuchungs-Vorlagen", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black)
            }

            if (state.templates.isEmpty()) {
                Text(
                    "Noch keine Vorlagen. Lege häufige Buchungen als Vorlage an, um sie mit einem Tipp zu buchen.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.templates.forEach { tpl ->
                    val cat = state.categories.find { it.id == tpl.categoryId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onEdit(tpl) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${formatCurrency(tpl.amount)} · ${cat?.name ?: tpl.type}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.deleteTemplate(tpl.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen",
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Neue Vorlage", fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Formular zum Anlegen/Bearbeiten einer Vorlage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormSheet(
    template: Template?,
    state: UiState,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var name        by remember { mutableStateOf(template?.name ?: "") }
    var txType      by remember { mutableStateOf(template?.type ?: "expense") }
    var amount      by remember { mutableStateOf((template?.amount ?: 0.0).let { if (it == 0.0) "" else it.toString() }) }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var categoryId  by remember { mutableStateOf(template?.categoryId ?: state.categories.firstOrNull()?.id ?: "") }
    var accountId   by remember { mutableStateOf(template?.accountId ?: state.accounts.find { it.name == "Girokonto" }?.id ?: state.accounts.firstOrNull()?.id ?: "") }
    var toAccountId by remember { mutableStateOf(template?.toAccountId ?: "") }
    var isSplit     by remember { mutableStateOf(template?.isSplit ?: false) }
    var splitMode   by remember { mutableStateOf(template?.splitMode ?: "half") }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (template == null) "Neue Vorlage" else "Vorlage bearbeiten",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (template != null) {
                    IconButton(onClick = { vm.deleteTemplate(template.id); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Vorlagenname (z. B. Tanken)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )

            // Typ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("expense" to "Ausgabe", "income" to "Einnahme", "transfer" to "Transfer")
                    .forEach { (type, label) ->
                        val bg = when {
                            txType == type && type == "income"   -> Color(0xFF059669)
                            txType == type && type == "transfer" -> Color(0xFF1D4ED8)
                            txType == type                       -> Color(0xFFDC2626)
                            else                                 -> Color.Transparent
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

            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Betrag") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Beschreibung") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )

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

            if (txType != "transfer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { isSplit = !isSplit }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Split-Topf", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Switch(checked = isSplit, onCheckedChange = { isSplit = it })
                }
                if (isSplit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("half" to "50% Split", "partner" to "100% Partner").forEach { (mode, label) ->
                            val selected = splitMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color(0xFF5B21B6) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { splitMode = mode }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0xFF7C3AED))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val amt = amount.parseLocalDouble() ?: 0.0
                    if (name.isNotBlank()) {
                        vm.saveTemplate(
                            id = template?.id, name = name, type = txType, amount = amt,
                            description = description, categoryId = categoryId,
                            accountId = accountId, toAccountId = toAccountId.ifBlank { null },
                            isSplit = isSplit, splitMode = splitMode
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
