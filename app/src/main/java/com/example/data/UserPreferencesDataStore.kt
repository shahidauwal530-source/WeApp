package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weAppDataStore: DataStore<Preferences> by preferencesDataStore(name = "weapp_prefs")

data class LocalAppPrefs(
    val onboardingCompleted: Boolean = false,
    val permissionsPrompted: Boolean = false,
    val themeMode: String = "dark",
    val fontSize: String = "medium",
    val language: String = GlobalLanguages.detectDeviceLanguage(),
    val dataSaver: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val showFewerAds: Boolean = false,
    val cloudinaryCloudName: String = "",
    val cloudinaryUploadPreset: String = ""
)

class UserPreferencesDataStore(context: Context) {
    private val dataStore = context.applicationContext.weAppDataStore

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PERMISSIONS_PROMPTED = booleanPreferencesKey("permissions_prompted")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val LANGUAGE = stringPreferencesKey("language")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val AUTO_PLAY = booleanPreferencesKey("auto_play_videos")
        val FEWER_ADS = booleanPreferencesKey("show_fewer_ads")
        val CLOUDINARY_CLOUD = stringPreferencesKey("cloudinary_cloud")
        val CLOUDINARY_PRESET = stringPreferencesKey("cloudinary_preset")
    }

    val prefsFlow: Flow<LocalAppPrefs> = dataStore.data.map { prefs ->
        LocalAppPrefs(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            permissionsPrompted = prefs[Keys.PERMISSIONS_PROMPTED] ?: false,
            themeMode = prefs[Keys.THEME_MODE] ?: "dark",
            fontSize = prefs[Keys.FONT_SIZE] ?: "medium",
            language = prefs[Keys.LANGUAGE] ?: GlobalLanguages.detectDeviceLanguage(),
            dataSaver = prefs[Keys.DATA_SAVER] ?: false,
            autoPlayVideos = prefs[Keys.AUTO_PLAY] ?: true,
            showFewerAds = prefs[Keys.FEWER_ADS] ?: false,
            cloudinaryCloudName = prefs[Keys.CLOUDINARY_CLOUD] ?: "",
            cloudinaryUploadPreset = prefs[Keys.CLOUDINARY_PRESET] ?: ""
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPermissionsPrompted(prompted: Boolean) {
        dataStore.edit { it[Keys.PERMISSIONS_PROMPTED] = prompted }
    }

    suspend fun setThemeMode(mode: String) {
        val safe = if (mode in listOf("dark", "light", "system")) mode else "dark"
        dataStore.edit { it[Keys.THEME_MODE] = safe }
    }

    suspend fun setFontSize(size: String) {
        val safe = if (size in listOf("small", "medium", "large")) size else "medium"
        dataStore.edit { it[Keys.FONT_SIZE] = safe }
    }

    suspend fun setLanguage(lang: String) {
        val safe = if (GlobalLanguages.supported.any { it.first == lang }) lang else "en"
        dataStore.edit { it[Keys.LANGUAGE] = safe }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun setAutoPlayVideos(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PLAY] = enabled }
    }

    suspend fun setShowFewerAds(enabled: Boolean) {
        dataStore.edit { it[Keys.FEWER_ADS] = enabled }
    }

    suspend fun setCloudinaryConfig(cloudName: String, uploadPreset: String) {
        dataStore.edit {
            it[Keys.CLOUDINARY_CLOUD] = cloudName.trim()
            it[Keys.CLOUDINARY_PRESET] = uploadPreset.trim()
        }
    }
}
