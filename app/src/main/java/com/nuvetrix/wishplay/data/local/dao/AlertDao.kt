package com.nuvetrix.wishplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nuvetrix.wishplay.data.local.entity.RecentAlertEntity
import com.nuvetrix.wishplay.data.local.entity.ScheduledAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM scheduled_alerts WHERE isFired = 0 ORDER BY targetTimeMillis ASC")
    fun observePendingScheduledAlerts(): Flow<List<ScheduledAlertEntity>>

    @Query("SELECT * FROM scheduled_alerts WHERE isFired = 0")
    suspend fun getPendingScheduledAlerts(): List<ScheduledAlertEntity>

    @Query("SELECT * FROM scheduled_alerts WHERE gameId = :gameId AND isFired = 0")
    suspend fun getPendingAlertsForGame(gameId: String): List<ScheduledAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledAlerts(alerts: List<ScheduledAlertEntity>)

    @Query("DELETE FROM scheduled_alerts WHERE gameId = :gameId")
    suspend fun deleteAlertsForGame(gameId: String)

    @Query("UPDATE scheduled_alerts SET isFired = 1 WHERE id = :alertId")
    suspend fun markAlertFired(alertId: String)

    @Query("SELECT * FROM recent_alerts ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentAlerts(): Flow<List<RecentAlertEntity>>

    @Query("SELECT * FROM recent_alerts ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentAlerts(): List<RecentAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentAlert(alert: RecentAlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentAlerts(alerts: List<RecentAlertEntity>)

    @Query("DELETE FROM recent_alerts WHERE id = :id")
    suspend fun deleteRecentAlert(id: String)
}
