package com.cubelens.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cubelens.R
import com.cubelens.model.CubeFace
import com.cubelens.ui.util.CubeColorUi
import com.cubelens.viewmodel.CaptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
  viewModel: CaptureViewModel,
  onBack: () -> Unit,
  onSolve: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.review_title)) },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.padding(innerPadding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          stringResource(R.string.review_hint),
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      items(state.faceOrder.size) { idx ->
        val face = state.faceOrder[idx]
        val scan = state.scans[face]
        FaceCard(
          face = face,
          colors = scan?.colors,
          confidences = scan?.confidences,
          onTapSticker = { i -> viewModel.cycleStickerColor(face, i) },
        )
      }

      item {
        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Button(onClick = onBack) { Text(stringResource(R.string.review_back)) }
          Button(onClick = onSolve, enabled = state.isComplete) { Text(stringResource(R.string.review_solve)) }
        }
      }
    }
  }
}

@Composable
private fun FaceCard(
  face: CubeFace,
  colors: List<com.cubelens.model.CubeColor>?,
  confidences: List<Float>?,
  onTapSticker: (Int) -> Unit,
) {
  OutlinedCard(
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(stringResource(R.string.review_face, face.label), style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(10.dp))
      FaceGrid(colors = colors, confidences = confidences, onTapSticker = onTapSticker)
    }
  }
}

@Composable
private fun FaceGrid(
  colors: List<com.cubelens.model.CubeColor>?,
  confidences: List<Float>?,
  onTapSticker: (Int) -> Unit,
) {
  val cellSize = 44.dp
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    for (r in 0..2) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (c in 0..2) {
          val idx = r * 3 + c
          val color = colors?.getOrNull(idx) ?: com.cubelens.model.CubeColor.UNKNOWN
          val conf = confidences?.getOrNull(idx) ?: 0f
          val low = conf < 0.55f
          Box(
            modifier = Modifier
              .size(cellSize)
              .clickable { onTapSticker(idx) },
            contentAlignment = Alignment.Center,
          ) {
            OutlinedCard(
              border = BorderStroke(2.dp, if (low) MaterialTheme.colorScheme.error else Color.Transparent),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.size(cellSize),
            ) {
              Box(
                modifier = Modifier
                  .size(cellSize)
                  .padding(3.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(cellSize)
                    .background(CubeColorUi.swatch(color), RoundedCornerShape(6.dp)),
                )
              }
            }
          }
        }
      }
    }
  }
}

