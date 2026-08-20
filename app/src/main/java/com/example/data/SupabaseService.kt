package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class SupabaseService(
    private var baseUrl: String = "https://yknlmdylmveqmsffewoo.supabase.co",
    private var apiKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlrbmxtZHlsbXZlcW1zZmZld29vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MjAxNTA0MDAwMH0.tXqW6N9vP8uK2mF5rT4yZ8wE1qL0sV3bX7nC9mM4pL2"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val urlField = buildConfigClass.getField("SUPABASE_URL")
            val keyField = buildConfigClass.getField("SUPABASE_ANON_KEY")
            val urlVal = urlField.get(null) as? String
            val keyVal = keyField.get(null) as? String
            if (!urlVal.isNullOrBlank() && urlVal.startsWith("http")) {
                baseUrl = urlVal.trimEnd('/')
            }
            if (!keyVal.isNullOrBlank() && keyVal.length > 20) {
                apiKey = keyVal.trim()
            }
        } catch (e: Exception) {
            Log.d("SupabaseService", "BuildConfig Supabase fields not present, using default configuration")
        }
    }

    fun configure(url: String, key: String) {
        if (url.isNotBlank() && url.startsWith("http")) {
            this.baseUrl = url.trimEnd('/')
        }
        if (key.isNotBlank()) {
            this.apiKey = key.trim()
        }
    }

    fun isConfigured(): Boolean {
        return baseUrl.isNotBlank() &&
                baseUrl.startsWith("http") &&
                apiKey.isNotBlank() &&
                apiKey.length > 20
    }

    fun getBaseUrl(): String = baseUrl
    fun getApiKey(): String = apiKey

    private fun buildRequest(endpoint: String, method: String, body: String? = null, preferHeader: String? = null): Request {
        val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl/rest/v1/$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")

        preferHeader?.let { builder.addHeader("Prefer", it) }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "{}").toRequestBody(jsonMediaType))
            "PUT" -> builder.put((body ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((body ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete()
        }
        return builder.build()
    }

    // --- USERS ---
    suspend fun fetchUsers(): List<UserEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("users?select=*", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch users failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<UserEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UserEntity(
                            employeeId = obj.optString("employee_id"),
                            employeeName = obj.optString("employee_name"),
                            username = obj.optString("username"),
                            email = obj.optString("email"),
                            department = obj.optString("department"),
                            plant = obj.optString("plant"),
                            role = obj.optString("role"),
                            passwordHash = obj.optString("password_hash"),
                            status = obj.optString("status", "Active"),
                            createdBy = obj.optString("created_by")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch users from Supabase", e)
            null
        }
    }

    suspend fun upsertUser(user: UserEntity): UserEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                put("employee_id", user.employeeId)
                put("employee_name", user.employeeName)
                put("username", user.username)
                put("email", user.email)
                put("department", user.department)
                put("plant", user.plant)
                put("role", user.role)
                put("password_hash", user.passwordHash)
                put("status", user.status)
                put("created_by", user.createdBy)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("users", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    user
                } else {
                    Log.w("SupabaseService", "Upsert user failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert user to Supabase", e)
            null
        }
    }

    suspend fun deleteUser(employeeId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("users?employee_id=eq.$employeeId", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to delete user from Supabase", e)
            false
        }
    }

    // --- SHOPS ---
    suspend fun fetchShops(): List<ShopEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("shops?select=*&order=id.asc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch shops failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<ShopEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ShopEntity(
                            id = obj.optInt("id"),
                            shopName = obj.optString("shop_name")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch shops from Supabase", e)
            null
        }
    }

    suspend fun upsertShop(shop: ShopEntity): ShopEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (shop.id > 0) put("id", shop.id)
                put("shop_name", shop.shopName)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("shops", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = shop.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", shop.id)
                        }
                    }
                    shop.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert shop failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert shop to Supabase", e)
            null
        }
    }

    suspend fun deleteShop(shopId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("shops?id=eq.$shopId", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to delete shop from Supabase", e)
            false
        }
    }

    // --- LINES ---
    suspend fun fetchLines(): List<LineEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("lines?select=*&order=id.asc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch lines failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<LineEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LineEntity(
                            id = obj.optInt("id"),
                            shopId = obj.optInt("shop_id", 1),
                            shopName = obj.optString("shop_name", "Weld Shop"),
                            lineName = obj.optString("line_name")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch lines from Supabase", e)
            null
        }
    }

    suspend fun upsertLine(line: LineEntity): LineEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (line.id > 0) put("id", line.id)
                put("shop_id", line.shopId)
                put("shop_name", line.shopName)
                put("line_name", line.lineName)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("lines", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = line.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", line.id)
                        }
                    }
                    line.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert line failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert line to Supabase", e)
            null
        }
    }

    suspend fun deleteLine(lineId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("lines?id=eq.$lineId", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to delete line from Supabase", e)
            false
        }
    }

    // --- MACHINES ---
    suspend fun fetchMachines(): List<MachineEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("machines?select=*&order=id.asc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch machines failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<MachineEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MachineEntity(
                            id = obj.optInt("id"),
                            lineId = obj.optInt("line_id", 1),
                            shopName = obj.optString("shop_name", "Weld Shop"),
                            lineName = obj.optString("line_name"),
                            machineName = obj.optString("machine_name"),
                            machineType = obj.optString("machine_type"),
                            manufacturer = obj.optString("manufacturer"),
                            model = obj.optString("model"),
                            status = obj.optString("status", "Operational")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch machines from Supabase", e)
            null
        }
    }

    suspend fun upsertMachine(machine: MachineEntity): MachineEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (machine.id > 0) put("id", machine.id)
                put("line_id", machine.lineId)
                put("shop_name", machine.shopName)
                put("line_name", machine.lineName)
                put("machine_name", machine.machineName)
                put("machine_type", machine.machineType)
                put("manufacturer", machine.manufacturer)
                put("model", machine.model)
                put("status", machine.status)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("machines", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = machine.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", machine.id)
                        }
                    }
                    machine.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert machine failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert machine to Supabase", e)
            null
        }
    }

    suspend fun deleteMachine(machineId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("machines?id=eq.$machineId", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to delete machine from Supabase", e)
            false
        }
    }

    // --- PATROL POINTS ---
    suspend fun fetchPatrolPoints(): List<PatrolPointEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("patrol_points?select=*&order=sequence_no.asc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch patrol points failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<PatrolPointEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PatrolPointEntity(
                            id = obj.optInt("id"),
                            machineId = obj.optInt("machine_id", 1),
                            machineName = obj.optString("machine_name"),
                            pointName = obj.optString("point_name"),
                            category = obj.optString("category", "Robot"),
                            standardValue = obj.optString("standard_value"),
                            sequenceNo = obj.optInt("sequence_no", 1),
                            frequency = obj.optString("frequency", "Every Shift"),
                            active = obj.optBoolean("active", true),
                            revisionNumber = obj.optInt("revision_number", 1),
                            description = obj.optString("description")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch patrol points from Supabase", e)
            null
        }
    }

    suspend fun upsertPatrolPoint(point: PatrolPointEntity): PatrolPointEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (point.id > 0) put("id", point.id)
                put("machine_id", point.machineId)
                put("machine_name", point.machineName)
                put("point_name", point.pointName)
                put("category", point.category)
                put("standard_value", point.standardValue)
                put("sequence_no", point.sequenceNo)
                put("frequency", point.frequency)
                put("active", point.active)
                put("revision_number", point.revisionNumber)
                put("description", point.description)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("patrol_points", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = point.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", point.id)
                        }
                    }
                    point.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert patrol point failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert patrol point to Supabase", e)
            null
        }
    }

    suspend fun deletePatrolPoint(pointId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("patrol_points?id=eq.$pointId", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to delete patrol point from Supabase", e)
            false
        }
    }

    // --- REVISIONS ---
    suspend fun fetchRevisions(): List<PatrolPointRevisionEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("patrol_point_revisions?select=*&order=revision_date.desc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch revisions failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<PatrolPointRevisionEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PatrolPointRevisionEntity(
                            id = obj.optInt("id"),
                            pointId = obj.optInt("point_id"),
                            revisionNumber = obj.optInt("revision_number", 1),
                            revisionDate = obj.optLong("revision_date", System.currentTimeMillis()),
                            revisedBy = obj.optString("revised_by"),
                            reason = obj.optString("reason"),
                            oldValue = obj.optString("old_value"),
                            newValue = obj.optString("new_value")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch revisions from Supabase", e)
            null
        }
    }

    suspend fun upsertRevision(rev: PatrolPointRevisionEntity): PatrolPointRevisionEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (rev.id > 0) put("id", rev.id)
                put("point_id", rev.pointId)
                put("revision_number", rev.revisionNumber)
                put("revision_date", rev.revisionDate)
                put("revised_by", rev.revisedBy)
                put("reason", rev.reason)
                put("old_value", rev.oldValue)
                put("new_value", rev.newValue)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("patrol_point_revisions", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = rev.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", rev.id)
                        }
                    }
                    rev.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert revision failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert revision to Supabase", e)
            null
        }
    }

    // --- PATROL LOGS ---
    suspend fun fetchPatrolLogs(): List<PatrolLogEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("patrol_logs?select=*&order=timestamp.desc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch patrol logs failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<PatrolLogEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PatrolLogEntity(
                            id = obj.optInt("id"),
                            patrolNumber = obj.optString("patrol_number", "PTL-${obj.optLong("timestamp", System.currentTimeMillis())}"),
                            shopName = obj.optString("shop_name", "Weld Shop"),
                            lineName = obj.optString("line_name"),
                            machineName = obj.optString("machine_name"),
                            machineId = obj.optInt("machine_id"),
                            employeeId = obj.optString("employee_id"),
                            employeeName = obj.optString("employee_name"),
                            shift = obj.optString("shift"),
                            timestamp = obj.optLong("timestamp"),
                            overallStatus = obj.optString("overall_status"),
                            notes = obj.optString("notes")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch patrol logs from Supabase", e)
            null
        }
    }

    suspend fun upsertPatrolLog(log: PatrolLogEntity): PatrolLogEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (log.id > 0) put("id", log.id)
                put("patrol_number", log.patrolNumber)
                put("shop_name", log.shopName)
                put("line_name", log.lineName)
                put("machine_name", log.machineName)
                put("machine_id", log.machineId)
                put("employee_id", log.employeeId)
                put("employee_name", log.employeeName)
                put("shift", log.shift)
                put("timestamp", log.timestamp)
                put("overall_status", log.overallStatus)
                put("notes", log.notes)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("patrol_logs", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string()
                    var finalId = log.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", log.id)
                        }
                    }
                    log.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert patrol log failed: ${resp.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert patrol log to Supabase", e)
            null
        }
    }

    // --- PATROL POINT RESULTS ---
    suspend fun fetchPatrolPointResults(): List<PatrolPointResultEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("patrol_point_results?select=*", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch patrol point results failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<PatrolPointResultEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PatrolPointResultEntity(
                            id = obj.optInt("id"),
                            patrolLogId = obj.optInt("patrol_log_id"),
                            patrolPointId = obj.optInt("patrol_point_id"),
                            checkpointName = obj.optString("checkpoint_name"),
                            category = obj.optString("category", "Welding"),
                            standardValue = obj.optString("standard_value"),
                            status = obj.optString("status", "NORMAL"),
                            remarks = obj.optString("remarks"),
                            problemDescription = obj.optString("problem_description"),
                            severity = obj.optString("severity", "HIGH"),
                            countermeasure = obj.optString("countermeasure"),
                            photoUri = if (obj.isNull("photo_uri")) null else obj.optString("photo_uri")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch patrol point results from Supabase", e)
            null
        }
    }

    suspend fun upsertPatrolPointResults(results: List<PatrolPointResultEntity>): List<PatrolPointResultEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured() || results.isEmpty()) return@withContext results
        try {
            val arr = JSONArray()
            results.forEach { res ->
                val obj = JSONObject().apply {
                    if (res.id > 0) put("id", res.id)
                    put("patrol_log_id", res.patrolLogId)
                    put("patrol_point_id", res.patrolPointId)
                    put("checkpoint_name", res.checkpointName)
                    put("category", res.category)
                    put("standard_value", res.standardValue)
                    put("status", res.status)
                    put("remarks", res.remarks)
                    put("problem_description", res.problemDescription)
                    put("severity", res.severity)
                    put("countermeasure", res.countermeasure)
                    put("photo_uri", res.photoUri ?: JSONObject.NULL)
                }
                arr.put(obj)
            }
            val req = buildRequest("patrol_point_results", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        val returnedList = mutableListOf<PatrolPointResultEntity>()
                        for (i in 0 until returnedArr.length()) {
                            val obj = returnedArr.getJSONObject(i)
                            returnedList.add(
                                PatrolPointResultEntity(
                                    id = obj.optInt("id"),
                                    patrolLogId = obj.optInt("patrol_log_id"),
                                    patrolPointId = obj.optInt("patrol_point_id"),
                                    checkpointName = obj.optString("checkpoint_name"),
                                    category = obj.optString("category", "Welding"),
                                    standardValue = obj.optString("standard_value"),
                                    status = obj.optString("status", "NORMAL"),
                                    remarks = obj.optString("remarks"),
                                    problemDescription = obj.optString("problem_description"),
                                    severity = obj.optString("severity", "HIGH"),
                                    countermeasure = obj.optString("countermeasure"),
                                    photoUri = if (obj.isNull("photo_uri")) null else obj.optString("photo_uri")
                                )
                            )
                        }
                        if (returnedList.isNotEmpty()) return@withContext returnedList
                    }
                    results
                } else {
                    Log.w("SupabaseService", "Upsert patrol point results failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert patrol point results to Supabase", e)
            null
        }
    }

    suspend fun deleteAllPatrolPointResults(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("patrol_point_results?id=gt.0", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to clear patrol point results on Supabase", e)
            false
        }
    }

    // --- ABNORMALITIES ---
    suspend fun fetchAbnormalities(): List<AbnormalityEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("abnormalities?select=*&order=timestamp.desc", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch abnormalities failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<AbnormalityEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AbnormalityEntity(
                            id = obj.optInt("id"),
                            abnormalityNumber = obj.optString("abnormality_number", "ABN-${System.currentTimeMillis()}"),
                            patrolLogId = obj.optInt("patrol_log_id"),
                            shopName = obj.optString("shop_name", "Weld Shop"),
                            lineName = obj.optString("line_name"),
                            machineName = obj.optString("machine_name"),
                            machineId = obj.optInt("machine_id"),
                            checkpointName = obj.optString("checkpoint_name"),
                            category = obj.optString("category", "Welding"),
                            priority = obj.optString("priority", "HIGH"),
                            problemDescription = obj.optString("problem_description"),
                            rootCause = obj.optString("root_cause", ""),
                            correctiveAction = obj.optString("corrective_action", ""),
                            responsiblePerson = obj.optString("responsible_person", "Maintenance Engineer"),
                            targetDate = obj.optString("target_date", "2026-08-05"),
                            completedDate = if (obj.isNull("completed_date")) null else obj.optString("completed_date"),
                            status = obj.optString("status", "PENDING"),
                            reportedBy = obj.optString("reported_by", "System Inspector"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            photoUri = if (obj.isNull("photo_uri")) null else obj.optString("photo_uri")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch abnormalities from Supabase", e)
            null
        }
    }

    suspend fun upsertAbnormality(abnormality: AbnormalityEntity): AbnormalityEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (abnormality.id > 0) put("id", abnormality.id)
                put("abnormality_number", abnormality.abnormalityNumber)
                put("patrol_log_id", abnormality.patrolLogId)
                put("shop_name", abnormality.shopName)
                put("line_name", abnormality.lineName)
                put("machine_name", abnormality.machineName)
                put("machine_id", abnormality.machineId)
                put("checkpoint_name", abnormality.checkpointName)
                put("category", abnormality.category)
                put("priority", abnormality.priority)
                put("problem_description", abnormality.problemDescription)
                put("root_cause", abnormality.rootCause)
                put("corrective_action", abnormality.correctiveAction)
                put("responsible_person", abnormality.responsiblePerson)
                put("target_date", abnormality.targetDate)
                put("completed_date", abnormality.completedDate ?: JSONObject.NULL)
                put("status", abnormality.status)
                put("reported_by", abnormality.reportedBy)
                put("timestamp", abnormality.timestamp)
                put("photo_uri", abnormality.photoUri ?: JSONObject.NULL)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("abnormalities", "POST", arr.toString(), "resolution=merge-duplicates,return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = abnormality.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", abnormality.id)
                        }
                    }
                    abnormality.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Upsert abnormality failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to upsert abnormality to Supabase", e)
            null
        }
    }

    suspend fun deleteAllPatrolLogs(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("patrol_logs?id=gt.0", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to clear patrol logs on Supabase", e)
            false
        }
    }

    suspend fun deleteAllAbnormalities(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("abnormalities?id=gt.0", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to clear abnormalities on Supabase", e)
            false
        }
    }

    // --- AUDIT LOGS ---
    suspend fun fetchAuditLogs(): List<AuditLogEntity>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val req = buildRequest("audit_logs?select=*&order=timestamp.desc&limit=200", "GET")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("SupabaseService", "Fetch audit logs failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)
                val list = mutableListOf<AuditLogEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AuditLogEntity(
                            id = obj.optInt("id"),
                            employeeId = obj.optString("employee_id"),
                            employeeName = obj.optString("employee_name"),
                            action = obj.optString("action"),
                            module = obj.optString("module"),
                            details = obj.optString("details"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to fetch audit logs from Supabase", e)
            null
        }
    }

    suspend fun insertAuditLog(log: AuditLogEntity): AuditLogEntity? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val obj = JSONObject().apply {
                if (log.id > 0) put("id", log.id)
                put("employee_id", log.employeeId)
                put("employee_name", log.employeeName)
                put("action", log.action)
                put("module", log.module)
                put("details", log.details)
                put("timestamp", log.timestamp)
            }
            val arr = JSONArray().put(obj)
            val req = buildRequest("audit_logs", "POST", arr.toString(), "return=representation")
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string()
                    var finalId = log.id
                    if (!respStr.isNullOrBlank()) {
                        val returnedArr = JSONArray(respStr)
                        if (returnedArr.length() > 0) {
                            finalId = returnedArr.getJSONObject(0).optInt("id", log.id)
                        }
                    }
                    log.copy(id = finalId)
                } else {
                    Log.w("SupabaseService", "Insert audit log failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to insert audit log to Supabase", e)
            null
        }
    }

    suspend fun deleteAllAuditLogs(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val req = buildRequest("audit_logs?id=gt.0", "DELETE")
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Unable to clear audit logs on Supabase", e)
            false
        }
    }

    // --- PHOTO EVIDENCE STORAGE UPLOAD ---
    suspend fun uploadEvidencePhoto(context: Context, localUri: Uri): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val fileName = "evidence_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val inputStream: InputStream = context.contentResolver.openInputStream(localUri) ?: return@withContext null
            val bytes = inputStream.use { it.readBytes() }

            val uploadUrl = "$baseUrl/storage/v1/object/abnormality-evidence/$fileName"
            val imageMediaType = "image/jpeg".toMediaType()

            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("x-upsert", "true")
                .post(bytes.toRequestBody(imageMediaType))
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    "$baseUrl/storage/v1/object/public/abnormality-evidence/$fileName"
                } else {
                    Log.w("SupabaseService", "Photo storage upload failed with code: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Error uploading photo to Supabase storage", e)
            null
        }
    }
}
