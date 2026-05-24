package com.cubelens.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsUtilsTest {

  @Test
  fun calculateAo5_trimsBestAndWorst() {
    val times = listOf(10_000L, 11_000L, 12_000L, 13_000L, 14_000L)
    assertEquals(12_000L, calculateAoN(times, 5))
  }

  @Test
  fun calculateAoN_returnsNullWhenInsufficient() {
    assertNull(calculateAoN(listOf(10_000L, 11_000L), 5))
  }
}
