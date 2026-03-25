package com.naman.brainback

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

class StatsManager(private val context: Context) {

    fun getTotalScreenTimeToday(): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Accurate Start of Day (Today 00:00:00)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        var totalTime = 0L
        var startTimestamp = 0L
        val appStates = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appStates[pkg] = event.timeStamp
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                val start = appStates[pkg]
                if (start != null) {
                    totalTime += (event.timeStamp - start)
                    appStates.remove(pkg)
                }
            }
        }

        // Add time for app currently in foreground
        appStates.forEach { (_, start) ->
            totalTime += (endTime - start)
        }

        return totalTime
    }
}
