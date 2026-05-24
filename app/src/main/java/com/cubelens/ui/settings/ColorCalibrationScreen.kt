package com.cubelens.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.R
import com.cubelens.data.PreferencesManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorCalibrationScreen(
  prefs: PreferencesManager,
  onBack: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val calibration by prefs.colorCalibration.collectAsStateWithLifecycle(
    initialValue = com.cubelens.camera.ColorCalibration.DEFAULT,
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.calibration_title)) },
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.calibration_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))

      Text(
        text = stringResource(
          R.string.calibration_hue_offset,
          calibration.hueOffsetDeg.roundToInt(),
        ),
        style = MaterialTheme.typography.titleSmall,
      )
      Slider(
        value = calibration.hueOffsetDeg,
        onValueChange = { scope.launch { prefs.setColorHueOffset(it) } },
        valueRange = -30f..30f,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text = stringResource(R.string.calibration_hue_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(24.dp))

      Text(
        text = stringResource(
          R.string.calibration_white_sat,
          (calibration.whiteSatMax * 100).roundToInt(),
        ),
        style = MaterialTheme.typography.titleSmall,
      )
      Slider(
        value = calibration.whiteSatMax,
        onValueChange = { scope.launch { prefs.setColorWhiteSatMax(it) } },
        valueRange = 0.08f..0.35f,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text = stringResource(R.string.calibration_white_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(32.dp))

      OutlinedButton(
        onClick = { scope.launch { prefs.resetColorCalibration() } },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.calibration_reset))
      }
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.calibration_done))
      }
    }
  }
}
