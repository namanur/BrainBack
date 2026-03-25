package com.naman.brainback

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
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
import androidx.compose.material.icons.filled.Menu
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
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var statsManager: StatsManager
    private lateinit var frictionManager: FrictionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statsManager = StatsManager(this)
        frictionManager = FrictionManager(this)
        
        setContent {
            BrainbackTheme {
                DashboardScreen()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen() {
        val isServiceEnabled = isAccessibilityServiceEnabled()
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        val monitoredBrowsers = remember { getMonitoredBrowsers() }

        var showMenu by remember { mutableStateOf(false) }
        var showPrivacyDialog by remember { mutableStateOf(false) }
        var showAIDialog by remember { mutableStateOf(false) }
        var showLockWarning by remember { mutableStateOf(false) } // NEW: Warning Dialog State
        var unlockInput by remember { mutableStateOf("") }

        // State-driven lock tracking
        var remainingMillis by remember { mutableStateOf(frictionManager.getRemainingMillis()) }
        var isLocked by remember { mutableStateOf(frictionManager.isLocked()) }
        var activeQuote by remember { mutableStateOf(frictionManager.getQuoteIfExpired()) }

        LaunchedEffect(isLocked, activeQuote) {
            while (frictionManager.isLocked()) {
                delay(1000)
                remainingMillis = frictionManager.getRemainingMillis()
            }
            // Transition to Quote phase once timer ends
            isLocked = false
            activeQuote = frictionManager.getQuoteIfExpired()
        }

        val totalBlocks = prefs.getInt("total_blocks", 0)
        
        val appUsageList = listOf(
            AppUsage("YouTube", prefs.getInt("block_count_com.google.android.youtube", 0), Color.White),
            AppUsage("Instagram", prefs.getInt("block_count_com.instagram.android", 0), Color(0xFFBBBBBB)),
            AppUsage("Snapchat", prefs.getInt("block_count_com.snapchat.android", 0), Color(0xFF888888))
        ).filter { it.blocks > 0 }

        var browserBlockTotal = 0
        monitoredBrowsers.forEach { pkg -> browserBlockTotal += prefs.getInt("block_count_$pkg", 0) }
        val finalUsageList = if (browserBlockTotal > 0) {
            appUsageList + AppUsage("Browsers", browserBlockTotal, Color(0xFF444444))
        } else appUsageList

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
                            DropdownMenuItem(text = { Text("Trust & Power", color = Color.White) }, onClick = { showMenu = false; showPrivacyDialog = true })
                            DropdownMenuItem(text = { Text("Share & AI", color = Color.White) }, onClick = { showMenu = false; showAIDialog = true })
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(24.dp))

                // STATUS CARD (PRE-BREAK PROTOCOL)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isServiceEnabled) "SYSTEM PROTECTED" else "PROTECTION OFF",
                            color = if (isServiceEnabled) Color.White else Color(0xFFBBBBBB),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (isLocked) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatMillisTimer(remainingMillis), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Monospace)
                                Text("PRE-BREAK COOLDOWN", color = Color(0xFF888888), fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        } else if (activeQuote != null) {
                            // ENFORCED TYPING CHALLENGE (Anti-Cheat)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INTENT CHALLENGE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Type this quote exactly to unlock:", 
                                    color = Color(0xFF888888), 
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Removed SelectionContainer to prevent copy-paste
                                Text(
                                    activeQuote!!, 
                                    color = Color.White, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Light, 
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                OutlinedTextField(
                                    value = unlockInput,
                                    onValueChange = { unlockInput = it },
                                    label = { Text("Manual Entry Required", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        if (frictionManager.unlock(unlockInput)) { 
                                            activeQuote = null
                                            unlockInput = "" 
                                        } 
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("VERIFY INTENT") }
                            }
                        } else {
                            Button(
                                onClick = { showLockWarning = true }, // Trigger Warning Pop-up
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("REQUEST BREAK (30M)", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MonochromeStat("SAVED", totalBlocks.toString(), Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    MonochromeStat("TOTAL USAGE", formatMillis(statsManager.getTotalScreenTimeToday()), Modifier.weight(1f).clickable {
                        val intent = Intent().apply {
                            setClassName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity")
                        }
                        try { startActivity(intent) }
                        catch (e: Exception) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    })
                }
                
                if (finalUsageList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(56.dp))
                    Text("DISTRACTION LOG", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)) {
                        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                            MonochromeCinematicPie(finalUsageList)
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        finalUsageList.forEach { app ->
                            MonochromeBar(app.label, app.blocks, app.color)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
                Row(modifier = Modifier.align(Alignment.CenterHorizontally).alpha(0.3f), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape)) {
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Crop)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Be back in your brain.", color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Serif)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // PRE-LOCK WARNING DIALOG (Transparency Layer)
        if (showLockWarning) {
            AlertDialog(
                onDismissRequest = { showLockWarning = false },
                containerColor = Color(0xFF111111),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Commit to Focus?", color = Color.White) },
                text = { 
                    Text(
                        "You are about to enter a 30-minute lock. The 'Manage' button will disappear. You cannot turn off the firewall until the timer ends AND you manually type a focus quote. Are you sure?",
                        color = Color(0xFFBBBBBB)
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showLockWarning = false
                            frictionManager.startLock(30)
                            isLocked = true 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) { Text("I COMMIT") }
                },
                dismissButton = {
                    TextButton(onClick = { showLockWarning = false }) { Text("CANCEL", color = Color.White) }
                }
            )
        }

        // Other Dialogs
        if (showAIDialog) {
            AlertDialog(onDismissRequest = { showAIDialog = false }, containerColor = Color(0xFF111111), shape = RoundedCornerShape(24.dp),
                title = { Text("Share & AI Analysis", color = Color.White) },
                text = { Text("Brainback generates a local JSON file. You are in control of your data.", color = Color(0xFFBBBBBB)) },
                confirmButton = {
                    Button(onClick = { showAIDialog = false; shareDataWithAI(totalBlocks, finalUsageList) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("SHARE") }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(onDismissRequest = { showPrivacyDialog = false }, containerColor = Color(0xFF111111), shape = RoundedCornerShape(24.dp),
                title = { Text("TRUST & POWER", color = Color.White) },
                text = { Text("100% LOCAL: Your data never leaves this device.", color = Color(0xFFBBBBBB)) },
                confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("OK", color = Color.White) } }
            )
        }
    }

    data class AppUsage(val label: String, val blocks: Int, val color: Color)

    private fun getMonitoredBrowsers(): List<String> {
        val knownMonitored = listOf("com.android.chrome", "com.brave.browser", "org.mozilla.firefox")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .filter { knownMonitored.contains(it) }.distinct()
    }

    @Composable
    fun MonochromeStat(label: String, value: String, modifier: Modifier = Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Text(label, color = Color(0xFFBBBBBB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(value, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Serif)
        }
    }

    @Composable
    fun MonochromeBar(label: String, count: Int, color: Color) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.uppercase(), color = Color.White, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(count.toString(), color = Color(0xFFBBBBBB), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(1.dp))) {
                Box(modifier = Modifier.fillMaxWidth(if (count > 0) minOf(count / 20f, 1f) else 0.01f).height(2.dp).background(color, RoundedCornerShape(1.dp)))
            }
        }
    }

    @Composable
    fun MonochromeCinematicPie(usageList: List<AppUsage>) {
        val total = usageList.sumOf { it.blocks }.toFloat().coerceAtLeast(1f)
        val rotation by rememberInfiniteTransition().animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "Rotation"
        )
        Canvas(modifier = Modifier.size(180.dp).rotate(rotation)) {
            var currentStartAngle = 0f
            usageList.forEach { app ->
                val sweep = (app.blocks / total) * 360f
                drawArc(color = app.color, startAngle = currentStartAngle, sweepAngle = sweep, useCenter = false, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                currentStartAngle += sweep
            }
        }
    }

    private fun shareDataWithAI(total: Int, usageList: List<AppUsage>) {
        val json = JSONObject().apply {
            put("brand", "Brainback")
            put("stats", JSONObject().apply {
                put("total_interventions", total)
                usageList.forEach { put(it.label.lowercase(), it.blocks) }
                put("uptime", formatMillis(statsManager.getTotalScreenTimeToday()))
            })
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, json.toString(4))
        }
        startActivity(Intent.createChooser(intent, "ANALYZE"))
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
}
