package com.cubelens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cubelens.camera.ColorCalibration
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cubelens_prefs")

enum class ThemeMode(val key: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromKey(key: String): ThemeMode =
      entries.firstOrNull { it.key == key } ?: SYSTEM
  }
}

class PreferencesManager(private val context: Context) {

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val INSPECTION_ENABLED = booleanPreferencesKey("inspection_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CAMERA_LENS_FACING = intPreferencesKey("camera_lens_facing")
    val COLOR_HUE_OFFSET = floatPreferencesKey("color_hue_offset")
    val COLOR_WHITE_SAT_MAX = floatPreferencesKey("color_white_sat_max")
  }

  // ── Onboarding ──────────────────────────────────────────────────────────────

  val onboardingCompleted: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

  val inspectionEnabled: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[Keys.INSPECTION_ENABLED] ?: true
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[Keys.ONBOARDING_COMPLETED] = completed
    }
  }

  suspend fun setInspectionEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[Keys.INSPECTION_ENABLED] = enabled
    }
  }

  // ── Theme ───────────────────────────────────────────────────────────────────

  val themeMode: Flow<ThemeMode> = context.dataStore.data
    .map { preferences ->
      val key = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.key
      ThemeMode.fromKey(key)
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { preferences ->
      preferences[Keys.THEME_MODE] = mode.key
    }
  }

  // ── Camera lens facing (0 = back, 1 = front) ────────────────────────────────

  val cameraLensFacing: Flow<Int> = context.dataStore.data
    .map { preferences ->
      preferences[Keys.CAMERA_LENS_FACING] ?: androidx.camera.core.CameraSelector.LENS_FACING_BACK
    }

  suspend fun setCameraLensFacing(lensFacing: Int) {
    context.dataStore.edit { preferences ->
      preferences[Keys.CAMERA_LENS_FACING] = lensFacing
    }
  }

  // ── Color calibration ─────────────────────────────────────────────────────────

  val colorCalibration: Flow<ColorCalibration> = context.dataStore.data
    .map { preferences ->
      ColorCalibration(
        hueOffsetDeg = preferences[Keys.COLOR_HUE_OFFSET] ?: 0f,
        whiteSatMax = preferences[Keys.COLOR_WHITE_SAT_MAX] ?: 0.20f,
      )
    }

  suspend fun setColorHueOffset(degrees: Float) {
    context.dataStore.edit { preferences ->
      preferences[Keys.COLOR_HUE_OFFSET] = degrees.coerceIn(-30f, 30f)
    }
  }

  suspend fun setColorWhiteSatMax(value: Float) {
    context.dataStore.edit { preferences ->
      preferences[Keys.COLOR_WHITE_SAT_MAX] = value.coerceIn(0.08f, 0.35f)
    }
  }

  suspend fun resetColorCalibration() {
    context.dataStore.edit { preferences ->
      preferences.remove(Keys.COLOR_HUE_OFFSET)
      preferences.remove(Keys.COLOR_WHITE_SAT_MAX)
    }
  }
}
