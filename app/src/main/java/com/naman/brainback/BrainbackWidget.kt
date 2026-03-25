package com.naman.brainback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider

class BrainbackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            BrainbackWidgetContent(context)
        }
    }

    @Composable
    private fun BrainbackWidgetContent(context: Context) {
        val prefs = context.getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        val totalBlocks = prefs.getInt("total_blocks", 0)
        
        val statsManager = StatsManager(context)
        val screenTime = statsManager.getTotalScreenTimeToday()
        val formattedTime = formatMillis(screenTime)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BRAINBACK",
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Tapping Time opens Wellbeing
                StatItem(
                    label = "USAGE", 
                    value = formattedTime,
                    onClick = {
                        val intent = Intent().apply {
                            component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = GlanceModifier.width(24.dp))
                
                // Tapping Blocks opens Brainback
                StatItem(
                    label = "SAVED", 
                    value = totalBlocks.toString(),
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.let { context.startActivity(it) }
                    }
                )
            }
        }
    }

    @Composable
    private fun StatItem(label: String, value: String, onClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.clickable { onClick() }
        ) {
            Text(text = label, style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp))
            Text(text = value, style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold))
        }
    }

    private fun formatMillis(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

class BrainbackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BrainbackWidget()
}
