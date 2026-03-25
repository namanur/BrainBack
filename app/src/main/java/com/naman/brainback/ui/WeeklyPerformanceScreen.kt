package com.naman.brainback.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import androidx.compose.ui.text.font.FontFamily
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeeklyPerformanceScreen(viewModel: WeeklyPerformanceViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "Weekly Performance",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )
        Text(
            "Mindful analysis of your week.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("BRAINBACK IMPACT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ImpactStat("TOTAL BLOCKS", uiState.totalBlocks.toString())
                    ImpactStat("TIME SAVED", "${uiState.totalBlocks * 4}m")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("SCREEN TIME (MINS)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.dailyScreenTime.isNotEmpty()) {
            val screenTimeData = uiState.dailyScreenTime.map { it.totalTimeMillis / (1000 * 60f) }
            val model = entryModelOf(*screenTimeData.toTypedArray())
            Chart(
                chart = columnChart(),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("PHONE UNLOCKS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.dailyUnlocks.isNotEmpty()) {
            val unlockData = uiState.dailyUnlocks.map { it.count.toFloat() }
            val model = entryModelOf(*unlockData.toTypedArray())
            Chart(
                chart = columnChart(),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.height(200.dp).fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("MOST BLOCKED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        uiState.appBreakdown.take(5).forEach { app ->
            AppBlockItem(app.appLabel, app.count)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("FIRST PICKUP TREND", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(16.dp)).padding(16.dp)) {
            uiState.firstPickups.forEach { pickup ->
                PickupItem(pickup.day, pickup.timestamp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("DONE", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ImpactStat(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Thin, fontFamily = FontFamily.Serif)
    }
}

@Composable
fun AppBlockItem(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Text(count.toString(), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
}

@Composable
fun PickupItem(day: Long, timestamp: Long) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(dayFormat.format(Date(day)), color = Color.Gray, fontSize = 12.sp)
        Text(timeFormat.format(Date(timestamp)), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
