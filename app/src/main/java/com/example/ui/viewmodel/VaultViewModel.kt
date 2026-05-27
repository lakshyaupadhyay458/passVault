package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.VaultEntry
import com.example.data.repository.VaultRepository
import com.example.data.security.CryptoHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = VaultRepository(database.vaultDao())

    // Decrypt on runtime demand
    fun decryptValue(cipher: String, iv: String): String {
        return CryptoHelper.decrypt(cipher, iv)
    }

    // Vault Entries Flow
    val allEntries: StateFlow<List<VaultEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state states
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Backup & Cloud Sync Mock Simulation States (Supports simulated encryption keys, endpoints & file backup metadata)
    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis() - 3600000 * 4) // 4 hours ago
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // Filtered entries
    val filteredEntries: StateFlow<List<VaultEntry>> = combine(
        allEntries,
        searchQuery,
        selectedCategory
    ) { entries, query, category ->
        entries.filter { entry ->
            val titleMatches = entry.title.contains(query, ignoreCase = true)
            val websiteMatches = entry.websiteUrl.contains(query, ignoreCase = true)
            val categoryMatches = category == "All" || entry.category.equals(category, ignoreCase = true)
            
            (titleMatches || websiteMatches) && categoryMatches
        }
    }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    // Auto-Saving simulator for Google Password Manager Autofill
    private val _isAutoImportEnabled = MutableStateFlow(true)
    val isAutoImportEnabled: StateFlow<Boolean> = _isAutoImportEnabled.asStateFlow()

    fun setAutoImportEnabled(enabled: Boolean) {
        _isAutoImportEnabled.value = enabled
    }

    // DB Operations
    fun addEntry(
        title: String,
        username: String,
        password: String,
        websiteUrl: String,
        category: String,
        isStarred: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertPlainText(title, username, password, websiteUrl, category, isStarred)
        }
    }

    fun updateEntry(
        id: Int,
        title: String,
        username: String,
        password: String,
        websiteUrl: String,
        category: String,
        isStarred: Boolean = false
    ) {
        viewModelScope.launch {
            repository.updatePlainText(id, title, username, password, websiteUrl, category, isStarred)
        }
    }

    fun toggleStarred(entry: VaultEntry) {
        viewModelScope.launch {
            repository.updatePlainText(
                id = entry.id,
                title = entry.title,
                plainUsername = CryptoHelper.decrypt(entry.encryptedUsername, entry.ivUsername),
                plainPassword = CryptoHelper.decrypt(entry.encryptedPassword, entry.ivPassword),
                websiteUrl = entry.websiteUrl,
                category = entry.category,
                isStarred = !entry.isStarred
            )
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    // Cloud Backup Sync Simulation (End-to-End Cryptographic flow)
    fun syncWithCloud() {
        viewModelScope.launch {
            _syncState.value = SyncStatus.Syncing
            kotlinx.coroutines.delay(1800) // Simulate latency of encryption + network handshake
            _syncState.value = SyncStatus.Success("Backup synced successfully. Vault locked with custom PBKDF2 Master Key.")
            _lastSyncTime.value = System.currentTimeMillis()
        }
    }

    // Google Password Manager Auto-Grab Importation simulation
    fun simulateGooglePasswordManagerSync() {
        viewModelScope.launch {
            _syncState.value = SyncStatus.Importing
            kotlinx.coroutines.delay(2000)
            
            // Insert mock common credentials seized from Chrome/Google Saved Autofill
            addEntry("Google Account", "user.email@gmail.com", "MyP@ssword2026", "https://accounts.google.com", "Personal", true)
            addEntry("Netflix", "bingewatcher@gmail.com", "NeflixAndCh1ll!", "https://netflix.com", "Entertainment", false)
            addEntry("Amazon Web Services", "aws_dev_user", "CloudSecure_23#", "https://aws.amazon.com", "Work", false)
            addEntry("Github Cloud", "upadhyaylakshya45", "GitCommit_Secure!", "https://github.com", "Work", true)
            addEntry("Spotify Premium", "musiclover_99", "BeatsAndMelodies8", "https://spotify.com", "Entertainment", false)

            _syncState.value = SyncStatus.Success("Imported 5 encrypted items from Google Password Manager.")
        }
    }

    fun dismissSyncState() {
        _syncState.value = SyncStatus.Idle
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Importing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val errorMsg: String) : SyncStatus()
}
