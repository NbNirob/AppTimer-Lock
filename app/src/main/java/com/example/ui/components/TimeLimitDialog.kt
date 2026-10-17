package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.UsageEngine
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeLimitDialog(
    appName: String,
    initialMinutes: Int = 30,
    isAlreadyConfigured: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }
    var sliderValue by remember { mutableFloatStateOf(initialMinutes.toFloat()) }
    var showConfirmationStep by remember { mutableStateOf(false) }

    val presetOptions = listOf(15, 30, 45, 60, 90, 120, 180)

    if (showConfirmationStep) {
        AlertDialog(
            onDismissRequest = { showConfirmationStep = false },
            title = {
                Text(
                    text = "সময়সীমা পরিবর্তন নিশ্চিত করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "আপনি '$appName' এর দৈনিক সীমা পরিবর্তন করে ${UsageEngine.formatDuration(selectedMinutes)} করতে চান? এটি বর্তমান ২৪-ঘণ্টার চক্রে কার্যকর হবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationStep = false
                        onConfirm(selectedMinutes)
                    },
                    modifier = Modifier.testTag("confirm_limit_change_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF0A0F1D))
                ) {
                    Text("হ্যাঁ, পরিবর্তন করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationStep = false }) {
                    Text("বাতিল")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAlreadyConfigured) "সময়সীমা সম্পাদনা" else "সময়সীমা নির্ধারণ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "দৈনিক সর্বোচ্চ ব্যবহারের সময় নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Current Selection Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = UsageEngine.formatDuration(selectedMinutes),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "প্রতি ২৪ ঘণ্টায় বরাদ্দকৃত সময়",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Preset Chips
                    Text(
                        text = "দ্রুত নির্বাচন:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetOptions.forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) CyanAccent else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedMinutes = mins
                                        sliderValue = mins.toFloat()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (mins >= 60) "${mins / 60}ঘ${if (mins % 60 > 0) "${mins % 60}মি" else ""}" else "${mins}মি",
                                    color = if (isSelected) Color(0xFF0A0F1D) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Slider
                    Text(
                        text = "কাস্টম স্লাইডার (৫ মিনিট থেকে ১৮০ মিনিট):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            selectedMinutes = it.toInt()
                        },
                        valueRange = 5f..180f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAlreadyConfigured) {
                            showConfirmationStep = true
                        } else {
                            onConfirm(selectedMinutes)
                        }
                    },
                    modifier = Modifier.testTag("save_limit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color(0xFF0A0F1D)
                    )
                ) {
                    Text(
                        text = if (isAlreadyConfigured) "পরিবর্তন করুন" else "লক সক্রিয় করুন",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("বাতিল")
                }
            }
        )
    }
}
