package com.cubelens.util

import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.solver.CubeState

object FaceletsLoader {
  private val CANONICAL_FACES = listOf(
    CubeFace.U, CubeFace.R, CubeFace.F, CubeFace.D, CubeFace.L, CubeFace.B,
  )

  fun scansFromFacelets(facelets: String): Map<CubeFace, FaceScan>? {
    val cube = CubeState.tryParse(facelets) ?: return null
    val stickers = cube.toStickerFaces()
    return CANONICAL_FACES.mapIndexed { faceIndex, face ->
      val colors = (0 until 9).map { cell ->
        cubeColorFromFaceIndex(stickers[faceIndex * 9 + cell])
      }
      face to FaceScan(
        face = face,
        colors = colors,
        confidences = List(9) { 1.0f },
        imagePath = null,
      )
    }.toMap()
  }

  private fun cubeColorFromFaceIndex(faceIndex: Int): CubeColor = when (faceIndex) {
    0 -> CubeColor.WHITE
    1 -> CubeColor.RED
    2 -> CubeColor.GREEN
    3 -> CubeColor.YELLOW
    4 -> CubeColor.ORANGE
    5 -> CubeColor.BLUE
    else -> CubeColor.UNKNOWN
  }
}
