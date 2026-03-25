package com.naman.brainback.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getDailyScreenTime(days: Int): List<DailyUsage> {
        val results = mutableListOf<DailyUsage>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until days) {
            val end = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            
            val totalTime = getTotalScreenTimeForRange(start, end)
            results.add(DailyUsage(start, totalTime))
            
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return results.reversed()
    }

    private fun getTotalScreenTimeForRange(start: Long, end: Long): Long {
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var totalTime = 0L
        val appStates = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> appStates[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startTime = appStates[pkg]
                    if (startTime != null) {
                        totalTime += (event.timeStamp - startTime)
                        appStates.remove(pkg)
                    }
                }
            }
        }
        return totalTime
    }

    fun getDailyUnlockCount(days: Int): List<DailyUnlock> {
        val results = mutableListOf<DailyUnlock>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until days) {
            val end = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            
            val unlockCount = countUnlocksForRange(start, end)
            results.add(DailyUnlock(start, unlockCount))
            
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return results.reversed()
    }

    private fun countUnlocksForRange(start: Long, end: Long): Int {
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var count = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // USER_UNLOCK or KEYGUARD_HIDDEN
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                count++
            }
        }
        return count
    }

    fun getFirstPickupTime(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val end = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                return event.timeStamp
            }
        }
        return 0L
    }
}

data class DailyUsage(val day: Long, val totalTimeMillis: Long)
data class DailyUnlock(val day: Long, val count: Int)
