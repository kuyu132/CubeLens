package com.cubelens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.cubelens.ui.CubeLensApp
import com.cubelens.ui.theme.CubeLensTheme
import com.cubelens.viewmodel.CaptureViewModel
import com.cubelens.viewmodel.SolveViewModel

class MainActivity : ComponentActivity() {
  private val captureViewModel by viewModels<CaptureViewModel>()
  private val solveViewModel by viewModels<SolveViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      CubeLensTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          CubeLensApp(
            captureViewModel = captureViewModel,
            solveViewModel = solveViewModel,
            context = this,
          )
        }
      }
    }
  }
}

