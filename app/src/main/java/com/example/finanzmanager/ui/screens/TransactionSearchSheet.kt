package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.Transaction
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.components.TransactionItem
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.components.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchSheet(
    state: UiState,
    onEditTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery      by remember { mutableStateOf("") }
    var selectedType     by remember { mutableStateOf("all") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAccount  by remember { mutableStateOf("") }

    val results = remember(state.transactions, searchQuery, selectedType, selectedCategory, selectedAccount) {
        state.transactions.filter { tx ->
            val matchesText = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery)
            val matchesType = selectedType == "all" || tx.type == selectedType
            val matchesCat  = selectedCategory.isEmpty() || tx.categoryId == selectedCategory
            val matchesAcc  = selectedAccount.isEmpty() || tx.accountId == selectedAccount
            matchesText && matchesType && matchesCat && matchesAcc
        }.sortedByDescending { it.date }
    }

    AnimatedWindow(onDismiss = onDismiss) { dismiss ->
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 20.dp, top = 4.dp, bottom = 4.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = dismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                    Text(
                        "Buchungen suchen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (results.isNotEmpty()) {
                    Text(
                        "${results.size} Treffer",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Beschreibung oder Betrag…", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Löschen")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(12.dp))

            // Type filter
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all"      to "Alle",
                    "expense"  to "Ausgaben",
                    "income"   to "Einnahmen",
                    "transfer" to "Überweisungen"
                ).forEach { (key, label) ->
                    val isSelected = selectedType == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Category filter
            if (state.categories.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory.isEmpty(),
                        onClick = { selectedCategory = "" },
                        label = { Text("Alle Kategorien", fontSize = 11.sp) }
                    )
                    state.categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (isSelected) "" else cat.id },
                            label = { Text(cat.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(parseHexColor(cat.color))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = parseHexColor(cat.color).copy(alpha = 0.2f),
                                selectedLabelColor = parseHexColor(cat.color)
                            )
                        )
                    }
                }
            }

            // Account filter
            if (state.accounts.size > 1) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAccount.isEmpty(),
                        onClick = { selectedAccount = "" },
                        label = { Text("Alle Konten", fontSize = 11.sp) }
                    )
                    state.accounts.forEach { acc ->
                        val isSelected = selectedAccount == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = if (isSelected) "" else acc.id },
                            label = { Text(acc.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Results
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isBlank() && selectedType == "all" && selectedCategory.isEmpty() && selectedAccount.isEmpty())
                                "Suche eingeben oder Filter setzen"
                            else "Keine Buchungen gefunden",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Summary row
                val totalAmount = results.sumOf { if (it.type == "expense") -it.amount else it.amount }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Summe",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(totalAmount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAmount >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { tx ->
                        val cat = state.categories.find { it.id == tx.categoryId }
                        TransactionItem(
                            tx = tx,
                            category = cat,
                            onClick = {
                                onEditTransaction(tx)
                                dismiss()
                            }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
    }
}
