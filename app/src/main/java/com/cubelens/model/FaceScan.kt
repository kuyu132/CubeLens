package com.cubelens.model

data class FaceScan(
  val face: CubeFace,
  val colors: List<CubeColor>,
  val confidences: List<Float>,
  val imagePath: String? = null,
) {
  init {
    require(colors.size == 9) { "FaceScan.colors must have size=9" }
    require(confidences.size == 9) { "FaceScan.confidences must have size=9" }
  }
}

