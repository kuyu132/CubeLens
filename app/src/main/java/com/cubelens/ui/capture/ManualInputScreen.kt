package com.cubelens.ui.capture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cubelens.R
import com.cubelens.solver.CubeState
import com.cubelens.util.ScrambleUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
  onBack: () -> Unit,
  onApply: (String) -> Boolean,
) {
  val context = LocalContext.current
  var text by remember { mutableStateOf(ScrambleUtils.SOLVED_FACELETS) }
  var error by remember { mutableStateOf<String?>(null) }
  val invalidMsg = stringResource(R.string.manual_input_invalid)

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.manual_input_title)) },
        navigationIcon = {
          TextButton(onClick = onBack) {
            Text(stringResource(R.string.review_back))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
    ) {
      Text(
        text = stringResource(R.string.manual_input_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      OutlinedTextField(
        value = text,
        onValueChange = {
          text = it.filterNot { ch -> ch.isWhitespace() }.uppercase().take(54)
          error = null
        },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        placeholder = { Text(ScrambleUtils.SOLVED_FACELETS) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
      )
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = {
          val normalized = text.filterNot { it.isWhitespace() }
          if (CubeState.tryParse(normalized) == null) {
            error = invalidMsg
            return@Button
          }
          if (!onApply(normalized)) {
            error = invalidMsg
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.manual_input_apply))
      }
    }
  }
}
