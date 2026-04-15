package com.example.finanzmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzmanager.domain.*
import com.example.finanzmanager.ui.FilterType
import com.example.finanzmanager.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import android.graphics.Color as AndroidColor

fun formatCurrency(value: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    return fmt.format(value)
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun accountIcon(iconName: String): ImageVector = when (iconName) {
    "bank" -> Icons.Default.AccountBalance
    "wallet" -> Icons.Default.AccountBalanceWallet
    "stock" -> Icons.Default.TrendingUp
    "card" -> Icons.Default.CreditCard
    "savings" -> Icons.Default.Savings
    else -> Icons.Default.AccountBalance
}

@Composable
fun FilterBar(
    selected: FilterType,
    onSelect: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterType.entries.forEach { type ->
            val label = when (type) {
                FilterType.TODAY -> "Heute"
                FilterType.WEEK -> "Woche"
                FilterType.MONTH -> "Monat"
                FilterType.CUSTOM -> "Indiv."
            }
            val isSelected = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInvestment = account.category == "Investments" || account.category == "Vorsorge"
    val gradientColors = if (isInvestment) {
        listOf(Color(0xFF1E3A5F), Color(0xFF1D4ED8))
    } else {
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInvestment) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: icon + name — takes all remaining space, clips if too long
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isInvestment) Color.White.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accountIcon(account.icon),
                            contentDescription = null,
                            tint = if (isInvestment) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isInvestment) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = account.category,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isInvestment) Color.White.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Right: balance — never wraps
                Text(
                    text = formatCurrency(account.balance),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    color = if (isInvestment) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    tx: Transaction,
    category: Category?,
    isSplitOverview: Boolean = false,
    onClick: () -> Unit
) {
    val isExpense = tx.type == "expense" || tx.type == "transfer"
    val displayAmount = if (isSplitOverview) {
        if (tx.splitMode == "partner") tx.amount else tx.amount / 2
    } else tx.amount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(category?.let { parseHexColor(it.color) } ?: Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tx.type) {
                    "transfer" -> Icons.Default.SwapHoriz
                    "income" -> Icons.Default.ArrowUpward
                    else -> Icons.Default.ArrowDownward
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { category?.name ?: tx.type },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = formatDateDisplay(tx.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isExpense) "-" else "+"}${formatCurrency(displayAmount)}",
                fontWeight = FontWeight.Bold,
                color = if (isExpense) AccentRed else AccentGreen,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!isSplitOverview && tx.isSplit) {
                Text(
                    text = if (tx.splitMode == "partner") "100%" else "50%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (tx.isSettlement) {
                Text(
                    text = "Ausgleich",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF3B82F6)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
}

fun formatDateDisplay(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } catch (e: Exception) { dateStr }
}
