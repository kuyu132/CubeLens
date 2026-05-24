package com.cubelens.ui.timer

import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubelens.R
import com.cubelens.data.SolvePenalty
import com.cubelens.solver.Move
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class TimerPhase {
  IDLE,
  HOLDING,      // finger down, waiting for release
  INSPECTING,
  RUNNING,
  STOPPED,
}

private data class TimerUiState(
  val phase: TimerPhase = TimerPhase.IDLE,
  val startTimeMs: Long = 0L,
  val elapsedMs: Long = 0L,
  val scramble: String = "",
  val inspectionStartedAtMs: Long = 0L,
  val inspectionEndsAtMs: Long = 0L,
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

fun formatInspectionSeconds(ms: Long): String {
  val sec = (ms + 999) / 1_000
  return sec.toString()
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
  inspectionEnabled: Boolean,
  initialScramble: String? = null,
  onInitialScrambleConsumed: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onSaveSolve: (scramble: String, timeMs: Long, penalty: String) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val view = LocalView.current
  var state by remember { mutableStateOf(TimerUiState()) }
  var elapsed by remember { mutableLongStateOf(0L) }
  var inspectionRemainMs by remember { mutableLongStateOf(0L) }
  var penalty by remember { mutableStateOf(SolvePenalty.NONE) }

  LaunchedEffect(initialScramble) {
    if (!initialScramble.isNullOrBlank()) {
      state = state.copy(scramble = initialScramble.trim())
      onInitialScrambleConsumed()
    }
  }

  fun ensureScramble(): String {
    if (state.scramble.isNotBlank()) return state.scramble
    val generated = generateScramble()
    state = state.copy(scramble = generated)
    return generated
  }

  fun beginInspection(scramble: String) {
    val now = System.currentTimeMillis()
    val end = now + INSPECTION_MS
    inspectionRemainMs = INSPECTION_MS
    penalty = SolvePenalty.NONE
    state = TimerUiState(
      phase = TimerPhase.INSPECTING,
      scramble = scramble,
      inspectionStartedAtMs = now,
      inspectionEndsAtMs = end,
    )
  }

  fun beginSolveFromInspection(autoStarted: Boolean) {
    val elapsedInspection = System.currentTimeMillis() - state.inspectionStartedAtMs
    penalty = inspectionPenaltyOnStart(elapsedInspection, autoStarted)
    if (penalty == SolvePenalty.DNF) {
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      state = state.copy(
        phase = TimerPhase.STOPPED,
        elapsedMs = 0L,
        inspectionEndsAtMs = 0L,
      )
      return
    }
    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    state = state.copy(
      phase = TimerPhase.RUNNING,
      startTimeMs = System.currentTimeMillis(),
      inspectionEndsAtMs = 0L,
    )
  }

  fun beginSolveDirect(scramble: String) {
    penalty = SolvePenalty.NONE
    state = TimerUiState(
      phase = TimerPhase.RUNNING,
      startTimeMs = System.currentTimeMillis(),
      scramble = scramble,
    )
  }

  fun stopSolve() {
    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    val finalTime = (System.currentTimeMillis() - state.startTimeMs).coerceAtLeast(0L)
    state = state.copy(phase = TimerPhase.STOPPED, elapsedMs = finalTime)
  }

  LaunchedEffect(state.phase, state.inspectionEndsAtMs, state.inspectionStartedAtMs) {
    if (state.phase != TimerPhase.INSPECTING || state.inspectionEndsAtMs == 0L) return@LaunchedEffect
    val end = state.inspectionEndsAtMs
    val dnfAt = state.inspectionStartedAtMs + INSPECTION_DNF_MS
    try {
      while (true) {
        if (state.phase != TimerPhase.INSPECTING) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now >= dnfAt) {
          penalty = SolvePenalty.DNF
          state = state.copy(phase = TimerPhase.STOPPED, elapsedMs = 0L, inspectionEndsAtMs = 0L)
          return@LaunchedEffect
        }
        if (now >= end) {
          inspectionRemainMs = 0L
          beginSolveFromInspection(autoStarted = true)
          return@LaunchedEffect
        }
        inspectionRemainMs = (end - now).coerceAtLeast(0L)
        delay(32)
      }
    } catch (_: CancellationException) {
      // Inspection skipped or screen left
    }
  }

  LaunchedEffect(state.phase, state.startTimeMs) {
    if (state.phase != TimerPhase.RUNNING) return@LaunchedEffect
    val start = state.startTimeMs
    try {
      while (true) {
        elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L)
        delay(10)
      }
    } catch (_: CancellationException) {
      // Stopped
    }
  }

  val displayTime = when {
    state.phase == TimerPhase.RUNNING -> elapsed
    state.phase == TimerPhase.STOPPED && penalty == SolvePenalty.DNF -> 0L
    state.phase == TimerPhase.STOPPED && penalty == SolvePenalty.PLUS2 -> state.elapsedMs + 2_000L
    else -> state.elapsedMs
  }

  val displayTimeLabel = when {
    state.phase == TimerPhase.STOPPED && penalty == SolvePenalty.DNF ->
      stringResource(R.string.timer_penalty_dnf)
    else -> formatTime(displayTime)
  }

  val gestureHint = when (state.phase) {
    TimerPhase.IDLE -> stringResource(R.string.timer_hold_to_start)
    TimerPhase.HOLDING -> stringResource(R.string.timer_release_to_start)
    TimerPhase.INSPECTING -> stringResource(R.string.timer_release_to_solve)
    TimerPhase.RUNNING -> stringResource(R.string.timer_touch_to_stop)
    TimerPhase.STOPPED -> ""
  }

  val holdOverlayColor = when (state.phase) {
    TimerPhase.HOLDING -> Color(0xFF2E7D32).copy(alpha = 0.18f)
    TimerPhase.RUNNING -> Color.Transparent
    else -> Color.Transparent
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.timer_title)) },
        actions = {
          IconButton(onClick = onNavigateToSettings) {
            Icon(
              imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
              contentDescription = stringResource(R.string.menu_settings),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .then(modifier)
        .padding(innerPadding)
        .background(holdOverlayColor)
        .pointerInput(state.phase, inspectionEnabled) {
          if (state.phase == TimerPhase.STOPPED) return@pointerInput
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pressStart = System.currentTimeMillis()
            when (state.phase) {
              TimerPhase.IDLE -> {
                state = state.copy(phase = TimerPhase.HOLDING)
                val up = waitForUpOrCancellation()
                val held = System.currentTimeMillis() - pressStart
                if (up == null) {
                  state = state.copy(phase = TimerPhase.IDLE)
                  return@awaitEachGesture
                }
                if (held < HOLD_THRESHOLD_MS) {
                  state = state.copy(phase = TimerPhase.IDLE)
                  return@awaitEachGesture
                }
                val scramble = ensureScramble()
                if (inspectionEnabled) beginInspection(scramble) else beginSolveDirect(scramble)
              }

              TimerPhase.INSPECTING -> {
                waitForUpOrCancellation()
                if (state.phase == TimerPhase.INSPECTING) {
                  beginSolveFromInspection(autoStarted = false)
                }
              }

              TimerPhase.RUNNING -> {
                waitForUpOrCancellation()
                if (state.phase == TimerPhase.RUNNING) stopSolve()
              }

              else -> waitForUpOrCancellation()
            }
          }
        },
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (state.scramble.isNotBlank() && state.phase != TimerPhase.RUNNING) {
          Text(
            text = state.scramble,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp),
          )
        }

        if (state.phase == TimerPhase.INSPECTING) {
          Text(
            text = stringResource(R.string.timer_inspection_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = formatInspectionSeconds(inspectionRemainMs),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (inspectionRemainMs <= 3_000L) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.secondary
            },
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.timer_inspection_hint, 15),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(16.dp))
          TextButton(
            onClick = { beginSolveFromInspection(autoStarted = false) },
          ) {
            Text(stringResource(R.string.timer_inspection_skip))
          }
        } else {
          Text(
            text = displayTimeLabel,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = when {
              state.phase == TimerPhase.STOPPED && penalty == SolvePenalty.DNF ->
                MaterialTheme.colorScheme.error
              state.phase == TimerPhase.RUNNING -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onBackground
            },
          )
        }

        if (gestureHint.isNotBlank()) {
          Spacer(Modifier.height(16.dp))
          Text(
            text = gestureHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        if (state.phase == TimerPhase.STOPPED) {
          Spacer(Modifier.height(32.dp))
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (penalty == SolvePenalty.PLUS2 && state.elapsedMs > 0L) {
              Text(
                text = stringResource(R.string.timer_inspection_plus2_applied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
              )
              Spacer(Modifier.height(8.dp))
            }
            Text(
              stringResource(R.string.timer_penalty_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = penalty == SolvePenalty.NONE,
                onClick = { penalty = SolvePenalty.NONE },
                label = { Text(stringResource(R.string.timer_penalty_ok)) },
              )
              FilterChip(
                selected = penalty == SolvePenalty.PLUS2,
                onClick = {
                  penalty = if (penalty == SolvePenalty.PLUS2) SolvePenalty.NONE else SolvePenalty.PLUS2
                },
                label = { Text(stringResource(R.string.timer_penalty_plus2)) },
              )
              FilterChip(
                selected = penalty == SolvePenalty.DNF,
                onClick = {
                  penalty = if (penalty == SolvePenalty.DNF) SolvePenalty.NONE else SolvePenalty.DNF
                },
                label = { Text(stringResource(R.string.timer_penalty_dnf)) },
              )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(
                onClick = {
                  onSaveSolve(state.scramble, state.elapsedMs, penalty)
                  penalty = SolvePenalty.NONE
                  state = TimerUiState(scramble = generateScramble())
                },
              ) {
                Text(stringResource(R.string.timer_save))
              }
              OutlinedButton(
                onClick = {
                  penalty = SolvePenalty.NONE
                  state = TimerUiState(scramble = generateScramble())
                },
              ) {
                Text(stringResource(R.string.timer_new_scramble))
              }
            }
          }
        }

        Spacer(Modifier.height(24.dp))

        if (state.phase == TimerPhase.IDLE) {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { state = state.copy(scramble = generateScramble()) }) {
              Text(stringResource(R.string.timer_new_scramble))
            }
            OutlinedButton(
              onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                if (clip.isBlank()) {
                  Toast.makeText(context, context.getString(R.string.timer_paste_empty), Toast.LENGTH_SHORT).show()
                } else {
                  state = state.copy(scramble = clip)
                }
              },
            ) {
              Text(stringResource(R.string.timer_paste_scramble))
            }
          }
        }
      }
    }
  }
}
