package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RedBlocked

@Composable
fun SettingsAndPermissionsScreen(
    hasUsageAccess: Boolean,
    hasAccessibility: Boolean,
    hasNotification: Boolean,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "অনুমতি ও সেটিংস",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "অ্যাপ ব্লকার ও টাইমার সুচারুভাবে চলার জন্য প্রয়োজনীয় সেটিংস",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Permissions Checklist
        item {
            Text(
                text = "সিস্টেম অনুমতিসমূহ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
        }

        // Accessibility Permission Card
        item {
            PermissionItemCard(
                title = "এক্সেসিবিলিটি সার্ভিস (Accessibility)",
                description = "ফরগ্রাউন্ড অ্যাপ পরিবর্তন শনাক্তকরণ এবং দৈনিক সীমা পৌঁছালে তাৎক্ষণিকভাবে ব্লকিং উইন্ডো প্রদর্শনের জন্য অত্যাবশ্যক।",
                isGranted = hasAccessibility,
                icon = Icons.Default.AccessibilityNew,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                tag = "accessibility"
            )
        }

        // Usage Access Permission Card
        item {
            PermissionItemCard(
                title = "ব্যবহারের পরিসংখ্যান (Usage Access)",
                description = "অ্যান্ড্রয়েড সিস্টেম থেকে প্রতিটি অ্যাপের সঠিক ব্যবহারের সময় ক্রস-চেক ও পরিমাপ করার জন্য প্রয়োজনীয়।",
                isGranted = hasUsageAccess,
                icon = Icons.Default.QueryStats,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                tag = "usage_access"
            )
        }

        // Notification Permission Card
        item {
            PermissionItemCard(
                title = "বিজ্ঞপ্তি (Notifications)",
                description = "দৈনিক সীমা শেষ হওয়ার ৫ মিনিট ও ১ মিনিট পূর্বে আগাম সতর্কবার্তা প্রদানের জন্য ব্যবহৃত হয়।",
                isGranted = hasNotification,
                icon = Icons.Default.Notifications,
                onOpenSettings = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                },
                tag = "notifications"
            )
        }

        // Section: Redmi Note 10 / MIUI Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberAlert.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = AmberAlert,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Redmi Note 10 (MIUI / Android 13) গাইড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "শাওমি (Redmi/MIUI) ডিভাইসে আক্রমণাত্মক ব্যাটারি ম্যানেজমেন্টের কারণে ব্যাকগ্রাউন্ড প্রসেস যাতে বন্ধ না হয়, সেজন্য নিচের ধাপগুলো নিশ্চিত করুন:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    InstructionBullet(
                        number = "১",
                        text = "অটোস্টার্ট (Autostart) চালু করুন: Security অ্যাপ > Manage Apps > AppTimer Lock > Autostart চালু করুন।"
                    )
                    InstructionBullet(
                        number = "২",
                        text = "ব্যাটারি সেভার: App info > Battery Saver > 'No restrictions' নির্বাচন করুন।"
                    )
                    InstructionBullet(
                        number = "৩",
                        text = "রিসেন্টসে লক: রিসেন্ট অ্যাপস স্ক্রিনে AppTimer Lock কার্ডটি চেপে ধরে লক আইকন চাপুন।"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("অ্যাপ ইনফো ও ব্যাটারি সেটিংস খুলুন")
                    }
                }
            }
        }

        // Section: Honest Security & System Limitations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্বচ্ছতা ও সুরক্ষা সীমাবদ্ধতা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "অ্যান্ড্রয়েড অপারেটিং সিস্টেমের নিরাপত্তা ফ্রেমওয়ার্ক অনুযায়ী সাধারণ কোনো তৃতীয় পক্ষের অ্যাপ সিস্টেম রুট বা ডিভাইস ম্যানেজমেন্ট পলিসি ছাড়া ফোর্স-স্টপ, আনইনস্টল বা সেফ মোডের বিরুদ্ধে শতভাগ অপ্রতিরোধ্য লকিংয়ের মিথ্যা দাবি করতে পারে না।\n\nAppTimer Lock লোকাল Room ডাটাবেসে নির্ভুলভাবে ২৪ ঘণ্টার সাইকেল স্টেট সংরক্ষণ করে, ডিভাইস রিবুট বা প্রসেস রিস্টার্টের পরও ডেটা অক্ষুণ্ণ রাখে এবং ফরগ্রাউন্ড অ্যাক্সেসিবিলিটি মনিটরের মাধ্যমে সর্বোচ্চ নিরপেক্ষ ডিজিটাল সুরক্ষা প্রদান করে।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Privacy note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldSafe,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "সম্পূর্ণ অফলাইন ও প্রাইভেট: আপনার কোনো ব্যবহার ডেটা ডিভাইসের বাইরে পাঠানো হয় না। কোনো বিজ্ঞাপন, সাবস্ক্রিপশন বা ক্লাউড ট্র্যাকিং নেই।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onOpenSettings: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("perm_card_$tag"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) EmeraldSafe.copy(alpha = 0.15f) else AmberAlert.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) EmeraldSafe else AmberAlert,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isGranted) EmeraldSafe.copy(alpha = 0.15f) else AmberAlert.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "সক্রিয়" else "অনুমতি দিন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) EmeraldSafe else AmberAlert
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_perm_$tag"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF0A0F1D))
                ) {
                    Text("অনুমতি দিতে সেটিংস খুলুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InstructionBullet(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 16.sp
        )
    }
}
