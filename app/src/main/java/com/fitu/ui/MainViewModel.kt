package com.fitu.ui

import androidx.lifecycle.ViewModel
import com.fitu.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isOnboardingComplete: Flow<Boolean> = userPreferencesRepository.isOnboardingComplete
}
