package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "protected_apps")
data class ProtectedApp(
    @PrimaryKey val packageName: String,
    val lockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val details: String
)

@Dao
interface AppLockDao {

    // Protected Apps
    @Query("SELECT * FROM protected_apps")
    fun getProtectedAppsFlow(): Flow<List<ProtectedApp>>

    @Query("SELECT packageName FROM protected_apps")
    suspend fun getProtectedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun protectApp(app: ProtectedApp)

    @Query("DELETE FROM protected_apps WHERE packageName = :packageName")
    suspend fun unprotectApp(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM protected_apps WHERE packageName = :packageName)")
    suspend fun isAppProtected(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtectedApps(apps: List<ProtectedApp>)

    @Query("DELETE FROM protected_apps")
    suspend fun clearAllProtectedApps()

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun getActivityLogsFlow(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

@Database(entities = [ProtectedApp::class, ActivityLog::class], version = 1, exportSchema = false)
abstract class AppLockDatabase : RoomDatabase() {
    abstract fun appLockDao(): AppLockDao
}
