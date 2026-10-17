package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.engine.UsageEngine
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RedBlocked
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkVariant
import kotlinx.coroutines.delay

class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "এই অ্যাপটি"
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val lockUntilMs = intent.getLongExtra(EXTRA_LOCK_UNTIL_MS, System.currentTimeMillis() + 86400000L)
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 30)
        val usageMinutes = intent.getIntExtra(EXTRA_USAGE_MINUTES, limitMinutes)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                BlockedScreen(
                    appName = appName,
                    packageName = packageName,
                    lockUntilMs = lockUntilMs,
                    limitMinutes = limitMinutes,
                    usageMinutes = usageMinutes,
                    onGoHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onOpenAppTimer = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_LOCK_UNTIL_MS = "extra_lock_until_ms"
        const val EXTRA_LIMIT_MINUTES = "extra_limit_minutes"
        const val EXTRA_USAGE_MINUTES = "extra_usage_minutes"
    }
}

@Composable
fun BlockedScreen(
    appName: String,
    packageName: String,
    lockUntilMs: Long,
    limitMinutes: Int,
    usageMinutes: Int,
    onGoHome: () -> Unit,
    onOpenAppTimer: () -> Unit
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val remainingLockMs = (lockUntilMs - currentTimeMs).coerceAtLeast(0L)

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MidnightBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing Lock Shield Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(RedBlocked.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .background(RedBlocked.copy(alpha = 0.2f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "লক করা হয়েছে",
                            tint = RedBlocked,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "সময়সীমা অতিক্রান্ত!",
                    color = RedBlocked,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "'$appName' এর আজকের বরাদ্দকৃত সময় শেষ হয়েছে",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (packageName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = packageName,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Lock Countdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = AmberAlert,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "বাকি লক সময়কাল",
                                color = AmberAlert,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = UsageEngine.formatMsToDisplay(remainingLockMs),
                            color = CyanAccent,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDarkVariant, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "পুনরায় উন্মুক্ত হবে:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = UsageEngine.formatTimestamp(lockUntilMs),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "দৈনিক সীমা",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$limitMinutes মি",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ব্যবহৃত সময়",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$usageMinutes মি",
                                color = AmberAlert,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "চক্রের স্থায়িত্ব",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "২৪ ঘণ্টা",
                                color = CyanAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ডিজিটাল সুস্থতা বজায় রাখতে এবং অতিরিক্ত স্ক্রিন আসক্তি প্রতিরোধে নির্ধারিত সময়সীমা পূর্ণ হওয়ার পর এই অ্যাপটি স্বয়ংক্রিয়ভাবে লক হয়েছে।",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("go_home_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = MidnightBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "হোম স্ক্রিনে ফিরে যান",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenAppTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_apptimer_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AppTimer Lock ড্যাশবোর্ড",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
