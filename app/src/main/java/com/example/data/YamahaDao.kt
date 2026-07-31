package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface YamahaDao {

    // --- USER MANAGEMENT ---
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY employeeName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE employeeId = :employeeId")
    suspend fun deleteUser(employeeId: String)

    // --- SHOPS ---
    @Query("SELECT * FROM shops ORDER BY shopName ASC")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity): Long

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShop(shopId: Int)

    // --- LINES ---
    @Query("SELECT * FROM lines ORDER BY lineName ASC")
    fun getAllLines(): Flow<List<LineEntity>>

    @Query("SELECT * FROM lines WHERE shopId = :shopId ORDER BY lineName ASC")
    fun getLinesForShop(shopId: Int): Flow<List<LineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: LineEntity): Long

    @Update
    suspend fun updateLine(line: LineEntity)

    @Query("DELETE FROM lines WHERE id = :lineId")
    suspend fun deleteLine(lineId: Int)

    // --- MACHINES ---
    @Query("SELECT * FROM machines ORDER BY machineName ASC")
    fun getAllMachines(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE lineId = :lineId ORDER BY machineName ASC")
    fun getMachinesForLine(lineId: Int): Flow<List<MachineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: MachineEntity): Long

    @Update
    suspend fun updateMachine(machine: MachineEntity)

    @Query("DELETE FROM machines WHERE id = :machineId")
    suspend fun deleteMachine(machineId: Int)

    // --- PATROL POINTS ---
    @Query("SELECT * FROM patrol_points ORDER BY sequenceNo ASC")
    fun getAllPatrolPoints(): Flow<List<PatrolPointEntity>>

    @Query("SELECT * FROM patrol_points WHERE machineId = :machineId AND active = 1 ORDER BY sequenceNo ASC")
    fun getPointsForMachine(machineId: Int): Flow<List<PatrolPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatrolPoint(point: PatrolPointEntity): Long

    @Update
    suspend fun updatePatrolPoint(point: PatrolPointEntity)

    @Query("DELETE FROM patrol_points WHERE id = :pointId")
    suspend fun deletePatrolPoint(pointId: Int)

    // --- REVISION HISTORY ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointRevision(revision: PatrolPointRevisionEntity)

    @Query("SELECT * FROM patrol_point_revisions ORDER BY revisionDate DESC")
    fun getAllRevisions(): Flow<List<PatrolPointRevisionEntity>>

    // --- PATROL LOGS & RESULTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatrolLog(log: PatrolLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatrolPointResults(results: List<PatrolPointResultEntity>)

    @Query("SELECT * FROM patrol_logs ORDER BY timestamp DESC")
    fun getAllPatrolLogs(): Flow<List<PatrolLogEntity>>

    @Query("SELECT * FROM patrol_point_results WHERE patrolLogId = :logId")
    suspend fun getResultsForLog(logId: Int): List<PatrolPointResultEntity>

    // --- ABNORMALITY MANAGEMENT ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbnormality(abnormality: AbnormalityEntity): Long

    @Query("SELECT * FROM abnormalities ORDER BY timestamp DESC")
    fun getAllAbnormalities(): Flow<List<AbnormalityEntity>>

    @Query("SELECT * FROM abnormalities WHERE status = :status ORDER BY priority DESC, timestamp DESC")
    fun getAbnormalitiesByStatus(status: String): Flow<List<AbnormalityEntity>>

    @Update
    suspend fun updateAbnormality(abnormality: AbnormalityEntity)

    // --- AUDIT LOGS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    // --- STATS & COUNTS ---
    @Query("SELECT COUNT(*) FROM patrol_logs")
    fun getTotalPatrolsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM abnormalities WHERE status != 'VERIFIED'")
    fun getPendingAbnormalitiesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM abnormalities WHERE priority = 'CRITICAL' AND status != 'VERIFIED'")
    fun getCriticalIssuesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM machines WHERE status = 'Operational'")
    fun getOperationalMachinesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM machines")
    fun getTotalMachinesCount(): Flow<Int>
}

