package com.cubelens.ui.timer

import com.cubelens.data.SolvePenalty

/** WCA-style inspection limits (milliseconds). */
const val INSPECTION_MS = 15_000L
const val INSPECTION_PLUS2_MS = 15_000L
const val INSPECTION_DNF_MS = 17_000L
const val HOLD_THRESHOLD_MS = 300L

/**
 * Penalty when leaving inspection and starting the solve.
 * [autoStarted] is true when the timer auto-starts exactly at 15 s (no penalty).
 */
fun inspectionPenaltyOnStart(inspectionElapsedMs: Long, autoStarted: Boolean = false): String {
  if (autoStarted) return SolvePenalty.NONE
  return when {
    inspectionElapsedMs > INSPECTION_DNF_MS -> SolvePenalty.DNF
    inspectionElapsedMs > INSPECTION_PLUS2_MS -> SolvePenalty.PLUS2
    else -> SolvePenalty.NONE
  }
}
