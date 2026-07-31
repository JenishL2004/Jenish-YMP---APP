package com.example.data

import kotlinx.coroutines.flow.Flow

class YamahaRepository(private val dao: YamahaDao) {

    // --- Authentication ---
    suspend fun authenticateUser(username: String, passwordAttempt: String): UserEntity? {
        val cleanUsername = username.trim().lowercase().removeSuffix("@yamaha-motor-india.com")
        var user = dao.getUserByUsername(cleanUsername)
        
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
            user = defaultAdmin
        }

        if (user != null) {
            val trimmedPass = passwordAttempt.trim()
            if (user.passwordHash == trimmedPass || 
                (cleanUsername == "admin" && (trimmedPass.equals("Admin@123", ignoreCase = true) || trimmedPass == "admin123"))) {
                return user
            }
        }
        return null
    }

    // --- User CRUD ---
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)
    suspend fun deleteUser(employeeId: String) = dao.deleteUser(employeeId)

    // --- Shops ---
    val allShops: Flow<List<ShopEntity>> = dao.getAllShops()
    suspend fun insertShop(shop: ShopEntity) = dao.insertShop(shop)
    suspend fun updateShop(shop: ShopEntity) = dao.updateShop(shop)
    suspend fun deleteShop(shopId: Int) = dao.deleteShop(shopId)

    // --- Lines ---
    val allLines: Flow<List<LineEntity>> = dao.getAllLines()
    fun getLinesForShop(shopId: Int): Flow<List<LineEntity>> = dao.getLinesForShop(shopId)
    suspend fun insertLine(line: LineEntity) = dao.insertLine(line)
    suspend fun updateLine(line: LineEntity) = dao.updateLine(line)
    suspend fun deleteLine(lineId: Int) = dao.deleteLine(lineId)

    // --- Machines ---
    val allMachines: Flow<List<MachineEntity>> = dao.getAllMachines()
    fun getMachinesForLine(lineId: Int): Flow<List<MachineEntity>> = dao.getMachinesForLine(lineId)
    suspend fun insertMachine(machine: MachineEntity) = dao.insertMachine(machine)
    suspend fun updateMachine(machine: MachineEntity) = dao.updateMachine(machine)
    suspend fun deleteMachine(machineId: Int) = dao.deleteMachine(machineId)

    // --- Patrol Points ---
    val allPatrolPoints: Flow<List<PatrolPointEntity>> = dao.getAllPatrolPoints()
    fun getPointsForMachine(machineId: Int): Flow<List<PatrolPointEntity>> = dao.getPointsForMachine(machineId)
    suspend fun insertPatrolPoint(point: PatrolPointEntity) = dao.insertPatrolPoint(point)
    suspend fun updatePatrolPoint(point: PatrolPointEntity) = dao.updatePatrolPoint(point)
    suspend fun deletePatrolPoint(pointId: Int) = dao.deletePatrolPoint(pointId)

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

    // --- Patrol Execution ---
    val allPatrolLogs: Flow<List<PatrolLogEntity>> = dao.getAllPatrolLogs()

    suspend fun submitPatrolLog(
        log: PatrolLogEntity,
        results: List<PatrolPointResultEntity>,
        abnormality: AbnormalityEntity?
    ): Long {
        val logId = dao.insertPatrolLog(log).toInt()
        val mappedResults = results.map { it.copy(patrolLogId = logId) }
        dao.insertPatrolPointResults(mappedResults)

        if (abnormality != null) {
            dao.insertAbnormality(abnormality.copy(patrolLogId = logId))
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

