package com.cubelens.ui.settings

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.BuildConfig
import com.cubelens.R
import com.cubelens.data.PreferencesManager
import com.cubelens.data.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  prefs: PreferencesManager,
  onBack: () -> Unit,
  onReplayOnboarding: () -> Unit = {},
  onClearAllData: () -> Unit = {},
  onColorCalibration: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
  val lensFacing by prefs.cameraLensFacing.collectAsStateWithLifecycle(initialValue = CameraSelector.LENS_FACING_BACK)
  val inspectionEnabled by prefs.inspectionEnabled.collectAsStateWithLifecycle(initialValue = true)
  var showAbout by remember { mutableStateOf(false) }
  var showReplayConfirm by remember { mutableStateOf(false) }
  var showClearDialog by remember { mutableStateOf(false) }

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
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      SectionHeader(stringResource(R.string.settings_appearance))
      Spacer(Modifier.height(8.dp))

      ThemeMode.entries.forEach { mode ->
        val selected = mode == themeMode
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { scope.launch { prefs.setThemeMode(mode) } }
            .padding(vertical = 10.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(
            selected = selected,
            onClick = { scope.launch { prefs.setThemeMode(mode) } },
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = when (mode) {
              ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
              ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
              ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
            },
            style = MaterialTheme.typography.bodyLarge,
          )
        }
      }

      Spacer(Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))

      SectionHeader(stringResource(R.string.settings_camera))
      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            val newLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
              CameraSelector.LENS_FACING_FRONT
            } else {
              CameraSelector.LENS_FACING_BACK
            }
            scope.launch { prefs.setCameraLensFacing(newLens) }
          }
          .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(stringResource(R.string.settings_camera_default), style = MaterialTheme.typography.bodyLarge)
          Text(
            text = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
              stringResource(R.string.settings_camera_back)
            } else {
              stringResource(R.string.settings_camera_front)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = lensFacing == CameraSelector.LENS_FACING_FRONT,
          onCheckedChange = {
            val newLens = if (it) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            scope.launch { prefs.setCameraLensFacing(newLens) }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
          ),
        )
      }

      Spacer(Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))

      ListItem(
        headlineContent = { Text(stringResource(R.string.settings_color_calibration)) },
        supportingContent = {
          Text(
            stringResource(R.string.settings_color_calibration_sub),
            style = MaterialTheme.typography.bodySmall,
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onColorCalibration() },
        trailingContent = {
          TextButton(onClick = onColorCalibration) {
            Text(stringResource(R.string.settings_calibration_open))
          }
        },
      )

      HorizontalDivider()
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
            onCheckedChange = { enabled ->
              scope.launch { prefs.setInspectionEnabled(enabled) }
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
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))

      SectionHeader(stringResource(R.string.settings_data))
      Spacer(Modifier.height(8.dp))

      Button(
        onClick = { showClearDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text(stringResource(R.string.settings_clear_all_data))
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.settings_clear_all_data_sub),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  if (showAbout) {
    AlertDialog(
      onDismissRequest = { showAbout = false },
      title = { Text(stringResource(R.string.settings_about_title)) },
      text = {
        Text(stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME))
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

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text(stringResource(R.string.settings_clear_all_title)) },
      text = { Text(stringResource(R.string.settings_clear_all_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            onClearAllData()
            showClearDialog = false
            Toast.makeText(context, context.getString(R.string.settings_clear_all_toast), Toast.LENGTH_SHORT).show()
          },
        ) {
          Text(stringResource(R.string.settings_clear_all_confirm), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text(stringResource(R.string.history_clear_cancel))
        }
      },
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.SemiBold,
  )
}
