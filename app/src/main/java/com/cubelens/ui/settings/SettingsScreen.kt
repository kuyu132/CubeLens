package com.cubelens.ui.settings

import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.data.PreferencesManager
import com.cubelens.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  prefs: PreferencesManager,
  onBack: () -> Unit,
  onClearAllData: () -> Unit,
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
  val lensFacing by prefs.cameraLensFacing.collectAsStateWithLifecycle(initialValue = 1)
  var showClearDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          TextButton(onClick = onBack) { Text("Back") }
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
      // ── Appearance ──────────────────────────────────────────────────────────
      SectionHeader("Appearance")
      Spacer(Modifier.height(8.dp))

      ThemeMode.entries.forEach { mode ->
        val selected = mode == themeMode
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { prefs.setThemeMode(mode) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(
            selected = selected,
            onClick = { prefs.setThemeMode(mode) },
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = when (mode) {
              ThemeMode.SYSTEM -> "Follow system"
              ThemeMode.LIGHT -> "Light"
              ThemeMode.DARK -> "Dark"
            },
            style = MaterialTheme.typography.bodyLarge,
          )
        }
      }

      Spacer(Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))

      // ── Camera ──────────────────────────────────────────────────────────────
      SectionHeader("Camera")
      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            val newLens = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
              androidx.camera.core.CameraSelector.LENS_FACING_FRONT
            else
              androidx.camera.core.CameraSelector.LENS_FACING_BACK
            prefs.setCameraLensFacing(newLens)
          }
          .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text("Default lens", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) "Back camera" else "Front camera",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT,
          onCheckedChange = {
            val newLens = if (it)
              androidx.camera.core.CameraSelector.LENS_FACING_FRONT
            else
              androidx.camera.core.CameraSelector.LENS_FACING_BACK
            prefs.setCameraLensFacing(newLens)
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

      // ── About ───────────────────────────────────────────────────────────────
      SectionHeader("About")
      Spacer(Modifier.height(8.dp))

      InfoRow("App", "CubeLens")
      InfoRow("Version", "0.1.0")
      InfoRow("Solver", "Kociemba 2-phase")
      InfoRow("Min SDK", "Android 8.0 (API 26)")

      Spacer(Modifier.height(24.dp))
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))

      // ── Data ────────────────────────────────────────────────────────────────
      SectionHeader("Data")
      Spacer(Modifier.height(8.dp))

      Button(
        onClick = { showClearDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text("Clear All Data")
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = "This will delete all solve history and reset preferences.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  // Clear all confirmation dialog
  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear All Data?") },
      text = { Text("This will permanently delete all solve records and reset all settings. This cannot be undone.") },
      confirmButton = {
        TextButton(
          onClick = {
            onClearAllData()
            showClearDialog = false
            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
          },
        ) {
          Text("Clear Everything", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text("Cancel")
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

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp, horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
    )
  }
}
