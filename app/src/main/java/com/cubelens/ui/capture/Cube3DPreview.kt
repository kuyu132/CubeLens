package com.cubelens.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.ui.util.CubeColorUi

/**
 * Isometric 3D cube preview shown during the scanning phase.
 * Displays the 3 visible faces (U, F, R) with scanned stickers in real color
 * and unscanned stickers as a dark placeholder.
 *
 * Coordinate system: preview area is treated as if the cube is viewed from
 * slightly above and to the right. U is at top, F is below U, R is to the right of U.
 */
@Composable
fun Cube3DPreview(
  modifier: Modifier = Modifier,
  scans: Map<CubeFace, FaceScan>,
) {
  // Compute colors before entering DrawScope (can't call @Composable from DrawScope)
  val uColors = remember(scans) { scans[CubeFace.U]?.colors ?: List(9) { CubeColor.UNKNOWN } }
  val fColors = remember(scans) { scans[CubeFace.F]?.colors ?: List(9) { CubeColor.UNKNOWN } }
  val rColors = remember(scans) { scans[CubeFace.R]?.colors ?: List(9) { CubeColor.UNKNOWN } }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black.copy(alpha = 0.7f))
      .padding(12.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasW = size.width

      // Scale reference: designed for 560px wide canvas
      val refScale = canvasW / 560f

      // Helper: draw a face with 3x3 sticker grid
      fun drawFace(
        faceCx: Float,
        faceCy: Float,
        faceS: Float,
        faceScaleY: Float,
        stickerColors: List<CubeColor>,
        borderColor: Color,
      ) {
        val sw = faceS * 2         // sticker width
        val sh = faceS * 2 * faceScaleY  // sticker height (foreshortened)
        val gap = faceS * 0.28f    // gap between stickers
        val borderW = 2f * refScale

        // Draw 3x3 sticker grid, row-major
        for (row in 0..2) {
          for (col in 0..2) {
            val idx = row * 3 + col
            val color = stickerColors.getOrNull(idx) ?: CubeColor.UNKNOWN
            val swatchColor = CubeColorUi.swatch(color)
            val border = if (color == CubeColor.UNKNOWN) Color.DarkGray else Color.Black

            // Sticker top-left: centered on face, then offset by (col, row)
            val localX = faceCx + (col - 1) * (sw + gap) + col * gap
            val localY = faceCy + (row - 1) * (sh + gap) + row * gap

            drawRect(
              color = swatchColor,
              topLeft = Offset(localX, localY),
              size = Size(sw, sh),
            )
            drawRect(
              color = border.copy(alpha = 0.5f),
              topLeft = Offset(localX, localY),
              size = Size(sw, sh),
              style = Stroke(width = 1f * refScale),
            )
          }
        }

        // Face outline rectangle (centered on face)
        val halfW = 3 * (sw + gap) / 2f + gap
        val halfH = 3 * (sh + gap) / 2f + gap
        drawRect(
          color = borderColor.copy(alpha = 0.5f),
          topLeft = Offset(faceCx - halfW, faceCy - halfH),
          size = Size(halfW * 2, halfH * 2),
          style = Stroke(width = borderW),
        )
      }

      // ── Layout: U at top-left, F below U, R to the right of U ──
      // Reference coordinates (designed for 560px canvas width)
      val uCx = 185f * refScale;  val uCy = 170f * refScale;  val uS = 42f * refScale
      val fCx = 185f * refScale;  val fCy = 340f * refScale;  val fS = 38f * refScale
      val rCx = 370f * refScale;  val rCy = 175f * refScale;  val rS = 36f * refScale

      drawFace(uCx, uCy, uS, 1.00f, uColors, Color.White)
      drawFace(fCx, fCy, fS, 0.82f, fColors, Color.White)
      drawFace(rCx, rCy, rS, 0.88f, rColors, Color.White)
    }
  }
}
