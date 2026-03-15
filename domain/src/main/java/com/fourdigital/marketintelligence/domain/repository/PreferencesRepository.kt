package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun getPreferences(): UserPreferences
    suspend fun updatePreferences(preferences: UserPreferences)
    suspend fun resetToDefaults()
}
