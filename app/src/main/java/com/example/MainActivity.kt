package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.Screen
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsAndPermissionsScreen
import com.example.ui.screens.UsageHistoryScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = AppTimerApplication.instance.repository,
            context = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.evaluateCycles()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val hasAccessibility by viewModel.hasAccessibility.collectAsStateWithLifecycle()
    val hasNotification by viewModel.hasNotification.collectAsStateWithLifecycle()

    val screens = listOf(
        Screen.Dashboard,
        Screen.AppSelection,
        Screen.History,
        Screen.Settings
    )

    // Handle Android system back gesture: if not on Dashboard, return to Dashboard
    if (currentScreen != Screen.Dashboard) {
        BackHandler {
            currentScreen = Screen.Dashboard
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                screens.forEach { screen ->
                    val isSelected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0A0F1D),
                            indicatorColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> {
                        DashboardScreen(
                            configs = configs,
                            hasUsageAccess = hasUsageAccess,
                            hasAccessibility = hasAccessibility,
                            onNavigateToAddApp = { currentScreen = Screen.AppSelection },
                            onNavigateToPermissions = { currentScreen = Screen.Settings },
                            onSetLimit = { pkg, name, mins ->
                                viewModel.setAppLimit(pkg, name, mins)
                            },
                            onRemoveLimit = { pkg ->
                                viewModel.removeAppLimit(pkg)
                            },
                            onToggleEnabled = { pkg, enabled ->
                                viewModel.toggleAppLimit(pkg, enabled)
                            }
                        )
                    }
                    Screen.AppSelection -> {
                        AppSelectionScreen(
                            installedApps = installedApps,
                            isLoading = isLoadingApps,
                            onSetLimit = { pkg, name, mins ->
                                viewModel.setAppLimit(pkg, name, mins)
                                currentScreen = Screen.Dashboard
                            }
                        )
                    }
                    Screen.History -> {
                        UsageHistoryScreen(
                            history = history,
                            onClearHistory = {
                                viewModel.clearAllHistory()
                            }
                        )
                    }
                    Screen.Settings -> {
                        SettingsAndPermissionsScreen(
                            hasUsageAccess = hasUsageAccess,
                            hasAccessibility = hasAccessibility,
                            hasNotification = hasNotification,
                            onRefreshPermissions = {
                                viewModel.refreshPermissions()
                            }
                        )
                    }
                }
            }
        }
    }
}
