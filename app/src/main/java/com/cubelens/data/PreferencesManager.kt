package com.cubelens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cubelens_prefs")

class PreferencesManager(private val context: Context) {

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
  }

  val onboardingCompleted: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[Keys.ONBOARDING_COMPLETED] = completed
    }
  }
}
