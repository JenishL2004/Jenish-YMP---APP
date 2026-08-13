package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class YamahaRepository(private val dao: YamahaDao) {

    val supabaseService = SupabaseService()

    suspend fun syncFromSupabase() = withContext(Dispatchers.IO) {
        if (!supabaseService.isConfigured()) return@withContext
        try {
            // 1. Sync Users
            val remoteUsers = supabaseService.fetchUsers()
            if (remoteUsers != null) {
                if (remoteUsers.isNotEmpty()) {
                    remoteUsers.forEach { dao.insertUser(it) }
                } else {
                    dao.getAllUsersDirect().forEach { supabaseService.upsertUser(it) }
                }
            }

            // 2. Sync Shops
            val remoteShops = supabaseService.fetchShops()
            if (remoteShops != null) {
                if (remoteShops.isNotEmpty()) {
                    remoteShops.forEach { dao.insertShop(it) }
                } else {
                    dao.getAllShopsDirect().forEach { supabaseService.upsertShop(it) }
                }
            }

            // 3. Sync Lines
            val remoteLines = supabaseService.fetchLines()
            if (remoteLines != null) {
                if (remoteLines.isNotEmpty()) {
                    remoteLines.forEach { dao.insertLine(it) }
                } else {
                    dao.getAllLinesDirect().forEach { supabaseService.upsertLine(it) }
                }
            }

            // 4. Sync Machines
            val remoteMachines = supabaseService.fetchMachines()
            if (remoteMachines != null) {
                if (remoteMachines.isNotEmpty()) {
                    remoteMachines.forEach { dao.insertMachine(it) }
                } else {
                    dao.getAllMachinesDirect().forEach { supabaseService.upsertMachine(it) }
                }
            }

            // 5. Sync Patrol Points
            val remotePoints = supabaseService.fetchPatrolPoints()
            if (remotePoints != null) {
                if (remotePoints.isNotEmpty()) {
                    remotePoints.forEach { dao.insertPatrolPoint(it) }
                } else {
                    dao.getAllPatrolPointsDirect().forEach { supabaseService.upsertPatrolPoint(it) }
                }
            }

            // 6. Sync Patrol Logs
            val remoteLogs = supabaseService.fetchPatrolLogs()
            if (remoteLogs != null) {
                if (remoteLogs.isNotEmpty()) {
                    remoteLogs.forEach { dao.insertPatrolLog(it) }
                } else {
                    dao.getAllPatrolLogsDirect().forEach { supabaseService.upsertPatrolLog(it) }
                }
            }

            // 7. Sync Abnormalities
            val remoteAbnormalities = supabaseService.fetchAbnormalities()
            if (remoteAbnormalities != null) {
                if (remoteAbnormalities.isNotEmpty()) {
                    remoteAbnormalities.forEach { dao.insertAbnormality(it) }
                } else {
                    dao.getAllAbnormalitiesDirect().forEach { supabaseService.upsertAbnormality(it) }
                }
            }

        } catch (e: Exception) {
            Log.w("YamahaRepository", "Sync from Supabase failed", e)
        }
    }

    // --- Authentication ---
    suspend fun authenticateUser(username: String, passwordAttempt: String): UserEntity? {
        val cleanUsername = username.trim().lowercase().removeSuffix("@yamaha-motor-india.com")
        var user = dao.getUserByUsername(cleanUsername)

        // Try syncing users if not found locally
        if (user == null && supabaseService.isConfigured()) {
            val remoteUsers = supabaseService.fetchUsers()
            remoteUsers?.forEach { dao.insertUser(it) }
            user = dao.getUserByUsername(cleanUsername)
        }
        
        // Ensure default Super Admin exists if database was initialized previously without it
        if (user == null && cleanUsername == "admin") {
            val defaultAdmin = UserEntity(
                employeeId = "YMH-ADM-001",
                employeeName = "System Super Admin",
                username = "admin",
                email = "admin@yamaha-motor-india.com",
                department = "Maintenance Engineering",
                plant = "Sriperumbudur Plant 1",
                role = "ADMIN",
                passwordHash = "Admin@123",
                status = "Active",
                createdBy = "System Installer"
            )
            dao.insertUser(defaultAdmin)
            supabaseService.upsertUser(defaultAdmin)
            user = defaultAdmin
        }

        if (user != null) {
            val trimmedPass = passwordAttempt.trim()
            if (user.passwordHash == trimmedPass) {
                return user
            }
        }
        return null
    }

    suspend fun clearTransactionalData() {
        dao.deleteAllPatrolPointResults()
        dao.deleteAllPatrolLogs()
        dao.deleteAllAbnormalities()
        dao.deleteAllAuditLogs()
        if (supabaseService.isConfigured()) {
            supabaseService.deleteAllPatrolLogs()
            supabaseService.deleteAllAbnormalities()
        }
    }

    // --- User CRUD ---
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    suspend fun insertUser(user: UserEntity) {
        dao.insertUser(user)
        supabaseService.upsertUser(user)
    }
    suspend fun updateUser(user: UserEntity) {
        dao.updateUser(user)
        supabaseService.upsertUser(user)
    }
    suspend fun deleteUser(employeeId: String) {
        dao.deleteUser(employeeId)
        supabaseService.deleteUser(employeeId)
    }

    // --- Shops ---
    val allShops: Flow<List<ShopEntity>> = dao.getAllShops()
    suspend fun insertShop(shop: ShopEntity) {
        val id = dao.insertShop(shop).toInt()
        val created = shop.copy(id = if (shop.id > 0) shop.id else id)
        supabaseService.upsertShop(created)
    }
    suspend fun updateShop(shop: ShopEntity) {
        dao.updateShop(shop)
        supabaseService.upsertShop(shop)
    }
    suspend fun deleteShop(shopId: Int) {
        dao.deleteShop(shopId)
        supabaseService.deleteShop(shopId)
    }

    // --- Lines ---
    val allLines: Flow<List<LineEntity>> = dao.getAllLines()
    fun getLinesForShop(shopId: Int): Flow<List<LineEntity>> = dao.getLinesForShop(shopId)
    suspend fun insertLine(line: LineEntity) {
        val id = dao.insertLine(line).toInt()
        val created = line.copy(id = if (line.id > 0) line.id else id)
        supabaseService.upsertLine(created)
    }
    suspend fun updateLine(line: LineEntity) {
        dao.updateLine(line)
        supabaseService.upsertLine(line)
    }
    suspend fun deleteLine(lineId: Int) {
        dao.deleteLine(lineId)
        supabaseService.deleteLine(lineId)
    }

    // --- Machines ---
    val allMachines: Flow<List<MachineEntity>> = dao.getAllMachines()
    fun getMachinesForLine(lineId: Int): Flow<List<MachineEntity>> = dao.getMachinesForLine(lineId)
    suspend fun insertMachine(machine: MachineEntity) {
        val id = dao.insertMachine(machine).toInt()
        val created = machine.copy(id = if (machine.id > 0) machine.id else id)
        supabaseService.upsertMachine(created)
    }
    suspend fun updateMachine(machine: MachineEntity) {
        dao.updateMachine(machine)
        supabaseService.upsertMachine(machine)
    }
    suspend fun deleteMachine(machineId: Int) {
        dao.deleteMachine(machineId)
        supabaseService.deleteMachine(machineId)
    }

    // --- Patrol Points ---
    val allPatrolPoints: Flow<List<PatrolPointEntity>> = dao.getAllPatrolPoints()
    fun getPointsForMachine(machineId: Int): Flow<List<PatrolPointEntity>> = dao.getPointsForMachine(machineId)
    suspend fun insertPatrolPoint(point: PatrolPointEntity) {
        val id = dao.insertPatrolPoint(point).toInt()
        val created = point.copy(id = if (point.id > 0) point.id else id)
        supabaseService.upsertPatrolPoint(created)
    }
    suspend fun updatePatrolPoint(point: PatrolPointEntity) {
        dao.updatePatrolPoint(point)
        supabaseService.upsertPatrolPoint(point)
    }
    suspend fun deletePatrolPoint(pointId: Int) {
        dao.deletePatrolPoint(pointId)
        supabaseService.deletePatrolPoint(pointId)
    }

    val allRevisions: Flow<List<PatrolPointRevisionEntity>> = dao.getAllRevisions()
    suspend fun revisePatrolPoint(
        point: PatrolPointEntity,
        newStandardValue: String,
        newCategory: String,
        newFrequency: String,
        reason: String,
        revisedBy: String
    ) {
        val oldSummary = "Category: ${point.category}, Standard: ${point.standardValue}, Freq: ${point.frequency}"
        val newSummary = "Category: $newCategory, Standard: $newStandardValue, Freq: $newFrequency"
        val nextRev = point.revisionNumber + 1

        val updatedPoint = point.copy(
            standardValue = newStandardValue,
            category = newCategory,
            frequency = newFrequency,
            revisionNumber = nextRev
        )
        dao.updatePatrolPoint(updatedPoint)
        supabaseService.upsertPatrolPoint(updatedPoint)

        val revisionLog = PatrolPointRevisionEntity(
            pointId = point.id,
            revisionNumber = nextRev,
            revisionDate = System.currentTimeMillis(),
            revisedBy = revisedBy,
            reason = reason,
            oldValue = oldSummary,
            newValue = newSummary
        )
        dao.insertPointRevision(revisionLog)
    }

    // --- Photo Evidence Upload ---
    suspend fun uploadEvidencePhoto(context: Context, localUri: Uri): String? {
        return supabaseService.uploadEvidencePhoto(context, localUri)
    }

    // --- Patrol Execution ---
    val allPatrolLogs: Flow<List<PatrolLogEntity>> = dao.getAllPatrolLogs()

    suspend fun submitPatrolLog(
        log: PatrolLogEntity,
        results: List<PatrolPointResultEntity>,
        abnormality: AbnormalityEntity?
    ): Long {
        val logId = dao.insertPatrolLog(log).toInt()
        val updatedLog = log.copy(id = logId)
        supabaseService.upsertPatrolLog(updatedLog)

        val mappedResults = results.map { it.copy(patrolLogId = logId) }
        dao.insertPatrolPointResults(mappedResults)

        if (abnormality != null) {
            val createdAbnormality = abnormality.copy(patrolLogId = logId)
            val abId = dao.insertAbnormality(createdAbnormality).toInt()
            val finalAbnormality = createdAbnormality.copy(id = if (createdAbnormality.id > 0) createdAbnormality.id else abId)
            supabaseService.upsertAbnormality(finalAbnormality)
        }

        // Add audit entry
        dao.insertAuditLog(
            AuditLogEntity(
                employeeId = log.employeeId,
                employeeName = log.employeeName,
                action = "PATROL_SUBMITTED",
                module = "Patrol Entry",
                details = "Submitted maintenance patrol for ${log.machineName} on ${log.lineName} (${log.overallStatus})"
            )
        )
        return logId.toLong()
    }

    suspend fun getResultsForLog(logId: Int): List<PatrolPointResultEntity> {
        return dao.getResultsForLog(logId)
    }

    // --- Abnormality Management ---
    val allAbnormalities: Flow<List<AbnormalityEntity>> = dao.getAllAbnormalities()
    fun getAbnormalitiesByStatus(status: String) = dao.getAbnormalitiesByStatus(status)

    suspend fun updateAbnormality(abnormality: AbnormalityEntity, updatedBy: UserEntity) {
        dao.updateAbnormality(abnormality)
        supabaseService.upsertAbnormality(abnormality)
        dao.insertAuditLog(
            AuditLogEntity(
                employeeId = updatedBy.employeeId,
                employeeName = updatedBy.employeeName,
                action = "ABNORMALITY_UPDATED",
                module = "Abnormality Management",
                details = "Updated abnormality #${abnormality.id} status to ${abnormality.status}"
            )
        )
    }

    // --- Audit Logs ---
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    suspend fun logAudit(user: UserEntity, action: String, module: String, details: String) {
        dao.insertAuditLog(
            AuditLogEntity(
                employeeId = user.employeeId,
                employeeName = user.employeeName,
                action = action,
                module = module,
                details = details
            )
        )
    }

    // --- Dashboard Counts ---
    val totalPatrolsCount: Flow<Int> = dao.getTotalPatrolsCount()
    val pendingAbnormalitiesCount: Flow<Int> = dao.getPendingAbnormalitiesCount()
    val criticalIssuesCount: Flow<Int> = dao.getCriticalIssuesCount()
    val operationalMachinesCount: Flow<Int> = dao.getOperationalMachinesCount()
    val totalMachinesCount: Flow<Int> = dao.getTotalMachinesCount()
}
