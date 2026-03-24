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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                secondary = Color(0xFFBBBBBB) // Increased contrast for secondary elements
            ),
            content = content
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen() {
        val isServiceEnabled = isAccessibilityServiceEnabled()
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        val discoveredBrowsers = remember { getInstalledBrowsers() }

        var showMenu by remember { mutableStateOf(false) }
        var showPrivacyDialog by remember { mutableStateOf(false) }
        var showAIDialog by remember { mutableStateOf(false) }

        var remainingMillis by remember { mutableStateOf(frictionManager.getRemainingMillis()) }
        val isLocked = frictionManager.isLocked()

        LaunchedEffect(isLocked) {
            while (frictionManager.isLocked()) {
                delay(1000)
                remainingMillis = frictionManager.getRemainingMillis()
            }
        }

        val totalBlocks = prefs.getInt("total_blocks", 0)
        val ytBlocks = prefs.getInt("block_count_com.google.android.youtube", 0)
        val igBlocks = prefs.getInt("block_count_com.instagram.android", 0)
        
        var totalBrowserBlocks = 0
        discoveredBrowsers.forEach { pkg ->
            totalBrowserBlocks += prefs.getInt("block_count_$pkg", 0)
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

                // STATUS CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isServiceEnabled) "SYSTEM PROTECTED" else "PROTECTION OFF",
                            color = if (isServiceEnabled) Color.White else Color(0xFFBBBBBB), // Increased contrast
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (isLocked) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatMillisTimer(remainingMillis), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Monospace)
                                Text("LOCK SEQUENCE ACTIVE", color = Color(0xFF888888), fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        } else {
                            Button(
                                onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("MANAGE SERVICE", fontWeight = FontWeight.Bold) }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { frictionManager.startLock() }) {
                                Text("START 30M FOCUS LOCK", color = Color(0xFFBBBBBB), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    MonochromeStat("SAVED", totalBlocks.toString(), Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    MonochromeStat("FOCUS", formatMillis(statsManager.getTotalScreenTimeToday()), Modifier.weight(1f).clickable {
                        val intent = Intent().apply {
                            setClassName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity")
                        }
                        try { startActivity(intent) }
                        catch (e: Exception) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    })
                }
                
                Spacer(modifier = Modifier.height(56.dp))
                Text("DISTRACTION LOG", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)) {
                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        MonochromeCinematicPie(ytBlocks, igBlocks, totalBrowserBlocks)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    MonochromeBar("YouTube", ytBlocks, Color.White)
                    MonochromeBar("Instagram", igBlocks, Color(0xFFBBBBBB)) // Increased contrast
                    MonochromeBar("Browsers", totalBrowserBlocks, Color(0xFF666666)) // Increased contrast
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    // READABLE Dynamic Defense text
                    Text(
                        "Dynamic Defense: ${discoveredBrowsers.size} local sources identified.", 
                        color = Color(0xFFBBBBBB), // High contrast readable text
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(80.dp))
                
                // MINIMAL FOOTER: Faded logo + quote only
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally).alpha(0.3f), // Very faded
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape)) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp), // Cropped minimal brain
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Be back in your brain.", color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Serif)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Pop-ups
        if (showAIDialog) {
            AlertDialog(onDismissRequest = { showAIDialog = false }, containerColor = Color(0xFF111111), shape = RoundedCornerShape(24.dp),
                title = { Text("Share & AI Analysis", color = Color.White) },
                text = { Text("Brainback will generate a local JSON file. You are in full control—you can review this data before sharing it with any AI. No data is stored on our servers.", color = Color(0xFFBBBBBB)) },
                confirmButton = {
                    Button(onClick = { showAIDialog = false; shareDataWithAI(totalBlocks, ytBlocks, igBlocks, totalBrowserBlocks) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("SHARE") }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(onDismissRequest = { showPrivacyDialog = false }, containerColor = Color(0xFF111111), shape = RoundedCornerShape(24.dp),
                title = { Text("TRUST & POWER", color = Color.White) },
                text = { Text("100% LOCAL: All data stays on this device.\n\nBATTERY POSITIVE: Reducing short-form video saves more power than this firewall consumes.", color = Color(0xFFBBBBBB)) },
                confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("OK", color = Color.White) } }
            )
        }
    }

    private fun getInstalledBrowsers(): List<String> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos.map { it.activityInfo.packageName }.distinct()
    }

    @Composable
    fun MonochromeStat(label: String, value: String, modifier: Modifier = Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            Text(label, color = Color(0xFFBBBBBB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) // Increased contrast
            Text(value, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Serif)
        }
    }

    @Composable
    fun MonochromeBar(label: String, count: Int, color: Color) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.uppercase(), color = Color.White, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(count.toString(), color = Color(0xFFBBBBBB), fontSize = 10.sp) // Increased contrast
            }
            Spacer(modifier = Modifier.height(6.dp))
            val fraction = if (count > 0) minOf(count / 20f, 1f) else 0.01f
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(1.dp))) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(2.dp).background(color, RoundedCornerShape(1.dp)))
            }
        }
    }

    @Composable
    fun MonochromeCinematicPie(yt: Int, ig: Int, browser: Int) {
        val total = (yt + ig + browser).toFloat().coerceAtLeast(1f)
        val ytSweep = (yt / total) * 360f
        val igSweep = (ig / total) * 360f
        val browserSweep = (browser / total) * 360f

        val infiniteTransition = rememberInfiniteTransition(label = "Rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "Rotation"
        )

        Canvas(modifier = Modifier.size(180.dp).rotate(rotation)) {
            drawArc(color = Color.White, startAngle = 0f, sweepAngle = ytSweep, useCenter = false, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = Color(0xFFBBBBBB), startAngle = ytSweep, sweepAngle = igSweep, useCenter = false, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = Color(0xFF444444), startAngle = ytSweep + igSweep, sweepAngle = browserSweep, useCenter = false, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }

    private fun shareDataWithAI(total: Int, yt: Int, ig: Int, browser: Int) {
        val json = JSONObject().apply {
            put("brand", "Brainback")
            put("stats", JSONObject().apply {
                put("total_interventions", total)
                put("youtube", yt)
                put("instagram", ig)
                put("browsers", browser)
                put("focus_time", formatMillis(statsManager.getTotalScreenTimeToday()))
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
