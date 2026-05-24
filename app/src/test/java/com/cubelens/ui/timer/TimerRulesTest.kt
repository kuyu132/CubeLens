package com.cubelens.ui.timer

import com.cubelens.data.SolvePenalty
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerRulesTest {

  @Test
  fun inspectionPenalty_noneWithin15s() {
    assertEquals(SolvePenalty.NONE, inspectionPenaltyOnStart(14_999L))
    assertEquals(SolvePenalty.NONE, inspectionPenaltyOnStart(15_000L))
  }

  @Test
  fun inspectionPenalty_plus2Between15And17s() {
    assertEquals(SolvePenalty.PLUS2, inspectionPenaltyOnStart(15_001L))
    assertEquals(SolvePenalty.PLUS2, inspectionPenaltyOnStart(17_000L))
  }

  @Test
  fun inspectionPenalty_dnfAfter17s() {
    assertEquals(SolvePenalty.DNF, inspectionPenaltyOnStart(17_001L))
  }

  @Test
  fun inspectionPenalty_autoStartNeverPenalized() {
    assertEquals(SolvePenalty.NONE, inspectionPenaltyOnStart(20_000L, autoStarted = true))
  }
}
