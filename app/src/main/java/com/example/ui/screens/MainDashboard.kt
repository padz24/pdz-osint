package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.ScanLog
import com.example.data.database.ScheduledScan
import com.example.data.database.Workspace







import com.example.ui.viewmodel.OsintModule
import com.example.ui.viewmodel.OsintViewModel
import com.example.ui.viewmodel.ThreatEvent
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: OsintViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }

    val workspaces by viewModel.workspaces.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val activeTier = viewModel.activeTier

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulse Indicator Dot (Sleek Theme style)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "PDZ-OSINT",

                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "v2.6.0",

                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary, // Sleek primary accent
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                "SESSION: PDZ_ALPHA_882",

                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Sleek themed tier badge: semi-transparent primary colored container with sharp outline
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "${activeTier} TIER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),

                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    // Workspace selection action
                    currentWorkspace?.let { ws ->
                        TextButton(
                            onClick = { showCreateWorkspaceDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Workspace")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                ws.name,
                                maxLines = 1,
                                fontSize = 13.sp,

                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val items = listOf(
                    Triple("Modules", Icons.Default.Search, Icons.Outlined.Search),
                    Triple("Console", Icons.Default.Terminal, Icons.Outlined.Terminal),
                    Triple("Workspaces", Icons.Default.Folder, Icons.Outlined.Folder),
                    Triple("Diff Engine", Icons.Default.Difference, Icons.Outlined.Difference),
                    Triple("Control", Icons.Default.Settings, Icons.Outlined.Settings)
                )

                items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        label = { Text(label, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> ModulesScreen(viewModel)
                1 -> ConsoleShellScreen(viewModel)
                2 -> WorkspaceManagerScreen(viewModel)
                3 -> DiffEngineScreen(viewModel)
                4 -> ControlPanelScreen(viewModel)
            }

            val isOnboardingActive by viewModel.isOnboardingActive.collectAsState()
            val onboardingStep by viewModel.onboardingStep.collectAsState()

            if (isOnboardingActive) {
                Dialog(onDismissRequest = { /* force user engagement */ }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                                    Text("CORE ONBOARDING", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Text("STEP ${onboardingStep + 1} OF 4", fontSize = 11.sp, color = Color.Gray)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            when (onboardingStep) {
                                0 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = "Terminal OSINT",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text(
                                            "PDZ-OSINT RECON SYSTEM",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,

                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "A comprehensive, security-vetted reconnaissance intelligence framework built to identify credentials safety, subdomains leak indicators, and system ports details.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("🚀 SYSTEM CORE ADVANTAGES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                listOf(
                                                    "🛡️ Zero-Trust Security Sandbox",
                                                    "📊 Cross-Workspace Diff target tracker",
                                                    "🏪 Integrated community module store",
                                                    "🤖 Telegram Bot C2 automated sync"
                                                ).forEach { term ->
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Check, contentDescription = "check", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                                        Text(term, fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Licensing Tiers",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text(
                                            "LICENSING & INTEGRATED CODES",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,

                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Access restrictions (Shodan Port Scanner, Tech Stack analyzer, Traceroute) are automatically unlocked matching active key configurations.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                Triple("FREE", "12 Tools", Color.Gray),
                                                Triple("PREMIUM", "28 Tools", MaterialTheme.colorScheme.primary),
                                                Triple("ULTRA", "33 Tools", MaterialTheme.colorScheme.error)
                                            ).forEach { (tier, spec, col) ->
                                                Card(
                                                    modifier = Modifier.weight(1f),
                                                    border = BorderStroke(1.dp, if (activeTier == tier) col else MaterialTheme.colorScheme.outlineVariant),
                                                    colors = CardDefaults.cardColors(containerColor = if (activeTier == tier) col.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(tier, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = col)
                                                        Text(spec, fontSize = 9.sp, color = Color.Gray)
                                                        if (activeTier == tier) {
                                                            Text("ACTIVE", fontSize = 8.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Simulate Tier Activation instantly:", fontSize = 11.sp, color = Color.Gray)
                                            
                                            Button(
                                                onClick = { viewModel.licenseKey = "PDZ-PREMIUM-2026" },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = Color.White),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("🔑 CLICK TO DEPLOY PREMIUM ACCESS", fontSize = 10.sp)
                                            }

                                            Button(
                                                onClick = { viewModel.licenseKey = "PDZ-ULTRA-2026" },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = Color.White),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("💎 CLICK TO DEPLOY ULTRA ACCESS", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hub,
                                            contentDescription = "Ports Setup",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text(
                                            "PERIPHERALS PORT DETECTOR",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,

                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Toggle high performance HTTP server to stream recon logs, or connect the Telegram C2 remote trigger bot listener.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Local API Web Server (8080/tcp)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text("Sync JSON data models", fontSize = 9.sp, color = Color.Gray)
                                                    }
                                                    val isServerActive by viewModel.isWebServerRunning.collectAsState()
                                                    Switch(
                                                        checked = isServerActive,
                                                        onCheckedChange = { viewModel.toggleWebServer(it) }
                                                    )
                                                }

                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Telegram C2 Bot Service", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text("Fire scans from chat", fontSize = 9.sp, color = Color.Gray)
                                                    }
                                                    var isTelegramActive by remember { mutableStateOf(viewModel.isTelegramBotEnabled) }
                                                    Switch(
                                                        checked = isTelegramActive,
                                                        onCheckedChange = {
                                                            isTelegramActive = it
                                                            viewModel.isTelegramBotEnabled = it
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = "Checklists",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text(
                                            "RECON ENVIRONMENT CHECK",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,

                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Complete baseline target queries details before launching first recon action.",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )

                                        var checkA by remember { mutableStateOf(true) }
                                        var checkB by remember { mutableStateOf(true) }
                                        var checkC by remember { mutableStateOf(false) }

                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                Triple("Configure Workspace tracking targets", checkA) { checkA = !checkA },
                                                Triple("Subscribe Threat feeds indices list", checkB) { checkB = !checkB },
                                                Triple("Input digital key validator hashes", checkC) { checkC = !checkC }
                                            ).forEach { (desc, isChecked, toggle) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { toggle() }
                                                        .border(1.dp, if (isChecked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                                        .background(if (isChecked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f) else Color.Transparent)
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { toggle() },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.tertiary)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(desc, fontSize = 11.sp, color = if (isChecked) Color.White else Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (onboardingStep > 0) {
                                            viewModel.setOnboardingStep(onboardingStep - 1)
                                        } else {
                                            viewModel.setOnboardingActive(false)
                                        }
                                    }
                                ) {
                                    Text(if (onboardingStep == 0) "SKIP TOUR" else "BACK", color = Color.Gray, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (onboardingStep < 3) {
                                            viewModel.setOnboardingStep(onboardingStep + 1)
                                        } else {
                                            viewModel.setOnboardingActive(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        if (onboardingStep == 3) "DEPLOY TERMINAL 🚀" else "CONTINUE",
                                        fontWeight = FontWeight.Bold,

                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateWorkspaceDialog) {
        WorkspaceSelectDialog(
            workspaces = workspaces,
            currentWorkspace = currentWorkspace,
            onSelect = {
                viewModel.setWorkspaceId(it.id)
                showCreateWorkspaceDialog = false
            },
            onAddWorkspace = { name, desc, target ->
                viewModel.addWorkspace(name, desc, target)
                showCreateWorkspaceDialog = false
            },
            onDeleteWorkspace = {
                viewModel.deleteWorkspace(it)
            },
            onDismiss = { showCreateWorkspaceDialog = false }
        )
    }
}

// --- TAB 1: MODULES SCREEN ---
@Composable
fun ModulesScreen(viewModel: OsintViewModel) {
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanOutput by viewModel.scanOutput.collectAsState()
    val currentScanningModule by viewModel.currentScanningModule.collectAsState()
    val activeTier = viewModel.activeTier

    // Local sub-tabs inside ModulesScreen
    var activeSubTab by remember { mutableStateOf(0) } // 0: Scanners, 1: Threat Feeds, 2: Community Hub

    var queryInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High-fidelity Sub-Tab Segment Row
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("💻 RECON CORE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("🛡️ THREAT FEEDS", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("🏪 COMMUNITY HUB", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (activeSubTab) {
            0 -> {
                // --- SUB-TABS 0: RECON ACTIONS GRID (Original functional scanners + logs panel) ---
                var searchQuery by remember { mutableStateOf("") }
                var selectedCategory by remember { mutableStateOf("ALL") }

                val allAvailable by viewModel.allAvailableModules.collectAsState()
                val filteredModules = remember(allAvailable, searchQuery, selectedCategory) {
                    allAvailable.filter {
                        (selectedCategory == "ALL" || it.tier == selectedCategory) &&
                                (it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.key.contains(searchQuery, ignoreCase = true) ||
                                        it.description.contains(searchQuery, ignoreCase = true))
                    }
                }

                var activeRunningModule by remember { mutableStateOf<OsintModule?>(null) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step 1: Query Target Inputs
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "🔍 TARGET PARAMETERS",

                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = queryInput,
                                    onValueChange = { queryInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_input_field"),
                                    placeholder = {
                                        Text(
                                            "e.g. google.com, 192.168.4.1, malicious-phish-domain.xyz",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    },
                                    label = { Text("Active Query Target") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    trailingIcon = {
                                        if (queryInput.isNotBlank()) {
                                            IconButton(onClick = { queryInput = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                currentWorkspace?.let { ws ->
                                    Text(
                                        "Default Workspace target is: ${ws.target.ifBlank { "Unset" }}",
                                        fontSize = 11.sp,
                                        color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Step 2: Module Filtering Tab controls
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "FREE", "PREMIUM", "ULTRA").forEach { cat ->
                                    val isCatSelected = selectedCategory == cat
                                    OutlinedButton(
                                        onClick = { selectedCategory = cat },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isCatSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            cat,
                                            color = if (isCatSelected) MaterialTheme.colorScheme.tertiary else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Filter available scanning modules...", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    // Step 3: Modules grid inside lazy list
                    item {
                        Text(
                            "SELECT MODULE RUNNER (${filteredModules.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray)
                    }

                    item {
                        // Display modules as custom list row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredModules.forEach { mod ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeRunningModule = mod
                                        },
                                    border = BorderStroke(
                                        1.dp,
                                        if (currentScanningModule == mod.key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    mod.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (mod.tier) {
                                                        "ULTRA" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                        "PREMIUM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        when (mod.tier) {
                                                            "ULTRA" -> MaterialTheme.colorScheme.error
                                                            "PREMIUM" -> MaterialTheme.colorScheme.primary
                                                            else -> Color.Gray
                                                        }
                                                    ),
                                                    contentColor = when (mod.tier) {
                                                        "ULTRA" -> MaterialTheme.colorScheme.error
                                                        "PREMIUM" -> MaterialTheme.colorScheme.primary
                                                        else -> Color.White
                                                    }
                                                ) {
                                                    Text(
                                                        mod.tier,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                mod.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.runScan(mod, queryInput)
                                            },
                                            modifier = Modifier.testTag("run_module_button_${mod.key}")
                                        ) {
                                            if (isScanning && currentScanningModule == mod.key) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.tertiary)
                                            } else {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Run",
                                                    tint = if (activeTier == "FREE" && mod.tier != "FREE") Color.DarkGray else MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 4: Active terminal live block
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "💻 OSINT TERMINAL OUTPUT",

                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 13.sp
                                    )
                                    IconButton(onClick = { viewModel.clearAllScanLogs() }) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Wipe logs", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 180.dp, max = 350.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        scanOutput,
                                        color = MaterialTheme.colorScheme.tertiary,

                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // --- SUB-TAB 1: THREAT INTELLIGENCE PORTAL ---
                val subscribedFeeds by viewModel.subscribedFeedIds.collectAsState()
                val matchedThreats = remember(queryInput, subscribedFeeds) {
                    viewModel.findThreatIndicatorsForTarget(queryInput)
                }

                var customSearchKey by remember { mutableStateOf("") }
                val manualMatchedThreats = remember(customSearchKey, subscribedFeeds) {
                    viewModel.findThreatIndicatorsForTarget(customSearchKey)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "🛡️ GLOBAL THREAT FEED SUBSCRIPTIONS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select and sync real-time intel blacklists. Compromised indicators are automatically flagged in targets searches.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Grid of standard Threat Feeds with licensing constraints
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.threatFeedsCatalog.forEach { feed ->
                                val isSubbed = subscribedFeeds.contains(feed.id)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSubbed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSubbed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    feed.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (feed.tier) {
                                                        "ULTRA" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                        "PREMIUM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else -> Color.DarkGray
                                                    },
                                                    contentColor = when (feed.tier) {
                                                        "ULTRA" -> MaterialTheme.colorScheme.error
                                                        "PREMIUM" -> MaterialTheme.colorScheme.primary
                                                        else -> Color.White
                                                    }
                                                ) {
                                                    Text(
                                                        feed.tier,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(
                                                feed.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Category: ${feed.category} • Version: ${feed.version}",
                                                fontSize = 9.sp,
                                                color = Color.Gray)
                                        }

                                        IconButton(onClick = { viewModel.toggleFeedSubscription(feed.id) }) {
                                            Icon(
                                                imageVector = if (isSubbed) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                                contentDescription = "Toggle",
                                                tint = if (isSubbed) MaterialTheme.colorScheme.tertiary else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Warnings matching license tiers
                    if (activeTier == "FREE") {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.error)
                                    Column {
                                        Text("ADVANCED THREAT INTEL BLOCKED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Upgrade to PREMIUM or ULTRA level inside Control options to launch dark web leak trackers and national agency threat indicators.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Threat Radar Block Matching Active Query Target
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "🛸 ACTIVE SEARCH MATCH RADAR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Threat indicators found matching your active target parameters: '${queryInput.ifBlank { "Unset" }}'",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (matchedThreats.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.VerifiedUser, "safe", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("NO DETECTED INDICATORS MATCHED", fontSize = 11.sp, color = Color.White)
                                            Text("Selected target is clean in the current active feeds databases.", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        matchedThreats.forEach { threat ->
                                            ThreatIndicatorAlertCard(threat)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Standalone Manual Indicator Tester
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "🔎 DIRECT INDICATOR DIRECTORY CHECK",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customSearchKey,
                                    onValueChange = { customSearchKey = it },
                                    label = { Text("Manual IP, Email, or Domain Audit") },
                                    placeholder = { Text("Try: 192.168.4.1 , user@email.com , 178.23.45.67", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                                    singleLine = true
                                )

                                if (customSearchKey.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (manualMatchedThreats.isEmpty()) {
                                        Text("No matched logs in database for: '$customSearchKey'. Ensure corresponding feed is subscribed.", fontSize = 11.sp, color = Color.Gray)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            manualMatchedThreats.forEach { threat ->
                                                ThreatIndicatorAlertCard(threat)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // --- SUB-TAB 2: COMMUNITY ENDPOINT MARKETPLACE (Modules sharing + rating) ---
                val communityModules by viewModel.communityModules.collectAsState()

                var uploadName by remember { mutableStateOf("") }
                var uploadDesc by remember { mutableStateOf("") }
                var uploadCreator by remember { mutableStateOf("") }
                var uploadCategory by remember { mutableStateOf("RECON") }
                var showPublishForm by remember { mutableStateOf(false) }

                var filterCategory by remember { mutableStateOf("ALL") }

                val displayedMarketModules = remember(communityModules, filterCategory) {
                    if (filterCategory == "ALL") communityModules else communityModules.filter { it.category == filterCategory }
                }

                // Star written review submission state variables
                var reviewTargetKey by remember { mutableStateOf("") }
                var reviewComment by remember { mutableStateOf("") }
                var reviewRaterName by remember { mutableStateOf("") }
                var reviewRatingValue by remember { mutableStateOf(5) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "🏪 COMMUNITY PLUGINS MARKETPLACE",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White)
                                    Text(
                                        "Download custom tool modules shared by global OSINT developers.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Button(
                                    onClick = { showPublishForm = !showPublishForm },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showPublishForm) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = if (showPublishForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, if (showPublishForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (showPublishForm) "CLOSE FORM" else "🚀 SHARE MODULE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Share Module Form Block
                    if (showPublishForm) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("PUBLISH CUSTOM TOOL PROTOCOL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    
                                    OutlinedTextField(
                                        value = uploadName,
                                        onValueChange = { uploadName = it },
                                        label = { Text("Module Title") },
                                        placeholder = { Text("e.g. DNS Hijacker Scout", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = uploadDesc,
                                        onValueChange = { uploadDesc = it },
                                        label = { Text("Functional Capability Description") },
                                        placeholder = { Text("Explain what target properties this extracts", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = uploadCreator,
                                            onValueChange = { uploadCreator = it },
                                            label = { Text("Author Alias") },
                                            placeholder = { Text("e.g. ghost_operator", color = Color.Gray) },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                                            singleLine = true
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Category", fontSize = 11.sp, color = Color.Gray)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("RECON", "EXPLOITS", "VULNERABILITY", "FORENSICS").forEach { cat ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clickable { uploadCategory = cat }
                                                            .border(1.dp, if (uploadCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                                            .background(if (uploadCategory == cat) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(cat, fontSize = 8.sp, color = if (uploadCategory == cat) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (uploadName.isNotBlank() && uploadDesc.isNotBlank()) {
                                                val creator = uploadCreator.ifBlank { "anonymous" }
                                                viewModel.uploadCommunityModule(uploadName, uploadDesc, uploadCategory, creator)
                                                uploadName = ""
                                                uploadDesc = ""
                                                uploadCreator = ""
                                                showPublishForm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("DEPLOY TO COMMUNITY LIVE PROTOCOL 🚀", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Group Switched Navigation
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "RECON", "EXPLOITS", "VULNERABILITY", "FORENSICS").forEach { cat ->
                                val currentCat = filterCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clickable { filterCategory = cat }
                                        .border(1.dp, if (currentCat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                        .background(if (currentCat) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        cat,
                                        color = if (currentCat) Color.White else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Database listing
                    items(displayedMarketModules) { mod ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            border = BorderStroke(1.dp, if (mod.isDownloaded) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Metadata row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            mod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "by ${mod.creator}",
                                            fontSize = 11.sp,
                                            color = Color.Gray)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (mod.isVetted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (mod.isVetted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error),
                                        contentColor = if (mod.isVetted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            if (mod.isVetted) "SAFE & AUDITED" else "UNVETTED PROTOCOL",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Text(
                                    mod.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("⭐ ${mod.rating} (${mod.reviewsCount} reviews)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        Text("•", fontSize = 11.sp, color = Color.Gray)
                                        Text("size: ${mod.sizeKb}kb", fontSize = 11.sp, color = Color.Gray)
                                        Text("•", fontSize = 11.sp, color = Color.Gray)
                                        Text(mod.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }

                                    if (mod.isDownloaded) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "V", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                            Text("COMPILED & DEPLOYED", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.downloadCommunityModule(mod.key) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("⬇️ DOWNLOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Expanded view detailing vetting log and reviewing
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Vetting text report
                                    Text("🔒 INTEGRITY & ISOLATION LOGS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(mod.vettingLog, fontSize = 11.sp, color = if (mod.isVetted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Writing user reviews inline
                                    Text("📝 FEEDBACK CRITIQUE PORTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Write rating audit critique:", fontSize = 11.sp, color = Color.White)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            (1..5).forEach { star ->
                                                val active = star <= reviewRatingValue
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "star",
                                                    tint = if (active) MaterialTheme.colorScheme.error else Color.DarkGray,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clickable {
                                                            reviewTargetKey = mod.key
                                                            reviewRatingValue = star
                                                        }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = if (reviewTargetKey == mod.key) reviewComment else "",
                                            onValueChange = {
                                                reviewTargetKey = mod.key
                                                reviewComment = it
                                            },
                                            placeholder = { Text("Leave comment about testing findings...", color = Color.Gray, fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                                        )

                                        OutlinedTextField(
                                            value = if (reviewTargetKey == mod.key) reviewRaterName else "",
                                            onValueChange = {
                                                reviewTargetKey = mod.key
                                                reviewRaterName = it
                                            },
                                            placeholder = { Text("Reviewer signature alias", color = Color.Gray, fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                                            singleLine = true
                                        )

                                        Button(
                                            onClick = {
                                                if (reviewComment.isNotBlank()) {
                                                    val sign = reviewRaterName.ifBlank { "anonymous" }
                                                    viewModel.submitModuleReview(mod.key, sign, reviewRatingValue, reviewComment)
                                                    reviewComment = ""
                                                    reviewRaterName = ""
                                                    reviewTargetKey = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("SUBMIT AUDIT FEEDBACK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Display written feedback critic lists
                                    if (mod.reviews.isNotEmpty()) {
                                        Text("USER REVIEWS & REPORTS (${mod.reviews.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            mod.reviews.forEach { rev ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.2f))
                                                        .padding(6.dp)
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("👤 ${rev.rater}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        Text("⭐ ${rev.rating}/5 • ${rev.timestamp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                                    }
                                                    Text(rev.comment, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatIndicatorAlertCard(threat: ThreatEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130707)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Text(
                        "THREAT DETECTED: ${threat.indicator}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        threat.severity,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(threat.description, fontSize = 12.sp, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Source: ${threat.sourceFeedName} • First detected: ${threat.firstSeen}", fontSize = 10.sp, color = Color.Gray)
            Text("Details: ${threat.details}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}


// --- TAB 2: CLI TERMINAL SHEET ---
@Composable
fun ConsoleShellScreen(viewModel: OsintViewModel) {
    val terminalHistory by viewModel.terminalHistory.collectAsState()
    var cmdInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Auto scroll bottom when console logs update
    LaunchedEffect(terminalHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408))
            .padding(12.dp)
    ) {
        // Terminal Window Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "root@pdz-osint-android:~",

                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "v2.6.0-sh",

                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        // Scrollable logs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            Column {
                terminalHistory.forEach { line ->
                    Text(
                        line,
                        color = if (line.startsWith("$>")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,

                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input shell line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "pdz > ",
                color = MaterialTheme.colorScheme.primary,

                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                placeholder = {
                    Text(
                        "run -m dns_resolver -t google.com",
                        color = Color.DarkGray,
                        fontSize = 12.sp)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (cmdInput.isNotBlank()) {
                        viewModel.executeShellCommand(cmdInput)
                        cmdInput = ""
                    }
                }),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.tertiary,

                    fontSize = 12.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Black,
                    unfocusedContainerColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = {
                    if (cmdInput.isNotBlank()) {
                        viewModel.executeShellCommand(cmdInput)
                        cmdInput = ""
                    }
                },
                modifier = Modifier.testTag("terminal_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- TAB 3: WORKSPACES MANAGER ---
@Composable
fun WorkspaceManagerScreen(viewModel: OsintViewModel) {
    val context = LocalContext.current
    val workspaces by viewModel.workspaces.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val logs by viewModel.currentWorkspaceLogs.collectAsState()

    var showCreateWorkspaceSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Workspace visual summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📂 WORKSPACE CONTAINER PROFILE",

                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showCreateWorkspaceSheet = true }) {
                            Icon(Icons.Default.AddBox, contentDescription = "New workspace", tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    currentWorkspace?.let { ws ->
                        Text(
                            "Container Name: ${ws.name}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Description : ${ws.description}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Investigation Target IP/Host: ${ws.target.ifBlank { "Unspecified" }}",

                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Scans listing inside Workspace
        item {
            Text(
                "HISTORIC CONTAINER RUNS (${logs.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Gray)
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HistoryToggleOff,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No scan history found inside this workspace container.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    log.moduleName,
                                    fontWeight = FontWeight.Bold,

                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Target: ${log.target}",
                                    fontSize = 11.sp,

                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Row {
                                IconButton(onClick = { viewModel.exportHtmlReport(context, log) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteScanLog(log) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Collapsed snippet view
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .border(1.dp, Color(0xFF1B1B1B))
                                .padding(8.dp)
                        ) {
                            Text(
                                log.output.take(240) + (if (log.output.length > 240) "\n...[TRUNCATED]" else ""),
                                color = if (log.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,

                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateWorkspaceSheet) {
        CreateWorkspaceDialog(
            onCreate = { name, desc, target ->
                viewModel.addWorkspace(name, desc, target)
                showCreateWorkspaceSheet = false
            },
            onDismiss = { showCreateWorkspaceSheet = false }
        )
    }
}

// --- TAB 4: DIFF ENGINE SCREEN ---
@Composable
fun DiffEngineScreen(viewModel: OsintViewModel) {
    val logs by viewModel.currentWorkspaceLogs.collectAsState()
    val logA by viewModel.diffLogA.collectAsState()
    val logB by viewModel.diffLogB.collectAsState()
    val diffResult by viewModel.diffResult.collectAsState()

    var showChooseADialog by remember { mutableStateOf(false) }
    var showChooseBDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔄 RECON DELTA / DIFF ENGINE",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,

                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Isolate structural changes across two distinct scans of host target configurations to identify dynamic port updates or DNS manipulations.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Selection row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BASELINE SCAN (A)",
                        fontSize = 10.sp,

                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showChooseADialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            logA?.let { "${it.moduleName}\n(${it.target})" } ?: "Select Scan",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "COMPARISON SCAN (B)",
                        fontSize = 10.sp,

                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showChooseBDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            logB?.let { "${it.moduleName}\n(${it.target})" } ?: "Select Scan",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White)
                    }
                }
            }
        }

        // Diff output visualizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "⚖️ DELTA RED/GREEN CONSOLE",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,

                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (diffResult.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "[No comparative diff parameters calculated]\nSelect Baseline Scans A and B above to evaluate.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(8.dp)
                        ) {
                            diffResult.forEach { line ->
                                val color = when (line.type) {
                                    OsintViewModel.DiffType.ADDED -> MaterialTheme.colorScheme.tertiary
                                    OsintViewModel.DiffType.REMOVED -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val bg = when (line.type) {
                                    OsintViewModel.DiffType.ADDED -> Color(0xFF112D18)
                                    OsintViewModel.DiffType.REMOVED -> Color(0xFF33161A)
                                    else -> Color.Transparent
                                }
                                Text(
                                    line.text,
                                    color = color,

                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChooseADialog) {
        ScanSelectDialog(
            logs = logs,
            onSelect = {
                viewModel.setDiffA(it)
                showChooseADialog = false
            },
            onDismiss = { showChooseADialog = false }
        )
    }

    if (showChooseBDialog) {
        ScanSelectDialog(
            logs = logs,
            onSelect = {
                viewModel.setDiffB(it)
                showChooseBDialog = false
            },
            onDismiss = { showChooseBDialog = false }
        )
    }
}

// --- TAB 5: CONTROL / SERVICES PANEL ---
@Composable
fun ControlPanelScreen(viewModel: OsintViewModel) {
    val isServerRunning by viewModel.isWebServerRunning.collectAsState()
    val serverLogs by viewModel.webServerLogs.collectAsState()
    val schedules by viewModel.scheduledScans.collectAsState()

    var shodanKey by remember { mutableStateOf(viewModel.shodanApiKey) }
    var tgToken by remember { mutableStateOf(viewModel.telegramBotToken) }
    var tgEnabled by remember { mutableStateOf(viewModel.isTelegramBotEnabled) }
    var activeLicenseKey by remember { mutableStateOf(viewModel.licenseKey) }

    var showAddScheduleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Local HTTP/REST Server Portal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🌐 LOCAL HTTP / REST GATE",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,

                                fontSize = 14.sp
                            )
                            Text(
                                "Serve dark UI & REST APIs on Port 8080",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = { viewModel.toggleWebServer(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Status: ${if (isServerRunning) "SERVER RUNNING" else "STOPPED"}",
                        fontWeight = FontWeight.Bold,
                        color = if (isServerRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp)

                    if (isServerRunning) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Local address endpoint: http://localhost:8080\nREST path: http://localhost:8080/api/scan",
                            color = Color.White,

                            fontSize = 11.sp
                        )
                    }

                    // REST Server output console
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color.Black)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            if (serverLogs.isEmpty()) {
                                item {
                                    Text(
                                        "No REST connection events logged.",
                                        color = Color.DarkGray,
                                        fontSize = 11.sp)
                                }
                            } else {
                                items(serverLogs) { line ->
                                    Text(
                                        line,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Scheduled scan profiles & webhook alerting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "⏰ TARGET RECON SCHEDULES",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,

                                fontSize = 14.sp
                            )
                            Text(
                                "Automated scanning alerts & Discord webhooks",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { showAddScheduleDialog = true }) {
                            Icon(Icons.Default.AlarmAdd, contentDescription = "Add Schedule", tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (schedules.isEmpty()) {
                        Text(
                            "No automated scheduled tasks configured.",
                            fontSize = 11.sp,
                            color = Color.Gray,

                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            schedules.forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "${s.moduleName} -> ${s.target}",

                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "Interval: ${s.intervalMinutes}m | Webhook: ${if (s.webhookUrl.isBlank()) "None" else "Active (Discord)"}",
                                            fontSize = 10.sp,
                                            color = Color.Gray)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (s.webhookUrl.isNotBlank()) {
                                            IconButton(onClick = { viewModel.testWebhook(s.webhookUrl) }) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = "Ping webhook", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Switch(
                                            checked = s.isActive,
                                            onCheckedChange = { viewModel.toggleSchedule(s) },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        IconButton(onClick = { viewModel.deleteSchedule(s) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: API Secrets, Bot Configurations, and Licensing credentials
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔑 CREDENTIALS & LICENSE VAULT",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,

                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // License key
                    OutlinedTextField(
                        value = activeLicenseKey,
                        onValueChange = {
                            activeLicenseKey = it
                            viewModel.licenseKey = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("PDZ Framework License code") },
                        placeholder = { Text("e.g. PDZ-ULTRA-2026", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    Text(
                        "Supports: PDZ-PREMIUM-2026 or PDZ-ULTRA-2026",
                        fontSize = 10.sp,
                        color = Color.Gray,

                        modifier = Modifier.padding(vertical = 3.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shodan API key
                    OutlinedTextField(
                        value = shodanKey,
                        onValueChange = {
                            shodanKey = it
                            viewModel.shodanApiKey = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Shodan API Secret Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Telegram Bot Token
                    OutlinedTextField(
                        value = tgToken,
                        onValueChange = {
                            tgToken = it
                            viewModel.telegramBotToken = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Telegram Bot Token Secret") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Deploy active Telegram Bot listener",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = tgEnabled,
                            onCheckedChange = {
                                tgEnabled = it
                                viewModel.isTelegramBotEnabled = it
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🧭 ENVIRONMENT OVERVIEW TOUR",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,

                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Review PDZ-OSINT capabilities, user licensing tier specifications, and system connection endpoints configurations.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.setOnboardingStep(0)
                            viewModel.setOnboardingActive(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LAUNCH INTERACTIVE TOUR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            modules = viewModel.modules,
            onAdd = { moduleName, target, interval, webhook ->
                viewModel.addSchedule(moduleName, target, interval, webhook)
                showAddScheduleDialog = false
            },
            onDismiss = { showAddScheduleDialog = false }
        )
    }
}

// Custom modifier helper to scale down switches
@Composable
fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.size((48 * scale).dp, (24 * scale).dp))

// --- DIALOGS CONTROLS ---

@Composable
fun WorkspaceSelectDialog(
    workspaces: List<Workspace>,
    currentWorkspace: Workspace?,
    onSelect: (Workspace) -> Unit,
    onAddWorkspace: (String, String, String) -> Unit,
    onDeleteWorkspace: (Workspace) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isCreatingNew) {
                    Text(
                        "🆕 DEPLOY NEW CONTAINER",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,

                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Container Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Recon Goals / Description") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Investigation Target domain") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCreatingNew = false }) {
                            Text("BACK", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onAddWorkspace(name, desc, target)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.Black)
                        ) {
                            Text("DEPLOY", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        "📂 MOUNT WORKSPACE CONTAINER",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,

                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
                        items(workspaces) { ws ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(ws) }
                                    .background(if (ws.id == currentWorkspace?.id) Color(0xFF162B17) else Color.Transparent)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ws.name,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ws.id == currentWorkspace?.id) MaterialTheme.colorScheme.tertiary else Color.White,

                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Target: ${ws.target.ifBlank { "Unset" }}",
                                        fontSize = 11.sp,
                                        color = Color.Gray)
                                }
                                if (workspaces.size > 1) {
                                    IconButton(onClick = { onDeleteWorkspace(ws) }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { isCreatingNew = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("CREATE NEW", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("CLOSE", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWorkspaceDialog(
    onCreate: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🆕 DEPLOY GENERAL WORKSPACE",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,

                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Container Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Recon Goals / Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Active Target IP/Domain") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onCreate(name, desc, target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.Black)
                    ) {
                        Text("CREATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanSelectDialog(
    logs: List<ScanLog>,
    onSelect: (ScanLog) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📊 DEFINE SCAN SOURCE",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,

                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                "No scans registered in this profile.",
                                color = Color.Gray,
                                fontSize = 12.sp)
                        }
                    } else {
                        items(logs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .clickable { onSelect(log) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        log.moduleName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp)
                                    Text(
                                        "Target: ${log.target} | ${java.text.DateFormat.getTimeInstance().format(java.util.Date(log.timestamp))}",
                                        fontSize = 10.sp,
                                        color = Color.Gray)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Select", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    modules: List<OsintModule>,
    onAdd: (String, String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedModule by remember { mutableStateOf(modules.first().name) }
    var targetInput by remember { mutableStateOf("") }
    var intervalStr by remember { mutableStateOf("60") }
    var webhookUrl by remember { mutableStateOf("") }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⏰ LAUNCH CRON RECON TRIGGER",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,

                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Module Select Box
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            "Module Selector: $selectedModule",
                            color = Color.White,
                            fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        modules.forEach { mod ->
                            DropdownMenuItem(
                                text = { Text(mod.name, color = Color.White) },
                                onClick = {
                                    selectedModule = mod.name
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Monitored Destination IP/Domain") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it },
                    label = { Text("Repetitive Sweep Interval (min)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Discord / Slack Webhook URL Connection") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val interval = intervalStr.toIntOrNull() ?: 60
                            if (targetInput.isNotBlank()) {
                                onAdd(selectedModule, targetInput, interval, webhookUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.Black)
                    ) {
                        Text("TRIGGER", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
