package com.naman.brainback.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.naman.brainback.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class WeeklyPerformanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageStatsRepository(application)
    private val database = BrainbackDatabase.getDatabase(application)
    
    private val _uiState = MutableStateFlow(WeeklyUiState())
    val uiState: StateFlow<WeeklyUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -6) // Start of week (7 days ago)
            val since = calendar.timeInMillis

            val blocks = database.blockEventDao().getGroupedCountsSince(since)
            val dailyBlocks = database.blockEventDao().getDailyCountsSince(since)
            val screenTime = repository.getDailyScreenTime(7)
            val unlocks = repository.getDailyUnlockCount(7)
            
            // Pickup times
            val pickups = mutableListOf<PickupTime>()
            val pickupCal = calendar.clone() as Calendar
            for (i in 0 until 7) {
                val time = repository.getFirstPickupTime(pickupCal.timeInMillis)
                if (time > 0) {
                    pickups.add(PickupTime(pickupCal.timeInMillis, time))
                }
                pickupCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            _uiState.value = WeeklyUiState(
                totalBlocks = dailyBlocks.sumOf { it.count },
                appBreakdown = blocks,
                dailyBlocks = dailyBlocks,
                dailyScreenTime = screenTime,
                dailyUnlocks = unlocks,
                firstPickups = pickups
            )
        }
    }
}

data class WeeklyUiState(
    val totalBlocks: Int = 0,
    val appBreakdown: List<AppBlockCount> = emptyList(),
    val dailyBlocks: List<DailyBlockCount> = emptyList(),
    val dailyScreenTime: List<DailyUsage> = emptyList(),
    val dailyUnlocks: List<DailyUnlock> = emptyList(),
    val firstPickups: List<PickupTime> = emptyList()
)

data class PickupTime(val day: Long, val timestamp: Long)
