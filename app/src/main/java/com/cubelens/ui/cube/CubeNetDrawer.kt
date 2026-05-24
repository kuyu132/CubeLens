package com.cubelens.ui.cube

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.ui.util.CubeColorUi

/**
 * Unfolded cube net (all 6 faces):
 *
 *       [U]
 *   [L][F][R][B]
 *       [D]
 */
object CubeNetDrawer {

  private val NET_LAYOUT: List<Pair<CubeFace, Pair<Int, Int>>> = listOf(
    CubeFace.U to (1 to 0),
    CubeFace.L to (0 to 1),
    CubeFace.F to (1 to 1),
    CubeFace.R to (2 to 1),
    CubeFace.B to (3 to 1),
    CubeFace.D to (1 to 2),
  )

  fun DrawScope.drawCubeNet(
    scans: Map<CubeFace, FaceScan>,
    highlightFace: CubeFace? = null,
  ) {
    val gridW = 4
    val gridH = 3
    val pad = 4.dp.toPx()
    val availW = size.width - pad * 2
    val availH = size.height - pad * 2
    val unit = minOf(availW / gridW, availH / gridH)
    val originX = (size.width - unit * gridW) / 2f
    val originY = (size.height - unit * gridH) / 2f
    val stickerGap = unit * 0.04f
    val stickerSize = (unit - stickerGap * 4) / 3f

    for ((face, gridPos) in NET_LAYOUT) {
      val (gx, gy) = gridPos
      val faceLeft = originX + gx * unit + stickerGap
      val faceTop = originY + gy * unit + stickerGap
      val colors = scans[face]?.colors ?: List(9) { CubeColor.UNKNOWN }
      val isHighlight = face == highlightFace

      for (row in 0..2) {
        for (col in 0..2) {
          val idx = row * 3 + col
          val color = colors.getOrNull(idx) ?: CubeColor.UNKNOWN
          val left = faceLeft + col * (stickerSize + stickerGap)
          val top = faceTop + row * (stickerSize + stickerGap)
          drawRect(
            color = CubeColorUi.swatch(color),
            topLeft = Offset(left, top),
            size = Size(stickerSize, stickerSize),
          )
          drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(stickerSize, stickerSize),
            style = Stroke(width = 1f),
          )
        }
      }

      val faceOutlineColor = when {
        isHighlight -> Color.White
        face in scans -> Color.White.copy(alpha = 0.45f)
        else -> Color.Gray.copy(alpha = 0.35f)
      }
      drawRect(
        color = faceOutlineColor,
        topLeft = Offset(originX + gx * unit, originY + gy * unit),
        size = Size(unit, unit),
        style = Stroke(width = if (isHighlight) 2.5f else 1.5f),
      )

    }
  }
}
