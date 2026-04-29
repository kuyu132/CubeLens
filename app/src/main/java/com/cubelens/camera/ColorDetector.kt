package com.cubelens.camera

import android.graphics.Bitmap
import android.graphics.Color
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.model.FaceScan
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ColorDetector {
  fun detectFace(face: CubeFace, bitmap: Bitmap, imagePath: String?): FaceScan {
    val square = centerCropSquare(bitmap)
    val (colors, confidences) = sample3x3(square)
    return FaceScan(face = face, colors = colors, confidences = confidences, imagePath = imagePath)
  }

  private fun centerCropSquare(src: Bitmap): Bitmap {
    val size = min(src.width, src.height)
    val x = (src.width - size) / 2
    val y = (src.height - size) / 2
    return Bitmap.createBitmap(src, x, y, size, size)
  }

  private fun sample3x3(bitmap: Bitmap): Pair<List<CubeColor>, List<Float>> {
    val w = bitmap.width
    val h = bitmap.height
    val padding = (w * 0.12f).toInt()
    val cell = (w - 2 * padding) / 3f
    val hsv = FloatArray(3)

    val samples = ArrayList<FloatArray>(9)
    val colors = ArrayList<CubeColor>(9)
    val conf = ArrayList<Float>(9)

    for (row in 0..2) {
      for (col in 0..2) {
        val cx = (padding + (col + 0.5f) * cell).toInt().coerceIn(0, w - 1)
        val cy = (padding + (row + 0.5f) * cell).toInt().coerceIn(0, h - 1)
        val rgb = bitmap.getPixel(cx, cy)
        Color.RGBToHSV(Color.red(rgb), Color.green(rgb), Color.blue(rgb), hsv)
        samples.add(hsv.clone())
      }
    }

    val medianV = samples.map { it[2] }.sorted()[4]
    val adaptiveWhiteV = max(0.70f, medianV * 0.92f)

    for (s in samples) {
      val result = classify(
        h = s[0],
        sat = s[1],
        v = s[2],
        adaptiveWhiteV = adaptiveWhiteV,
      )
      colors.add(result.first)
      conf.add(result.second)
    }
    return colors to conf
  }

  private fun classify(h: Float, sat: Float, v: Float, adaptiveWhiteV: Float): Pair<CubeColor, Float> {
    // 1) White (robust under warm/cool lighting)
    if (sat < 0.20f && v > adaptiveWhiteV) {
      val sScore = 1f - (sat / 0.20f).coerceIn(0f, 1f)
      val vScore = ((v - adaptiveWhiteV) / (1f - adaptiveWhiteV)).coerceIn(0f, 1f)
      return CubeColor.WHITE to (0.55f * sScore + 0.45f * vScore)
    }

    // Low saturation or too dark => uncertain
    if (sat < 0.10f || v < 0.15f) {
      return CubeColor.UNKNOWN to 0.1f
    }

    val hue = normalizeHue(h)

    fun inRange(start: Float, end: Float): Boolean = hue >= start && hue <= end

    // Yellow: H 40-70, S > 0.3
    if (inRange(40f, 70f) && sat > 0.30f) {
      return CubeColor.YELLOW to hueConfidence(hue, 40f, 70f, sat, 0.30f)
    }
    // Orange: H 20-40, S > 0.5
    if (inRange(20f, 40f) && sat > 0.50f) {
      return CubeColor.ORANGE to hueConfidence(hue, 20f, 40f, sat, 0.50f)
    }
    // Green: H 70-170
    if (inRange(70f, 170f)) {
      return CubeColor.GREEN to hueConfidence(hue, 70f, 170f, sat, 0.20f)
    }
    // Blue: H 170-270
    if (inRange(170f, 270f)) {
      return CubeColor.BLUE to hueConfidence(hue, 170f, 270f, sat, 0.20f)
    }
    // Red: H 0-20 or 330-360, S > 0.3
    if ((hue <= 20f || hue >= 330f) && sat > 0.30f) {
      // Wrap-around range
      val hueScore = if (hue <= 20f) hueConfidence(hue, 0f, 20f, sat, 0.30f) else hueConfidence(hue, 330f, 360f, sat, 0.30f)
      return CubeColor.RED to hueScore
    }

    return CubeColor.UNKNOWN to 0.25f
  }

  private fun hueConfidence(hue: Float, start: Float, end: Float, sat: Float, satMin: Float): Float {
    val mid = (start + end) / 2f
    val half = (end - start) / 2f
    val hueScore = (1f - (abs(hue - mid) / half).coerceIn(0f, 1f))
    val satScore = ((sat - satMin) / (1f - satMin)).coerceIn(0f, 1f)
    return (0.65f * hueScore + 0.35f * satScore).coerceIn(0f, 1f)
  }

  private fun normalizeHue(h: Float): Float {
    var x = h % 360f
    if (x < 0f) x += 360f
    return x
  }
}

