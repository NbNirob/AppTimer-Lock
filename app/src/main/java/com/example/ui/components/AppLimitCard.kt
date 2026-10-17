package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLimitConfig
import com.example.engine.UsageEngine
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.RedBlocked

@Composable
fun AppLimitCard(
    config: AppLimitConfig,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val usedMinutes = (config.accumulatedUsageMs / 60000L).toInt()
    val progress = config.progressFraction
    val isLocked = config.isLocked || config.accumulatedUsageMs >= config.limitMs

    val statusColor by animateColorAsState(
        targetValue = when {
            isLocked -> RedBlocked
            progress >= 0.8f -> AmberAlert
            else -> CyanAccent
        },
        label = "statusColor"
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "অ্যাপের সীমা মুছে ফেলতে চান?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "আপনি কি '${config.appName}' এর সময়সীমা এবং ট্র্যাকিং অপসারণ করতে চান? এর ফলে অ্যাপটির উপর লক বা ব্যবহারের কোনো সীমা থাকবে না।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onRemoveClick()
                    },
                    modifier = Modifier.testTag("confirm_delete_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RedBlocked)
                ) {
                    Text("হ্যাঁ, অপসারণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_limit_card_${config.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: App name, status badge, toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // App Icon Fallback
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Security,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = config.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLocked) "লক করা হয়েছে" else if (config.isEnabled) "সক্রিয় পর্যবেক্ষণ" else "সাময়িক স্থগিত",
                            fontSize = 12.sp,
                            color = if (isLocked) RedBlocked else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Switch to enable/disable
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanAccent,
                        checkedTrackColor = CyanAccent.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.testTag("toggle_switch_${config.packageName}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Usage & Remaining Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ব্যবহৃত: ${usedMinutes}মি / ${config.dailyLimitMinutes}মি",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isLocked) "সময় শেষ" else "${UsageEngine.formatTimeRemainingHuman(config.remainingMs)} বাকি",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24h Cycle End info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "চক্র শেষ ও আনলক:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = UsageEngine.formatTimestamp(config.cycleEndTimeMs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions (Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp).testTag("edit_limit_${config.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "সম্পাদনা করুন",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp).testTag("delete_limit_${config.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "অপসারণ করুন",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
