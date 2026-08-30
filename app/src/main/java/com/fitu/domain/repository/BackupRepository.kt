package com.fitu.domain.repository

import android.net.Uri

interface BackupRepository {
    /**
     * Export all app data to a JSON file
     * @param includeApiKey Whether to include the API key in the backup
     * @return Result containing the URI of the created backup file
     */
    suspend fun exportData(includeApiKey: Boolean = false): Result<Uri>

    /**
     * Import data from a backup JSON file
     * @param uri The URI of the backup file
     * @return Result indicating success or failure with error message
     */
    suspend fun importData(uri: Uri): Result<Unit>

    /**
     * Get a preview of what's in a backup file without importing
     * @param uri The URI of the backup file
     * @return Result containing backup metadata
     */
    suspend fun getBackupInfo(uri: Uri): Result<BackupInfo>
}

data class BackupInfo(
    val appVersion: String,
    val exportDate: Long,
    val stepRecords: Int,
    val mealRecords: Int,
    val workoutRecords: Int,
    val workoutPlans: Int,
    val hasUserProfile: Boolean,
    val hasApiKey: Boolean
)
