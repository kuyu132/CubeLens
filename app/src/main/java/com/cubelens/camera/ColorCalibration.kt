package com.cubelens.camera

data class ColorCalibration(
  /** Global hue shift in degrees applied before classification. */
  val hueOffsetDeg: Float = 0f,
  /** Max saturation (0–1) to still classify as white. Lower = stricter white detection. */
  val whiteSatMax: Float = 0.20f,
) {
  companion object {
    val DEFAULT = ColorCalibration()
  }
}
