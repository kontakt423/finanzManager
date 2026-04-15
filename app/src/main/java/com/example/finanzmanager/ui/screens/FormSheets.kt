package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.Account
import com.example.finanzmanager.domain.Category
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.components.InvestmentChart
import com.example.finanzmanager.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    account: Account?,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balance by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var category by remember { mutableStateOf(account?.category ?: "Liquide Mittel") }
    var type by remember { mutableStateOf(account?.type ?: "cash") }
    var icon by remember { mutableStateOf(account?.icon ?: "bank") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Konto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (account != null) {
                    IconButton(onClick = { vm.deleteAccount(account.id); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Saldo (z.B. 1500,00 oder 1500.00)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            // Show parsed value so user knows input was recognised
            val parsedBal = balance.parseLocalDouble()
            if (balance.isNotBlank()) {
                Text(
                    text = if (parsedBal != null)
                        "✓ ${com.example.finanzmanager.ui.components.formatCurrency(parsedBal)}"
                    else
                        "⚠ Komma oder Punkt als Dezimalzeichen verwenden",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (parsedBal != null)
                        androidx.compose.ui.graphics.Color(0xFF10B981)
                    else
                        androidx.compose.ui.graphics.Color(0xFFEF4444)
                )
            }

            ExposedDropdownSelector(
                label = "Kategorie",
                options = listOf(
                    "Liquide Mittel" to "Liquide Mittel",
                    "Investments" to "Investments",
                    "Vorsorge" to "Vorsorge",
                    "Sonstiges" to "Sonstiges"
                ),
                selected = category,
                onSelect = { category = it }
            )

            ExposedDropdownSelector(
                label = "Icon",
                options = listOf("bank" to "Bank", "wallet" to "Wallet", "stock" to "Investments", "card" to "Karte", "savings" to "Sparen"),
                selected = icon,
                onSelect = { icon = it }
            )

            Button(
                onClick = {
                    vm.saveAccount(
                        id = account?.id, name = name,
                        balance = balance.parseLocalDouble() ?: 0.0,
                        category = category, type = if (category == "Investments" || category == "Vorsorge") "investment" else "cash",
                        icon = icon
                    )
                    onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailSheet(
    account: Account,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var newBalance by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Investment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(account.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(formatCurrency(account.balance), style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                InvestmentChart(history = account.history, modifier = Modifier.padding(8.dp))
            }

            Text("Aktuellen Marktwert eingeben", fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = newBalance,
                onValueChange = { newBalance = it },
                placeholder = { Text("0,00 oder 0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            val parsedInv = newBalance.parseLocalDouble()
            if (newBalance.isNotBlank()) {
                Text(
                    text = if (parsedInv != null)
                        "✓ ${com.example.finanzmanager.ui.components.formatCurrency(parsedInv)}"
                    else
                        "⚠ Komma oder Punkt als Dezimalzeichen verwenden",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (parsedInv != null)
                        androidx.compose.ui.graphics.Color(0xFF10B981)
                    else
                        androidx.compose.ui.graphics.Color(0xFFEF4444)
                )
            }

            Button(
                onClick = {
                    newBalance.parseLocalDouble()?.let { vm.updateInvestment(account, it) }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Saldo aktualisieren", fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    category: Category?,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    val defaultColors = listOf("#ef4444","#f59e0b","#10b981","#3b82f6","#6366f1","#a855f7","#ec4899","#64748b")
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(category?.color ?: defaultColors[0]) }
    var type by remember { mutableStateOf(category?.type ?: "expense") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kategorie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                if (category != null) {
                    IconButton(onClick = { vm.deleteCategory(category.id); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true
            )

            ExposedDropdownSelector(
                label = "Typ",
                options = listOf("expense" to "Ausgabe", "income" to "Einnahme"),
                selected = type, onSelect = { type = it }
            )

            Text("Farbe", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                defaultColors.forEach { c ->
                    Surface(
                        modifier = Modifier.size(36.dp).clickable { color = c },
                        shape = RoundedCornerShape(50),
                        color = try { Color(android.graphics.Color.parseColor(c)) } catch (e: Exception) { Color.Gray },
                        tonalElevation = if (color == c) 8.dp else 0.dp,
                        shadowElevation = if (color == c) 6.dp else 0.dp
                    ) {}
                }
            }

            OutlinedTextField(
                value = color, onValueChange = { color = it },
                label = { Text("Hex-Farbe") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), singleLine = true,
                placeholder = { Text("#3b82f6") }
            )

            Button(
                onClick = { vm.saveCategory(category?.id, name, color, type); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
