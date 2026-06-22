package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.MainApplication
import com.example.data.database.ScanLog
import com.example.data.database.ScheduledScan
import com.example.data.database.Workspace
import com.example.data.engine.OsintEngine
import com.example.data.repository.OsintRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

data class OsintModule(
    val key: String,
    val name: String,
    val description: String,
    val tier: String // FREE, PREMIUM, ULTRA
)

data class ThreatFeed(
    val id: String,
    val name: String,
    val description: String,
    val tier: String, // FREE, PREMIUM, ULTRA
    val provider: String,
    val category: String, // IP Reputation, Malware C2, Phishing Vector, Cybercrime
    val version: String
)

data class ThreatEvent(
    val feedId: String,
    val indicator: String,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val description: String,
    val sourceFeedName: String,
    val firstSeen: String,
    val details: String
)

data class CommunityModule(
    val key: String,
    val name: String,
    val description: String,
    val category: String, // RECON, VULNERABILITY, EXPLOITS, FORENSICS
    val creator: String,
    val rating: Float,
    val reviewsCount: Int,
    val sizeKb: Int,
    val isVetted: Boolean, // Vetted by security team
    val vettingLog: String,
    val isDownloaded: Boolean = false,
    val reviews: List<ModuleReview> = emptyList()
)

data class ModuleReview(
    val rater: String,
    val rating: Int,
    val comment: String,
    val timestamp: String
)

class OsintViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MainApplication).repository

    // --- 1. THREAT INTELLIGENCE STATE ---
    val threatFeedsCatalog = listOf(
        ThreatFeed("alienvault_otx", "AlienVault IP Reputation OTX", "Active indicator analysis for compromised and high-spam hosts", "FREE", "AlienVault", "IP Reputation", "v1.2.9"),
        ThreatFeed("phishtank_live", "PhishTank Live Feed", "Real-time verified phishing domain coordinates and URL clusters", "FREE", "PhishTank", "Phishing Vector", "v4.0.2"),
        ThreatFeed("spamhaus_drop", "Spamhaus DROP List", "Identifies hijacked CIDR blocks and toxic network route nets", "PREMIUM", "Spamhaus Org", "Malware C2 Server", "v2.10.0"),
        ThreatFeed("ransomware_tracker", "Abuse.ch Ransomware Tracker", "Active tracking list of malware-associated ransom payout gateways", "PREMIUM", "abuse.ch", "Malware C2 Server", "v1.4.3"),
        ThreatFeed("darkweb_leak_intel", "Tor leak Intel Directories", "Dark Web forum listings, hacker index groups, and dynamic breaches data", "ULTRA", "Underground Monitors", "Credentials Dump", "v9.3.0"),
        ThreatFeed("apt_threat_intel", "APT Group Indicators Tracker", "State-sponsored cyber threat groups target servers and IP nodes", "ULTRA", "Cyber Security Agency", "Advanced Persistent Threat", "v5.1.0")
    )

    private val _subscribedFeedIds = MutableStateFlow<Set<String>>(setOf("alienvault_otx", "phishtank_live"))
    val subscribedFeedIds: StateFlow<Set<String>> = _subscribedFeedIds

    // Static list of mock indicators that are matched in searches
    val threatEventsDatabase = listOf(
        ThreatEvent("alienvault_otx", "192.168.4.1", "HIGH", "Observed running active SSH brute-force attacks.", "AlienVault IP Reputation OTX", "2 hrs ago", "Node targeting class-C subnets via port 22 scanner scripts."),
        ThreatEvent("phishtank_live", "malicious-phish-domain.xyz", "CRITICAL", "Active Citibank login cloning phishing node.", "PhishTank Live Feed", "10 mins ago", "Faking customer banking portals to harvest card details."),
        ThreatEvent("spamhaus_drop", "185.220.101.4", "CRITICAL", "Host registered as Spamhaus DROP malicious Tor exit node.", "Spamhaus DROP List", "1 day ago", "Command and Control server relaying infected botnet outputs."),
        ThreatEvent("ransomware_tracker", "bitcoin-ransom-payment.onion", "HIGH", "LockBit 3.0 cyber ransom ledger address.", "Abuse.ch Ransomware Tracker", "5 mins ago", "Linked with corporate file locker payment pathways."),
        ThreatEvent("darkweb_leak_intel", "user@email.com", "MEDIUM", "Compromised inside 2026 Space-Exfiltration Leak database.", "Tor leak Intel Directories", "3 days ago", "PlainText email credentials exposed on hacker dark web index."),
        ThreatEvent("apt_threat_intel", "178.23.45.67", "CRITICAL", "APT-29 Russian Storm payload distribution point.", "APT Group Indicators Tracker", "12 hrs ago", "Compromised DNS server pushing weaponized office templates.")
    )

    // Dynamic matched threat events based on active workspace target or query input
    fun findThreatIndicatorsForTarget(target: String): List<ThreatEvent> {
        if (target.isBlank()) return emptyList()
        val subs = _subscribedFeedIds.value
        return threatEventsDatabase.filter { event ->
            subs.contains(event.feedId) && 
            (target.contains(event.indicator, ignoreCase = true) || event.indicator.contains(target, ignoreCase = true) ||
             (target.contains(".") && event.indicator.contains(target.substringBefore("."))))
        }
    }

    fun toggleFeedSubscription(feedId: String) {
        val feed = threatFeedsCatalog.find { it.id == feedId } ?: return
        
        // License tier verification
        val meetsTier = when (feed.tier) {
            "ULTRA" -> activeTier == "ULTRA"
            "PREMIUM" -> activeTier == "PREMIUM" || activeTier == "ULTRA"
            else -> true
        }

        if (!meetsTier) {
            _scanOutput.value = "[!] LICENSING RESTRICTION: Subscribing to Feed '${feed.name}' requires the ${feed.tier} Tier."
            return
        }

        val current = _subscribedFeedIds.value.toMutableSet()
        if (current.contains(feedId)) {
            current.remove(feedId)
        } else {
            current.add(feedId)
        }
        _subscribedFeedIds.value = current
    }

    // --- 2. COMMUNITY MODULE MARKETPLACE STATE ---
    private val _communityModules = MutableStateFlow<List<CommunityModule>>(listOf(
        CommunityModule("exploits_scanner", "Splunk RCE Exploit Auditor", "Query CVE configurations specifically identifying active web endpoints vulnerability exploits.", "EXPLOITS", "GhostProtocol", 4.8f, 18, 42, true, "PASSED: Full code audit. Secure local sandbox verification.", false, listOf(
            ModuleReview("hacker_0x00", 5, "Discovered two open target server systems vulnerabilities instantly! Sleek integration.", "1 day ago"),
            ModuleReview("cyber_patriot", 4, "Extremely polished code, executes very cleanly.", "3 days ago")
        )),
        CommunityModule("credential_dumper", "Hacked Credentials Expose Scraper", "Harvest public leaks lists indices matching active company domain names.", "FORENSICS", "StormBreaker", 4.9f, 32, 112, true, "PASSED: Secure sandbox execution. Non-network exfiltration certified.", false, listOf(
            ModuleReview("sec_boss", 5, "Incredibly useful during pen-testing assignments.", "Yesterday"),
            ModuleReview("red_team_lead", 5, "Fast, secure, and complies fully with local sandbox integrity.", "4 days ago")
        )),
        CommunityModule("camera_recon", "Public Camera Scout Map", "Check open RTSP stream servers near host geolocation records.", "RECON", "MapGeek", 3.2f, 8, 14, false, "WARNING: Modules is not vetted. May contains raw sockets connections.", false, listOf(
            ModuleReview("noob_sec", 2, "Vastly unstable on low-speed networks.", "1 week ago"),
            ModuleReview("net_admin", 4, "Works fine if you set correct targets parameters manually.", "2 weeks ago")
        )),
        CommunityModule("ssh_brute", "SSH Weak Passwords Auditor", "Probes secure shell servers on targeted workspace containers for weak admin root credentials.", "VULNERABILITY", "BruteMaster", 4.5f, 22, 60, true, "PASSED: Secure loop testing verified.", false, listOf(
            ModuleReview("sec_guy", 5, "Perfect audit helper.", "5 days ago")
        ))
    ))
    val communityModules: StateFlow<List<CommunityModule>> = _communityModules

    // Installed additional community modules in current session
    private val _downloadedModuleRegistry = MutableStateFlow<List<OsintModule>>(emptyList())
    val downloadedModuleRegistry: StateFlow<List<OsintModule>> = _downloadedModuleRegistry

    fun downloadCommunityModule(modKey: String) {
        val list = _communityModules.value.map { mod ->
            if (mod.key == modKey) {
                mod.copy(isDownloaded = true)
            } else mod
        }
        _communityModules.value = list

        val downloadedModule = list.find { it.key == modKey } ?: return
        val currentRegistry = _downloadedModuleRegistry.value.toMutableList()
        if (currentRegistry.none { it.key == modKey }) {
            currentRegistry.add(
                OsintModule(
                    key = downloadedModule.key,
                    name = "👤 [COMM] " + downloadedModule.name,
                    description = downloadedModule.description,
                    tier = "FREE" // Downloaded modules operate for everyone
                )
            )
            _downloadedModuleRegistry.value = currentRegistry
            
            // Log update
            _scanOutput.value = "[+] SYSTEM UPDATE: Community Module '${downloadedModule.name}' downloaded, compiled, and registered successfully to terminal options."
        }
    }

    fun submitModuleReview(modKey: String, rater: String, rating: Int, comment: String) {
        val list = _communityModules.value.map { mod ->
            if (mod.key == modKey) {
                val updatedReviews = mod.reviews.toMutableList()
                updatedReviews.add(0, ModuleReview(rater, rating, comment, "Just now"))
                val newRating = ((mod.rating * mod.reviewsCount) + rating) / (mod.reviewsCount + 1)
                mod.copy(
                    reviews = updatedReviews,
                    reviewsCount = mod.reviewsCount + 1,
                    rating = String.format("%.1f", newRating).toFloat()
                )
            } else mod
        }
        _communityModules.value = list
    }

    fun uploadCommunityModule(name: String, description: String, category: String, creator: String) {
        val cleanKey = name.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
        val newMod = CommunityModule(
            key = cleanKey,
            name = name,
            description = description,
            category = category,
            creator = creator,
            rating = 5.0f,
            reviewsCount = 1,
            sizeKb = (12..95).random(),
            isVetted = false, // Starts unvetted!
            vettingLog = "PENDING VETTING: Newly shared by community developer. Inspect source prior to running.",
            isDownloaded = false,
            reviews = listOf(ModuleReview(creator, 5, "Initial operational deployment of scanning structures.", "Just now"))
        )
        val currentList = _communityModules.value.toMutableList()
        currentList.add(0, newMod)
        _communityModules.value = currentList
        
        _scanOutput.value = "[+] MARKETPLACE DEPLOYMENT: Custom Module '$name' shared to PDZ community portal."
    }

    // --- 3. ONBOARDING STATE FLOW ---
    private val _onboardingStep = MutableStateFlow(0) // 0 is Welcome intro step
    val onboardingStep: StateFlow<Int> = _onboardingStep

    private val _isOnboardingActive = MutableStateFlow(true) // Initial launch triggers onboarding modal
    val isOnboardingActive: StateFlow<Boolean> = _isOnboardingActive

    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step
    }

    fun setOnboardingActive(active: Boolean) {
        _isOnboardingActive.value = active
    }

    // DB list states bound to flows
    val workspaces: StateFlow<List<Workspace>> = repository.allWorkspaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScanLogs: StateFlow<List<ScanLog>> = repository.allScanLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledScans: StateFlow<List<ScheduledScan>> = repository.allScheduledScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active environment context state
    private val _currentWorkspaceId = MutableStateFlow<Int?>(null)
    val currentWorkspaceId: StateFlow<Int?> = _currentWorkspaceId

    val currentWorkspace: StateFlow<Workspace?> = combine(workspaces, _currentWorkspaceId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull() ?: run {
            // Autocreate default workspace on first start!
            if (list.isEmpty()) {
                viewModelScope.launch {
                    val newId = repository.createWorkspace("Default Workspace", "Workspace for general recon", "google.com")
                    _currentWorkspaceId.value = newId.toInt()
                }
            }
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Current workspace logs
    val currentWorkspaceLogs: StateFlow<List<ScanLog>> = combine(allScanLogs, currentWorkspace) { logs, ws ->
        if (ws == null) emptyList() else logs.filter { it.workspaceId == ws.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Web Server state
    val isWebServerRunning: StateFlow<Boolean> = repository.webServerActive
    val webServerLogs: StateFlow<List<String>> = repository.webServerLogs

    // App Setting strings
    var shodanApiKey: String
        get() = repository.shodanApiKey
        set(value) { repository.shodanApiKey = value }

    var telegramBotToken: String
        get() = repository.telegramBotToken
        set(value) { repository.telegramBotToken = value }

    var isTelegramBotEnabled: Boolean
        get() = repository.isTelegramBotEnabled
        set(value) { repository.isTelegramBotEnabled = value }

    var licenseKey: String
        get() = repository.licenseKey
        set(value) { repository.licenseKey = value }

    val activeTier: String
        get() = repository.activeTier

    // Active Scan states
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanOutput = MutableStateFlow("[+] PDZ-OSINT Toolkit Environment Initialized.\nSelect a module and insert query input to begin recon.")
    val scanOutput: StateFlow<String> = _scanOutput

    private val _currentScanningModule = MutableStateFlow<String?>(null)
    val currentScanningModule: StateFlow<String?> = _currentScanningModule

    // Diff Engine States
    private val _diffLogA = MutableStateFlow<ScanLog?>(null)
    val diffLogA: StateFlow<ScanLog?> = _diffLogA

    private val _diffLogB = MutableStateFlow<ScanLog?>(null)
    val diffLogB: StateFlow<ScanLog?> = _diffLogB

    private val _diffResult = MutableStateFlow<List<DiffLine>>(emptyList())
    val diffResult: StateFlow<List<DiffLine>> = _diffResult

    data class DiffLine(val text: String, val type: DiffType)
    enum class DiffType { UNCHANGED, ADDED, REMOVED }

    // Intersecting Shell Console logs
    private val _terminalHistory = MutableStateFlow<List<String>>(listOf(
        "PDZ-OSINT Toolkit v2.6.0 [Termux API Console Node]",
        "Type 'help' or 'modules' to list terminal commands.",
        "--------------------------------------------------"
    ))
    val terminalHistory: StateFlow<List<String>> = _terminalHistory

    // Fully-declared static 33 Modules catalog categorized inside list
    val modules = listOf(
        // --- FREE (12) ---
        OsintModule("ip_lookup", "IP Lookup", "Resolve IP or domain host server details", "FREE"),
        OsintModule("dns_resolver", "DNS Resolver", "Fetch DNS records A, AAAA, MX, TXT, CNAME", "FREE"),
        OsintModule("whois_rdap", "WHOIS RDAP", "Retrieve registered root domain WHOIS metadata", "FREE"),
        OsintModule("jwt_decoder", "JWT Decoder", "Deconstruct header/claims of formatted auth keys", "FREE"),
        OsintModule("url_expander", "URL Expander", "Expose destination redirect targets in real-time", "FREE"),
        OsintModule("ua_parser", "User-Agent Parser", "Extract operating system and browser indices from UA", "FREE"),
        OsintModule("mac_lookup", "MAC Lookup", "Identify physical MAC address vendor registrations", "FREE"),
        OsintModule("hash_gen", "Hash Generator", "Generate MD5, SHA-1, SHA-256 local integrity hashes", "FREE"),
        OsintModule("base64_tool", "Base64 Encoder", "Encode/Decode plaintext Base64 strings locally", "FREE"),
        OsintModule("geoip_tracer", "GeoIP Tracer", "Pinpoint city and country coordinate networks of targets", "FREE"),
        OsintModule("asn_lookup", "ASN Lookup", "Identify Autonomous System numbers & network blocks", "FREE"),
        OsintModule("html_fetch", "HTML Snippet Fetch", "Download raw HTML home structures & header records", "FREE"),

        // --- PREMIUM (16) ---
        OsintModule("subdomain_enum", "Subdomain Scanner", "Search crt.sh certificate logs for subdomains", "PREMIUM"),
        OsintModule("port_scanner", "TCP Port Scanner", "Measure active socket openings on target systems", "PREMIUM"),
        OsintModule("username_search", "Username Tracker", "Query user profile matches across 10+ social platforms", "PREMIUM"),
        OsintModule("email_verifier", "Email Host MX Auditor", "Validate syntax structures and MX deliverable hosts", "PREMIUM"),
        OsintModule("ip_reputation", "Reputation Scout", "Identify abuse scores, proxy, spam risk directories", "PREMIUM"),
        OsintModule("dns_sec_check", "DNSSEC Inspector", "Verify active cryptographic sign configurations", "PREMIUM"),
        OsintModule("ping_test", "ICMP Ping / Reachability", "Measure roundtrip connection latencies", "PREMIUM"),
        OsintModule("password_strength", "Entropy Analyzer", "Measure robustness and safety of passwords", "PREMIUM"),
        OsintModule("robots_txt", "robots.txt Map", "Retrieve target site crawler instructions file", "PREMIUM"),
        OsintModule("headers_inspector", "HTTP Headers Audit", "Extract server type and cookies config properties", "PREMIUM"),
        OsintModule("subnet_calc", "CIDR Subnet Calc", "Calculate subnet ranges, usable hosts, and masks", "PREMIUM"),
        OsintModule("reverse_dns", "Reverse PTR DNS", "Resolve PTR records to locate authoritative hostname", "PREMIUM"),
        OsintModule("security_txt", "security.txt Search", "Crawl web well-known contact boundaries", "PREMIUM"),
        OsintModule("dork_helper", "Dorking Assistant", "Provide actionable Google Dork recon templates", "PREMIUM"),
        OsintModule("spf_check", "SPF Phish Auditor", "Check server spoof protections", "PREMIUM"),
        OsintModule("mx_analyzer", "MX Mail Gate Audit", "Validate mail exchange route priorities", "PREMIUM"),

        // --- ULTRA (5) ---
        OsintModule("ssl_inspector", "SSL Handshake Inspector", "Expose Certificate Authority chains and parameters", "ULTRA"),
        OsintModule("tech_detector", "Web Tech Analyzer", "Scan headers & scripts to output Web tech profiles", "ULTRA"),
        OsintModule("shodan_scout", "Shodan Port Audit", "Fetch indexing ports from Shodan on public nodes", "ULTRA"),
        OsintModule("security_headers", "Security Headers Audit", "Evaluate protection flags for CSRF/XSS", "ULTRA"),
        OsintModule("traceroute", "Hop Route Simulator", "Calculate transmission jumps to targeted endpoints", "ULTRA")
    )

    val allAvailableModules: StateFlow<List<OsintModule>> = combine(
        MutableStateFlow(modules), _downloadedModuleRegistry
    ) { base, downloaded ->
        base + downloaded
    }.stateIn(viewModelScope, SharingStarted.Eagerly, modules)

    // Actions
    fun setWorkspaceId(id: Int) {
        _currentWorkspaceId.value = id
    }

    fun addWorkspace(name: String, description: String, target: String) {
        viewModelScope.launch {
            val newId = repository.createWorkspace(name, description, target)
            _currentWorkspaceId.value = newId.toInt()
        }
    }

    fun deleteWorkspace(workspace: Workspace) {
        viewModelScope.launch {
            repository.deleteWorkspace(workspace)
            if (_currentWorkspaceId.value == workspace.id) {
                _currentWorkspaceId.value = null
            }
        }
    }

    fun runScan(module: OsintModule, inputTarget: String) {
        val currentWs = currentWorkspace.value ?: return
        val target = inputTarget.ifBlank { currentWs.target }

        if (target.isBlank() && !module.key.equals("ip_lookup", true)) {
            _scanOutput.value = "[!] Error: Target is empty and no workspace target set."
            return
        }

        // Tier enforcement verification
        val tierCode = activeTier
        val meetsTier = when (module.tier) {
            "ULTRA" -> tierCode == "ULTRA"
            "PREMIUM" -> tierCode == "PREMIUM" || tierCode == "ULTRA"
            else -> true
        }

        if (!meetsTier) {
            _scanOutput.value = """
                [!] LICENSING CONSTRAINTS
                Module '${module.name}' is restricted under your active tier: [$tierCode].
                Please upgrade to [${module.tier}] to run advanced recon.
                Enter legal license code (e.g. 'PDZ-${module.tier}-2026') inside Settings.
            """.trimIndent()
            return
        }

        _isScanning.value = true
        _currentScanningModule.value = module.key
        _scanOutput.value = "[*] Launching module ${module.name} on: $target...\n[+] Running background TCP socket or JSON-API network probes...\n"

        viewModelScope.launch {
            try {
                val log = repository.runAndSaveScan(currentWs.id, module.key, module.name, module.tier, target)
                _scanOutput.value = log.output
            } catch (e: Exception) {
                _scanOutput.value = "[!] Engine Exception: ${e.message}"
            } finally {
                _isScanning.value = false
                _currentScanningModule.value = null
            }
        }
    }

    fun deleteScanLog(log: ScanLog) {
        viewModelScope.launch {
            // Check references in diffs
            if (_diffLogA.value?.id == log.id) _diffLogA.value = null
            if (_diffLogB.value?.id == log.id) _diffLogB.value = null
            // We delete log
            // Let's create a suspend function in dao to delete scan log
            MainApplication.instance.database.dao().deleteScanLog(log)
        }
    }

    fun clearAllScanLogs() {
        val currentWs = currentWorkspace.value ?: return
        viewModelScope.launch {
            repository.clearScanLogs(currentWs.id)
            _diffLogA.value = null
            _diffLogB.value = null
            _scanOutput.value = "[+] Workspace scan history wiped clean."
        }
    }

    // Server Switch Actions
    fun toggleWebServer(enabled: Boolean) {
        if (enabled) {
            repository.startLocalWebServer()
        } else {
            repository.stopLocalWebServer()
        }
    }

    // Schedule actions
    fun addSchedule(moduleName: String, target: String, interval: Int, webhook: String) {
        val currentWs = currentWorkspace.value ?: return
        viewModelScope.launch {
            repository.addScheduledScan(currentWs.id, moduleName, target, interval, webhook)
        }
    }

    fun deleteSchedule(scan: ScheduledScan) {
        viewModelScope.launch {
            repository.deleteScheduledScan(scan)
        }
    }

    fun toggleSchedule(scan: ScheduledScan) {
        viewModelScope.launch {
            repository.toggleScheduledScan(scan)
        }
    }

    fun testWebhook(url: String) {
        viewModelScope.launch {
            repository.addWebLog("Testing Webhook Endpoint POST request...")
            val res = repository.fireWebhook(url, "PDZ-OSINT Manual Webhook Ping check successful! Host Target node reporting live.")
            repository.addWebLog("Webhook Deliver Payload status: $res")
        }
    }

    // Diff Engine logic
    fun setDiffA(log: ScanLog?) {
        _diffLogA.value = log
        calculateDiff()
    }

    fun setDiffB(log: ScanLog?) {
        _diffLogB.value = log
        calculateDiff()
    }

    private fun calculateDiff() {
        val logA = _diffLogA.value
        val logB = _diffLogB.value
        if (logA == null || logB == null) {
            _diffResult.value = emptyList()
            return
        }

        val linesA = logA.output.split("\n")
        val linesB = logB.output.split("\n")

        val result = mutableListOf<DiffLine>()
        // Simple visual comparison
        val maxLines = maxOf(linesA.size, linesB.size)
        for (i in 0 until maxLines) {
            val lineA = linesA.getOrNull(i)
            val lineB = linesB.getOrNull(i)

            if (lineA == lineB) {
                if (lineA != null) result.add(DiffLine(lineA, DiffType.UNCHANGED))
            } else {
                if (lineA != null) result.add(DiffLine("- $lineA", DiffType.REMOVED))
                if (lineB != null) result.add(DiffLine("+ $lineB", DiffType.ADDED))
            }
        }
        _diffResult.value = result
    }

    // Command Line Interface parsing engine
    fun executeShellCommand(cmdText: String) {
        val text = cmdText.trim()
        if (text.isBlank()) return

        val history = _terminalHistory.value.toMutableList()
        history.add("$> $text")

        val parts = text.split(" ")
        val baseCmd = parts[0].lowercase()

        when (baseCmd) {
            "help" -> {
                history.add("PDZ OSINT Command Suite v2.6.0")
                history.add("  help                       View available commands")
                history.add("  modules                    List all 33 recon tools")
                history.add("  workspace list             List workspace configurations")
                history.add("  workspace create <name>    Instantiate empty research container")
                history.add("  run -m <module> -t <target> Execute reconnaissance module")
                history.add("  license <key>              Activate ultra/premium validation key")
                history.add("  clear                      Clear terminal console rows")
            }
            "clear" -> {
                history.clear()
                history.add("Console rows flushed.")
            }
            "modules" -> {
                val availableList = allAvailableModules.value
                history.add("--- OSINT MODULE REGISTRY (${availableList.size} MODULES) ---")
                availableList.forEach {
                    history.add(" • [${it.tier}] ${it.key} : ${it.name}")
                }
            }
            "license" -> {
                val key = parts.getOrNull(1)
                if (key != null) {
                    licenseKey = key
                    history.add("License key updated to: $key. Active authorization: [$activeTier]")
                } else {
                    history.add("Active license tier: [$activeTier]. Usage: license <PDZ-PREMIUM-2026>")
                }
            }
            "workspace" -> {
                val sub = parts.getOrNull(1)?.lowercase()
                if (sub == "list") {
                    history.add("--- ACTIVE WORKING CONTAINERS ---")
                    workspaces.value.forEach {
                        history.add(" • ID [${it.id}] ${it.name} - Target: ${it.target}")
                    }
                } else if (sub == "create") {
                    val name = parts.drop(2).joinToString(" ")
                    if (name.isNotBlank()) {
                        addWorkspace(name, "Terminal created workspace", "google.com")
                        history.add("Workspace '$name' deployed.")
                    } else {
                        history.add("Syntax: workspace create <Name>")
                    }
                } else {
                    history.add("Usage: workspace [list / create <name>]")
                }
            }
            "run" -> {
                // Parse options
                val modIdx = parts.indexOf("-m")
                val tarIdx = parts.indexOf("-t")

                val mKey = if (modIdx != -1) parts.getOrNull(modIdx + 1) else null
                val targetText = if (tarIdx != -1) parts.getOrNull(tarIdx + 1) else null

                val module = allAvailableModules.value.find { it.key.equals(mKey, true) }

                if (module != null && targetText != null) {
                    history.add("[*] Firing module ${module.name} target $targetText...")
                    _terminalHistory.value = history // Pre-update UI listing
                    
                    viewModelScope.launch {
                        val activeKey = if (module.key == "shodan_scout") shodanApiKey else ""
                        val outputText = OsintEngine.executeModule(module.key, targetText, activeKey)
                        
                        val updatedHistory = _terminalHistory.value.toMutableList()
                        updatedHistory.add("--- OUTPUT: ${module.name} ---")
                        outputText.split("\n").forEach { line ->
                            updatedHistory.add("  $line")
                        }
                        updatedHistory.add("------------- RUN COMPLETE -------------")
                        _terminalHistory.value = updatedHistory
                    }
                    return // Escape early as coroutine operates async
                } else {
                    history.add("Syntax Error: run -m <module_key> -t <target>")
                    history.add("Provide valid key registry listings (e.g. run -m dns_resolver -t google.com)")
                }
            }
            else -> {
                history.add("pdz: command not found: $baseCmd. Enter 'help' for command directories.")
            }
        }

        _terminalHistory.value = history
    }

    // Html report share function: Writes beautiful responsive report to local files
    fun exportHtmlReport(context: Context, log: ScanLog) {
        val fileName = "pdz_osint_report_${log.id}.html"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <title>PDZ-OSINT Target Investigation Report</title>
                <style>
                    body { font-family: Helvetica, Arial, sans-serif; background: #0f141c; color: #abb2bf; padding: 30px; }
                    .card { background: #1e2430; border-radius: 8px; padding: 25px; border-left: 5px solid #4a90e2; box-shadow: 0 4px 12px rgba(0,0,0,0.3); }
                    h1 { color: #528bff; font-weight: 300; margin-top: 0; }
                    h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 1px; color: #828a99; }
                    pre { background: #11151d; border: 1px solid #2d3139; color: #00ff66; padding: 15px; border-radius: 5px; overflow-x: auto; font-family: monospace; }
                    .meta { margin-bottom: 20px; font-size: 13px; color: #5c6370; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>🛡️ PADIL DEVELOPER ZONE: RECON SYSTEM REPORT</h1>
                    <h2>PDZ-OSINT Framework v2.6.0 Pro Enterprise Node</h2>
                    <hr style="border: 0; border-bottom: 1px dashed #2d3139; margin-bottom: 20px;"/>
                    <div class="meta">
                        <strong>Target Scanned :</strong> ${log.target}<br/>
                        <strong>Module Used    :</strong> ${log.moduleName} (${log.tier})<br/>
                        <strong>Timestamp      :</strong> ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(log.timestamp))}
                    </div>
                    <h2>CONSOLIDATED RECON TERMINAL LOGS:</h2>
                    <pre>${log.output}</pre>
                    <p style="text-align: center; margin-top: 30px; font-size: 11px; color: #4b5263;">
                        Padil Developer Zone Intel Reconnaissance Module. Confidential audit trail.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()

        try {
            val cacheFile = File(context.cacheDir, fileName)
            val fw = FileWriter(cacheFile)
            fw.write(htmlContent)
            fw.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PDZ OSINT Report: ${log.target}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Investigative Report HTML"))
        } catch (e: Exception) {
            Log.e("OsintViewModel", "Error sharing report", e)
        }
    }
}

// Custom ViewModel Factory supporting single parameter init
class OsintViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OsintViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OsintViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
