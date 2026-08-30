package com.fitu

import android.app.Application
import com.fitu.data.local.dao.StepDao
import com.fitu.data.service.StepCounterService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class FituApplication : Application() {
    
    @Inject
    lateinit var stepDaoProvider: Provider<StepDao>
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        applicationScope.launch {
            try {
                StepCounterService.preloadSteps(stepDaoProvider.get())
            } catch (e: Exception) {
                android.util.Log.w("FituApplication", "Failed to preload steps", e)
            }
        }
    }
}
