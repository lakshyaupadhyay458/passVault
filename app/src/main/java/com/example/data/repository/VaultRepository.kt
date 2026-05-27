package com.example.data.repository

import com.example.data.database.VaultDao
import com.example.data.database.VaultEntry
import com.example.data.security.CryptoHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(private val vaultDao: VaultDao) {
    val allEntries: Flow<List<VaultEntry>> = vaultDao.getAllEntriesFlow()

    suspend fun insertPlainText(
        title: String,
        plainUsername: String,
        plainPassword: String,
        websiteUrl: String,
        category: String,
        isStarred: Boolean = false
    ): Long {
        val encryptedUser = CryptoHelper.encrypt(plainUsername)
        val encryptedPass = CryptoHelper.encrypt(plainPassword)

        val entry = VaultEntry(
            title = title,
            encryptedUsername = encryptedUser.base64Cipher,
            ivUsername = encryptedUser.base64Iv,
            encryptedPassword = encryptedPass.base64Cipher,
            ivPassword = encryptedPass.base64Iv,
            websiteUrl = websiteUrl,
            category = category,
            lastUpdated = System.currentTimeMillis(),
            isStarred = isStarred
        )
        return vaultDao.insertEntry(entry)
    }

    suspend fun updatePlainText(
        id: Int,
        title: String,
        plainUsername: String,
        plainPassword: String,
        websiteUrl: String,
        category: String,
        isStarred: Boolean = false
    ) {
        val encryptedUser = CryptoHelper.encrypt(plainUsername)
        val encryptedPass = CryptoHelper.encrypt(plainPassword)

        val entry = VaultEntry(
            id = id,
            title = title,
            encryptedUsername = encryptedUser.base64Cipher,
            ivUsername = encryptedUser.base64Iv,
            encryptedPassword = encryptedPass.base64Cipher,
            ivPassword = encryptedPass.base64Iv,
            websiteUrl = websiteUrl,
            category = category,
            lastUpdated = System.currentTimeMillis(),
            isStarred = isStarred
        )
        vaultDao.updateEntry(entry)
    }

    suspend fun getEntryById(id: Int): VaultEntry? = vaultDao.getEntryById(id)

    suspend fun deleteEntry(entry: VaultEntry) = vaultDao.deleteEntry(entry)

    suspend fun deleteEntryById(id: Int) = vaultDao.deleteEntryById(id)
}
