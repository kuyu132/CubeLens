package com.cubelens.solver

import kotlin.math.max

/**
 * Cubie representation used by the Kociemba 2-phase algorithm.
 *
 * Corner order: URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB (0..7)
 * Edge order:   UR,  UF,  UL,  UB,  DR,  DF,  DL,  DB,  FR,  FL,  BL,  BR (0..11)
 */
internal class CubieCube(
  val cp: IntArray = IntArray(8) { it },
  val co: IntArray = IntArray(8),
  val ep: IntArray = IntArray(12) { it },
  val eo: IntArray = IntArray(12),
) {
  /** Create a shallow copy with copied arrays. */
  fun copy(): CubieCube = CubieCube(cp.copyOf(), co.copyOf(), ep.copyOf(), eo.copyOf())

  /** Copy all arrays from [other] into this instance (avoids allocation). */
  fun loadFrom(other: CubieCube) {
    System.arraycopy(other.cp, 0, cp, 0, 8)
    System.arraycopy(other.co, 0, co, 0, 8)
    System.arraycopy(other.ep, 0, ep, 0, 12)
    System.arraycopy(other.eo, 0, eo, 0, 12)
  }

  fun setSolved() {
    for (i in 0 until 8) {
      cp[i] = i
      co[i] = 0
    }
    for (i in 0 until 12) {
      ep[i] = i
      eo[i] = 0
    }
  }

  fun apply(move: CubieCube, scratch: Scratch) {
    for (i in 0 until 8) {
      val b = move.cp[i]
      scratch.cp[i] = cp[b]
      var ori = co[b] + move.co[i]
      // ori in 0..4
      if (ori >= 3) ori -= 3
      scratch.co[i] = ori
    }
    for (i in 0 until 12) {
      val b = move.ep[i]
      scratch.ep[i] = ep[b]
      scratch.eo[i] = eo[b] xor move.eo[i]
    }
    for (i in 0 until 8) {
      cp[i] = scratch.cp[i]
      co[i] = scratch.co[i]
    }
    for (i in 0 until 12) {
      ep[i] = scratch.ep[i]
      eo[i] = scratch.eo[i]
    }
  }

  fun getTwist(): Int {
    var twist = 0
    for (i in 0 until 7) twist = twist * 3 + co[i]
    return twist
  }

  fun setTwist(twist: Int) {
    var x = twist
    var sum = 0
    for (i in 6 downTo 0) {
      val v = x % 3
      x /= 3
      co[i] = v
      sum += v
    }
    co[7] = (3 - (sum % 3)) % 3
    for (i in 0 until 8) cp[i] = i
    for (i in 0 until 12) {
      ep[i] = i
      eo[i] = 0
    }
  }

  fun getFlip(): Int {
    var flip = 0
    for (i in 0 until 11) flip = (flip shl 1) or eo[i]
    return flip
  }

  fun setFlip(flip: Int) {
    var x = flip
    var sum = 0
    for (i in 10 downTo 0) {
      val v = x and 1
      x = x ushr 1
      eo[i] = v
      sum += v
    }
    eo[11] = sum and 1
    for (i in 0 until 8) {
      cp[i] = i
      co[i] = 0
    }
    for (i in 0 until 12) ep[i] = i
  }

  /**
   * Phase-1 slice coordinate: which 4 edges are in the middle slice (FR,FL,BL,BR), ignoring their order.
   * Range: 0..494 (12 choose 4).
   */
  fun getSlice(): Int {
    var a = 0
    var x = 0
    for (i in 11 downTo 0) {
      if (ep[i] >= 8) {
        a += MathTables.CNK[i][x + 1]
        x++
      }
    }
    return a
  }

  fun setSlice(slice: Int) {
    for (i in 0 until 8) {
      cp[i] = i
      co[i] = 0
    }
    for (i in 0 until 12) eo[i] = 0

    val sliceEdges = intArrayOf(8, 9, 10, 11) // FR, FL, BL, BR
    val otherEdges = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7) // UR..DB
    for (i in 0 until 12) ep[i] = -1

    var a = slice
    var x = 3
    for (i in 11 downTo 0) {
      if (a >= MathTables.CNK[i][x + 1]) {
        ep[i] = sliceEdges[x]
        a -= MathTables.CNK[i][x + 1]
        x--
        if (x < 0) break
      }
    }
    var j = 0
    for (i in 0 until 12) {
      if (ep[i] == -1) ep[i] = otherEdges[j++]
    }
  }

  fun getCornerPerm(): Int = MathTables.permToIndex(cp, 8)

  fun setCornerPerm(index: Int) {
    for (i in 0 until 8) co[i] = 0
    for (i in 0 until 12) {
      ep[i] = i
      eo[i] = 0
    }
    MathTables.indexToPerm(index, cp, 8)
  }

  /**
   * Phase-2 edge permutation coordinate for the 8 U/D layer edges (UR..DB), ignoring the 4 slice edges.
   * Range: 0..40319 (8!).
   */
  fun getEdgePerm8(): Int {
    val tmp = MathTables.TMP8
    for (i in 0 until 8) tmp[i] = ep[i]
    return MathTables.permToIndex(tmp, 8)
  }

  fun setEdgePerm8(index: Int) {
    for (i in 0 until 8) {
      cp[i] = i
      co[i] = 0
    }
    for (i in 0 until 12) eo[i] = 0
    // Fix slice edges in the slice positions for a canonical representative.
    ep[8] = 8
    ep[9] = 9
    ep[10] = 10
    ep[11] = 11
    val perm = MathTables.TMP8
    MathTables.indexToPerm(index, perm, 8)
    for (i in 0 until 8) ep[i] = perm[i]
  }

  /**
   * Phase-2 slice edge permutation coordinate (FR,FL,BL,BR) within the slice positions.
   * Range: 0..23 (4!).
   */
  fun getUdSlicePerm(): Int {
    val tmp = MathTables.TMP4
    for (i in 0 until 4) tmp[i] = ep[8 + i] - 8
    return MathTables.permToIndex(tmp, 4)
  }

  fun setUdSlicePerm(index: Int) {
    for (i in 0 until 8) {
      cp[i] = i
      co[i] = 0
    }
    for (i in 0 until 8) ep[i] = i
    for (i in 0 until 12) eo[i] = 0
    val perm = MathTables.TMP4
    MathTables.indexToPerm(index, perm, 4)
    for (i in 0 until 4) ep[8 + i] = perm[i] + 8
  }

  fun isSolvedPhase2(): Boolean = getCornerPerm() == 0 && getEdgePerm8() == 0 && getUdSlicePerm() == 0

  /** Apply a Singmaster Move to this cube in-place. */
  fun move(m: Move) {
    MoveCube.applyMove(this, m)
  }

  companion object {

    private val _scratch = Scratch()
    fun scratch(): Scratch = _scratch

    private val CORNER_FACELETS = arrayOf(
      intArrayOf(8, 9, 20),   // URF: U8, R0, F2
      intArrayOf(6, 18, 38),  // UFL: U6, F0, L2
      intArrayOf(0, 36, 47),  // ULB: U0, L0, B2
      intArrayOf(2, 45, 11),  // UBR: U2, B0, R2
      intArrayOf(29, 26, 15), // DFR: D2, F8, R6
      intArrayOf(27, 44, 24), // DLF: D0, L8, F6
      intArrayOf(33, 53, 42), // DBL: D6, B8, L6
      intArrayOf(35, 17, 51), // DRB: D8, R8, B6
    )

    private val CORNER_COLORS = arrayOf(
      intArrayOf(0, 1, 2), // URF: U R F
      intArrayOf(0, 2, 4), // UFL: U F L
      intArrayOf(0, 4, 5), // ULB: U L B
      intArrayOf(0, 5, 1), // UBR: U B R
      intArrayOf(3, 2, 1), // DFR: D F R
      intArrayOf(3, 4, 2), // DLF: D L F
      intArrayOf(3, 5, 4), // DBL: D B L
      intArrayOf(3, 1, 5), // DRB: D R B
    )

    private val EDGE_FACELETS = arrayOf(
      intArrayOf(5, 10),  // UR: U5, R1
      intArrayOf(7, 19),  // UF: U7, F1
      intArrayOf(3, 37),  // UL: U3, L1
      intArrayOf(1, 46),  // UB: U1, B1
      intArrayOf(32, 16), // DR: D5, R7
      intArrayOf(28, 25), // DF: D1, F7
      intArrayOf(30, 43), // DL: D3, L7
      intArrayOf(34, 52), // DB: D7, B7
      intArrayOf(23, 12), // FR: F5, R3
      intArrayOf(21, 41), // FL: F3, L5
      intArrayOf(50, 39), // BL: B5, L3
      intArrayOf(48, 14), // BR: B3, R5
    )

    private val EDGE_COLORS = arrayOf(
      intArrayOf(0, 1), // UR: U R
      intArrayOf(0, 2), // UF: U F
      intArrayOf(0, 4), // UL: U L
      intArrayOf(0, 5), // UB: U B
      intArrayOf(3, 1), // DR: D R
      intArrayOf(3, 2), // DF: D F
      intArrayOf(3, 4), // DL: D L
      intArrayOf(3, 5), // DB: D B
      intArrayOf(2, 1), // FR: F R
      intArrayOf(2, 4), // FL: F L
      intArrayOf(5, 4), // BL: B L
      intArrayOf(5, 1), // BR: B R
    )

    /**
     * Converts a sticker-face array (0..5 for U,R,F,D,L,B) into a cubie cube.
     * Returns null if the cube is not physically solvable.
     */
    fun fromStickers(stickers: IntArray): CubieCube? {
      if (stickers.size != 54) return null
      val cube = CubieCube()

      val cornerSeen = BooleanArray(8)
      for (pos in 0 until 8) {
        val f = CORNER_FACELETS[pos]
        val c0 = stickers[f[0]]
        val c1 = stickers[f[1]]
        val c2 = stickers[f[2]]
        var found = false
        for (cubie in 0 until 8) {
          val col = CORNER_COLORS[cubie]
          if (c0 == col[0] && c1 == col[1] && c2 == col[2]) {
            cube.cp[pos] = cubie
            cube.co[pos] = 0
            cornerSeen[cubie] = true
            found = true
            break
          }
          if (c0 == col[1] && c1 == col[2] && c2 == col[0]) {
            cube.cp[pos] = cubie
            cube.co[pos] = 1
            cornerSeen[cubie] = true
            found = true
            break
          }
          if (c0 == col[2] && c1 == col[0] && c2 == col[1]) {
            cube.cp[pos] = cubie
            cube.co[pos] = 2
            cornerSeen[cubie] = true
            found = true
            break
          }
        }
        if (!found) return null
      }
      if (!cornerSeen.all { it }) return null

      val edgeSeen = BooleanArray(12)
      for (pos in 0 until 12) {
        val f = EDGE_FACELETS[pos]
        val c0 = stickers[f[0]]
        val c1 = stickers[f[1]]
        var found = false
        for (cubie in 0 until 12) {
          val col = EDGE_COLORS[cubie]
          if (c0 == col[0] && c1 == col[1]) {
            cube.ep[pos] = cubie
            cube.eo[pos] = 0
            edgeSeen[cubie] = true
            found = true
            break
          }
          if (c0 == col[1] && c1 == col[0]) {
            cube.ep[pos] = cubie
            cube.eo[pos] = 1
            edgeSeen[cubie] = true
            found = true
            break
          }
        }
        if (!found) return null
      }
      if (!edgeSeen.all { it }) return null

      // Validity checks (physical solvability).
      var cornerOriSum = 0
      for (i in 0 until 8) cornerOriSum += cube.co[i]
      if (cornerOriSum % 3 != 0) return null

      var edgeOriSum = 0
      for (i in 0 until 12) edgeOriSum += cube.eo[i]
      if (edgeOriSum % 2 != 0) return null

      val cornerParity = parity(cube.cp, 8)
      val edgeParity = parity(cube.ep, 12)
      if (cornerParity != edgeParity) return null

      return cube
    }

    private fun parity(perm: IntArray, n: Int): Int {
      var p = 0
      for (i in 0 until n) {
        for (j in i + 1 until n) {
          if (perm[i] > perm[j]) p = p xor 1
        }
      }
      return p
    }
  }

  /** Scratch arrays for apply() to avoid allocations. */
  class Scratch {
    val cp = IntArray(8)
    val co = IntArray(8)
    val ep = IntArray(12)
    val eo = IntArray(12)
  }
}

internal object MathTables {
  val FACT: IntArray = IntArray(13)
  val CNK: Array<IntArray> = Array(12) { IntArray(13) }

  // Thread-confined scratch arrays used in table generation / fast indexing.
  val TMP8: IntArray = IntArray(8)
  val TMP4: IntArray = IntArray(4)

  init {
    FACT[0] = 1
    for (i in 1 until FACT.size) FACT[i] = FACT[i - 1] * i

    for (n in 0..11) {
      CNK[n][0] = 1
      for (k in 1..(n + 1)) {
        CNK[n][k] = if (k == n + 1) 1 else CNK[n - 1][k - 1] + CNK[n - 1][k]
      }
    }
  }

  fun permToIndex(perm: IntArray, n: Int): Int {
    var idx = 0
    val seen = BooleanArray(n)
    for (i in 0 until n) {
      var smaller = 0
      val v = perm[i]
      for (j in 0 until v) if (!seen[j]) smaller++
      idx += smaller * FACT[n - 1 - i]
      seen[v] = true
    }
    return idx
  }

  fun indexToPerm(index: Int, out: IntArray, n: Int) {
    var idx = index
    val elems = IntArray(n) { it }
    var len = n
    for (i in 0 until n) {
      val f = FACT[n - 1 - i]
      val q = idx / f
      idx %= f
      out[i] = elems[q]
      // remove elems[q]
      for (j in q until len - 1) elems[j] = elems[j + 1]
      len--
    }
  }
}

