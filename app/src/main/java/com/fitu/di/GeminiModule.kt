package com.fitu.di

import android.util.Log
import com.fitu.data.local.SecureStorage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider that creates GenerativeModel instances with the current API key.
 * Includes retry logic, fallback models, and detailed error handling.
 */
@Singleton
class GeminiModelProvider @Inject constructor(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val TAG = "GeminiModelProvider"
        
        // Primary and fallback models
        const val PRIMARY_MODEL = "gemini-3-flash-preview"
        const val FALLBACK_MODEL = "gemini-2.0-flash"
        
        // Retry configuration
        const val MAX_RETRIES = 3
        const val INITIAL_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 10000L
    }

    fun getModel(modelName: String = PRIMARY_MODEL): GenerativeModel? {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "API key is blank")
            return null
        }
        
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    /**
     * Generate content with automatic retry and fallback model support.
     */
    suspend fun generateContentWithRetry(
        prompt: Content,
        modelName: String = PRIMARY_MODEL,
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_DELAY_MS,
        useFallback: Boolean = true
    ): GenerateContentResponse {
        var lastException: Exception? = null
        var currentDelay = initialDelay
        
        // Try primary model
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Attempt ${attempt + 1}/$maxRetries with model: $modelName")
                val model = getModel(modelName) 
                    ?: throw GeminiException(GeminiErrorType.API_KEY_MISSING, "API key not configured")
                
                val response = model.generateContent(prompt)
                
                if (response.text.isNullOrBlank()) {
                    throw GeminiException(GeminiErrorType.EMPTY_RESPONSE, "Model returned empty response")
                }
                
                Log.d(TAG, "Success with model: $modelName")
                return response
                
            } catch (e: GeminiException) {
                lastException = e
                // Don't retry for certain errors
                if (e.errorType == GeminiErrorType.API_KEY_MISSING ||
                    e.errorType == GeminiErrorType.API_KEY_INVALID) {
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                
                // Parse and categorize the error
                val geminiError = parseException(e)
                
                // Don't retry for permanent errors
                if (geminiError.errorType == GeminiErrorType.API_KEY_INVALID ||
                    geminiError.errorType == GeminiErrorType.PERMISSION_DENIED ||
                    geminiError.errorType == GeminiErrorType.INVALID_REQUEST) {
                    throw geminiError
                }
                
                // For rate limit, wait longer
                if (geminiError.errorType == GeminiErrorType.RATE_LIMITED) {
                    currentDelay = maxOf(currentDelay, 5000L)
                }
            }
            
            if (attempt < maxRetries - 1) {
                Log.d(TAG, "Waiting ${currentDelay}ms before retry...")
                delay(currentDelay)
                currentDelay = minOf(currentDelay * 2, MAX_DELAY_MS)
            }
        }
        
        // Try fallback model if enabled and primary failed
        if (useFallback && modelName != FALLBACK_MODEL) {
            Log.d(TAG, "Trying fallback model: $FALLBACK_MODEL")
            try {
                val fallbackModel = getModel(FALLBACK_MODEL)
                    ?: throw GeminiException(GeminiErrorType.API_KEY_MISSING, "API key not configured")
                
                val response = fallbackModel.generateContent(prompt)
                
                if (response.text.isNullOrBlank()) {
                    throw GeminiException(GeminiErrorType.EMPTY_RESPONSE, "Fallback model returned empty response")
                }
                
                Log.d(TAG, "Success with fallback model: $FALLBACK_MODEL")
                return response
                
            } catch (e: Exception) {
                Log.e(TAG, "Fallback model also failed: ${e.message}")
                // Continue to throw the original error
            }
        }
        
        // All attempts failed
        throw lastException?.let { parseException(it) } 
            ?: GeminiException(GeminiErrorType.UNKNOWN, "All attempts failed")
    }

    /**
     * Parse exception into a categorized GeminiException
     */
    private fun parseException(e: Exception): GeminiException {
        val message = e.message?.lowercase() ?: ""
        
        return when {
            e is GeminiException -> e
            
            message.contains("api_key_invalid") || 
            message.contains("invalid api key") ||
            message.contains("api key not valid") ->
                GeminiException(GeminiErrorType.API_KEY_INVALID, "Invalid API key. Please check your settings.")
            
            message.contains("permission_denied") ||
            message.contains("permission denied") ->
                GeminiException(GeminiErrorType.PERMISSION_DENIED, "API key doesn't have permission. Enable Generative Language API in Google Cloud Console.")
            
            message.contains("quota_exceeded") ||
            message.contains("quota") ||
            message.contains("resource_exhausted") ||
            message.contains("rate limit") ->
                GeminiException(GeminiErrorType.RATE_LIMITED, "API rate limit reached. Please wait a moment and try again.")
            
            message.contains("unable to resolve host") ||
            message.contains("no internet") ||
            message.contains("network") ||
            message.contains("connect") ||
            message.contains("unreachable") ->
                GeminiException(GeminiErrorType.NETWORK_ERROR, "No internet connection. Please check your network.")
            
            message.contains("timeout") ||
            message.contains("timed out") ->
                GeminiException(GeminiErrorType.TIMEOUT, "Request timed out. Please try again.")
            
            message.contains("invalid") ||
            message.contains("bad request") ||
            message.contains("400") ->
                GeminiException(GeminiErrorType.INVALID_REQUEST, "Invalid request. Please try a different image.")
            
            message.contains("model") ||
            message.contains("not found") ||
            message.contains("404") ->
                GeminiException(GeminiErrorType.MODEL_NOT_FOUND, "AI model not available. Trying alternative...")
            
            message.contains("safety") ||
            message.contains("blocked") ->
                GeminiException(GeminiErrorType.CONTENT_BLOCKED, "Content was blocked by safety filters. Please try a different image.")
            
            else ->
                GeminiException(GeminiErrorType.UNKNOWN, "Something went wrong: ${e.message?.take(100) ?: "Unknown error"}")
        }
    }
}

/**
 * Custom exception for Gemini API errors with categorized error types
 */
class GeminiException(
    val errorType: GeminiErrorType,
    override val message: String
) : Exception(message)

/**
 * Categorized error types for better error handling
 */
enum class GeminiErrorType {
    API_KEY_MISSING,
    API_KEY_INVALID,
    PERMISSION_DENIED,
    RATE_LIMITED,
    NETWORK_ERROR,
    TIMEOUT,
    INVALID_REQUEST,
    MODEL_NOT_FOUND,
    CONTENT_BLOCKED,
    EMPTY_RESPONSE,
    UNKNOWN
}

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiModelProvider(
        secureStorage: SecureStorage
    ): GeminiModelProvider {
        return GeminiModelProvider(secureStorage)
    }
}
