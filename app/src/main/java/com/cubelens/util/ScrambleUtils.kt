package com.cubelens.util

import com.cubelens.solver.CubeState
import com.cubelens.solver.Move

object ScrambleUtils {
  const val SOLVED_FACELETS = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB"

  private val FACE_CHARS = charArrayOf('U', 'R', 'F', 'D', 'L', 'B')

  fun isFaceletsString(value: String): Boolean {
    val normalized = value.filterNot { it.isWhitespace() }
    return normalized.length == 54
  }

  fun solutionToScramble(solution: String): String {
    val moves = solution.split(Regex("\\s+")).filter { it.isNotBlank() }.map { Move.parse(it) }
    return moves.asReversed().map { it.inverse() }.joinToString(" ") { it.toString() }
  }

  fun faceletsFromScramble(scramble: String): String? = runCatching {
    val stickers = CubeState.parse(SOLVED_FACELETS).toStickerFaces()
    val scratch = IntArray(54)
    scramble.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { token ->
      CubeState.applyMoveInPlace(stickers, Move.parse(token), scratch)
    }
    stickersToFacelets(stickers)
  }.getOrNull()

  fun faceletsForSolveRecord(scramble: String, solution: String): String? {
    if (isFaceletsString(scramble)) return scramble.filterNot { it.isWhitespace() }
    if (scramble.isNotBlank()) return faceletsFromScramble(scramble)
    if (solution.isNotBlank()) return faceletsFromScramble(solutionToScramble(solution))
    return null
  }

  private fun stickersToFacelets(stickers: IntArray): String =
    buildString(54) {
      for (face in stickers) append(FACE_CHARS[face])
    }
}
