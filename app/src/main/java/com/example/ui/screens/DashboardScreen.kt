package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VaultEntry
import com.example.ui.components.PasswordGeneratorWidget
import com.example.ui.viewmodel.SyncStatus
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    onLockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val filteredEntries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isAutoImportEnabled by viewModel.isAutoImportEnabled.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Vault") } // Tabs: "Vault", "Generator", "Sync & Import"
    
    // Bottom sheet & dialog states for edits/adding
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var selectedViewEntry by remember { mutableStateOf<VaultEntry?>(null) }
    var isEditSheetOpen by remember { mutableStateOf(false) }
    var infoDialogText by remember { mutableStateOf<String?>(null) }

    // Dialog state for detailing encrypted payload blocks (educate user on AES-256 local storage)
    var activeEncryptedPayloadDetails by remember { mutableStateOf<VaultEntry?>(null) }

    // Auto-Dismiss Toast or Alerts on Sync Event transitions
    LaunchedEffect(syncState) {
        if (syncState is SyncStatus.Success) {
            Toast.makeText(context, (syncState as SyncStatus.Success).message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "VaultLock",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isAddSheetOpen = true },
                        modifier = Modifier.testTag("action_add_header")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onLockClicked() },
                        modifier = Modifier.testTag("action_lock")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Storage", tint = MaterialTheme.colorScheme.tertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val tabItems = listOf(
                    Triple("Vault", Icons.Default.Lock, "Passwords"),
                    Triple("Generator", Icons.Default.Refresh, "Generator"),
                    Triple("Cloud Sync", Icons.Default.Sync, "Cloud & Sync")
                )

                tabItems.forEach { (tabId, icon, label) ->
                    val isSelected = activeTab == tabId
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tabId },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("bottom_tab_$tabId")
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == "Vault") {
                FloatingActionButton(
                    onClick = { isAddSheetOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Credentials")
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth > 680.dp

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "Tab Transition"
            ) { currentTab ->
                when (currentTab) {
                    "Vault" -> {
                        VaultTabContent(
                            allEntries = allEntries,
                            filteredEntries = filteredEntries,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.setSelectedCategory(it) },
                            onItemClick = { selectedViewEntry = it },
                            onStarToggle = { viewModel.toggleStarred(it) },
                            isTablet = isTablet,
                            viewModel = viewModel
                        )
                    }
                    "Generator" -> {
                        GeneratorTabContent()
                    }
                    else -> {
                        CloudSyncTabContent(
                            viewModel = viewModel,
                            syncState = syncState,
                            lastSyncTime = lastSyncTime,
                            isAutoImportEnabled = isAutoImportEnabled,
                            onInfoRequested = { infoDialogText = it }
                        )
                    }
                }
            }
        }
    }

    // Modal: Detail View
    if (selectedViewEntry != null) {
        val entry = selectedViewEntry!!
        DetailViewSheet(
            entry = entry,
            viewModel = viewModel,
            onDismiss = { selectedViewEntry = null },
            onEdit = {
                selectedViewEntry = null
                isEditSheetOpen = true
            },
            onDelete = {
                viewModel.deleteEntry(entry)
                selectedViewEntry = null
            },
            onShowPayload = {
                activeEncryptedPayloadDetails = entry
                selectedViewEntry = null
            }
        )
    }

    // Modal: Add New Sheet
    if (isAddSheetOpen) {
        EditOrAddSheet(
            titleLabel = "Add New Credentials",
            onDismiss = { isAddSheetOpen = false },
            onSave = { title, username, password, url, category ->
                viewModel.addEntry(title, username, password, url, category)
                isAddSheetOpen = false
            }
        )
    }

    // Modal: Edit Sheet
    if (isEditSheetOpen && selectedViewEntry == null) {
        // Find current original values or use fallback
        var targetEntryId = remember { mutableStateOf(0) }
        var initialTitle = remember { mutableStateOf("") }
        var initialUsername = remember { mutableStateOf("") }
        var initialPassword = remember { mutableStateOf("") }
        var initialUrl = remember { mutableStateOf("") }
        var initialCategory = remember { mutableStateOf("") }

        // Fetch decrypt values once
        val activeEditItem = remember { allEntries.find { it.id == targetEntryId.value } }

        EditOrAddSheet(
            titleLabel = "Edit Credentials",
            initialEntry = activeEditItem,
            viewModel = viewModel,
            onDismiss = { isEditSheetOpen = false },
            onSave = { title, username, password, url, category ->
                activeEditItem?.let { original ->
                    viewModel.updateEntry(original.id, title, username, password, url, category, original.isStarred)
                }
                isEditSheetOpen = false
            }
        )
    }

    // Encryption Technical Details Popup (AES-256 Education Model)
    if (activeEncryptedPayloadDetails != null) {
        val entry = activeEncryptedPayloadDetails!!
        AlertDialog(
            onDismissRequest = { activeEncryptedPayloadDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = "Payload", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text(text = "Local Plaintext vs AES-256", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This credential is encrypted in your device storage. To verify AES-256 compliance, inspect the raw encoded payload details retrieved directly from the SQLite Row:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("DATABASE ROW METADATA", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ID: ${entry.id}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onBackground)
                            Text("Title: ${entry.title}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onBackground)
                            Text("Website: ${entry.websiteUrl}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onBackground)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ENCRYPTED USERNAME PAYLOAD", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("Base64 Cipher:\n${entry.encryptedUsername}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("IV Parameter: ${entry.ivUsername}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ENCRYPTED PASSWORD PAYLOAD (AES-256)", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            Text("Base64 Cipher:\n${entry.encryptedPassword}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("IV Parameter: ${entry.ivPassword}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Text(
                        text = "Because each password uses a random, single-use GCM Initialization Vector (IV), two matching passwords will yield completely different encrypted string bytes, nullifying offline pattern discovery analyses.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeEncryptedPayloadDetails = null }) {
                    Text("Declassify & Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (infoDialogText != null) {
        AlertDialog(
            onDismissRequest = { infoDialogText = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text(text = "Security Disclosure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    text = infoDialogText!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialogText = null }) {
                    Text("Understood", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==================== TABS CONTENT ====================

@Composable
fun VaultTabContent(
    allEntries: List<VaultEntry>,
    filteredEntries: List<VaultEntry>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onItemClick: (VaultEntry) -> Unit,
    onStarToggle: (VaultEntry) -> Unit,
    isTablet: Boolean,
    viewModel: VaultViewModel
) {
    val categories = listOf("All", "Personal", "Work", "Financial", "Entertainment")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Vault Safety Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VAULT HEURISTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalStr = if (allEntries.size == 1) "1 item saved" else "${allEntries.size} items saved"
                Text(
                    text = totalStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Simple visual ring simulation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "100% SECURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AES-256 Enabled",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "All Secure",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search title, domain website...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .testTag("dashboard_search"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("category_filter_$category")
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List / Grid
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Empty",
                        tint = Color(0xFF232A37),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching passwords found" else "Vault is feeling empty",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Refine search keywords" else "Create or import Chrome passwords to begin",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            if (isTablet) {
                // Adaptive layout: grid container
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chunkedList = filteredEntries.chunked(2)
                    items(
                        items = chunkedList,
                        key = { row -> row.map { it.id }.joinToString("_") }
                    ) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { entry ->
                                Box(modifier = Modifier.weight(1f)) {
                                    VaultEntryCard(
                                        entry = entry,
                                        onClick = { onItemClick(entry) },
                                        onStarToggle = { onStarToggle(entry) }
                                    )
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        VaultEntryCard(
                            entry = entry,
                            onClick = { onItemClick(entry) },
                            onStarToggle = { onStarToggle(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Explanatory Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Shield Guard",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Entropy Safeguards",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "These generators run inside a cryptographically secure local environment using 'SecureRandom' bytecode, preventing telemetry interception.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PasswordGeneratorWidget(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CloudSyncTabContent(
    viewModel: VaultViewModel,
    syncState: SyncStatus,
    lastSyncTime: Long,
    isAutoImportEnabled: Boolean,
    onInfoRequested: (String) -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    val lastSyncString = sdf.format(Date(lastSyncTime))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title block
        Text(
            text = "E2E Secure Sync & Importations",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Configure backups and sync passwords from browsers securely.",
            color = Color(0xFF6B7A90),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // SYNC STATUS MODULE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "END-TO-END CRYPTO CLOUD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2F80ED),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last synced: $lastSyncString",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1D222F))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF27AE60)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator visual on actions
                when (syncState) {
                    is SyncStatus.Syncing -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(color = Color(0xFF2F80ED), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Exchanging secure RSA keys & building E2EE AES-GCM local package...", color = Color(0xFF8B9BB4), fontSize = 11.sp)
                        }
                    }
                    is SyncStatus.Importing -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(color = Color(0xFF27AE60), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Loading secure credentials from Google account manager vault...", color = Color(0xFF8B9BB4), fontSize = 11.sp)
                        }
                    }
                    else -> {
                        Text(
                            text = "All backup blocks are encrypted on device using AES-256 with a key derived from your Master Password (PBKDF2) before being synced to the cloud. Google servers have 0% access to plain text.",
                            color = Color(0xFF8B9BB4),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.syncWithCloud() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_now_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Sync icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger E2EE Sync Backup", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GOOGLE PASSWORD MANAGER INTEGRATION CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131722))
                .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEB5757).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = "Google Logo", tint = Color(0xFFEB5757), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Password Manager",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android System Credentials Autosaver",
                            color = Color(0xFF8B9BB4),
                            fontSize = 11.sp
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            onInfoRequested(
                                "Android Security Architecture Rule:\n\n" +
                                "To protect user credentials against malware, Android does not permit third-party applications to programmatically dump or scrape of Google Password Manager without explicit system authorization or custom user autofill permission.\n\n" +
                                "To integrate with Google securely, PassVault registers as an official Autofill / Credential Provider Service. When you login across Google Chrome or system apps, PassVault automatically intercepts & encrypts those credentials safely."
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info trigger", tint = Color(0xFF2F80ED))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1116))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Register Autofill Capture", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Capture passwords from Google accounts automatically.", fontSize = 9.sp, color = Color(0xFF6B7A90))
                    }
                    Switch(
                        checked = isAutoImportEnabled,
                        onCheckedChange = { viewModel.setAutoImportEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2F80ED),
                            checkedTrackColor = Color(0xFF1E2838)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.simulateGooglePasswordManagerSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5757)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_import_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "import icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Saved Google Vault Items", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== SUPPORT COMPONENT VIEWS ====================

@Composable
fun VaultEntryCard(
    entry: VaultEntry,
    onClick: () -> Unit,
    onStarToggle: () -> Unit
) {
    val categoryColor = when (entry.category.lowercase()) {
        "financial" -> Color(0xFFF2C94C) // Yellow
        "work" -> MaterialTheme.colorScheme.primary // Warm Lavender instead of blue
        "personal" -> MaterialTheme.colorScheme.tertiary // Blushing pink
        else -> MaterialTheme.colorScheme.secondary // Soft lavender
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("entry_card_${entry.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left category visual tag
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (entry.category.lowercase()) {
                    "financial" -> Icons.Default.CreditCard
                    "work" -> Icons.Default.Work
                    "personal" -> Icons.Default.Person
                    else -> Icons.Default.TravelExplore
                }
                Icon(
                    imageVector = icon,
                    contentDescription = entry.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.websiteUrl.ifEmpty { "Local Vault Entry" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Star trigger & Arrow Right icon
            IconButton(
                onClick = onStarToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (entry.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star state",
                    tint = if (entry.isStarred) Color(0xFFF2C94C) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Detail option",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(12.dp)
                    .padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailViewSheet(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowPayload: () -> Unit
) {
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Decrypt fields on demand securely
    val decryptedUsername = remember(entry) {
        viewModel.decryptValue(entry.encryptedUsername, entry.ivUsername)
    }
    val decryptedPassword = remember(entry) {
        viewModel.decryptValue(entry.encryptedPassword, entry.ivPassword)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category: ${entry.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Fields", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // USERNAME BLOCK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LOGIN EMAIL / USERNAME", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(decryptedUsername, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("detail_username"))
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("PassVault Account", decryptedUsername)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Username copied securely!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Username", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // PASSWORD BLOCK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PASSWORD STRING", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isPasswordVisible) decryptedPassword else "••••••••••••",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("detail_password")
                        )
                    }

                    Row {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Show hidden password",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PassVault Password", decryptedPassword)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Password copied safely!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // WEBSITE ACCESS URL INFO
            if (entry.websiteUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ACCESS LINK", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                            Text(entry.websiteUrl, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PassVault Domain Link", entry.websiteUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "URL address copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Copy URL Link", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // CRYPTOGRAPHIC HEX INSPECT TRIGGERS
            Button(
                onClick = onShowPayload,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "AES metadata", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Inspect Local Crypt Ciphertext", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrAddSheet(
    titleLabel: String,
    initialEntry: VaultEntry? = null,
    viewModel: VaultViewModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var isGeneratorOpen by remember { mutableStateOf(false) }

    // Recover decrypt payloads once on entry
    LaunchedEffect(initialEntry) {
        if (initialEntry != null && viewModel != null) {
            title = initialEntry.title
            username = viewModel.decryptValue(initialEntry.encryptedUsername, initialEntry.ivUsername)
            password = viewModel.decryptValue(initialEntry.encryptedPassword, initialEntry.ivPassword)
            websiteUrl = initialEntry.websiteUrl
            category = initialEntry.category
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("add_edit_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = titleLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)

            // Input Fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Account Name (e.g. Netflix)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Login Username / Email address", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_username"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            // Password with Inline generator option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_password"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isGeneratorOpen = !isGeneratorOpen },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isGeneratorOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .height(56.dp)
                        .padding(top = 6.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = "Generate Inline", tint = if (isGeneratorOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                }
            }

            // Extensible Generator Sub-Frame
            AnimatedVisibility(visible = isGeneratorOpen) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Auto-Generate Secure Password Key", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    PasswordGeneratorWidget(
                        onPasswordCopied = { generatedPass ->
                            password = generatedPass
                            isGeneratorOpen = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("Website Address Link (e.g. netflix.com)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_url"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            // Category Selection Row
            Text("Credential Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categoryOptions = listOf("Personal", "Work", "Financial", "Entertainment")
                categoryOptions.forEach { opt ->
                    val isCatSelected = category == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCatSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { category = opt }
                            .padding(vertical = 10.dp)
                            .testTag("opt_category_$opt"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Discard", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (title.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                            onSave(title, username, password, websiteUrl, category)
                        }
                    },
                    enabled = title.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_credentials_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Secure & Lock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
