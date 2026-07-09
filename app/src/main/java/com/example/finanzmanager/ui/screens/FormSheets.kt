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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    account: Account?,
    vm: FinanzViewModel,
    onDismiss: () -> Unit
) {
    var name             by remember { mutableStateOf(account?.name ?: "") }
    var balance          by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    var category         by remember { mutableStateOf(account?.category ?: "Liquide Mittel") }
    var type             by remember { mutableStateOf(account?.type ?: "cash") }
    var icon             by remember { mutableStateOf(account?.icon ?: "bank") }
    var interestEnabled  by remember { mutableStateOf((account?.interestRate ?: 0.0) > 0.0) }
    var interestRate     by remember { mutableStateOf(account?.interestRate?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    var interestInterval by remember { mutableStateOf(account?.interestInterval ?: "monthly") }
    var nextInterestRun  by remember { mutableStateOf(account?.nextInterestRun ?: "") }

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

            HorizontalDivider()

            // Interest rate section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Zinsen aktivieren", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Automatische Zinsgutschrift", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = interestEnabled,
                    onCheckedChange = { enabled ->
                        interestEnabled = enabled
                        if (!enabled) {
                            interestRate = ""
                            nextInterestRun = ""
                        } else if (nextInterestRun.isEmpty()) {
                            nextInterestRun = nextInterestDate(interestInterval)
                        }
                    }
                )
            }

            if (interestEnabled) {
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it.replace(",", ".") },
                    label = { Text("Zinssatz % p.a.") },
                    placeholder = { Text("z.B. 2.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text("%", fontSize = 14.sp,
                        modifier = Modifier.padding(end = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Quick preview of interest amount
                val rateVal = interestRate.toDoubleOrNull()
                val balVal  = balance.parseLocalDouble()
                if (rateVal != null && rateVal > 0 && balVal != null && balVal > 0) {
                    val monthlyInterest = balVal * rateVal / 12.0 / 100.0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monatliche Gutschrift (ca.)", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(formatCurrency(monthlyInterest), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Interval selector
                Text("Buchungsintervall", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "monthly"   to "Monatlich",
                        "quarterly" to "Vierteljährlich",
                        "yearly"    to "Jährlich"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = interestInterval == key,
                            onClick = {
                                interestInterval = key
                                nextInterestRun = nextInterestDate(key)
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Next run info
                if (nextInterestRun.isNotEmpty()) {
                    val parts = nextInterestRun.split("-")
                    Text(
                        "Nächste Buchung: ${parts[2]}.${parts[1]}.${parts[0]}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    val rate = if (interestEnabled) interestRate.toDoubleOrNull() ?: 0.0 else 0.0
                    val nextRun = if (interestEnabled && rate > 0) {
                        nextInterestRun.ifEmpty { nextInterestDate(interestInterval) }
                    } else ""
                    vm.saveAccount(
                        id = account?.id, name = name,
                        balance = balance.parseLocalDouble() ?: 0.0,
                        category = category,
                        type = if (category == "Investments" || category == "Vorsorge") "investment" else "cash",
                        icon = icon,
                        interestRate = rate,
                        interestInterval = interestInterval,
                        nextInterestRun = nextRun
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

private fun nextInterestDate(interval: String): String {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = java.time.LocalDate.now()
    return when (interval) {
        "quarterly" -> {
            val m = today.monthValue
            when {
                m < 4  -> today.withMonth(4).withDayOfMonth(1)
                m < 7  -> today.withMonth(7).withDayOfMonth(1)
                m < 10 -> today.withMonth(10).withDayOfMonth(1)
                else   -> today.plusYears(1).withMonth(1).withDayOfMonth(1)
            }
        }
        "yearly" -> today.plusYears(1).withMonth(1).withDayOfMonth(1)
        else     -> today.plusMonths(1).withDayOfMonth(1)
    }.format(fmt)
}
