package com.cubelens.solver

/**
 * Standard Singmaster moves for a 3x3 cube.
 *
 * Naming:
 * - `U` means 90° clockwise when looking at the U face.
 * - `_PRIME` means 90° counterclockwise.
 * - `2` means 180°.
 */
enum class Move(
  val face: Face,
  /** 1 = quarter clockwise, 2 = half, 3 = quarter counterclockwise (prime). */
  val power: Int,
) {
  U(Face.U, 1),
  U2(Face.U, 2),
  U_PRIME(Face.U, 3),

  R(Face.R, 1),
  R2(Face.R, 2),
  R_PRIME(Face.R, 3),

  F(Face.F, 1),
  F2(Face.F, 2),
  F_PRIME(Face.F, 3),

  D(Face.D, 1),
  D2(Face.D, 2),
  D_PRIME(Face.D, 3),

  L(Face.L, 1),
  L2(Face.L, 2),
  L_PRIME(Face.L, 3),

  B(Face.B, 1),
  B2(Face.B, 2),
  B_PRIME(Face.B, 3),
  ;

  enum class Face { U, R, F, D, L, B }

  fun inverse(): Move = when (power) {
    1 -> from(face, 3)
    2 -> this
    3 -> from(face, 1)
    else -> error("Invalid power: $power")
  }

  override fun toString(): String = when (power) {
    1 -> face.name
    2 -> face.name + "2"
    3 -> face.name + "'"
    else -> face.name
  }

  companion object {
    fun from(face: Face, power: Int): Move = entries.first { it.face == face && it.power == power }

    fun parse(token: String): Move {
      require(token.isNotBlank()) { "Empty move token" }
      val face = when (token[0]) {
        'U' -> Face.U
        'R' -> Face.R
        'F' -> Face.F
        'D' -> Face.D
        'L' -> Face.L
        'B' -> Face.B
        else -> throw IllegalArgumentException("Invalid move face: ${token[0]}")
      }
      val suffix = token.substring(1)
      val power = when (suffix) {
        "" -> 1
        "2" -> 2
        "'" -> 3
        else -> throw IllegalArgumentException("Invalid move token: $token")
      }
      return from(face, power)
    }
  }
}
