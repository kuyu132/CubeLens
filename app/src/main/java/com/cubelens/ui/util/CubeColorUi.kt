package com.cubelens.ui.util

import androidx.compose.ui.graphics.Color
import com.cubelens.model.CubeColor

object CubeColorUi {
  fun swatch(color: CubeColor): Color = when (color) {
    CubeColor.WHITE -> Color(0xFFF4F4F4)
    CubeColor.YELLOW -> Color(0xFFFFD700)
    CubeColor.GREEN -> Color(0xFF2E7D32)
    CubeColor.BLUE -> Color(0xFF1565C0)
    CubeColor.RED -> Color(0xFFC62828)
    CubeColor.ORANGE -> Color(0xFFEF6C00)
    CubeColor.UNKNOWN -> Color(0xFF9E9E9E)
  }
}

