package com.cubelens.ui.solving

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.solver.CubeState
import com.cubelens.solver.Move
import com.cubelens.ui.util.CubeColorUi
import com.cubelens.viewmodel.CaptureViewModel
import com.cubelens.viewmodel.SolveViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolvingScreen(
    captureViewModel: CaptureViewModel,
    solveViewModel: SolveViewModel,
    onBack: () -> Unit,
    onSaveSolve: ((scramble: String, solution: String, moveCount: Int) -> Unit)? = null,
) {
    val captureState by captureViewModel.uiState.collectAsStateWithLifecycle()
    val solveState by solveViewModel.uiState.collectAsStateWithLifecycle()
    val onBackLatest by rememberUpdatedState(onBack)
    val context = LocalContext.current

    var showSaveDialog by remember { mutableStateOf(false) }

    val facelets = remember(captureState.faceOrder, captureState.scans) {
        faceletsFromScans(faceOrder = captureState.faceOrder, scans = captureState.scans)
    }

    LaunchedEffect(facelets) {
        if (facelets != null) solveViewModel.solve(facelets)
    }

    // Monitor when user reaches last step
    val result = solveState.result
    val total = result?.moves?.size ?: 0
    LaunchedEffect(solveState.currentStep, total) {
        if (total > 0 && solveState.currentStep >= total - 1 && !solveState.isPlaying) {
            // User has watched all steps
            showSaveDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solving") },
                navigationIcon = {
                    IconButton(onClick = onBackLatest) { Text("←") }
                },
                actions = {
                    if (result != null && result.moves.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val moves = result.moves.joinToString(" ")
                                val scramble = facelets ?: ""
                                val shareText = buildShareText(scramble, moves)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Solution"))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (facelets == null) {
                Text(
                    "Cube is incomplete or invalid. Go back to review.",
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBackLatest) { Text("Back") }
                return@Column
            }

            if (solveState.isSolving) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Solving…")
                    }
                }
                return@Column
            }

            val result = solveState.result ?: run {
                Text("No solve result yet.")
                return@Column
            }

            if (result.errorMessage != null) {
                Text(result.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            val moves = result.moves
            val total = moves.size
            val currentStep = solveState.currentStep.coerceIn(0, (total - 1).coerceAtLeast(0))

            // Header: step counter + current move label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (total == 0) "Solved!" else "Move ${currentStep + 1} / $total",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (total == 0) "✓" else moves[currentStep].toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Animated 3D cube preview
            AnimatedCube3DPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                facelets = facelets,
                moves = moves,
                currentStep = currentStep,
                animProgress = solveState.animProgress,
            )

            // Controls: Prev | Play/Pause | Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { solveViewModel.prevStep() },
                    enabled = total > 0 && currentStep > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("◀ Prev") }

                Button(
                    onClick = { solveViewModel.togglePlay() },
                    enabled = total > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (solveState.isPlaying) "⏸ Pause" else "▶ Play")
                }

                OutlinedButton(
                    onClick = { solveViewModel.nextStep() },
                    enabled = total > 0 && currentStep < total - 1,
                    modifier = Modifier.weight(1f),
                ) { Text("Next ▶") }
            }

            // Move list — auto-scroll to current
            val listState = rememberLazyListState()
            LaunchedEffect(currentStep) {
                if (total > 0) listState.animateScrollToItem(currentStep)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(moves) { idx, move ->
                    val isCurrent = idx == currentStep && total > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${idx + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(move.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    // Save dialog when user reaches last step
    if (showSaveDialog && onSaveSolve != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save this solve?") },
            text = {
                val movesText = result?.moves?.joinToString(" ") ?: ""
                Text("Record this solution with ${result?.moves?.size ?: 0} moves?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val movesText = result?.moves?.joinToString(" ") ?: ""
                        val moveCount = result?.moves?.size ?: 0
                        onSaveSolve?.invoke(facelets ?: "", movesText, moveCount)
                        showSaveDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Animated 3D preview
// ---------------------------------------------------------------------------

@Composable
private fun AnimatedCube3DPreview(
    modifier: Modifier,
    facelets: String,
    moves: List<Move>,
    currentStep: Int,
    animProgress: Float,
) {
    // Recompute sticker arrays only when step boundary changes, not every frame
    val (beforeStickers, afterStickers, currentMove) = remember(facelets, moves, currentStep) {
        val cube = CubeState.parse(facelets)
        val scratch = IntArray(54)

        val before = cube.toStickerFaces()
        repeat(currentStep.coerceIn(0, moves.size)) { i ->
            CubeState.applyMoveInPlace(before, moves[i], scratch)
        }

        val move = moves.getOrNull(currentStep)
        val after = before.clone()
        if (move != null) CubeState.applyMoveInPlace(after, move, scratch)

        Triple(before, after, move)
    }

    val isAnimating = animProgress < 1f && currentMove != null

    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height) / 8.2f
        val vX    = Offset(s, 0f)
        val vY    = Offset(0f, s)
        val vBack = Offset(-s * 0.72f, -s * 0.42f)
        val vBackR = Offset(s * 0.72f, -s * 0.42f)

        val cubeW = 3 * s + 3 * vBackR.x
        val cubeH = 3 * s + 3 * (-vBack.y)

        val originFront = Offset(
            x = (size.width - cubeW) / 2f + 3f * (-vBack.x),
            y = (size.height - cubeH) / 2f + 3f * (-vBack.y),
        )
        val originRight = originFront + vX * 3f

        // Face descriptors: (face enum, screen origin, axis-a, axis-b, fill alpha)
        data class FaceDesc(
            val face: Move.Face,
            val origin: Offset,
            val a: Offset,
            val b: Offset,
            val alpha: Float,
        )

        val faceDescs = listOf(
            FaceDesc(Move.Face.U, originFront,  vX,     vBack,  0.96f),
            FaceDesc(Move.Face.F, originFront,  vX,     vY,     1.00f),
            FaceDesc(Move.Face.R, originRight,  vBackR, vY,     0.92f),
        )

        if (!isAnimating || currentMove == null) {
            // Static render
            for (fd in faceDescs) {
                drawFaceStatic(fd.face, fd.origin, fd.a, fd.b, afterStickers, fd.alpha)
            }
        } else {
            val rotFace = currentMove.face
            val power   = currentMove.power

            for (fd in faceDescs) {
                when {
                    fd.face == rotFace -> {
                        // Animate the rotating face: rotate stickers around face center
                        drawFaceAnimated(
                            face       = fd.face,
                            origin     = fd.origin,
                            a          = fd.a,
                            b          = fd.b,
                            stickers   = beforeStickers,
                            power      = power,
                            progress   = animProgress,
                            fillAlpha  = fd.alpha,
                        )
                    }
                    else -> {
                        // Adjacent strips crossfade; rest is static (after state)
                        drawFaceWithStripFade(
                            face          = fd.face,
                            origin        = fd.origin,
                            a             = fd.a,
                            b             = fd.b,
                            beforeStickers = beforeStickers,
                            afterStickers  = afterStickers,
                            rotatingFace  = rotFace,
                            progress      = animProgress,
                            fillAlpha     = fd.alpha,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Draw helpers
// ---------------------------------------------------------------------------

/** Draw a face with no animation. */
private fun DrawScope.drawFaceStatic(
    face: Move.Face,
    origin: Offset,
    a: Offset,
    b: Offset,
    stickers: IntArray,
    fillAlpha: Float,
) {
    val outline = Stroke(width = 1.dp.toPx())
    val border  = Color.Black.copy(alpha = 0.38f)
    val fi = faceIndex(face)

    for (r in 0..2) {
        for (c in 0..2) {
            val corners = stickerCorners(origin, a, b, r, c)
            val color = CubeColorUi.swatch(faceColor(stickers[fi * 9 + r * 3 + c])).copy(alpha = fillAlpha)
            drawStickerPath(corners, color, border, outline)
        }
    }
}

/**
 * Draw the rotating face: each sticker rotates around the face center in screen space.
 * The rotation angle is interpolated from 0 to (power * 90°) based on [progress].
 */
private fun DrawScope.drawFaceAnimated(
    face: Move.Face,
    origin: Offset,
    a: Offset,
    b: Offset,
    stickers: IntArray,
    power: Int,
    progress: Float,
    fillAlpha: Float,
) {
    val outline = Stroke(width = 1.dp.toPx())
    val border  = Color.Black.copy(alpha = 0.38f)
    val fi = faceIndex(face)

    // Face center in screen space
    val center = origin + a * 1.5f + b * 1.5f

    // Rotation direction: U/D rotate one way, F/R/B/L the other (matches physical cube)
    val sign = if (face == Move.Face.U || face == Move.Face.D) 1f else 1f
    val totalAngle = sign * power * (PI / 2).toFloat()
    val angle = totalAngle * progress

    val cosA = cos(angle)
    val sinA = sin(angle)

    for (r in 0..2) {
        for (c in 0..2) {
            val rawCorners = stickerCorners(origin, a, b, r, c)
            // Rotate each corner around face center
            val rotCorners = rawCorners.map { pt ->
                val dx = pt.x - center.x
                val dy = pt.y - center.y
                Offset(
                    x = center.x + (dx * cosA - dy * sinA).toFloat(),
                    y = center.y + (dx * sinA + dy * cosA).toFloat(),
                )
            }
            val color = CubeColorUi.swatch(faceColor(stickers[fi * 9 + r * 3 + c])).copy(alpha = fillAlpha)
            drawStickerPath(rotCorners, color, border, outline)
        }
    }
}

/**
 * Draw a non-rotating face, but crossfade the strips that are adjacent to the rotating face.
 */
private fun DrawScope.drawFaceWithStripFade(
    face: Move.Face,
    origin: Offset,
    a: Offset,
    b: Offset,
    beforeStickers: IntArray,
    afterStickers: IntArray,
    rotatingFace: Move.Face,
    progress: Float,
    fillAlpha: Float,
) {
    val outline = Stroke(width = 1.dp.toPx())
    val border  = Color.Black.copy(alpha = 0.38f)
    val fi = faceIndex(face)

    // Determine which (row/col, index) is the affected strip on this face
    val affectedStrip: Pair<Boolean, Int>? = affectedStrip(face, rotatingFace)

    for (r in 0..2) {
        for (c in 0..2) {
            val inStrip = affectedStrip?.let { (isRow, idx) ->
                if (isRow) r == idx else c == idx
            } ?: false

            val corners = stickerCorners(origin, a, b, r, c)

            if (inStrip) {
                // Crossfade: draw before (fading out) then after (fading in)
                val beforeColor = CubeColorUi.swatch(faceColor(beforeStickers[fi * 9 + r * 3 + c]))
                    .copy(alpha = fillAlpha * (1f - progress))
                val afterColor  = CubeColorUi.swatch(faceColor(afterStickers[fi * 9 + r * 3 + c]))
                    .copy(alpha = fillAlpha * progress)
                drawStickerPath(corners, beforeColor, border.copy(alpha = 0f), outline)
                drawStickerPath(corners, afterColor,  border, outline)
            } else {
                val color = CubeColorUi.swatch(faceColor(afterStickers[fi * 9 + r * 3 + c]))
                    .copy(alpha = fillAlpha)
                drawStickerPath(corners, color, border, outline)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Geometry helpers
// ---------------------------------------------------------------------------

/** Returns the 4 screen-space corners of sticker (r, c) on a face. */
private fun stickerCorners(origin: Offset, a: Offset, b: Offset, r: Int, c: Int): List<Offset> {
    val base = origin + a * c.toFloat() + b * r.toFloat()
    return listOf(
        base,
        base + a,
        base + a + b,
        base + b,
    )
}

private fun DrawScope.drawStickerPath(
    corners: List<Offset>,
    fill: Color,
    border: Color,
    stroke: Stroke,
) {
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        lineTo(corners[1].x, corners[1].y)
        lineTo(corners[2].x, corners[2].y)
        lineTo(corners[3].x, corners[3].y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, border, style = stroke)
}

// ---------------------------------------------------------------------------
// Cube data helpers
// ---------------------------------------------------------------------------

private fun faceIndex(face: Move.Face): Int = when (face) {
    Move.Face.U -> 0
    Move.Face.R -> 1
    Move.Face.F -> 2
    Move.Face.D -> 3
    Move.Face.L -> 4
    Move.Face.B -> 5
}

private fun faceColor(faceIdx: Int): CubeColor = when (faceIdx) {
    0 -> CubeColor.WHITE
    1 -> CubeColor.RED
    2 -> CubeColor.GREEN
    3 -> CubeColor.YELLOW
    4 -> CubeColor.ORANGE
    5 -> CubeColor.BLUE
    else -> CubeColor.UNKNOWN
}

/**
 * Returns which strip (isRow, index) on [face] is adjacent to [rotatingFace].
 * Only covers the three visible faces: U, F, R.
 * Returns null if no visible strip is affected.
 */
private fun affectedStrip(face: Move.Face, rotatingFace: Move.Face): Pair<Boolean, Int>? =
    when (rotatingFace) {
        Move.Face.U -> when (face) {
            Move.Face.F -> Pair(true, 0)   // F top row
            Move.Face.R -> Pair(true, 0)   // R top row
            else -> null
        }
        Move.Face.D -> when (face) {
            Move.Face.F -> Pair(true, 2)   // F bottom row
            Move.Face.R -> Pair(true, 2)   // R bottom row
            else -> null
        }
        Move.Face.F -> when (face) {
            Move.Face.U -> Pair(true, 2)   // U bottom row
            Move.Face.R -> Pair(false, 0)  // R left col
            else -> null
        }
        Move.Face.B -> when (face) {
            Move.Face.U -> Pair(true, 0)   // U top row
            Move.Face.R -> Pair(false, 2)  // R right col
            else -> null
        }
        Move.Face.R -> when (face) {
            Move.Face.U -> Pair(false, 2)  // U right col
            Move.Face.F -> Pair(false, 2)  // F right col
            else -> null
        }
        Move.Face.L -> when (face) {
            Move.Face.U -> Pair(false, 0)  // U left col
            Move.Face.F -> Pair(false, 0)  // F left col
            else -> null
        }
    }

// ---------------------------------------------------------------------------
// Facelets from scans
// ---------------------------------------------------------------------------

private fun faceletsFromScans(
    faceOrder: List<CubeFace>,
    scans: Map<CubeFace, FaceScan>,
): String? {
    if (faceOrder.any { it !in scans }) return null

    val centerColorToFaceChar = HashMap<CubeColor, Char>(6)
    for (face in faceOrder) {
        val scan   = scans[face] ?: return null
        val center = scan.colors.getOrNull(4) ?: return null
        val ch     = face.label.firstOrNull() ?: return null
        centerColorToFaceChar[center] = ch
    }
    if (centerColorToFaceChar.size != 6) return null

    val canonical = listOf(CubeFace.U, CubeFace.R, CubeFace.F, CubeFace.D, CubeFace.L, CubeFace.B)
    val sb = StringBuilder(54)
    for (face in canonical) {
        val scan = scans[face] ?: return null
        for (c in scan.colors) {
            val ch = centerColorToFaceChar[c] ?: return null
            sb.append(ch)
        }
    }
    return sb.toString()
}

private fun buildShareText(scramble: String, moves: String): String {
    val moveCount = moves.split(" ").filter { it.isNotBlank() }.size
    return buildString {
        appendLine("🧊 CubeLens")
        appendLine("Scramble: $scramble")
        appendLine("Solution: $moves ($moveCount moves)")
        appendLine("Time: --")
    }.trimEnd()
}
