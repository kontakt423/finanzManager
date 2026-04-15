package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.finanzmanager.domain.*
import com.example.finanzmanager.ui.*
import com.example.finanzmanager.ui.components.*
import com.example.finanzmanager.ui.screens.DatePickerButton
import com.example.finanzmanager.ui.FilterType
import com.example.finanzmanager.ui.theme.*

@Composable
fun DashboardScreen(
    state: UiState,
    vm: FinanzViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onEditAccount: (Account) -> Unit,
    onInvestmentDetail: (Account) -> Unit,
    onSplitPotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTxs = vm.filteredTransactions()
    val analysis = vm.analysis()
    val totals = vm.totals()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "FinanzManager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Übersicht",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                FilledIconButton(
                    onClick = onAddTransaction,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                }
            }
        }

        // Total Balance Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "GESAMTVERMÖGEN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                formatCurrency(totals.total),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroStat("LIQUIDE", formatCurrency(totals.liquid), Color.White)
                        HeroStat("EINNAHMEN", "+${formatCurrency(analysis.income)}", Color(0xFF86EFAC))
                        HeroStat("AUSGABEN", "-${formatCurrency(analysis.expenses)}", Color(0xFFFCA5A5))
                    }
                }
            }
        }

        // Filter Bar
        item {
            FilterBar(
                selected = state.filterType,
                onSelect = { vm.setFilterType(it) }
            )
        }

        // Custom date range pickers — only shown when "Indiv." is selected
        if (state.filterType == FilterType.CUSTOM) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePickerButton(
                        label = "Von",
                        value = state.customFrom,
                        onDateSelected = { vm.setCustomRange(it, state.customTo) },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    DatePickerButton(
                        label = "Bis",
                        value = state.customTo,
                        onDateSelected = { vm.setCustomRange(state.customFrom, it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // VAT / Net Revenue (visible when Rgnr transactions exist)
        if (analysis.netRevenue > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Netto-Umsatz", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("nach 19% MwSt.", color = Color.White.copy(0.6f), fontSize = 10.sp)
                            }
                        }
                        Text(formatCurrency(analysis.netRevenue), fontWeight = FontWeight.Black, color = AccentGreen, fontSize = 16.sp)
                    }
                }
            }
        }

        // Accounts
        item {
            SectionHeader("Konten", onAdd = onAddTransaction)
        }

        val cashAccounts = state.accounts.filter { it.category != "Investments" && it.category != "Vorsorge" }
        val investAccounts = state.accounts.filter { it.category == "Investments" || it.category == "Vorsorge" }

        items(cashAccounts) { acc ->
            AccountCard(account = acc, onClick = { onEditAccount(acc) })
        }

        if (investAccounts.isNotEmpty()) {
            item {
                Text(
                    "INVESTMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            items(investAccounts) { acc ->
                AccountCard(account = acc, onClick = { onInvestmentDetail(acc) })
            }
        }

        // Split Pot - always show when enabled (even with 0 values)
        if (state.splitPotEnabled) {
            item {
                SplitPotCard(
                    splitExpenses = analysis.totalSplitExpenses,
                    splitIncome = analysis.totalSplitIncome,
                    settlements = analysis.totalSettlements,
                    startDate = state.splitPotStartDate,
                    onClick = onSplitPotClick
                )
            }
        }

        // Recent Transactions
        item {
            SectionHeader("Letzte Buchungen", onAdd = null)
        }

        if (filteredTxs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Keine Buchungen", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        } else {
            items(filteredTxs.take(15)) { tx ->
                val cat = state.categories.find { it.id == tx.categoryId }
                TransactionItem(tx = tx, category = cat, onClick = { onEditTransaction(tx) })
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun HeroStat(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
    }
}

@Composable
fun SectionHeader(title: String, onAdd: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp
        )
        if (onAdd != null) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).clickable(onClick = onAdd)
            )
        }
    }
}

@Composable
fun SplitPotCard(splitExpenses: Double, splitIncome: Double, settlements: Double = 0.0, startDate: String = "", onClick: () -> Unit = {}) {
    val net = splitExpenses - splitIncome
    val netAfterSettlements = (net - settlements).coerceAtLeast(0.0)
    val netPositive = net >= 0
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5B21B6))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SPLIT-TOPF", fontWeight = FontWeight.Black,
                        color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                // Net balance badge
                val badgeColor = if (netPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                val badgeText = "${if (netPositive) "+" else ""}${formatCurrency(net)}"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
            }
            if (startDate.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Gilt ab $startDate",
                    fontSize = 9.sp, color = Color.White.copy(0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(Modifier.height(14.dp))
            // Two columns
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Guthaben", color = Color.White.copy(0.6f), fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("(Partner schuldet dir)", color = Color.White.copy(0.5f), fontSize = 8.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formatCurrency(splitExpenses), color = Color(0xFF86EFAC),
                        fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Abzug", color = Color.White.copy(0.6f), fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("(du schuldest Partner)", color = Color.White.copy(0.5f), fontSize = 8.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formatCurrency(splitIncome), color = Color(0xFFF9A8D4),
                        fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
            // Settlements paid row (only shown when > 0)
            if (settlements > 0.0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Handshake,
                            contentDescription = null,
                            tint = Color.White.copy(0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Bereits ausgeglichen",
                            color = Color.White.copy(0.6f),
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        "-${formatCurrency(settlements)}",
                        color = Color.White.copy(0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (netAfterSettlements == 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "✓ Split-Topf vollständig ausgeglichen",
                        color = Color(0xFF86EFAC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (splitExpenses == 0.0 && splitIncome == 0.0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Noch keine Split-Buchungen vorhanden. " +
                    "Beim Buchen den Schalter 'Split-Topf' aktivieren.",
                    color = Color.White.copy(0.5f), fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
