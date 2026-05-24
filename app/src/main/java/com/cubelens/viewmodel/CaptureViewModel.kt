package com.cubelens.viewmodel

import android.app.Application
import android.graphics.Bitmap
import com.cubelens.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cubelens.camera.ColorCalibration
import com.cubelens.camera.ColorDetector
import com.cubelens.data.PreferencesManager
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import com.cubelens.util.FaceletsLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureUiState(
  val faceOrder: List<CubeFace> = listOf(CubeFace.U, CubeFace.R, CubeFace.F, CubeFace.D, CubeFace.L, CubeFace.B),
  val currentFaceIndex: Int = 0,
  val scans: Map<CubeFace, FaceScan> = emptyMap(),
  val scanHistory: List<CubeFace> = emptyList(),  // ordered list of scanned faces (for undo)
  val isProcessing: Boolean = false,
  val message: String? = null,
) {
  val currentFace: CubeFace get() = faceOrder[currentFaceIndex.coerceIn(faceOrder.indices)]
  val isComplete: Boolean get() = scans.size == faceOrder.size
  val canUndo: Boolean get() = scanHistory.isNotEmpty()
}

class CaptureViewModel(app: Application) : AndroidViewModel(app) {
  private val prefs = PreferencesManager(app)
  private val colorDetector = ColorDetector()

  val colorCalibration: StateFlow<ColorCalibration> = prefs.colorCalibration.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ColorCalibration.DEFAULT,
  )

  private val _uiState = MutableStateFlow(CaptureUiState())
  val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

  fun reset() {
    _uiState.value = CaptureUiState()
  }

  /** Load all six faces from a 54-character facelets string (URFDLB centers). */
  fun applyFacelets(facelets: String): Boolean {
    val scans = FaceletsLoader.scansFromFacelets(facelets) ?: return false
    _uiState.value = CaptureUiState(
      scans = scans,
      scanHistory = scans.keys.toList(),
      currentFaceIndex = CaptureUiState().faceOrder.lastIndex,
      message = null,
    )
    return true
  }

  fun setCapturedFaceBitmap(face: CubeFace, bitmap: Bitmap, imagePath: String?) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.update { it.copy(isProcessing = true, message = null) }
      val calibration = colorCalibration.value
      val scan = try {
        colorDetector.detectFace(
          face = face,
          bitmap = bitmap,
          imagePath = imagePath,
          calibration = calibration,
        )
      } catch (t: Throwable) {
        null
      }
      _uiState.update { state ->
        val nextScans = if (scan != null) state.scans + (face to scan) else state.scans
        val nextHistory = if (scan != null && face !in state.scans) state.scanHistory + face else state.scanHistory
        state.copy(
          scans = nextScans,
          scanHistory = nextHistory,
          isProcessing = false,
          message = if (scan == null) {
            getApplication<Application>().getString(R.string.capture_detect_failed)
          } else {
            null
          },
        )
      }
      if (scan != null) {
        goNext()
      }
    }
  }

  /**
   * Undo the last scan: removes the most recently scanned face from history
   * and from the scans map, then navigates back to that face.
   */
  fun undo() {
    _uiState.update { state ->
      if (state.scanHistory.isEmpty()) return@update state
      val lastFace = state.scanHistory.last()
      val nextHistory = state.scanHistory.dropLast(1)
      val nextScans = state.scans - lastFace
      val nextIndex = state.faceOrder.indexOf(lastFace).coerceAtLeast(0)
      state.copy(scans = nextScans, scanHistory = nextHistory, currentFaceIndex = nextIndex)
    }
  }

  fun goNext() {
    _uiState.update { state ->
      val nextIndex = (state.currentFaceIndex + 1).coerceAtMost(state.faceOrder.lastIndex)
      state.copy(currentFaceIndex = nextIndex)
    }
  }

  fun goPrev() {
    _uiState.update { state ->
      val nextIndex = (state.currentFaceIndex - 1).coerceAtLeast(0)
      state.copy(currentFaceIndex = nextIndex)
    }
  }

  fun goToFace(index: Int) {
    _uiState.update { state ->
      if (index in state.faceOrder.indices) {
        state.copy(currentFaceIndex = index)
      } else state
    }
  }

  fun updateStickerColor(face: CubeFace, index: Int, color: CubeColor) {
    _uiState.update { state ->
      val scan = state.scans[face] ?: return@update state
      if (index !in 0..8) return@update state
      val nextColors = scan.colors.toMutableList().also { it[index] = color }
      val nextConf = scan.confidences.toMutableList().also { it[index] = 1.0f }
      state.copy(scans = state.scans + (face to scan.copy(colors = nextColors, confidences = nextConf)))
    }
  }

  fun cycleStickerColor(face: CubeFace, index: Int) {
    val order = listOf(
      CubeColor.WHITE,
      CubeColor.YELLOW,
      CubeColor.GREEN,
      CubeColor.BLUE,
      CubeColor.RED,
      CubeColor.ORANGE,
    )
    val scan = _uiState.value.scans[face] ?: return
    val current = scan.colors.getOrNull(index) ?: return
    val next = order[(order.indexOf(current).takeIf { it >= 0 } ?: 0).let { (it + 1) % order.size }]
    updateStickerColor(face, index, next)
  }
}

