package com.cubelens.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object BitmapUtils {
  fun decodeUpright(path: String, maxSize: Int = 1400): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

    val scale = max(opts.outWidth, opts.outHeight).toFloat() / maxSize.toFloat()
    val sampleSize = scale.toInt().coerceAtLeast(1)
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeFile(path, decodeOpts) ?: return null

    val exif = runCatching { ExifInterface(path) }.getOrNull()
    val rotation = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
      ExifInterface.ORIENTATION_ROTATE_90 -> 90
      ExifInterface.ORIENTATION_ROTATE_180 -> 180
      ExifInterface.ORIENTATION_ROTATE_270 -> 270
      else -> 0
    }
    if (rotation == 0) return bitmap
    val m = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
  }
}

