package com.naman.brainback

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var statsManager: StatsManager
    private lateinit var frictionManager: FrictionManager
    private lateinit var validator: LockValidator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statsManager = StatsManager(this)
        frictionManager = FrictionManager(this)
        validator = LockValidator(this)
        
        setContent {
            BrainbackTheme {
                MainNavigation()
            }
        }
    }

    @Composable
    fun BrainbackTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Color(0xFF000000),
                surface = Color(0xFF111111),
                primary = Color(0xFFFFFFFF),
                secondary = Color(0xFFBBBBBB)
            ),
            content = content
        )
    }

    @Composable
    fun MainNavigation() {
        var currentScreen by remember { mutableStateOf("dashboard") }
        
        when (currentScreen) {
            "dashboard" -> DashboardScreen(onNavigateToPreflight = { currentScreen = "preflight" })
            "preflight" -> PreflightScreen(onBack = { currentScreen = "dashboard" })
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(onNavigateToPreflight: () -> Unit) {
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        val monitoredBrowsers = remember { getMonitoredBrowsers() }

        var showMenu by remember { mutableStateOf(false) }
        var remainingMillis by remember { mutableStateOf(frictionManager.getRemainingMillis()) }
        var isLocked by remember { mutableStateOf(frictionManager.isLocked()) }
        var isOnBreak by remember { mutableStateOf(frictionManager.isOnBreak()) }
        var expiredQuote by remember { mutableStateOf(frictionManager.getQuoteIfExpired()) }
        var unlockInput by remember { mutableStateOf("") }

        LaunchedEffect(isLocked, isOnBreak) {
            while (frictionManager.getRemainingMillis() > 0) {
                delay(1000)
                remainingMillis = frictionManager.getRemainingMillis()
            }
            isLocked = frictionManager.isLocked()
            isOnBreak = frictionManager.isOnBreak()
            expiredQuote = frictionManager.getQuoteIfExpired()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(54.dp), contentScale = ContentScale.Crop)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("BRAINBACK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 6.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF111111))) {
                            DropdownMenuItem(text = { Text("Transparency", color = Color.White) }, onClick = { showMenu = false; onNavigateToPreflight() })
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isOnBreak) {
                            Text("BREAK WINDOW ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(formatMillisTimer(remainingMillis), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Thin)
                            Text("FIREWALL IS TEMPORARILY OFF", color = Color(0xFF444444), fontSize = 9.sp)
                        } else if (isLocked) {
                            Text("COOLDOWN ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(formatMillisTimer(remainingMillis), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Thin)
                            Text("SYSTEM HARD-LOCKED", color = Color(0xFFB06161), fontSize = 9.sp)
                        } else if (expiredQuote != null) {
                            Text("COOLDOWN COMPLETE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(expiredQuote!!, color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                            OutlinedTextField(value = unlockInput, onValueChange = { unlockInput = it }, label = { Text("Type quote perfectly") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { if (frictionManager.unlock(unlockInput)) unlockInput = "" }, modifier = Modifier.padding(top = 12.dp)) { Text("TAKE BREAK") }
                        } else {
                            Text("SYSTEM ARMED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { onNavigateToPreflight() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("REQUEST UNLOCK") }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MonochromeStat("SAVED", prefs.getInt("total_blocks", 0).toString(), Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    MonochromeStat("USAGE", formatMillis(statsManager.getTotalScreenTimeToday()), Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(80.dp))
                Row(modifier = Modifier.align(Alignment.CenterHorizontally).alpha(0.3f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape)) { Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Crop) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Be back in your brain.", color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }

    @Composable
    fun PreflightScreen(onBack: () -> Unit) {
        val isAccessibilityOk = validator.hasAccessibilityService()
        val isStatsOk = validator.hasUsageStatsPermission()
        val isOverlayOk = validator.canDrawOverlays()
        val isAdminOk = validator.isDeviceAdmin()
        val isSystemReady = validator.isSystemReady()

        Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(28.dp).verticalScroll(rememberScrollState())) {
            Text("Pre-Lock Validation", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 32.dp))
            Text("The system must be fully validated to enable Anti-Uninstall protection.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            PreflightItem("Fortress Admin", "Prevents uninstallation.", isAdminOk) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@MainActivity, AdminReceiver::class.java))
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This permission allows Brainback to prevent impulsive uninstallation during lock periods.")
                }
                startActivity(intent)
            }
            PreflightItem("Accessibility", "Used to detect Shorts.", isAccessibilityOk) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            PreflightItem("Usage Stats", "Used to track progress.", isStatsOk) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            PreflightItem("Overlays", "Used for self-preservation.", isOverlayOk) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { frictionManager.startLock(30); onBack() },
                enabled = isSystemReady,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(if (isSystemReady) "COMMIT TO 30M LOCK" else "VALIDATION REQUIRED") }
            
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    }

    @Composable
    fun PreflightItem(name: String, desc: String, ok: Boolean, onFix: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (ok) Color.Green else Color.Red)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(desc, color = Color.Gray, fontSize = 11.sp)
                }
                if (!ok) { TextButton(onClick = onFix) { Text("FIX", color = Color.White) } }
            }
        }
    }

    private fun getMonitoredBrowsers(): List<String> = listOf("com.android.chrome", "com.brave.browser", "org.mozilla.firefox")

    @Composable
    fun MonochromeStat(label: String, value: String, modifier: Modifier = Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Text(label, color = Color(0xFFBBBBBB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(value, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Serif)
        }
    }

    private fun formatMillis(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) "${hours}H ${minutes}M" else "${minutes}M"
    }

    private fun formatMillisTimer(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/com.naman.brainback.BlockerService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServices.contains(expectedService)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}

data class AppUsage(val label: String, val blocks: Int, val color: Color)
