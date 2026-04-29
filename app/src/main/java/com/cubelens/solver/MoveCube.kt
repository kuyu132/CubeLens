package com.cubelens.solver

import com.cubelens.solver.Move.Face

/**
 * Move tables for a CubieCube (Kociemba 2-phase).
 *
 * Corner order: URF=0, UFL=1, ULB=2, UBR=3, DFR=4, DLF=5, DBL=6, DRB=7
 *
 * Corner orientation delta per basic face turn:
 *   U: all 0      D: all 0
 *   R: URF+2, UBR+1, DFR+1, DRB+2  (corners on R face)
 *   L: UFL+1, ULB+2, DLF+2, DBL+1  (corners on L face)
 *   F: URF+1, UFL+2, DFR+2, DLF+1  (corners on F face)
 *   B: ULB+2, UBR+1, DRB+1, DBL+2  (corners on B face)
 *
 * Edge order: UR=0, UF=1, UL=2, UB=3, DR=4, DF=5, DL=6, DB=7, FR=8, FL=9, BL=10, BR=11
 * Edge orientation: F/B moves flip UF,UB,DF,DB edges.
 *
 * Corner permutation (8 elements: which cubie at each position):
 *   U: [3,0,1,7,4,5,2,6]   (URF UFL ULB UBR DFR DLF DBL DRB)
 *   R: [4,1,2,0,7,5,6,3]   (URF→UBR→DRB→DFR→URF)
 *   F: [1,5,2,3,0,4,6,7]   (URF→UFL→DLF→DFR→URF)
 *   D: [0,1,2,3,5,6,7,4]   (DFR DLF DBL DRB rotate)
 *   L: [0,2,6,3,4,1,5,7]   (UFL→ULB→DBL→DLF→UFL)
 *   B: [2,1,0,3,4,5,7,6]   (ULB→UBR→DRB→DBL→ULB)
 *
 * Edge permutation (12 elements: which cubie at each position):
 *   U: [7,0,1,2,3,4,5,6,8,9,10,11]   (UR→UB→UL→UF→UR)
 *   R: [8,1,2,3,11,5,6,7,4,9,10,0]   (UR→BR→DR→FR→UR)
 *   F: [0,8,2,3,4,9,6,7,1,5,10,11]   (UF→FR→DF→FL→UF)
 *   D: [0,1,2,3,5,6,7,4,8,9,10,11]   (DR→DF→DL→DB→DR)
 *   L: [0,1,9,3,4,5,10,7,8,2,6,11]   (UL→FL→DL→BL→UL)
 *   B: [0,1,2,11,4,5,6,10,8,9,3,7]   (UB→BR→DB→BL→UB)
 */
internal object MoveCube {

    // ── Corner data ────────────────────────────────────────────────────────────

    private val CP = arrayOf(
        intArrayOf(3, 0, 1, 7, 4, 5, 2, 6), // U
        intArrayOf(4, 1, 2, 0, 7, 5, 6, 3), // R
        intArrayOf(1, 5, 2, 3, 0, 4, 6, 7), // F
        intArrayOf(0, 1, 2, 3, 5, 6, 7, 4), // D
        intArrayOf(0, 2, 6, 3, 4, 1, 5, 7), // L
        intArrayOf(2, 1, 0, 3, 4, 5, 7, 6), // B
    )

    // Corner orientation delta (mod 3) for each face turn.
    private val CO = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0), // U: no orientation change
        intArrayOf(2, 0, 0, 1, 1, 0, 0, 2), // R: URF+2, UBR+1, DFR+1, DRB+2
        intArrayOf(1, 2, 0, 0, 2, 1, 0, 0), // F: URF+1, UFL+2, DFR+2, DLF+1
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0), // D: no orientation change
        intArrayOf(0, 1, 2, 0, 0, 2, 1, 0), // L: UFL+1, ULB+2, DLF+2, DBL+1
        intArrayOf(0, 0, 2, 1, 0, 0, 1, 2), // B: ULB+2, UBR+1, DRB+1, DBL+2
    )

    // ── Edge data ─────────────────────────────────────────────────────────────

    private val EP = arrayOf(
        intArrayOf(7, 0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11), // U
        intArrayOf(8, 1, 2, 3, 11, 5, 6, 7, 4, 9, 10, 0), // R
        intArrayOf(0, 8, 2, 3, 4, 9, 6, 7, 1, 5, 10, 11), // F
        intArrayOf(0, 1, 2, 3, 5, 6, 7, 4, 8, 9, 10, 11), // D
        intArrayOf(0, 1, 9, 3, 4, 5, 10, 7, 8, 2, 6, 11), // L
        intArrayOf(0, 1, 2, 11, 4, 5, 6, 10, 8, 9, 3, 7), // B
    )

    // Edge orientation delta: F/B flip UF(1),UB(3),DF(5),DB(7); others 0.
    private val EO = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // U
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // R
        intArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0), // F: UF,UB,DF,DB flip
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // D
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), // L
        intArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0), // B
    )

    // ── Move CubieCubes (built lazily) ─────────────────────────────────────────

    private val moveCubes: Array<CubieCube> by lazy {
        Array(18) { idx ->
            val face = Face.entries[idx / 3]
            val power = (idx % 3) + 1
            buildMoveCube(face, power)
        }
    }

    private fun buildMoveCube(face: Face, power: Int): CubieCube {
        val cube = CubieCube()
        cube.setSolved()
        repeat(power) {
            val s = CubieCube.Scratch()
            val src = CubieCube(CP[face.ordinal], CO[face.ordinal], EP[face.ordinal], EO[face.ordinal])
            cube.apply(src, s)
        }
        return cube
    }

    /** Apply a Singmaster move to a CubieCube (modifies it in place). */
    fun applyMove(cube: CubieCube, m: Move) {
        val moveCube = moveCubes[(m.face.ordinal * 3) + (m.power - 1)]
        val s = CubieCube.Scratch()
        cube.apply(moveCube, s)
    }

    /** Apply a face turn N times (N=1..3) to a CubieCube. */
    fun applyMoveN(cube: CubieCube, face: Face, n: Int) {
        require(n in 1..3) { "power must be 1-3, got $n" }
        val moveCube = moveCubes[(face.ordinal * 3) + (n - 1)]
        val s = CubieCube.Scratch()
        cube.apply(moveCube, s)
    }
}
