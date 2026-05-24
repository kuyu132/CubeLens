package com.cubelens.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.ui.cube.CubeNetDrawer

/**
 * Full 6-face cube net preview during scanning.
 * Highlights the face currently being captured.
 */
@Composable
fun Cube3DPreview(
  modifier: Modifier = Modifier,
  scans: Map<CubeFace, FaceScan>,
  highlightFace: CubeFace? = null,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black.copy(alpha = 0.7f))
      .padding(8.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      with(CubeNetDrawer) {
        drawCubeNet(scans = scans, highlightFace = highlightFace)
      }
    }
  }
}
