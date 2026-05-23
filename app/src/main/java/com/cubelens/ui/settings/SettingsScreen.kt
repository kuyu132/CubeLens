package com.cubelens.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.BuildConfig
import com.cubelens.R
import com.cubelens.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  prefs: PreferencesManager,
  onBack: () -> Unit,
  onReplayOnboarding: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val inspectionEnabled by prefs.inspectionEnabled.collectAsStateWithLifecycle(initialValue = true)
  var showAbout by remember { mutableStateOf(false) }
  var showReplayConfirm by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
          TextButton(onClick = onBack) {
            Text(stringResource(R.string.settings_back))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .padding(innerPadding)
        .verticalScroll(rememberScrollState()),
    ) {
      ListItem(
        headlineContent = { Text(stringResource(R.string.settings_inspection)) },
        supportingContent = {
          Text(
            stringResource(R.string.settings_inspection_sub),
            style = MaterialTheme.typography.bodySmall,
          )
        },
        trailingContent = {
          Switch(
            checked = inspectionEnabled,
            onCheckedChange = { v ->
              scope.launch { prefs.setInspectionEnabled(v) }
            },
          )
        },
        modifier = Modifier.fillMaxWidth(),
      )
      HorizontalDivider()
      ListItem(
        headlineContent = { Text(stringResource(R.string.settings_replay_onboarding)) },
        supportingContent = {
          Text(
            stringResource(R.string.settings_replay_onboarding_sub),
            style = MaterialTheme.typography.bodySmall,
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        trailingContent = {
          TextButton(onClick = { showReplayConfirm = true }) {
            Text(stringResource(R.string.settings_replay_action))
          }
        },
      )
      HorizontalDivider()
      ListItem(
        headlineContent = { Text(stringResource(R.string.settings_about)) },
        supportingContent = {
          Text(
            stringResource(R.string.settings_about_sub),
            style = MaterialTheme.typography.bodySmall,
          )
        },
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
          TextButton(onClick = { showAbout = true }) {
            Text(stringResource(R.string.settings_open_about))
          }
        },
      )
      Spacer(Modifier.height(24.dp))
    }
  }

  if (showAbout) {
    AlertDialog(
      onDismissRequest = { showAbout = false },
      title = { Text(stringResource(R.string.settings_about_title)) },
      text = {
        Text(
          stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
        )
      },
      confirmButton = {
        TextButton(onClick = { showAbout = false }) {
          Text(stringResource(R.string.settings_ok))
        }
      },
    )
  }

  if (showReplayConfirm) {
    AlertDialog(
      onDismissRequest = { showReplayConfirm = false },
      title = { Text(stringResource(R.string.settings_replay_confirm_title)) },
      text = { Text(stringResource(R.string.settings_replay_confirm_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            showReplayConfirm = false
            onReplayOnboarding()
          },
        ) {
          Text(stringResource(R.string.settings_replay_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showReplayConfirm = false }) {
          Text(stringResource(R.string.history_clear_cancel))
        }
      },
    )
  }
}
