package com.cubelens.util

/**
 * Calculate average of N (removing best and worst).
 * Used for Ao5, Ao12, Ao100 calculations.
 * Returns null if insufficient records.
 */
fun calculateAoN(times: List<Long>, n: Int): Long? {
  if (times.size < n || n < 3) return null

  val recentN = times.take(n)
  val sorted = recentN.sorted()
  // Remove best (first) and worst (last)
  val trimmed = sorted.subList(1, sorted.size - 1)
  return trimmed.average().toLong()
}

/**
 * Format time in mm:ss.SSS or ss.SSS format
 */
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
