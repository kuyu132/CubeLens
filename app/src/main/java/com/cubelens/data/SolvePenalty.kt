package com.cubelens.data

/** Stored in [SolveRecord.penalty]. */
object SolvePenalty {
  const val NONE = ""
  const val PLUS2 = "PLUS2"
  const val DNF = "DNF"
}

/** Milliseconds used only inside Ao trimming when a solve is DNF (treated as worst). */
internal const val DNF_SORT_SENTINEL_MS: Long = 999_000_000L

fun SolveRecord.contestMillisForStats(): Long =
  when (penalty) {
    SolvePenalty.DNF -> DNF_SORT_SENTINEL_MS
    SolvePenalty.PLUS2 -> timeMs + 2_000L
    else -> timeMs
  }

fun SolveRecord.isDnf(): Boolean = penalty == SolvePenalty.DNF

fun SolveRecord.effectiveNonDnfMillis(): Long? =
  if (penalty == SolvePenalty.DNF) null else contestMillisForStats()
