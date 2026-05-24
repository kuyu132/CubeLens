package com.cubelens.util

import com.cubelens.solver.CubeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrambleUtilsTest {

  @Test
  fun solutionToScramble_invertsMoves() {
    val solution = "R U R' U'"
    val scramble = ScrambleUtils.solutionToScramble(solution)
    assertEquals("U R U' R'", scramble)
  }

  @Test
  fun faceletsFromScramble_roundTrip() {
    val scramble = "R U R' U'"
    val facelets = ScrambleUtils.faceletsFromScramble(scramble)
    assertNotNull(facelets)
    assertEquals(54, facelets!!.length)
    CubeState.parse(facelets) // throws if invalid
  }

  @Test
  fun isFaceletsString_detects54CharState() {
    assertTrue(ScrambleUtils.isFaceletsString(ScrambleUtils.SOLVED_FACELETS))
  }
}
