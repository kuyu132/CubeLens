package com.cubelens.solver

import android.util.Log
import com.cubelens.solver.Move.Face

/**
 * Kociemba 2-phase solver using IDA* search.
 *
 * Phase 1: find shortest path from scrambled cube to any G1 state
 *          (twist=0, flip=0, slice edges fixed in positions 8-11).
 *          Uses all 18 Singmaster moves with twist×flip pruning.
 *
 * Phase 2: from each G1 node, IDA* to solved using G2 moves
 *          (U/D any power + half-turns) with corner×slice pruning.
 *
 * Target: solve any valid cube in <3 seconds on mobile.
 */
class KociembaSolver(
    private val maxTimeMs: Long = 5000L,
) {

    private val allMoves = Move.entries.toTypedArray()

    // G2 (phase-2) moves: U/D (all powers) + half-turns.
    private val g2Moves = arrayOf(
        Move.U, Move.U2, Move.U_PRIME,
        Move.D, Move.D2, Move.D_PRIME,
        Move.R2, Move.L2, Move.F2, Move.B2,
    )

    // Index each face→which move indices belong to it (for skip-same-face optimization).
    private val faceMoves = Array(Face.entries.size) { fi ->
        allMoves.indices.filter { allMoves[it].face.ordinal == fi }.toIntArray()
    }

    // ── Scratch state ─────────────────────────────────────────────────────────

    private val phase1Path = IntArray(20)
    private val phase2Path = IntArray(20)

    // ── Public API ───────────────────────────────────────────────────────────

    fun solve(facelets: String): List<Move> {
        val cubeState = CubeState.tryParse(facelets) ?: return emptyList()
        val stickers = cubeState.toStickerFaces()

        val startCube = CubieCube.fromStickers(stickers) ?: return emptyList()
        if (startCube.isSolvedPhase2()) return emptyList()

        return solve2Phase(startCube)
    }

    // ── 2-Phase search ───────────────────────────────────────────────────────

    private fun solve2Phase(startCube: CubieCube): List<Move> {
        val startNs = System.nanoTime()

        // Phase 1: IDA* from startCube to G1.
        val twist0 = startCube.getTwist()
        val flip0 = startCube.getFlip()
        var bound = PruningTables.phase1Depth(twist0, flip0)
        val maxPhase1Depth = 13

        while (bound <= maxPhase1Depth) {
            if (elapsedMs(startNs) >= maxTimeMs) break

            // Fresh copy of start state for this iteration.
            val cube = startCube.copy()
            val found = phase1Search(cube, bound, 0, twist0, flip0, -1, startNs)

            if (found >= 0) {
                // Apply phase-1 path to reach G1 state.
                cube.setSolved()
                for (i in 0 until found) {
                    cube.move(allMoves[phase1Path[i]])
                }

                // Phase 2 from this G1 state.
                val phase2Sol = phase2Search(cube, 18, startNs)
                if (phase2Sol >= 0) {
                    val moves = ArrayList<Move>(found + phase2Sol)
                    for (i in 0 until found) moves.add(allMoves[phase1Path[i]])
                    for (i in 0 until phase2Sol) moves.add(g2Moves[phase2Path[i]])
                    Log.i(TAG, "Solved in ${found}+${phase2Sol} = ${moves.size} moves in ${elapsedMs(startNs)}ms")
                    return moves
                }
            }

            bound++
        }

        Log.w(TAG, "No solution found within ${maxTimeMs}ms")
        return emptyList()
    }

    /**
     * Phase-1 IDA* search.
     * Returns path length on success, -1 if no solution at this bound, -2 if timeout.
     */
    private fun phase1Search(
        cube: CubieCube,
        bound: Int,
        g: Int,
        twist: Int,
        flip: Int,
        prevFace: Int,
        startNs: Long,
    ): Int {
        val f = g + PruningTables.phase1Depth(twist, flip)
        if (f > bound) return -1
        if (twist == 0 && flip == 0) return g  // G1 reached!

        if (elapsedMs(startNs) >= maxTimeMs) return -2

        for (face in 0..5) {
            if (face == prevFace) continue

            for (moveIdx in faceMoves[face]) {
                phase1Path[g] = moveIdx
                cube.move(allMoves[moveIdx])
                val newTwist = cube.getTwist()
                val newFlip = cube.getFlip()

                val result = phase1Search(cube, bound, g + 1, newTwist, newFlip, face, startNs)
                if (result >= 0) return result

                cube.move(allMoves[moveIdx].inverse())
            }
        }
        return -1
    }

    /**
     * Phase-2 IDA* search from a G1 state (twist=0, flip=0).
     * Returns path length on success, -1 otherwise.
     */
    private fun phase2Search(
        cube: CubieCube,
        bound: Int,
        startNs: Long,
    ): Int {
        val cp8 = cube.getCornerPerm()
        val uds = cube.getSlice()
        val ep8 = cube.getEdgePerm8()

        if (cp8 == 0 && uds == 0 && ep8 == 0) return 0

        val f = PruningTables.phase2Depth(cp8, uds)
        if (f > bound) return -1

        return phase2Dfs(cube, bound, 0, -1, startNs)
    }

    private fun phase2Dfs(
        cube: CubieCube,
        bound: Int,
        g: Int,
        prevFace: Int,
        startNs: Long,
    ): Int {
        val cp8 = cube.getCornerPerm()
        val uds = cube.getSlice()
        val ep8 = cube.getEdgePerm8()

        val f = g + PruningTables.phase2Depth(cp8, uds)
        if (f > bound) return -1
        if (cp8 == 0 && uds == 0 && ep8 == 0) return g

        if (elapsedMs(startNs) >= maxTimeMs) return -1

        for (i in g2Moves.indices) {
            val m = g2Moves[i]
            if (m.face.ordinal == prevFace) continue

            phase2Path[g] = i
            cube.move(m)

            val result = phase2Dfs(cube, bound, g + 1, m.face.ordinal, startNs)
            if (result >= 0) return result

            cube.move(m.inverse())
        }
        return -1
    }

    private inline fun elapsedMs(startNs: Long): Long =
        (System.nanoTime() - startNs) / 1_000_000

    companion object {
        private const val TAG = "KociembaSolver"

        @Volatile
        private var instance: KociembaSolver? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: KociembaSolver().also { instance = it }
        }
    }
}
