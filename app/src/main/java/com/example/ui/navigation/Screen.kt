package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "ড্যাশবোর্ড",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object AppSelection : Screen(
        route = "app_selection",
        title = "অ্যাপ নির্বাচন",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )

    data object History : Screen(
        route = "history",
        title = "ইতিহাস",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Settings : Screen(
        route = "settings",
        title = "সেটিংস",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
