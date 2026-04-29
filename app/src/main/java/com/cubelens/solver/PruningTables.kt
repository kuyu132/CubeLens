package com.cubelens.solver

import android.util.Log
import com.cubelens.solver.Move.Face

/**
 * Precomputed pruning tables for the Kociemba 2-phase algorithm.
 *
 * Phase 1:  twist(0..2186) × flip(0..2047) → depth
 *           2187 × 2048 = 4,478,976 entries ≈ 4.5 MB.
 *
 * Phase 2:  corner-perm8(0..40319) × ud-slice(0..494) → depth
 *           40320 × 495 = 19,958,400 entries ≈ 20 MB.
 *
 * BFS frontiers are capped at MAX_FRONTIER states to stay within the Android
 * 256 MB heap limit. Unfilled entries are filled with maxDepth.
 */
internal object PruningTables {

    private const val TAG = "PruningTables"
    private const val MAX_FRONTIER = 20_000   // cap to stay within heap (20k ≈ 3.2MB for CubieCubes)

    private var cacheDir: java.io.File? = null

    /** Pre-warm pruning tables (call from Application.onCreate on IO dispatcher). */
    fun init(filesDir: java.io.File) {
        cacheDir = java.io.File(filesDir, "pruning").also { it.mkdirs() }
        phase1Table
        phase2Table
        Log.i(TAG, "PruningTables initialized")
    }

    // ── Disk cache helpers ───────────────────────────────────────────────────

    private fun loadOrCompute(name: String, expectedSize: Int, compute: () -> ByteArray): ByteArray {
        val dir = cacheDir ?: return compute()
        val file = java.io.File(dir, name)
        if (file.exists() && file.length() == expectedSize.toLong()) {
            try {
                val bytes = file.readBytes()
                if (bytes.size == expectedSize) {
                    Log.i(TAG, "Loaded $name from cache (${bytes.size} bytes)")
                    return bytes
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cache $name", e)
            }
        }
        val result = compute()
        try {
            file.writeBytes(result)
            Log.i(TAG, "Cached $name (${result.size} bytes)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write cache $name", e)
        }
        return result
    }

    // ── Phase 1 ───────────────────────────────────────────────────────────────

    /**
     * Phase-1 pruning table: twist × flip → depth.
     * Single-list BFS with front/back pointers. Frontier capped at MAX_FRONTIER.
     * Missing entries filled with maxDepth.
     */
    private val phase1Table: ByteArray by lazy {
        val size = 2187 * 2048
        loadOrCompute("phase1.bin", size) {
            Log.i(TAG, "Computing Phase-1 pruning table ($size entries)...")
            val table = ByteArray(size) { -1 }
            val startMs = System.currentTimeMillis()

            val moveFaces = intArrayOf(
                Face.U.ordinal, Face.U.ordinal, Face.U.ordinal,
                Face.R.ordinal, Face.R.ordinal, Face.R.ordinal,
                Face.F.ordinal, Face.F.ordinal, Face.F.ordinal,
                Face.D.ordinal, Face.D.ordinal, Face.D.ordinal,
                Face.L.ordinal, Face.L.ordinal, Face.L.ordinal,
                Face.B.ordinal, Face.B.ordinal, Face.B.ordinal,
            )
            val movePowers = intArrayOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3)

            val buf = CubieCube()
            val solved = CubieCube()
            solved.setSolved()
            val solvedKey = solved.getTwist() * 2048 + solved.getFlip()
            table[solvedKey] = 0

            // Single list BFS: only expand states added in the current depth layer.
            // Bug fix: prevEnd tracks where the current layer starts (not full frontier size).
            val frontier = mutableListOf<CubieCube>()
            frontier.add(CubieCube().also { it.loadFrom(solved) })
            var prevEnd = 1      // index after last item from previous depth
            var frontierEnd = 1 // index after last item added in current depth
            var depth = 0
            val maxDepth = 13

            while (depth < maxDepth) {
                var added = 0

                // Only expand states added in the PREVIOUS depth (prevEnd..frontierEnd)
                for (i in prevEnd until frontierEnd) {
                    val state = frontier[i]
                    for (j in 0 until 18) {
                        buf.loadFrom(state)
                        MoveCube.applyMoveN(buf, Face.entries[moveFaces[j]], movePowers[j])
                        val key = buf.getTwist() * 2048 + buf.getFlip()
                        if (table[key] < 0) {
                            table[key] = (depth + 1).toByte()
                            if (added < MAX_FRONTIER) {
                                val child = CubieCube()
                                child.loadFrom(buf)
                                frontier.add(child)
                                frontierEnd++
                                added++
                            }
                        }
                    }
                }

                if (added == 0) break

                if (added >= MAX_FRONTIER) {
                    Log.w(TAG, "Phase-1 depth=$depth added=$added (cap $MAX_FRONTIER)")
                }

                // Next layer: prevEnd = where current layer started, frontierEnd = end of new layer
                prevEnd = frontierEnd - added
                depth++
                Log.i(TAG, "Phase-1 depth=$depth frontierSize=${frontierEnd}, prevLayer=${prevEnd}, unfilled=${table.count { it < 0 }}")
            }

            // Fill remaining with maxDepth
            val unfilled = table.count { it < 0 }
            if (unfilled > 0) {
                Log.w(TAG, "Phase-1 filling $unfilled unfilled entries with depth=$depth")
                for (i in table.indices) {
                    if (table[i] < 0) table[i] = depth.toByte()
                }
            }

            val ms = System.currentTimeMillis() - startMs
            val filled = table.count { it >= 0 }
            Log.i(TAG, "Phase-1 table done in ${ms}ms, filled=$filled/$size")
            table
        }
    }

    fun phase1Depth(twist: Int, flip: Int): Int =
        phase1Table[twist * 2048 + flip].toInt() and 0xFF

    // ── Phase 2 ───────────────────────────────────────────────────────────────

    private val phase2Table: ByteArray by lazy {
        val CP8 = 40320
        val UDS = 495
        val size = CP8 * UDS
        loadOrCompute("phase2.bin", size) {
            Log.i(TAG, "Computing Phase-2 pruning table ($size entries)...")
            val startMs = System.currentTimeMillis()

            val tableA = IntArray(CP8 * UDS) { -1 }
            val tableB = IntArray(UDS * CP8) { -1 }

            val g2Faces = intArrayOf(
                Face.U.ordinal, Face.U.ordinal, Face.U.ordinal,
                Face.D.ordinal, Face.D.ordinal, Face.D.ordinal,
                Face.R.ordinal, Face.L.ordinal, Face.F.ordinal, Face.B.ordinal,
            )
            val g2Powers = intArrayOf(1, 2, 3, 1, 2, 3, 2, 2, 2, 2)

            val buf = CubieCube()
            val solved = CubieCube()
            solved.setSolved()
            tableA[0] = 0
            tableB[0] = 0

            data class Phase2State(val cc: CubieCube, val cp8: Int, val slice: Int, val ep8: Int)

            val frontier = mutableListOf<Phase2State>()
            frontier.add(Phase2State(solved, 0, 0, 0))
            var depth = 0
            val maxDepth = 18

            // Two pointers: frontier[0..frontierEnd) = current depth, rest = pending
            var frontierEnd = 1

            while (depth < maxDepth) {
                val curEnd = frontier.size
                var added = 0

                for (i in frontierEnd until curEnd) {
                    val st = frontier[i]
                    for (k in g2Faces.indices) {
                        buf.loadFrom(st.cc)
                        MoveCube.applyMoveN(buf, Face.entries[g2Faces[k]], g2Powers[k])
                        val newCp8 = buf.getCornerPerm()
                        val newSlice = buf.getSlice()
                        val newEp8 = buf.getEdgePerm8()

                        val idxA = newCp8 * UDS + newSlice
                        if (tableA[idxA] < 0) {
                            tableA[idxA] = depth + 1
                            if (added < MAX_FRONTIER) {
                                val child = CubieCube()
                                child.loadFrom(buf)
                                frontier.add(Phase2State(child, newCp8, newSlice, newEp8))
                                added++
                            }
                        }

                        val idxB = newSlice * CP8 + newEp8
                        if (tableB[idxB] < 0) {
                            tableB[idxB] = depth + 1
                        }
                    }
                }

                if (added == 0) break

                frontierEnd = curEnd
                depth++
                val filledA = tableA.count { it >= 0 }
                Log.i(TAG, "Phase-2 depth=$depth frontier=${frontier.size}, filledA=$filledA/${CP8 * UDS}")
            }

            // Best B depth for each slice
            val bestBForSlice = IntArray(UDS)
            for (s in 0 until UDS) {
                var best = 0
                val base = s * CP8
                for (e in 0 until CP8) {
                    val b = tableB[base + e]
                    if (b > best) best = b
                }
                bestBForSlice[s] = best
            }

            // Combined
            val combined = ByteArray(CP8 * UDS)
            for (cp8 in 0 until CP8) {
                val base = cp8 * UDS
                for (s in 0 until UDS) {
                    val a = tableA[base + s]
                    val b = bestBForSlice[s]
                    combined[base + s] = maxOf(a, b).coerceAtMost(255).toByte()
                }
            }

            val ms = System.currentTimeMillis() - startMs
            val filledA = tableA.count { it >= 0 }
            Log.i(TAG, "Phase-2 table done in ${ms}ms, filledA=$filledA/${CP8 * UDS}")
            combined
        }
    }

    fun phase2Depth(cp8: Int, slice: Int): Int =
        phase2Table[cp8 * 495 + slice].toInt() and 0xFF
}
