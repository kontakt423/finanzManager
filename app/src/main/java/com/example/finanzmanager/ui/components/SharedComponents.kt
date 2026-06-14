package com.example.finanzmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    "bank"    -> Icons.Default.AccountBalance
    "wallet"  -> Icons.Default.AccountBalanceWallet
    "stock"   -> Icons.Default.TrendingUp
    "card"    -> Icons.Default.CreditCard
    "savings" -> Icons.Default.Savings
    else      -> Icons.Default.AccountBalance
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
                FilterType.TODAY  -> "Heute"
                FilterType.WEEK   -> "Woche"
                FilterType.MONTH  -> "Monat"
                FilterType.CUSTOM -> "Indiv."
            }
            val isSelected = selected == type
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(200),
                label = "filterBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "filterText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onSelect(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
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
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInvestment) 6.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isInvestment) Color.White.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accountIcon(account.icon),
                            contentDescription = null,
                            tint = if (isInvestment) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
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
                            color = if (isInvestment) Color.White.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = formatCurrency(account.balance),
                    fontSize = 14.sp,
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

    val catColor = category?.let { parseHexColor(it.color) } ?: MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(catColor.copy(alpha = 0.15f))
                .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tx.type) {
                    "transfer" -> Icons.Default.SwapHoriz
                    "income"   -> Icons.Default.ArrowUpward
                    else       -> Icons.Default.ArrowDownward
                },
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { category?.name ?: tx.type },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDateDisplay(tx.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isExpense) "−" else "+"}${formatCurrency(displayAmount)}",
                fontWeight = FontWeight.Bold,
                color = if (isExpense) AccentRed else AccentGreen,
                fontSize = 13.sp
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
                    color = PrimaryBlue
                )
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        thickness = 0.5.dp
    )
}

fun formatDateDisplay(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } catch (e: Exception) { dateStr }
}
