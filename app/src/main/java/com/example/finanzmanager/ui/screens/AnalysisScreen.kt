package com.example.finanzmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.StandingOrder
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.theme.AccentGreen
import com.example.finanzmanager.ui.theme.AccentRed

@Composable
fun AnalysisScreen(
    state: UiState,
    vm: FinanzViewModel,
    onAddOrder: () -> Unit,
    onEditOrder: (StandingOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val compData = vm.comparisonData()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Planung & Analyse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Zeitvergleich ──────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CompareArrows, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "ZEITVERGLEICH",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp, letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.compDate1,
                            onValueChange = { vm.setCompDate1(it) },
                            singleLine = true,
                            label = { Text("Von", fontSize = 10.sp) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold, fontSize = 12.sp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowForward, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        OutlinedTextField(
                            value = state.compDate2,
                            onValueChange = { vm.setCompDate2(it) },
                            singleLine = true,
                            label = { Text("Bis", fontSize = 10.sp) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold, fontSize = 12.sp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ComparisonCard(
                        label        = "Gesamtvermögen",
                        from         = compData.res1.total,
                        to           = compData.res2.total,
                        diff         = compData.diffTotal,
                        percent      = compData.percentTotal,
                        accentColor  = MaterialTheme.colorScheme.onSurface,
                        accentBorder = false
                    )

                    ComparisonCard(
                        label        = "Liquide Mittel",
                        from         = compData.res1.liquid,
                        to           = compData.res2.liquid,
                        diff         = compData.diffLiquid,
                        percent      = compData.percentLiquid,
                        accentColor  = MaterialTheme.colorScheme.primary,
                        accentBorder = true
                    )
                }
            }
        }

        // ── Daueraufträge ──────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DAUERAUFTRÄGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp, letterSpacing = 1.5.sp
                )
                FilledTonalIconButton(
                    onClick = onAddOrder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Neu", modifier = Modifier.size(16.dp))
                }
            }
        }

        if (state.standingOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keine Daueraufträge",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(state.standingOrders) { order ->
                StandingOrderItem(order = order, onClick = { onEditOrder(order) })
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ComparisonCard(
    label: String,
    from: Double,
    to: Double,
    diff: Double,
    percent: Double,
    accentColor: Color,
    accentBorder: Boolean
) {
    val diffColor = if (diff >= 0) AccentGreen else AccentRed
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (accentBorder) Modifier.border(1.dp, accentColor.copy(alpha = 0.35f), shape)
                else Modifier
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label.uppercase(),
                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = if (accentBorder) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Vorher", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text(formatCurrency(from), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Aktuell", fontSize = 8.sp, color = if (accentBorder) accentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text(
                        formatCurrency(to),
                        fontWeight = FontWeight.Black, fontSize = 18.sp,
                        color = if (accentBorder) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Differenz", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = diffColor)
                Text(
                    "${if (diff > 0) "+" else ""}${formatCurrency(diff)} (${String.format(java.util.Locale.US, "%.1f", percent)}%)",
                    fontSize = 10.sp, fontWeight = FontWeight.Black, color = diffColor
                )
            }
        }
    }
}

@Composable
fun StandingOrderItem(order: StandingOrder, onClick: () -> Unit) {
    val intervalLabel = when (order.interval) {
        "weekly" -> "Wöchentlich"
        "yearly" -> "Jährlich"
        else     -> "Monatlich"
    }
    val amountColor = if (order.type == "income") AccentGreen else AccentRed

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(amountColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Repeat, contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    order.description.ifBlank { "Dauerauftrag" },
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$intervalLabel · ab ${order.nextRun}",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                formatCurrency(order.amount),
                fontWeight = FontWeight.Black,
                color = amountColor,
                fontSize = 15.sp
            )
        }
    }
}
