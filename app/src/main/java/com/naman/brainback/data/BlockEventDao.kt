package com.naman.brainback.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEvent)

    @Query("SELECT * FROM block_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<BlockEvent>

    @Query("SELECT packageName, appLabel, COUNT(*) as count FROM block_events WHERE timestamp >= :since GROUP BY packageName ORDER BY count DESC")
    suspend fun getGroupedCountsSince(since: Long): List<AppBlockCount>

    @Query("SELECT (timestamp / 86400000) * 86400000 as day, COUNT(*) as count FROM block_events WHERE timestamp >= :since GROUP BY day ORDER BY day ASC")
    suspend fun getDailyCountsSince(since: Long): List<DailyBlockCount>
}

data class AppBlockCount(
    val packageName: String,
    val appLabel: String,
    val count: Int
)

data class DailyBlockCount(
    val day: Long,
    val count: Int
)
