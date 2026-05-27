package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val encryptedUsername: String,
    val ivUsername: String,
    val encryptedPassword: String,
    val ivPassword: String,
    val websiteUrl: String,
    val category: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false
)
