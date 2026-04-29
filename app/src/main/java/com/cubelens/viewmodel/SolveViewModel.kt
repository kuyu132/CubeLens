package com.cubelens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubelens.model.SolveResult
import com.cubelens.solver.KociembaSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SolveUiState(
  val isSolving: Boolean = false,
  val result: SolveResult? = null,
  val currentStep: Int = 0,
  /** 0f..1f animation progress for current step's move. 0 = not started, 1 = fully applied. */
  val animProgress: Float = 1f,
  val isPlaying: Boolean = false,
) {
  val totalSteps: Int get() = result?.moves?.size ?: 0
  val moves: List<com.cubelens.solver.Move> get() = result?.moves ?: emptyList()
}

class SolveViewModel : ViewModel() {
  private val solver = KociembaSolver()

  private val _uiState = MutableStateFlow(SolveUiState())
  val uiState: StateFlow<SolveUiState> = _uiState.asStateFlow()

  private var playJob: Job? = null

  fun reset() {
    playJob?.cancel()
    _uiState.value = SolveUiState()
  }

  fun solve(facelets: String) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.update { it.copy(isSolving = true, result = null, currentStep = 0, animProgress = 1f, isPlaying = false) }
      val result = withTimeoutOrNull(5000L) {
        try {
          val moves = solver.solve(facelets)
          SolveResult(moves = moves)
        } catch (t: Throwable) {
          SolveResult(moves = emptyList(), errorMessage = t.message ?: "Solve failed")
        }
      } ?: SolveResult(moves = emptyList(), errorMessage = "Solve timed out")

      _uiState.update { it.copy(isSolving = false, result = result, currentStep = 0, animProgress = 1f) }
    }
  }

  fun nextStep() {
    pause()
    _uiState.update { state ->
      val total = state.totalSteps
      if (total <= 0) state else state.copy(currentStep = (state.currentStep + 1).coerceAtMost(total - 1), animProgress = 1f)
    }
  }

  fun prevStep() {
    pause()
    _uiState.update { state ->
      val total = state.totalSteps
      if (total <= 0) state else state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(0), animProgress = 1f)
    }
  }

  fun togglePlay() {
    if (_uiState.value.isPlaying) pause() else play()
  }

  private fun play() {
    if (_uiState.value.isPlaying) return
    _uiState.update { it.copy(isPlaying = true) }
    playJob = viewModelScope.launch {
      while (_uiState.value.currentStep < _uiState.value.totalSteps) {
        // Animate current step
        animateStep()
        // Move to next step
        _uiState.update { state ->
          val next = state.currentStep + 1
          if (next >= state.totalSteps) {
            state.copy(currentStep = state.totalSteps - 1, animProgress = 1f, isPlaying = false)
          } else {
            state.copy(currentStep = next, animProgress = 0f)
          }
        }
        if (!_uiState.value.isPlaying) break
        delay(300) // Pause between moves
      }
      _uiState.update { it.copy(isPlaying = false) }
    }
  }

  private suspend fun animateStep() {
    val durationMs = 450L
    val startMs = System.currentTimeMillis()
    while (true) {
      val elapsed = System.currentTimeMillis() - startMs
      val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
      // Ease-in-out cubic
      val eased = if (progress < 0.5f) {
        4f * progress * progress * progress
      } else {
        1f - (-2f * progress + 2f).let { it * it * it } / 2f
      }
      _uiState.update { it.copy(animProgress = eased) }
      if (progress >= 1f) break
      delay(16)
    }
  }

  private fun pause() {
    playJob?.cancel()
    playJob = null
    _uiState.update { it.copy(isPlaying = false, animProgress = 1f) }
  }

  override fun onCleared() {
    super.onCleared()
    playJob?.cancel()
  }
}
