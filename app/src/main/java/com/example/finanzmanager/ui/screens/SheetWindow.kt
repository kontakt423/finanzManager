package com.example.finanzmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Vollbild-Fenster mit Ein-/Ausblend-Animation. Öffnet sich mit fadeIn und
 * schließt mit fadeOut; [onDismiss] wird erst nach der Ausblend-Animation
 * aufgerufen. Das `content` erhält einen `dismiss`-Callback, den seine
 * Buttons (Schließen/Speichern) statt onDismiss aufrufen sollten, damit die
 * Animation läuft. Systemzurück löst ebenfalls das Ausblenden aus.
 */
@Composable
fun AnimatedWindow(
    onDismiss: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val dismiss: () -> Unit = { visibleState.targetState = false }

    // Erst als "gezeigt" markieren, wenn die Einblend-Animation durch ist …
    var hasBeenShown by remember { mutableStateOf(false) }
    LaunchedEffect(visibleState.currentState) {
        if (visibleState.currentState) hasBeenShown = true
    }
    // … und erst danach nach der Ausblend-Animation tatsächlich schließen.
    LaunchedEffect(visibleState.isIdle, hasBeenShown) {
        if (hasBeenShown && visibleState.isIdle && !visibleState.currentState) onDismiss()
    }

    BackHandler(enabled = visibleState.targetState) { dismiss() }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        content(dismiss)
    }
}

/**
 * Standard-Gerüst für ein Vollbild-Formular-Fenster: fixierte Kopfzeile
 * (Schließen-X, Titel, optional Löschen) und scrollbarer Inhaltsbereich.
 * Die Tastatur schiebt nur den Inhalt (imePadding), das Fenster bleibt oben.
 */
@Composable
fun FullScreenSheetScaffold(
    title: String,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = titleColor
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}
