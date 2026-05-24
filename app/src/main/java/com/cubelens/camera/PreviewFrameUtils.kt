package com.cubelens.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.min

fun ImageProxy.toRgbaBitmap(): Bitmap? {
  if (format != ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) return null
  val buffer = planes[0].buffer.duplicate()
  buffer.rewind()
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  bitmap.copyPixelsFromBuffer(buffer)
  return bitmap
}

fun centerGridCrop(src: Bitmap, fraction: Float = 0.74f): Bitmap {
  val side = (min(src.width, src.height) * fraction).toInt().coerceAtLeast(1)
  val x = (src.width - side) / 2
  val y = (src.height - side) / 2
  return Bitmap.createBitmap(src, x.coerceAtLeast(0), y.coerceAtLeast(0), side, side)
}
