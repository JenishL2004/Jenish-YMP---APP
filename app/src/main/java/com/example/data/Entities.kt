package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val employeeId: String,
    val employeeName: String,
    val username: String,
    val email: String,
    val department: String,
    val plant: String,
    val role: String, // ADMIN, SUPERVISOR, MAINTENANCE_ENGINEER, OPERATOR
    val passwordHash: String,
    val status: String = "Active",
    val createdBy: String = "System",
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String
)

@Entity(tableName = "lines")
data class LineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int = 1,
    val shopName: String = "Weld Shop",
    val lineName: String
)

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lineId: Int = 1,
    val shopName: String = "Weld Shop",
    val lineName: String,
    val machineName: String,
    val machineType: String,
    val manufacturer: String,
    val model: String,
    val status: String = "Operational" // Operational, Under Patrol, Maintenance Required, Stopped
)

@Entity(tableName = "patrol_points")
data class PatrolPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val machineId: Int = 1,
    val machineName: String = "",
    val pointName: String,
    val category: String = "Robot", // Robot, Welding, Quality, Electrical, Mechanical, Safety
    val standardValue: String,
    val sequenceNo: Int = 1,
    val frequency: String = "Every Shift",
    val active: Boolean = true,
    val description: String = "",
    val revisionNumber: Int = 1
)

@Entity(tableName = "patrol_point_revisions")
data class PatrolPointRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pointId: Int,
    val revisionNumber: Int,
    val revisionDate: Long = System.currentTimeMillis(),
    val revisedBy: String,
    val reason: String,
    val oldValue: String,
    val newValue: String
)

@Entity(tableName = "patrol_logs")
data class PatrolLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patrolNumber: String = "PTL-${System.currentTimeMillis()}",
    val shopName: String = "Weld Shop",
    val lineName: String,
    val machineName: String,
    val machineId: Int = 0,
    val employeeId: String,
    val employeeName: String,
    val shift: String = "Morning Shift",
    val timestamp: Long = System.currentTimeMillis(),
    val overallStatus: String = "NORMAL", // NORMAL, ABNORMAL
    val notes: String = ""
)

@Entity(tableName = "patrol_point_results")
data class PatrolPointResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patrolLogId: Int,
    val patrolPointId: Int = 0,
    val checkpointName: String,
    val category: String = "Welding",
    val standardValue: String,
    val status: String, // NORMAL, ABNORMAL
    val remarks: String = "",
    val problemDescription: String = "",
    val severity: String = "HIGH",
    val countermeasure: String = "",
    val photoUri: String? = null
)

@Entity(tableName = "abnormalities")
data class AbnormalityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val abnormalityNumber: String = "ABN-${System.currentTimeMillis()}",
    val patrolLogId: Int = 0,
    val shopName: String = "Weld Shop",
    val lineName: String,
    val machineName: String,
    val machineId: Int = 0,
    val checkpointName: String,
    val category: String = "Welding",
    val priority: String = "HIGH", // CRITICAL, HIGH, MEDIUM, LOW
    val problemDescription: String = "",
    val rootCause: String = "",
    val correctiveAction: String = "",
    val responsiblePerson: String = "Maintenance Engineer",
    val targetDate: String = "2026-08-05",
    val completedDate: String? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, RESOLVED, VERIFIED
    val reportedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoUri: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val employeeName: String,
    val action: String, // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, DOWNLOAD, PRINT, PATROL_SUBMIT
    val module: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

