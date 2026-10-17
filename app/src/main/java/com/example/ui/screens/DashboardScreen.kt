package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLimitConfig
import com.example.engine.UsageEngine
import com.example.ui.components.AppLimitCard
import com.example.ui.components.TimeLimitDialog
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedBlocked

@Composable
fun DashboardScreen(
    configs: List<AppLimitConfig>,
    hasUsageAccess: Boolean,
    hasAccessibility: Boolean,
    onNavigateToAddApp: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onSetLimit: (packageName: String, appName: String, minutes: Int) -> Unit,
    onRemoveLimit: (packageName: String) -> Unit,
    onToggleEnabled: (packageName: String, enabled: Boolean) -> Unit
) {
    var editingConfig by remember { mutableStateOf<AppLimitConfig?>(null) }

    val allPermissionsGranted = hasUsageAccess && hasAccessibility
    val totalMonitored = configs.size
    val lockedCount = configs.count { it.isLocked || it.accumulatedUsageMs >= it.limitMs }
    val totalUsedMinutes = configs.sumOf { (it.accumulatedUsageMs / 60000L).toInt() }

    if (editingConfig != null) {
        TimeLimitDialog(
            appName = editingConfig!!.appName,
            initialMinutes = editingConfig!!.dailyLimitMinutes,
            isAlreadyConfigured = true,
            onDismiss = { editingConfig = null },
            onConfirm = { newMinutes ->
                onSetLimit(editingConfig!!.packageName, editingConfig!!.appName, newMinutes)
                editingConfig = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddApp,
                containerColor = CyanAccent,
                contentColor = Color(0xFF0A0F1D),
                modifier = Modifier.testTag("fab_add_app")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "অ্যাপ যোগ করুন")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অ্যাপ যোগ করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Alert Banner if degraded
            if (!allPermissionsGranted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPermissions() }
                            .testTag("permission_warning_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AmberAlert,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "সুরক্ষা সেবা অসম্পূর্ণ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AmberAlert
                                )
                                Text(
                                    text = "সঠিক ট্র্যাকিং ও তাৎক্ষণিক লকিং সক্রিয় করতে এক্সেসিবিলিটি ও ব্যবহার অনুমতি দিন।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ঠিক করুন ›",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert
                            )
                        }
                    }
                }
            }

            // Summary Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (allPermissionsGranted) EmeraldSafe.copy(alpha = 0.2f) else AmberAlert.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (allPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (allPermissionsGranted) EmeraldSafe else AmberAlert,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "দৈনিক সুরক্ষা স্থিতি",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (allPermissionsGranted) "২৪ ঘণ্টার সক্রিয় ট্র্যাকিং চালু" else "অনুমতি যাচাই প্রয়োজন",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox(
                                title = "নিয়ন্ত্রিত অ্যাপ",
                                value = "$totalMonitored টি",
                                accent = CyanAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            StatBox(
                                title = "লক করা",
                                value = "$lockedCount টি",
                                accent = if (lockedCount > 0) RedBlocked else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            StatBox(
                                title = "আজকের ব্যবহার",
                                value = UsageEngine.formatDuration(totalUsedMinutes),
                                accent = AmberAlert,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পর্যবেক্ষণাধীন অ্যাপসমূহ (${configs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Empty state if no configs
            if (configs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কোনো অ্যাপ নির্বাচন করা হয়নি",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "আপনার স্ক্রিন টাইম নিয়ন্ত্রণ করতে এবং নির্দিষ্ট অ্যাপে দৈনিক সময়সীমা বসাতে নিচের বোতামে ট্যাপ করুন।",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(configs, key = { it.packageName }) { config ->
                    AppLimitCard(
                        config = config,
                        onEditClick = { editingConfig = config },
                        onRemoveClick = { onRemoveLimit(config.packageName) },
                        onToggleEnabled = { enabled -> onToggleEnabled(config.packageName, enabled) }
                    )
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
