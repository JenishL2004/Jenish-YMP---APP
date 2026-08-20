package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance Supabase Realtime Client using Phoenix Channels WebSocket protocol.
 * Subscribes to PostgreSQL database table changes (INSERT, UPDATE, DELETE) in real-time.
 */
class SupabaseRealtimeManager(
    private var baseUrl: String,
    private var apiKey: String,
    private val scope: CoroutineScope,
    private val onEventReceived: suspend (table: String, action: String, record: JSONObject?, oldRecord: JSONObject?) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive for WebSocket
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var isConnecting = false
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private val tables = listOf(
        "users",
        "shops",
        "lines",
        "machines",
        "patrol_points",
        "patrol_point_revisions",
        "patrol_logs",
        "patrol_point_results",
        "abnormalities",
        "audit_logs"
    )

    fun updateConfig(newBaseUrl: String, newApiKey: String) {
        this.baseUrl = newBaseUrl
        this.apiKey = newApiKey
        disconnect()
        connect()
    }

    @Synchronized
    fun connect() {
        if (isConnected || isConnecting) return
        if (baseUrl.isBlank() || apiKey.isBlank()) return

        isConnecting = true
        val cleanHost = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')

        val wsUrl = "wss://$cleanHost/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"
        Log.i("SupabaseRealtime", "Connecting to Realtime WebSocket: wss://$cleanHost/realtime/v1/websocket")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("apikey", apiKey)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("SupabaseRealtime", "Realtime WebSocket Connected successfully!")
                isConnected = true
                isConnecting = false
                startHeartbeat()
                joinChannels()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("SupabaseRealtime", "WebSocket closing: $code / $reason")
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("SupabaseRealtime", "WebSocket closed: $code / $reason")
                isConnected = false
                isConnecting = false
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("SupabaseRealtime", "WebSocket connection failure: ${t.message}")
                isConnected = false
                isConnecting = false
                scheduleReconnect()
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && isConnected) {
                delay(25000)
                try {
                    val ref = refCounter.incrementAndGet().toString()
                    val heartbeatMsg = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", ref)
                    }
                    webSocket?.send(heartbeatMsg.toString())
                } catch (e: Exception) {
                    Log.w("SupabaseRealtime", "Heartbeat send error", e)
                }
            }
        }
    }

    private fun joinChannels() {
        try {
            // 1. Join Supabase v2 Postgres Changes Channel
            val postgresChangesArray = JSONArray()
            tables.forEach { tableName ->
                postgresChangesArray.put(JSONObject().apply {
                    put("event", "*")
                    put("schema", "public")
                    put("table", tableName)
                })
            }

            val v2JoinMsg = JSONObject().apply {
                put("topic", "realtime:public")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("broadcast", JSONObject().put("self", false))
                        put("presence", JSONObject().put("key", ""))
                        put("postgres_changes", postgresChangesArray)
                    })
                })
                put("ref", refCounter.incrementAndGet().toString())
            }
            webSocket?.send(v2JoinMsg.toString())
            Log.d("SupabaseRealtime", "Sent join message for realtime:public")

            // 2. Join individual table topics for legacy Supabase Realtime v1 compatibility
            tables.forEach { tableName ->
                val tableJoinMsg = JSONObject().apply {
                    put("topic", "realtime:public:$tableName")
                    put("event", "phx_join")
                    put("payload", JSONObject())
                    put("ref", refCounter.incrementAndGet().toString())
                }
                webSocket?.send(tableJoinMsg.toString())
            }
        } catch (e: Exception) {
            Log.e("SupabaseRealtime", "Error joining channels", e)
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val topic = json.optString("topic")
            val payload = json.optJSONObject("payload") ?: JSONObject()

            // Skip heartbeat / reply events
            if (event == "phx_reply" || event == "heartbeat") return

            Log.d("SupabaseRealtime", "Incoming Realtime message: topic=$topic, event=$event")

            var table: String? = null
            var action: String? = null
            var record: JSONObject? = null
            var oldRecord: JSONObject? = null

            // Format 1: Supabase v2 postgres_changes
            if (event == "postgres_changes") {
                val data = payload.optJSONObject("data") ?: payload
                action = data.optString("type")
                table = data.optString("table")
                record = data.optJSONObject("record")
                oldRecord = data.optJSONObject("old_record")
            }
            // Format 2: Direct Phoenix Realtime Table Broadcast (e.g. topic: "realtime:public:users", event: "INSERT")
            else if (event in listOf("INSERT", "UPDATE", "DELETE")) {
                action = event
                table = payload.optString("table").ifBlank { topic.removePrefix("realtime:public:") }
                record = payload.optJSONObject("record") ?: payload.optJSONObject("new")
                oldRecord = payload.optJSONObject("old_record") ?: payload.optJSONObject("old")
            }
            // Format 3: Custom Broadcast
            else if (payload.has("type") && payload.optString("type") in listOf("INSERT", "UPDATE", "DELETE")) {
                action = payload.optString("type")
                table = payload.optString("table").ifBlank { topic.removePrefix("realtime:public:") }
                record = payload.optJSONObject("record") ?: payload.optJSONObject("new")
                oldRecord = payload.optJSONObject("old_record") ?: payload.optJSONObject("old")
            }

            if (!table.isNullOrBlank() && !action.isNullOrBlank()) {
                val normalizedAction = action.uppercase()
                Log.i("SupabaseRealtime", "Dispatching Realtime Event: Table=$table, Action=$normalizedAction")
                scope.launch(Dispatchers.IO) {
                    onEventReceived(table, normalizedAction, record, oldRecord)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseRealtime", "Error parsing incoming realtime message: $text", e)
        }
    }

    private fun scheduleReconnect() {
        if (!scope.isActive) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(5000)
            if (!isConnected && !isConnecting) {
                Log.i("SupabaseRealtime", "Attempting automatic reconnection to Supabase Realtime...")
                connect()
            }
        }
    }

    @Synchronized
    fun disconnect() {
        isConnected = false
        isConnecting = false
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "Normal Closure")
        } catch (e: Exception) {
            // Ignore
        }
        webSocket = null
    }
}
