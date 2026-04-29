package com.cubelens.solver

/**
 * A validated 3x3 cube state using the standard Kociemba facelet order:
 * U(0..8), R(9..17), F(18..26), D(27..35), L(36..44), B(45..53).
 *
 * Internally, each sticker is stored as an Int in 0..5 corresponding to faces U,R,F,D,L,B.
 */
class CubeState private constructor(
  private val stickers: IntArray,
) {
  init {
    require(stickers.size == 54) { "Expected 54 stickers, got ${stickers.size}" }
  }

  fun toStickerFaces(): IntArray = stickers.clone()

  fun isSolved(): Boolean = isSolved(stickers)

  companion object {
    private const val STICKER_COUNT = 54

    private val CENTER_INDEX_BY_FACE = intArrayOf(
      4,  // U
      13, // R
      22, // F
      31, // D
      40, // L
      49, // B
    )

    /**
     * Parses and validates a facelets string.
     *
     * Validation:
     * - After trimming whitespace, must be 54 chars
     * - Exactly 6 distinct center chars (at positions 4, 13, 22, 31, 40, 49)
     * - All chars must be one of the 6 center chars
     * - Each of the 6 chars must occur exactly 9 times
     *
     * The actual symbols can be any 6 characters (e.g. URFDLB, RGBYOW, ...); centers define the mapping.
     */
    fun parse(facelets: String): CubeState {
      val normalized = facelets.filterNot { it.isWhitespace() }
      require(normalized.length == STICKER_COUNT) {
        "Facelets must have exactly 54 non-whitespace characters (got ${normalized.length})"
      }

      val centerChars = CharArray(6) { idx -> normalized[CENTER_INDEX_BY_FACE[idx]] }
      require(centerChars.toSet().size == 6) { "Invalid cube: center stickers must be 6 distinct values" }

      val charToFace = HashMap<Char, Int>(6)
      for (face in 0 until 6) charToFace[centerChars[face]] = face

      val counts = IntArray(6)
      val stickers = IntArray(STICKER_COUNT)
      for (i in 0 until STICKER_COUNT) {
        val face = charToFace[normalized[i]]
          ?: throw IllegalArgumentException("Invalid cube: unexpected facelet '${normalized[i]}' at index $i")
        stickers[i] = face
        counts[face]++
      }
      require(counts.all { it == 9 }) { "Invalid cube: each facelet color must appear exactly 9 times" }

      return CubeState(stickers)
    }

    fun tryParse(facelets: String): CubeState? = runCatching { parse(facelets) }.getOrNull()

    internal fun isSolved(stickers: IntArray): Boolean {
      for (face in 0 until 6) {
        val base = face * 9
        val v = stickers[base]
        for (i in 1 until 9) {
          if (stickers[base + i] != v) return false
        }
      }
      return true
    }

    internal fun isOpposite(a: Move.Face, b: Move.Face): Boolean = when (a) {
      Move.Face.U -> b == Move.Face.D
      Move.Face.D -> b == Move.Face.U
      Move.Face.R -> b == Move.Face.L
      Move.Face.L -> b == Move.Face.R
      Move.Face.F -> b == Move.Face.B
      Move.Face.B -> b == Move.Face.F
    }

    internal fun applyMoveInPlace(stickers: IntArray, move: Move, scratch: IntArray) {
      require(stickers.size == STICKER_COUNT) { "Expected 54 stickers, got ${stickers.size}" }
      require(scratch.size == STICKER_COUNT) { "Expected 54 scratch slots, got ${scratch.size}" }
      val perm = MOVE_PERMUTATIONS[move.ordinal]
      for (dest in 0 until STICKER_COUNT) {
        scratch[dest] = stickers[perm[dest]]
      }
      for (i in 0 until STICKER_COUNT) stickers[i] = scratch[i]
    }

    private data class Vec3(val x: Int, val y: Int, val z: Int) {
      operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
      operator fun times(k: Int) = Vec3(x * k, y * k, z * k)
      fun dot(o: Vec3): Int = x * o.x + y * o.y + z * o.z
    }

    private data class Sticker(val pos: Vec3, val normal: Vec3)

    private data class FaceBasis(val normal: Vec3, val u: Vec3, val v: Vec3)

    // Canonical face order: U, R, F, D, L, B.
    private val BASES = arrayOf(
      FaceBasis(normal = Vec3(0, 1, 0), u = Vec3(1, 0, 0), v = Vec3(0, 0, 1)),   // U
      FaceBasis(normal = Vec3(1, 0, 0), u = Vec3(0, 0, -1), v = Vec3(0, -1, 0)),  // R
      FaceBasis(normal = Vec3(0, 0, 1), u = Vec3(1, 0, 0), v = Vec3(0, -1, 0)),   // F
      FaceBasis(normal = Vec3(0, -1, 0), u = Vec3(1, 0, 0), v = Vec3(0, 0, -1)),  // D
      FaceBasis(normal = Vec3(-1, 0, 0), u = Vec3(0, 0, 1), v = Vec3(0, -1, 0)),  // L
      FaceBasis(normal = Vec3(0, 0, -1), u = Vec3(-1, 0, 0), v = Vec3(0, -1, 0)), // B
    )

    private val INDEX_TO_STICKER: Array<Sticker> = Array(STICKER_COUNT) { idx ->
      val face = idx / 9
      val offset = idx % 9
      val row = offset / 3
      val col = offset % 3
      val basis = BASES[face]
      val pos = basis.normal + basis.u * (col - 1) + basis.v * (row - 1)
      Sticker(pos = pos, normal = basis.normal)
    }

    private fun faceFromNormal(n: Vec3): Int = when {
      n.y == 1 -> 0 // U
      n.x == 1 -> 1 // R
      n.z == 1 -> 2 // F
      n.y == -1 -> 3 // D
      n.x == -1 -> 4 // L
      n.z == -1 -> 5 // B
      else -> error("Invalid normal: $n")
    }

    private fun indexFromSticker(sticker: Sticker): Int {
      val face = faceFromNormal(sticker.normal)
      val basis = BASES[face]
      val col = sticker.pos.dot(basis.u) + 1
      val row = sticker.pos.dot(basis.v) + 1
      require(row in 0..2 && col in 0..2) { "Invalid sticker position for face $face: $sticker" }
      return face * 9 + row * 3 + col
    }

    private fun rotateQuarter(v: Vec3, axisOutward: Vec3): Vec3 = when (axisOutward) {
      Vec3(0, 0, 1) -> Vec3(v.y, -v.x, v.z)     // F
      Vec3(0, 0, -1) -> Vec3(-v.y, v.x, v.z)    // B
      Vec3(0, 1, 0) -> Vec3(v.z, v.y, -v.x)     // U
      Vec3(0, -1, 0) -> Vec3(-v.z, v.y, v.x)    // D
      Vec3(1, 0, 0) -> Vec3(v.x, v.z, -v.y)     // R
      Vec3(-1, 0, 0) -> Vec3(v.x, -v.z, v.y)    // L
      else -> error("Invalid axis: $axisOutward")
    }

    private fun quarterPermutationFor(face: Move.Face): IntArray {
      val axis = when (face) {
        Move.Face.U -> BASES[0].normal
        Move.Face.R -> BASES[1].normal
        Move.Face.F -> BASES[2].normal
        Move.Face.D -> BASES[3].normal
        Move.Face.L -> BASES[4].normal
        Move.Face.B -> BASES[5].normal
      }
      val perm = IntArray(STICKER_COUNT) { it } // dest -> src
      for (src in 0 until STICKER_COUNT) {
        val s = INDEX_TO_STICKER[src]
        val inLayer = s.pos.dot(axis) == 1
        val rotated = if (!inLayer) s else Sticker(
          pos = rotateQuarter(s.pos, axis),
          normal = rotateQuarter(s.normal, axis),
        )
        val dest = indexFromSticker(rotated)
        perm[dest] = src
      }
      return perm
    }

    private fun powPermutation(quarter: IntArray, power: Int): IntArray {
      require(power in 1..3) { "Invalid move power: $power" }
      var current = IntArray(STICKER_COUNT) { it }
      repeat(power) {
        val next = IntArray(STICKER_COUNT)
        for (dest in 0 until STICKER_COUNT) next[dest] = current[quarter[dest]]
        current = next
      }
      return current
    }

    private val QUARTER_PERMUTATIONS_BY_FACE: Map<Move.Face, IntArray> = mapOf(
      Move.Face.U to quarterPermutationFor(Move.Face.U),
      Move.Face.R to quarterPermutationFor(Move.Face.R),
      Move.Face.F to quarterPermutationFor(Move.Face.F),
      Move.Face.D to quarterPermutationFor(Move.Face.D),
      Move.Face.L to quarterPermutationFor(Move.Face.L),
      Move.Face.B to quarterPermutationFor(Move.Face.B),
    )

    private val MOVE_PERMUTATIONS: Array<IntArray> = Array(Move.entries.size) { i ->
      val move = Move.entries[i]
      val quarter = QUARTER_PERMUTATIONS_BY_FACE.getValue(move.face)
      powPermutation(quarter, move.power)
    }
  }
}

