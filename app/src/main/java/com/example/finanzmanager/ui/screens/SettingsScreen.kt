package com.example.finanzmanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import com.example.finanzmanager.data.repository.SettingsRepository
import com.example.finanzmanager.domain.Account
import com.example.finanzmanager.domain.Category
import com.example.finanzmanager.sync.SyncServer
import com.example.finanzmanager.sync.SyncServerStatus
import com.example.finanzmanager.sync.lokaleIpAdressen
import com.example.finanzmanager.ui.FinanzViewModel
import com.example.finanzmanager.ui.UiState
import com.example.finanzmanager.ui.canUseBiometric
import com.example.finanzmanager.ui.components.formatCurrency
import com.example.finanzmanager.ui.components.parseHexColor
import com.example.finanzmanager.ui.today

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    vm: FinanzViewModel,
    onEditAccount: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onAddCategory: () -> Unit,
    onManageTemplates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showSplitStartDatePicker by remember { mutableStateOf(false) }
    var pendingJsonToSave by remember { mutableStateOf<String?>(null) }

    // ── Launcher 1: CreateDocument ──────────────────────────────────────
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { destUri: Uri? ->
        if (destUri == null) {
            pendingJsonToSave = null
            return@rememberLauncherForActivityResult
        }
        val json = pendingJsonToSave ?: return@rememberLauncherForActivityResult
        pendingJsonToSave = null
        try {
            context.contentResolver.openOutputStream(destUri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            vm.showSnackbar("Backup gespeichert ✓")
        } catch (e: Exception) {
            vm.showSnackbar("Fehler: ${e.message}")
        }
    }

    // ── Launcher 2: Import ──────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            vm.importData(context, it) { success ->
                vm.showSnackbar(
                    if (success) "Daten erfolgreich importiert ✓"
                    else "Import fehlgeschlagen"
                )
            }
        }
    }

    fun saveToFile() {
        vm.exportData(context) { uri ->
            if (uri == null) { vm.showSnackbar("Export fehlgeschlagen"); return@exportData }
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (json.isNullOrBlank()) { vm.showSnackbar("Keine Daten zum Speichern"); return@exportData }
                pendingJsonToSave = json
                createDocumentLauncher.launch("FinanzBackup_${today()}.json")
            } catch (e: Exception) {
                vm.showSnackbar("Fehler: ${e.message}")
            }
        }
    }

    fun shareBackup() {
        vm.exportData(context) { uri ->
            if (uri == null) { vm.showSnackbar("Export fehlgeschlagen"); return@exportData }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FinanzManager Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Backup teilen"))
        }
    }

    // Split-Topf DatePickerDialog
    if (showSplitStartDatePicker) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentDate = state.splitPotStartDate.takeIf { it.isNotBlank() } ?: today()
        val initialMillis = try {
            LocalDate.parse(currentDate, fmt).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: Exception) { System.currentTimeMillis() }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showSplitStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        vm.setSplitPotStartDate(picked.format(fmt))
                        vm.showSnackbar("Split-Topf gilt ab ${picked.format(fmt)}")
                    }
                    showSplitStartDatePicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showSplitStartDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = pickerState) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Einstellungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── DARSTELLUNG ─────────────────────────────────────────────────
        item { SectionLabel("DARSTELLUNG") }
        item {
            SettingsGroup {
                SettingsToggleItem(
                    icon    = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    title   = "Dark Mode",
                    checked = state.isDarkMode,
                    onToggle = { vm.toggleDarkMode() }
                )
            }
        }

        // ── SICHERHEIT ──────────────────────────────────────────────────
        item { SectionLabel("SICHERHEIT") }
        item {
            SettingsGroup {
                SettingsToggleItem(
                    icon     = Icons.Default.Fingerprint,
                    title    = "App-Sperre (Biometrie)",
                    subtitle = "Beim Öffnen per Fingerabdruck/Gesicht entsperren",
                    checked  = state.appLockEnabled,
                    onToggle = {
                        if (!state.appLockEnabled) {
                            if (canUseBiometric(context)) vm.setAppLock(true)
                            else vm.showSnackbar("Keine Biometrie oder Geräte-PIN eingerichtet")
                        } else {
                            vm.setAppLock(false)
                        }
                    }
                )
            }
        }

        // ── SCHNELLBUCHUNG ──────────────────────────────────────────────
        item { SectionLabel("SCHNELLBUCHUNG") }
        item {
            SettingsGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageTemplates)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Bolt, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Vorlagen verwalten", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("${state.templates.size} Vorlage(n)", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── SPLIT-TOPF ──────────────────────────────────────────────────
        item { SectionLabel("SPLIT-TOPF") }
        item {
            SettingsGroup {
                SettingsToggleItem(
                    icon    = Icons.Default.CallSplit,
                    title   = "Split-Topf aktiviert",
                    checked = state.splitPotEnabled,
                    onToggle = { vm.toggleSplitPot() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                SettingsToggleItem(
                    icon     = Icons.Default.People,
                    title    = "Split-Einnahmen voll zählen",
                    subtitle = "Sonst nur 50% im Gesamtsaldo",
                    checked  = state.countFullSplitIncome,
                    onToggle = { vm.toggleCountFullSplitIncome() }
                )
            }
        }

        // ── SPLIT-TOPF ZURÜCKSETZEN ──────────────────────────────────────
        item { SectionLabel("SPLIT-TOPF ZURÜCKSETZEN") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp).padding(top = 1.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Setze ein Startdatum: nur Buchungen ab diesem Tag " +
                            "fließen in den Split-Topf ein. Nützlich nach einer " +
                            "Überweisung, wenn der Topf bei Null starten soll.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    val hasStartDate = state.splitPotStartDate.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Zählt ab", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (hasStartDate) state.splitPotStartDate else "Alle Buchungen (kein Limit)",
                                fontSize = 12.sp,
                                fontWeight = if (hasStartDate) FontWeight.Black else FontWeight.Normal,
                                color = if (hasStartDate) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showSplitStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B21B6))
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Datum wählen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (hasStartDate) {
                            OutlinedButton(
                                onClick = {
                                    vm.clearSplitPotStartDate()
                                    vm.showSnackbar("Split-Topf zählt wieder alle Buchungen")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Zurücksetzen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── DATEN & BACKUP ───────────────────────────────────────────────
        item { SectionLabel("DATEN & BACKUP") }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "\"Speichern\" öffnet den Dateibrowser — du wählst selbst wo " +
                        "die Datei abgelegt wird (z.B. Dokumente, Downloads oder Google Drive).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { saveToFile() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Speichern", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { shareBackup() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Teilen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup laden", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Sync mit Taxologic ──────────────────────────────────────────────
        item { SectionLabel("SYNC MIT TAXOLOGIC") }
        item { SyncSection() }

        // ── Konten ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("KONTEN")
                IconButton(onClick = onAddAccount) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            SettingsGroup {
                state.accounts.forEachIndexed { idx, acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditAccount(acc) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            formatCurrency(acc.balance),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (idx < state.accounts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

        // ── KATEGORIEN ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("KATEGORIEN")
                IconButton(onClick = onAddCategory) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { cat ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onEditCategory(cat) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(10.dp),
                                        shape = RoundedCornerShape(50),
                                        color = parseHexColor(cat.color)
                                    ) {}
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        cat.name, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── RESET ───────────────────────────────────────────────────────
        item {
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Alle Daten löschen?") },
                    text = { Text("Diese Aktion ist unwiderruflich. Alle Konten, Transaktionen und Einstellungen werden gelöscht.") },
                    confirmButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("Abbrechen") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Löschen", color = Color(0xFFEF4444))
                        }
                    }
                )
            }
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Daten zurücksetzen",
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

/** Shared label for settings sections */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/**
 * Zeigt Adresse + Pairing-Code für den lokalen Sync-Server (siehe sync/SyncServer.kt)
 * an. Taxologic trägt beide Werte manuell (oder per QR-Code) in seinem eigenen
 * "Privat"-Modul ein und holt sich die Daten von dort aktiv ab – diese App schreibt
 * nie etwas zurück.
 */
@Composable
fun SyncSection() {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var ipAdressen by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        token = SettingsRepository(context).ensureSyncToken()
        ipAdressen = lokaleIpAdressen()
    }

    val adresse = (ipAdressen.firstOrNull() ?: "—") + ":" + SyncServer.PORT

    fun kopieren(label: String, wert: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, wert))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Info, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp).padding(top = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Taxologic kann private Buchungen von hier abrufen (nur lesend, " +
                    "nur im selben WLAN bzw. auf demselben Gerät). Adresse und Pairing-Code " +
                    "dort im Bereich \"Privat\" eintragen.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            SyncWertZeile(label = "Adresse", wert = adresse, onKopieren = { kopieren("Adresse", adresse) })
            SyncWertZeile(label = "Pairing-Code", wert = token.ifEmpty { "…" },
                onKopieren = { kopieren("Pairing-Code", token) })

            // Falls die geratene Adresse falsch ist (z.B. Gerät hat mehrere aktive
            // Netzwerk-Schnittstellen): alle gefundenen Kandidaten zum manuellen
            // Ausprobieren anzeigen.
            if (ipAdressen.size > 1) {
                Text(
                    "Andere gefundene Adressen (falls obige nicht erreichbar ist): " +
                    ipAdressen.drop(1).joinToString(", ") { "$it:${SyncServer.PORT}" },
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp
                )
            }

            val serverFehler = SyncServerStatus.fehler
            if (serverFehler != null) {
                Text("Server-Fehler beim Start: $serverFehler",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.error, lineHeight = 15.sp)
            } else if (SyncServerStatus.running) {
                Text("Server aktiv ✓", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Server startet…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SyncWertZeile(label: String, wert: String, onKopieren: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(wert, fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onKopieren) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Kopieren",
                modifier = Modifier.size(16.dp))
        }
    }
}

/** Card container that groups related settings items */
@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
