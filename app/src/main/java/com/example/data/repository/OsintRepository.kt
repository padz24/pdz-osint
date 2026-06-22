package com.example.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.example.data.database.*
import com.example.data.engine.OsintEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket

class OsintRepository(
    private val dao: AppDao,
    private val context: Context
) {
    private val client = OkHttpClient()

    // Flows from DB
    val allWorkspaces: Flow<List<Workspace>> = dao.getAllWorkspaces()
    val allScanLogs: Flow<List<ScanLog>> = dao.getAllScanLogs()
    val allScheduledScans: Flow<List<ScheduledScan>> = dao.getAllScheduledScans()

    // Web Server state
    private val _webServerActive = MutableStateFlow(false)
    val webServerActive: StateFlow<Boolean> = _webServerActive

    private val _webServerLogs = MutableStateFlow<List<String>>(emptyList())
    val webServerLogs: StateFlow<List<String>> = _webServerLogs

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // App Preferences / Settings
    private val sharedPrefs = context.getSharedPreferences("pdz_osint_prefs", Context.MODE_PRIVATE)

    var shodanApiKey: String
        get() = sharedPrefs.getString("shodan_api_key", "") ?: ""
        set(value) = sharedPrefs.edit().putString("shodan_api_key", value).apply()

    var telegramBotToken: String
        get() = sharedPrefs.getString("telegram_bot_token", "") ?: ""
        set(value) = sharedPrefs.edit().putString("telegram_bot_token", value).apply()

    var isTelegramBotEnabled: Boolean
        get() = sharedPrefs.getBoolean("telegram_bot_enabled", false)
        set(value) = sharedPrefs.edit().putBoolean("telegram_bot_enabled", value).apply()

    var licenseKey: String
        get() = sharedPrefs.getString("license_key", "") ?: ""
        set(value) = sharedPrefs.edit().putString("license_key", value).apply()

    // Helper to get active API lists, etc.
    fun addWebLog(logLine: String) {
        val current = _webServerLogs.value.toMutableList()
        current.add(0, "[${System.currentTimeMillis() % 100000}] $logLine")
        _webServerLogs.value = current.take(15) // Keep last 15 logs
    }

    // License tier derivation
    val activeTier: String
        get() {
            val key = licenseKey.trim().uppercase()
            return when {
                key == "PDZ-ULTRA-2026" || key.contains("ULTRA") -> "ULTRA"
                key == "PDZ-PREMIUM-2026" || key.contains("PREMIUM") -> "PREMIUM"
                else -> "FREE"
            }
        }

    // Database Actions
    suspend fun createWorkspace(name: String, description: String, target: String): Long {
        val ws = Workspace(name = name, description = description, target = target)
        return dao.insertWorkspace(ws)
    }

    suspend fun deleteWorkspace(workspace: Workspace) {
        dao.deleteWorkspace(workspace)
    }

    suspend fun getWorkspaceById(id: Int): Workspace? {
        return dao.getWorkspaceById(id)
    }

    fun getScanLogsForWorkspace(workspaceId: Int): Flow<List<ScanLog>> {
        return dao.getScanLogsForWorkspace(workspaceId)
    }

    suspend fun runAndSaveScan(workspaceId: Int, moduleKey: String, moduleName: String, tier: String, target: String): ScanLog {
        // Run look up
        addWebLog("Executing Module: $moduleKey targeting $target")
        val rawOutput = OsintEngine.executeModule(moduleKey, target, shodanApiKey)

        val log = ScanLog(
            workspaceId = workspaceId,
            moduleName = moduleName,
            tier = tier,
            target = target,
            output = rawOutput,
            isSuccess = !rawOutput.startsWith("[!]")
        )
        val id = dao.insertScanLog(log)
        val savedLog = log.copy(id = id.toInt())

        // Trigger dynamic monitoring checks such as custom alerts
        val schedules = withContext(Dispatchers.IO) {
            // Check scheduled scans for webhook triggering
            // If webhook configured, send standard POST request
            val activeSchedules = dao.getAllScheduledScans()
            // In real code we could check if alert matches target
        }

        return savedLog
    }

    suspend fun clearScanLogs(workspaceId: Int) {
        dao.clearScanLogsForWorkspace(workspaceId)
    }

    // Scheduled Scan management
    suspend fun addScheduledScan(workspaceId: Int, moduleName: String, target: String, interval: Int, webhook: String) {
        val scan = ScheduledScan(
            workspaceId = workspaceId,
            moduleName = moduleName,
            target = target,
            intervalMinutes = interval,
            webhookUrl = webhook,
            isActive = true
        )
        dao.insertScheduledScan(scan)
    }

    suspend fun deleteScheduledScan(scan: ScheduledScan) {
        dao.deleteScheduledScan(scan)
    }

    suspend fun toggleScheduledScan(scan: ScheduledScan) {
        dao.updateScheduledScan(scan.copy(isActive = !scan.isActive))
    }

    // Local REST Server / Web UI implementation on Port 8080!
    fun startLocalWebServer() {
        if (_webServerActive.value) return

        serverJob = repositoryScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8080)
                _webServerActive.value = true
                addWebLog("HTTP REST server successfully listening on Port 8080")
                addWebLog("Aesthetic Web Console available at http://${getLocalIp()}:8080")

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(socket)
                    }
                }
            } catch (e: Exception) {
                addWebLog("Server Error: ${e.message}")
            } finally {
                _webServerActive.value = false
            }
        }
    }

    fun stopLocalWebServer() {
        _webServerActive.value = false
        try {
            serverSocket?.close()
            serverJob?.cancel()
        } catch (e: Exception) {
            Log.e("OsintRepository", "WebServer close error", e)
        }
        addWebLog("Local HTTP/REST Server stopped.")
    }

    private suspend fun handleClientConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = PrintStream(socket.getOutputStream())

            val reqLine = reader.readLine() ?: return
            addWebLog("Client Connection: $reqLine")

            val tokens = reqLine.split(" ")
            if (tokens.size < 2) {
                socket.close()
                return
            }

            val path = tokens[1]
            val method = tokens[0]

            if (path.startsWith("/api/scan")) {
                // Parse parameters
                // e.g. /api/scan?module=ip_lookup&target=8.8.8.8
                val query = path.split("?").getOrNull(1) ?: ""
                val params = parseQueryParams(query)
                val module = params["module"] ?: "ip_lookup"
                val target = params["target"] ?: ""
                
                addWebLog("REST GET Request for module '$module': target '$target'")

                val scanResult = OsintEngine.executeModule(module, target, shodanApiKey)
                val jsonRes = JSONObject()
                jsonRes.put("framework", "PDZ-OSINT Toolkit v2.6.0")
                jsonRes.put("status", "success")
                jsonRes.put("module", module)
                jsonRes.put("target", target)
                jsonRes.put("timestamp", System.currentTimeMillis())
                jsonRes.put("output", scanResult)

                val body = jsonRes.toString()
                sendResponse(out, "application/json", body)
            } else if (path == "/") {
                // Serve fully-interactive, cyber-styled single-page dark dashboard!
                val html = """
                    <!DOCTYPE html>
                    <html lang="id">
                    <head>
                        <meta charset="UTF-8">
                        <title>PDZ-OSINT Toolkit v2.6.0 Dashboard</title>
                        <style>
                            body {
                                background-color: #0d1117;
                                color: #58a6ff;
                                font-family: 'Courier New', Courier, monospace;
                                padding: 20px;
                            }
                            h1 { color: #58a6ff; font-weight: bold; font-size: 24px; border-bottom: 2px solid #58a6ff; padding-bottom: 10px; }
                            .terminal {
                                background-color: #161b22;
                                border: 1px solid #30363d;
                                border-radius: 6px;
                                padding: 15px;
                                margin-top: 15px;
                                max-height: 500px;
                                overflow-y: auto;
                                white-space: pre-wrap;
                                color: #39ff14;
                            }
                            input, select, button {
                                background-color: #21262d;
                                border: 1px solid #30363d;
                                color: #c9d1d9;
                                padding: 8px 12px;
                                border-radius: 4px;
                                font-family: inherit;
                                margin-right: 5px;
                            }
                            button {
                                background-color: #1f6feb;
                                cursor: pointer;
                                color: #ffffff;
                                font-weight: bold;
                                transition: background 0.2s;
                            }
                            button:hover {
                                background-color: #388bfd;
                            }
                        </style>
                        <script>
                            async function runScan() {
                                const module = document.getElementById("module").value;
                                const target = document.getElementById("target").value;
                                const output = document.getElementById("output");
                                output.innerHTML = "[*] Querying remote OSINT engine: " + module + " on " + target + "...\n\n";
                                try {
                                    const res = await fetch("/api/scan?module=" + encodeURIComponent(module) + "&target=" + encodeURIComponent(target));
                                    const json = await res.json();
                                    output.innerHTML = json.output;
                                } catch (e) {
                                    output.innerHTML = "[!] Web Error running scan: " + e.message;
                                }
                            }
                        </script>
                    </head>
                    <body>
                        <h1>⚡ PDZ-OSINT API & WEB PORTAL (V2.6.0)</h1>
                        <p>Welcome to the external, browser-oriented cyber-recon portal running directly from target Android device nodes.</p>
                        <div style="margin-top: 20px;">
                            <select id="module">
                                <option value="ip_lookup">IP / GeoIP Lookup</option>
                                <option value="dns_resolver">DNS Resolution</option>
                                <option value="whois_rdap">WHOIS RDAP Lookup</option>
                                <option value="jwt_decoder">JWT Base64 Decoder</option>
                                <option value="url_expander">Short-URL Expander</option>
                                <option value="ua_parser">User-Agent Parser</option>
                                <option value="hash_gen">Local Cryptographic Hash</option>
                                <option value="mac_lookup">MAC Vendor Lookup</option>
                                <option value="port_scanner">Port Scanner (Real Connection)</option>
                                <option value="username_search">Username Tracker</option>
                                <option value="email_verifier">Email Host DNS Auditor</option>
                                <option value="ssl_inspector">SSL Handshake Inspector</option>
                                <option value="tech_detector">Web Tech Detector</option>
                            </select>
                            <input type="text" id="target" placeholder="Query input target (Domain/IP/JWT/Username)" style="width: 300px;"/>
                            <button onclick="runScan()">EXECUTE RECON</button>
                        </div>
                        <div class="terminal" id="output">
[+] TERMINAL CONSOLE READY
Type target parameters above and click EXECUTE RECON.
All engine queries resolve straight from the active Android client nodes.
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                sendResponse(out, "text/html", html)
            } else {
                sendResponse(out, "text/plain", "PDZ-OSINT REST Service running. Route not matches.", statusCode = 404)
            }

            out.close()
            reader.close()
            socket.close()
        } catch (e: Exception) {
            Log.e("OsintRepository", "Error handling socket client", e)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isBlank()) return result
        try {
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx > 0) {
                    val key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                    val value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    result[key] = value
                }
            }
        } catch (ignored: Exception) {}
        return result
    }

    private fun sendResponse(out: PrintStream, contentType: String, content: String, statusCode: Int = 200) {
        val statusMsg = if (statusCode == 200) "OK" else "Not Found"
        out.println("HTTP/1.1 $statusCode $statusMsg")
        out.println("Content-Type: $contentType; charset=UTF-8")
        out.println("Content-Length: ${content.toByteArray().size}")
        out.println("Connection: close")
        out.println()
        out.print(content)
        out.flush()
    }

    private fun getLocalIp(): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }

    // Discord / Slack Webhook execution helper! (REAL network POST)
    suspend fun fireWebhook(webhookUrl: String, content: String): String = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) return@withContext "Webhook empty"
        try {
            val json = JSONObject()
            // Support standard slack/discord content parameter
            json.put("content", "🔔 **PDZ-OSINT Monitor Triggered**:\n$content")
            json.put("text", "PDZ-OSINT Monitored Event alert!")

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                "HTTP Response: ${response.code} / ${response.message}"
            }
        } catch (e: Exception) {
            "Error firing webhook: ${e.localizedMessage ?: e.message}"
        }
    }
}
