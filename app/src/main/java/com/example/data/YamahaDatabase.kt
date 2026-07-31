package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ShopEntity::class,
        LineEntity::class,
        MachineEntity::class,
        PatrolPointEntity::class,
        PatrolPointRevisionEntity::class,
        PatrolLogEntity::class,
        PatrolPointResultEntity::class,
        AbnormalityEntity::class,
        AuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class YamahaDatabase : RoomDatabase() {

    abstract fun yamahaDao(): YamahaDao

    companion object {
        @Volatile
        private var INSTANCE: YamahaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): YamahaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YamahaDatabase::class.java,
                    "yamaha_patrol_database"
                )
                    .addCallback(YamahaDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class YamahaDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateSeedData(database.yamahaDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.yamahaDao()
                        if (dao.getUserByUsername("admin") == null) {
                            dao.insertUser(
                                UserEntity(
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
                            )
                        }
                    }
                }
            }

            suspend fun populateSeedData(dao: YamahaDao) {
                // 1. Seed Default Admin
                dao.insertUser(
                    UserEntity(
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
                )

                // 2. Seed Maintenance Engineer User
                dao.insertUser(
                    UserEntity(
                        employeeId = "YMH-ENG-102",
                        employeeName = "Ramesh Verma",
                        username = "ramesh.verma",
                        email = "ramesh.verma@yamaha-motor-india.com",
                        department = "Weld Shop Maintenance",
                        plant = "Sriperumbudur Plant 1",
                        role = "MAINTENANCE_ENGINEER",
                        passwordHash = "yamaha123",
                        status = "Active",
                        createdBy = "System Admin"
                    )
                )

                // 3. Seed Shops
                val weldShopId = dao.insertShop(ShopEntity(shopName = "Weld Shop")).toInt()
                val pressShopId = dao.insertShop(ShopEntity(shopName = "Press Shop")).toInt()
                val assemblyShopId = dao.insertShop(ShopEntity(shopName = "Assembly Shop")).toInt()

                // 4. Seed Lines for Weld Shop
                val frameLine1Id = dao.insertLine(LineEntity(shopId = weldShopId, shopName = "Weld Shop", lineName = "Frame Line 1")).toInt()
                val frameLine2Id = dao.insertLine(LineEntity(shopId = weldShopId, shopName = "Weld Shop", lineName = "Frame Line 2")).toInt()
                val fuelTankLineId = dao.insertLine(LineEntity(shopId = weldShopId, shopName = "Weld Shop", lineName = "Fuel Tank Line")).toInt()
                val swingarmLineId = dao.insertLine(LineEntity(shopId = weldShopId, shopName = "Weld Shop", lineName = "Swingarm Line")).toInt()

                // 5. Seed Machines
                val m1Id = dao.insertMachine(
                    MachineEntity(
                        lineId = frameLine1Id,
                        shopName = "Weld Shop",
                        lineName = "Frame Line 1",
                        machineName = "Frame Robot A",
                        machineType = "Robot Welding",
                        manufacturer = "OTC Daihen",
                        model = "FD-V8",
                        status = "Operational"
                    )
                ).toInt()

                val m2Id = dao.insertMachine(
                    MachineEntity(
                        lineId = frameLine1Id,
                        shopName = "Weld Shop",
                        lineName = "Frame Line 1",
                        machineName = "Yaskawa Robot 01",
                        machineType = "Robot Welding",
                        manufacturer = "Yaskawa",
                        model = "MA1440",
                        status = "Operational"
                    )
                ).toInt()

                val m3Id = dao.insertMachine(
                    MachineEntity(
                        lineId = frameLine2Id,
                        shopName = "Weld Shop",
                        lineName = "Frame Line 2",
                        machineName = "Spot Welding Machine",
                        machineType = "Spot Welding",
                        manufacturer = "Nash",
                        model = "SW-40",
                        status = "Maintenance Required"
                    )
                ).toInt()

                val m4Id = dao.insertMachine(
                    MachineEntity(
                        lineId = frameLine2Id,
                        shopName = "Weld Shop",
                        lineName = "Frame Line 2",
                        machineName = "Seam Welding Machine",
                        machineType = "Seam Welding",
                        manufacturer = "Yamaha-M",
                        model = "SM-200",
                        status = "Operational"
                    )
                ).toInt()

                val m5Id = dao.insertMachine(
                    MachineEntity(
                        lineId = fuelTankLineId,
                        shopName = "Weld Shop",
                        lineName = "Fuel Tank Line",
                        machineName = "Leak Testing Machine",
                        machineType = "Leak Testing",
                        manufacturer = "Aero",
                        model = "LT-50",
                        status = "Operational"
                    )
                ).toInt()

                val m6Id = dao.insertMachine(
                    MachineEntity(
                        lineId = swingarmLineId,
                        shopName = "Weld Shop",
                        lineName = "Swingarm Line",
                        machineName = "Nash Spot Welding Machine",
                        machineType = "Spot Welding",
                        manufacturer = "Nash",
                        model = "NSW-80",
                        status = "Operational"
                    )
                ).toInt()

                val m7Id = dao.insertMachine(
                    MachineEntity(
                        lineId = swingarmLineId,
                        shopName = "Weld Shop",
                        lineName = "Swingarm Line",
                        machineName = "WIDMA Fine Boring Machine",
                        machineType = "Fine Boring",
                        manufacturer = "WIDMA",
                        model = "FB-100",
                        status = "Operational"
                    )
                ).toInt()

                // 6. Seed Patrol Points for Frame Robot A (OTC Daihen FD-V8)
                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m1Id,
                        machineName = "Frame Robot A",
                        pointName = "Robot Home Position",
                        category = "Robot",
                        standardValue = "Robot returns to zero position without abnormal sound",
                        sequenceNo = 1,
                        frequency = "Every Shift",
                        active = true,
                        description = "Inspect robot axis zeroing and home position sensor LED"
                    )
                )

                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m1Id,
                        machineName = "Frame Robot A",
                        pointName = "Torch Cable Condition",
                        category = "Welding",
                        standardValue = "No damage, no excessive bending, insulation intact",
                        sequenceNo = 2,
                        frequency = "Every Shift",
                        active = true,
                        description = "Inspect MIG torch power conduit, water line, and insulation sleeve"
                    )
                )

                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m1Id,
                        machineName = "Frame Robot A",
                        pointName = "Spatter Condition",
                        category = "Quality",
                        standardValue = "Spatter within acceptable limit, nozzle tip clean",
                        sequenceNo = 3,
                        frequency = "Every Shift",
                        active = true,
                        description = "Verify automatic nozzle reamer anti-spatter spray cycle"
                    )
                )

                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m1Id,
                        machineName = "Frame Robot A",
                        pointName = "Gas Pressure & Flow Rate",
                        category = "Welding",
                        standardValue = "15 - 20 L/min Ar/CO2 blend, regulator at 4.5 bar",
                        sequenceNo = 4,
                        frequency = "Every Shift",
                        active = true,
                        description = "Check digital flowmeter and gas cylinder regulator pressure"
                    )
                )

                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m1Id,
                        machineName = "Frame Robot A",
                        pointName = "Servo Alarm Check",
                        category = "Electrical",
                        standardValue = "No active servo driver fault code on teaching pendant",
                        sequenceNo = 5,
                        frequency = "Every Shift",
                        active = true,
                        description = "Check OTC Daihen teach pendant error log and servo drive status"
                    )
                )

                // 7. Seed Patrol Points for Spot Welding Machine
                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m3Id,
                        machineName = "Spot Welding Machine",
                        pointName = "Electrode Tip Dress Condition",
                        category = "Quality",
                        standardValue = "Tip mushrooming < 0.5mm, redressed smooth surface",
                        sequenceNo = 1,
                        frequency = "Every Shift",
                        active = true,
                        description = "Check copper electrode tip diameter and wear profile"
                    )
                )

                dao.insertPatrolPoint(
                    PatrolPointEntity(
                        machineId = m3Id,
                        machineName = "Spot Welding Machine",
                        pointName = "Pneumatic Cylinder Pressure",
                        category = "Mechanical",
                        standardValue = "4.5 - 5.5 bar cylinder squeeze pressure",
                        sequenceNo = 2,
                        frequency = "Every Shift",
                        active = true,
                        description = "Check pneumatic pressure regulator gauge reading"
                    )
                )

                // 8. Seed Initial Patrol Log & Abnormality with photo reference
                val now = System.currentTimeMillis()
                val log1Id = dao.insertPatrolLog(
                    PatrolLogEntity(
                        shopName = "Weld Shop",
                        lineName = "Frame Line 1",
                        machineName = "Frame Robot A",
                        machineId = m1Id,
                        employeeId = "YMH-ENG-102",
                        employeeName = "Ramesh Verma",
                        shift = "Morning Shift (06:00 - 14:00)",
                        timestamp = now - 3600000 * 2,
                        overallStatus = "ABNORMAL",
                        notes = "Torch cable outer insulation showed minor rubbing wear on joint J3 axis."
                    )
                ).toInt()

                dao.insertPatrolPointResults(
                    listOf(
                        PatrolPointResultEntity(
                            patrolLogId = log1Id,
                            patrolPointId = 1,
                            checkpointName = "Robot Home Position",
                            category = "Robot",
                            standardValue = "Returns to zero position without abnormal sound",
                            status = "NORMAL",
                            remarks = "Home position verified normal"
                        ),
                        PatrolPointResultEntity(
                            patrolLogId = log1Id,
                            patrolPointId = 2,
                            checkpointName = "Torch Cable Condition",
                            category = "Welding",
                            standardValue = "No damage, no excessive bending",
                            status = "ABNORMAL",
                            remarks = "Rubbing wear on J3 axis conduit sleeve",
                            problemDescription = "Torch cable conduit rubbing against robot arm bracket during rotation",
                            severity = "HIGH",
                            countermeasure = "Adjust cable hanger spring tension and fit protective spiral wrap",
                            photoUri = "drawable/img_welding_machine_1785404965577"
                        )
                    )
                )

                dao.insertAbnormality(
                    AbnormalityEntity(
                        patrolLogId = log1Id,
                        shopName = "Weld Shop",
                        lineName = "Frame Line 1",
                        machineName = "Frame Robot A",
                        machineId = m1Id,
                        checkpointName = "Torch Cable Condition",
                        category = "Welding",
                        priority = "HIGH",
                        problemDescription = "Torch cable conduit rubbing against robot arm bracket during rotation",
                        rootCause = "Cable suspension spring slack due to extended duty cycles",
                        correctiveAction = "Re-tension cable hanger spring and install heavy-duty protective spiral wrap",
                        responsiblePerson = "Ramesh Verma (Maintenance Engineer)",
                        targetDate = "2026-08-02",
                        status = "IN_PROGRESS",
                        reportedBy = "Ramesh Verma (YMH-ENG-102)",
                        timestamp = now - 3600000 * 2,
                        photoUri = "drawable/img_welding_machine_1785404965577"
                    )
                )

                // Seed Audit Log
                dao.insertAuditLog(
                    AuditLogEntity(
                        employeeId = "YMH-ENG-102",
                        employeeName = "Ramesh Verma",
                        action = "PATROL_SUBMITTED",
                        module = "Patrol Entry",
                        details = "Executed Weld Shop patrol on Frame Robot A (OTC Daihen FD-V8)",
                        timestamp = now - 3600000 * 2
                    )
                )
            }
        }
    }
}

