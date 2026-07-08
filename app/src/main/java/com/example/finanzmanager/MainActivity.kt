package com.example.finanzmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzmanager.data.database.AppDatabase
import com.example.finanzmanager.data.repository.FinanzRepository
import com.example.finanzmanager.data.repository.SettingsRepository
import com.example.finanzmanager.domain.Account
import com.example.finanzmanager.domain.Category
import com.example.finanzmanager.domain.StandingOrder
import com.example.finanzmanager.domain.Transaction
import com.example.finanzmanager.ocr.ReceiptScanScreen
import com.example.finanzmanager.ocr.ScannedReceipt
import com.example.finanzmanager.ui.*
import com.example.finanzmanager.ui.screens.*
import com.example.finanzmanager.ui.theme.FinanzManagerTheme

sealed class Sheet {
    object None : Sheet()
    data class AddTransaction(val tx: Transaction? = null) : Sheet()
    data class AddStandingOrder(val order: StandingOrder? = null) : Sheet()
    data class EditAccount(val account: Account?) : Sheet()
    data class InvestmentDetail(val account: Account) : Sheet()
    data class EditCategory(val category: Category?) : Sheet()
    object SplitDetail : Sheet()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = FinanzRepository(db)
            val settings = SettingsRepository(applicationContext)
            val vm: FinanzViewModel = viewModel(factory = FinanzViewModelFactory(repo, settings))
            val state by vm.state.collectAsStateWithLifecycle()

            FinanzManagerTheme(darkTheme = state.isDarkMode) {
                FinanzManagerApp(vm = vm, state = state)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanzManagerApp(vm: FinanzViewModel, state: UiState) {
    var activeSheet by remember { mutableStateOf<Sheet>(Sheet.None) }
    var scannerActive by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<ScannedReceipt?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(item.icon, contentDescription = item.label,
                                modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text(item.label, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp)
                        },
                        selected = when (item) {
                            NavItem.HOME -> state.activeTab == ActiveTab.DASHBOARD
                            NavItem.ADD -> false
                            NavItem.ANALYSIS -> state.activeTab == ActiveTab.ANALYSIS
                            NavItem.SETTINGS -> state.activeTab == ActiveTab.SETTINGS
                        },
                        onClick = {
                            when (item) {
                                NavItem.ADD -> activeSheet = Sheet.AddTransaction()
                                NavItem.HOME -> vm.setActiveTab(ActiveTab.DASHBOARD)
                                NavItem.ANALYSIS -> vm.setActiveTab(ActiveTab.ANALYSIS)
                                NavItem.SETTINGS -> vm.setActiveTab(ActiveTab.SETTINGS)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (state.activeTab) {
                ActiveTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    vm = vm,
                    onAddTransaction = { activeSheet = Sheet.AddTransaction() },
                    onEditTransaction = { activeSheet = Sheet.AddTransaction(it) },
                    onEditAccount = { acc ->
                        if (acc.category == "Investments" || acc.category == "Vorsorge")
                            activeSheet = Sheet.InvestmentDetail(acc)
                        else
                            activeSheet = Sheet.EditAccount(acc)
                    },
                    onInvestmentDetail = { activeSheet = Sheet.InvestmentDetail(it) },
                    onSplitPotClick = { activeSheet = Sheet.SplitDetail }
                )
                ActiveTab.ANALYSIS -> AnalysisScreen(
                    state = state,
                    vm = vm,
                    onAddOrder = { activeSheet = Sheet.AddStandingOrder() },
                    onEditOrder = { activeSheet = Sheet.AddStandingOrder(it) }
                )
                ActiveTab.SETTINGS -> SettingsScreen(
                    state = state,
                    vm = vm,
                    onEditAccount = { activeSheet = Sheet.EditAccount(it) },
                    onAddAccount = { activeSheet = Sheet.EditAccount(null) },
                    onEditCategory = { activeSheet = Sheet.EditCategory(it) },
                    onAddCategory = { activeSheet = Sheet.EditCategory(null) }
                )
            }
        }
    }

    // Sheets
    when (val sheet = activeSheet) {
        is Sheet.AddTransaction -> TransactionFormSheet(
            initialTx = sheet.tx,
            isStandingOrder = false,
            scanned = scannedResult,
            onScanReceipt = {
                activeSheet = Sheet.None
                scannerActive = true
            },
            state = state, vm = vm,
            onDismiss = {
                activeSheet = Sheet.None
                scannedResult = null
            }
        )
        is Sheet.AddStandingOrder -> TransactionFormSheet(
            initialTx = null,
            isStandingOrder = true,
            initialOrder = sheet.order,
            state = state, vm = vm,
            onDismiss = { activeSheet = Sheet.None }
        )
        is Sheet.EditAccount -> AccountFormSheet(
            account = sheet.account, vm = vm,
            onDismiss = { activeSheet = Sheet.None }
        )
        is Sheet.InvestmentDetail -> InvestmentDetailSheet(
            account = sheet.account, vm = vm,
            onDismiss = { activeSheet = Sheet.None }
        )
        is Sheet.EditCategory -> CategoryFormSheet(
            category = sheet.category, vm = vm,
            onDismiss = { activeSheet = Sheet.None }
        )
        is Sheet.SplitDetail -> SplitDetailSheet(
            state = state,
            vm = vm,
            onEditTransaction = { activeSheet = Sheet.AddTransaction(it) },
            onDismiss = { activeSheet = Sheet.None }
        )
        Sheet.None -> Unit
    }

    // Vollbild-Kamerascan (liegt über allem)
    if (scannerActive) {
        ReceiptScanScreen(
            onDismiss = { scannerActive = false },
            onResult = { receipt ->
                scannerActive = false
                scannedResult = receipt
                activeSheet = Sheet.AddTransaction()
            }
        )
    }
}

enum class NavItem(val icon: ImageVector, val label: String) {
    HOME(Icons.Default.Home, "Home"),
    ADD(Icons.Default.AddCircle, "Neu"),
    ANALYSIS(Icons.Default.BarChart, "Planung"),
    SETTINGS(Icons.Default.Settings, "Optionen")
}
