package com.cubelens.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubelens.solver.Move
import kotlinx.coroutines.delay
import kotlin.random.Random

data class TimerState(
  val isRunning: Boolean = false,
  val startTimeMs: Long = 0L,
  val elapsedMs: Long = 0L,
  val scramble: String = "",
)

fun formatTime(ms: Long): String {
  val minutes = ms / 60_000
  val seconds = (ms % 60_000) / 1_000
  val millis = ms % 1_000
  return if (minutes > 0) {
    "%d:%02d.%03d".format(minutes, seconds, millis)
  } else {
    "%d.%03d".format(seconds, millis)
  }
}

fun generateScramble(length: Int = 20): String {
  val faces = Move.Face.entries
  val faceNames = faces.map { it.name }
  val moves = mutableListOf<String>()
  var prevFace = -1
  var prevPrevFace = -1

  for (i in 0 until length) {
    var faceIdx: Int
    do {
      faceIdx = Random.nextInt(faces.size)
    } while (faceIdx == prevFace || (faceIdx == prevPrevFace && isOpposite(faceIdx, prevFace)))

    val power = Random.nextInt(3)
    val suffix = when (power) {
      1 -> "'"
      2 -> "2"
      else -> ""
    }
    moves.add(faceNames[faceIdx] + suffix)
    prevPrevFace = prevFace
    prevFace = faceIdx
  }
  return moves.joinToString(" ")
}

private fun isOpposite(a: Int, b: Int): Boolean =
  (a == 0 && b == 3) || (a == 3 && b == 0) ||
  (a == 1 && b == 4) || (a == 4 && b == 1) ||
  (a == 2 && b == 5) || (a == 5 && b == 2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
  onSaveSolve: (scramble: String, timeMs: Long) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  var state by remember { mutableStateOf(TimerState()) }
  var elapsed by remember { mutableLongStateOf(0L) }

  LaunchedEffect(state.isRunning) {
    while (state.isRunning) {
      elapsed = System.currentTimeMillis() - state.startTimeMs
      delay(10)
    }
  }

  val displayTime = if (state.isRunning) elapsed else state.elapsedMs

  Scaffold(
    topBar = {
      TopAppBar(title = { Text("Timer") })
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Scramble display
      if (state.scramble.isNotBlank() && !state.isRunning) {
        Text(
          text = state.scramble,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          modifier = Modifier.padding(bottom = 32.dp),
        )
      }

      // Timer display
      Text(
        text = formatTime(displayTime),
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (state.isRunning) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onBackground,
      )

      Spacer(Modifier.height(48.dp))

      // Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        if (!state.isRunning && state.elapsedMs == 0L) {
          // Ready to start
          Button(
            onClick = {
              val scramble = if (state.scramble.isBlank()) generateScramble() else state.scramble
              state = TimerState(
                isRunning = true,
                startTimeMs = System.currentTimeMillis(),
                scramble = scramble,
              )
            },
            modifier = Modifier.size(width = 200.dp, height = 64.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
          ) {
            Text("Start", fontSize = 20.sp)
          }
        } else if (state.isRunning) {
          // Running — stop
          Button(
            onClick = {
              val finalTime = System.currentTimeMillis() - state.startTimeMs
              state = state.copy(isRunning = false, elapsedMs = finalTime)
            },
            modifier = Modifier.size(width = 200.dp, height = 64.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
            ),
          ) {
            Text("Stop", fontSize = 20.sp)
          }
        } else {
          // Stopped — save or reset
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = formatTime(state.elapsedMs),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(
                onClick = {
                  onSaveSolve(state.scramble, state.elapsedMs)
                  state = TimerState(scramble = generateScramble())
                },
              ) {
                Text("Save")
              }
              OutlinedButton(
                onClick = {
                  state = TimerState(scramble = generateScramble())
                },
              ) {
                Text("New Scramble")
              }
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      // Generate scramble button (when idle)
      if (!state.isRunning && state.elapsedMs == 0L) {
        OutlinedButton(
          onClick = { state = state.copy(scramble = generateScramble()) },
        ) {
          Text("New Scramble")
        }
      }
    }
  }
}
