package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Workspace Entity
@Entity(tableName = "workspaces")
data class Workspace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val target: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// 2. ScanLog Entity
@Entity(
    tableName = "scan_logs",
    foreignKeys = [
        ForeignKey(
            entity = Workspace::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workspaceId")]
)
data class ScanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workspaceId: Int,
    val moduleName: String,
    val tier: String, // FREE, PREMIUM, ULTRA
    val target: String,
    val timestamp: Long = System.currentTimeMillis(),
    val output: String,
    val isSuccess: Boolean = true
)

// 3. ScheduledScan Entity
@Entity(
    tableName = "scheduled_scans",
    foreignKeys = [
        ForeignKey(
            entity = Workspace::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workspaceId")]
)
data class ScheduledScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workspaceId: Int,
    val moduleName: String,
    val target: String,
    val intervalMinutes: Int = 60,
    val nextRunAt: Long = System.currentTimeMillis(),
    val webhookUrl: String = "",
    val isActive: Boolean = true
)

// 4. Data Access Object (DAO)
@Dao
interface AppDao {
    // Workspaces
    @Query("SELECT * FROM workspaces ORDER BY createdAt DESC")
    fun getAllWorkspaces(): Flow<List<Workspace>>

    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceById(id: Int): Workspace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: Workspace): Long

    @Delete
    suspend fun deleteWorkspace(workspace: Workspace)

    // Scan Logs
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getAllScanLogs(): Flow<List<ScanLog>>

    @Query("SELECT * FROM scan_logs WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    fun getScanLogsForWorkspace(workspaceId: Int): Flow<List<ScanLog>>

    @Query("SELECT * FROM scan_logs WHERE id = :id LIMIT 1")
    suspend fun getScanLogById(id: Int): ScanLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanLog(scanLog: ScanLog): Long

    @Delete
    suspend fun deleteScanLog(scanLog: ScanLog)

    @Query("DELETE FROM scan_logs WHERE workspaceId = :workspaceId")
    suspend fun clearScanLogsForWorkspace(workspaceId: Int)

    // Scheduled Scans
    @Query("SELECT * FROM scheduled_scans ORDER BY id DESC")
    fun getAllScheduledScans(): Flow<List<ScheduledScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledScan(scan: ScheduledScan): Long

    @Update
    suspend fun updateScheduledScan(scan: ScheduledScan)

    @Delete
    suspend fun deleteScheduledScan(scan: ScheduledScan)
}

// 5. Database Class
@Database(
    entities = [Workspace::class, ScanLog::class, ScheduledScan::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
