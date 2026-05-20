package com.cubelens.ui.capture

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.FileOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cubelens.ui.util.BitmapUtils
import com.cubelens.viewmodel.CaptureViewModel
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
  viewModel: CaptureViewModel,
  onReview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var hasCameraPermission by remember { mutableStateOf(false) }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted -> hasCameraPermission = granted },
  )

  LaunchedEffect(Unit) {
    permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Capture • Face ${state.currentFace.label}") },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera permission is required.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
              Text("Grant permission")
            }
          }
        }
        return@Column
      }

      // Thumbnail row for captured faces
      if (state.scans.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          state.faceOrder.forEach { face ->
            val scan = state.scans[face]
            val isCurrent = face == state.currentFace
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (isCurrent) Color.White.copy(alpha = 0.3f)
                  else Color.DarkGray.copy(alpha = 0.5f)
                )
                .clickable(enabled = scan != null || isCurrent) {
                  val idx = state.faceOrder.indexOf(face)
                  if (idx >= 0) viewModel.goToFace(idx)
                },
              contentAlignment = Alignment.Center,
            ) {
              if (scan?.imagePath != null) {
                val bmp = remember(scan.imagePath) {
                  BitmapFactory.decodeFile(scan.imagePath)?.let {
                    val sz = 96
                    Bitmap.createScaledBitmap(it, sz, sz, true)
                  }
                }
                if (bmp != null) {
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = face.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                  )
                }
              } else {
                Text(
                  text = face.label,
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
              // Current face indicator border
              if (isCurrent) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                      drawContent()
                      drawRect(
                        color = Color.White,
                        topLeft = Offset.Zero,
                        size = size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                      )
                    },
                )
              }
            }
          }
        }

        // 3D cube preview — shows U, F, R faces with scanned/unspecified colors
        if (state.scans.isNotEmpty()) {
          Cube3DPreview(
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp)
              .padding(horizontal = 12.dp, vertical = 6.dp),
            scans = state.scans,
          )
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        CameraPreviewWithCapture(
          faceLabel = state.currentFace.label,
          enabled = !state.isProcessing,
          onPhotoSaved = { path ->
            val bmp = BitmapUtils.decodeUpright(path) ?: return@CameraPreviewWithCapture
            viewModel.setCapturedFaceBitmap(state.currentFace, bmp, path)
          },
        )

        if (state.isProcessing) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Undo + Prev on the left
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { viewModel.undo() },
            enabled = state.canUndo && !state.isProcessing,
          ) {
            Text("↩")
          }

          Button(
            onClick = { viewModel.goPrev() },
            enabled = state.currentFaceIndex > 0 && !state.isProcessing,
          ) {
            Text("Prev")
          }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${state.scans.size}/6 captured",
            style = MaterialTheme.typography.bodyMedium,
          )
          state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        Button(
          onClick = onReview,
          enabled = state.isComplete && !state.isProcessing,
        ) {
          Text("Review")
        }
      }
    }
  }
}

@Composable
private fun CameraPreviewWithCapture(
  faceLabel: String,
  enabled: Boolean,
  onPhotoSaved: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }

  val imageCapture = remember {
    ImageCapture.Builder()
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
      .build()
  }

  LaunchedEffect(Unit) {
    val provider = ProcessCameraProvider.getInstance(context).get()
    provider.unbindAll()
    val preview = Preview.Builder().build().apply {
      setSurfaceProvider(previewView.surfaceProvider)
    }
    provider.bindToLifecycle(
      lifecycleOwner,
      CameraSelector.DEFAULT_BACK_CAMERA,
      preview,
      imageCapture,
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    GridOverlay(faceLabel = faceLabel)

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(20.dp),
    ) {
      CaptureButton(
        enabled = enabled,
        onClick = {
          // Capture grid region from the actual image based on preview dimensions.
          // GridOverlay draws a square of side = minOf(w,h) * 0.74 centered in the preview.
          // We crop that exact region from the saved image to keep only the cube.
          val previewW = previewView.width
          val previewH = previewView.height
          if (previewW > 0 && previewH > 0) {
            takePhoto(context, imageCapture, executor, previewW, previewH, onPhotoSaved)
          } else {
            takePhoto(context, imageCapture, executor, 0, 0, onPhotoSaved)
          }
        },
      )
    }
  }
}

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit) {
  val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  Box(
    modifier = Modifier
      .size(76.dp)
      .background(
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
        shape = CircleShape,
      )
      .then(
        if (enabled) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
          )
        } else Modifier
      ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(62.dp)
        .background(
          color = if (enabled) Color.Red else Color.Gray,
          shape = CircleShape,
        ),
    )
  }
}

private fun takePhoto(
  context: Context,
  imageCapture: ImageCapture,
  executor: Executor,
  previewW: Int,
  previewH: Int,
  onPhotoSaved: (String) -> Unit,
) {
  val outDir = File(context.cacheDir, "cubelens").apply { mkdirs() }
  val file = File(outDir, "face_${System.currentTimeMillis()}.jpg")
  val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
  imageCapture.takePicture(
    outputOptions,
    executor,
    object : ImageCapture.OnImageSavedCallback {
      override fun onError(exception: ImageCaptureException) = Unit
      override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        val croppedFile = cropGridRegion(file, previewW, previewH)
        onPhotoSaved(croppedFile.absolutePath)
      }
    },
  )
}

/**
 * Crops the saved image to the grid region that was displayed in the camera preview.
 *
 * GridOverlay draws a square of side = minOf(w, h) * 0.74 centered in the preview.
 * This function maps those preview-space coordinates to image-space coordinates
 * (accounting for aspect-ratio letterboxing and EXIF rotation), then crops and saves
 * the cropped square, overwriting the original temp file.
 */
private fun cropGridRegion(file: File, previewW: Int, previewH: Int): File {
  if (previewW <= 0 || previewH <= 0) return file

  val srcBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file

  val imgW0 = srcBitmap.width
  val imgH0 = srcBitmap.height
  if (imgW0 <= 0 || imgH0 <= 0) return file

  // Read EXIF rotation
  val rotation = try {
    val exif = ExifInterface(file.absolutePath)
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
      ExifInterface.ORIENTATION_ROTATE_90  -> 90
      ExifInterface.ORIENTATION_ROTATE_180 -> 180
      ExifInterface.ORIENTATION_ROTATE_270 -> 270
      else -> 0
    }
  } catch (_: Exception) { 0 }

  // Apply rotation to get effective image dimensions
  val imgW = if (rotation == 90 || rotation == 270) imgH0 else imgW0
  val imgH = if (rotation == 90 || rotation == 270) imgW0 else imgH0

  if (imgW <= 0 || imgH <= 0) return file

  // --- Grid bounds in preview coordinates ---
  val gridSide    = minOf(previewW.toFloat(), previewH.toFloat()) * 0.74f
  val gridLeft    = (previewW - gridSide) / 2f
  val gridTop     = (previewH - gridSide) / 2f
  val gridRight   = gridLeft + gridSide
  val gridBottom  = gridTop  + gridSide

  // --- Map preview coordinates to image coordinates ---
  // CameraX back-camera portrait: image is landscape (imgW > imgH) letterboxed in portrait preview
  val previewRatio = previewW.toFloat() / previewH.toFloat()
  val imgRatio     = imgW.toFloat()    / imgH.toFloat()

  val leftImgF: Float
  val topImgF: Float
  val rightImgF: Float
  val bottomImgF: Float

  if (imgRatio > previewRatio) {
    // Image is wider than preview: letterboxed on top/bottom
    val scaleInv     = previewH.toFloat() / imgH.toFloat()
    val scaledImgW   = imgW.toFloat() * scaleInv
    val letterboxPad = (imgW - scaledImgW) / 2f

    if (rotation == 90 || rotation == 270) {
      leftImgF   = letterboxPad + gridTop    * scaleInv
      topImgF   = imgW        - gridRight   * scaleInv
      rightImgF  = letterboxPad + gridBottom * scaleInv
      bottomImgF = imgW        - gridLeft   * scaleInv
    } else {
      leftImgF   = letterboxPad + gridLeft  * scaleInv
      topImgF   = letterboxPad + gridTop   * scaleInv
      rightImgF  = letterboxPad + gridRight * scaleInv
      bottomImgF = letterboxPad + gridBottom * scaleInv
    }
  } else {
    // Image is taller than preview: letterboxed on left/right
    val scaleInv     = previewW.toFloat() / imgW.toFloat()
    val scaledImgH   = imgH.toFloat() * scaleInv
    val letterboxPad = (imgH - scaledImgH) / 2f

    if (rotation == 90 || rotation == 270) {
      leftImgF   = letterboxPad + gridTop    * scaleInv
      topImgF   = imgW        - gridRight   * scaleInv
      rightImgF  = letterboxPad + gridBottom * scaleInv
      bottomImgF = imgW        - gridLeft   * scaleInv
    } else {
      leftImgF   = letterboxPad + gridLeft  * scaleInv
      topImgF   = letterboxPad + gridTop   * scaleInv
      rightImgF  = letterboxPad + gridRight * scaleInv
      bottomImgF = letterboxPad + gridBottom * scaleInv
    }
  }

  val l = leftImgF.toInt().coerceIn(0, imgW - 1)
  val t = topImgF.toInt().coerceIn(0, imgH - 1)
  val r = rightImgF.toInt().coerceIn(l + 1, imgW)
  val b = bottomImgF.toInt().coerceIn(t + 1, imgH)
  val cropW = r - l
  val cropH = b - t

  if (cropW < 20 || cropH < 20) return file

  // Crop from the pre-EXIF-rotated bitmap (bitmap may still need EXIF rotation applied)
  val cropped = Bitmap.createBitmap(srcBitmap, l, t, cropW, cropH)
  srcBitmap.recycle()

  if (cropped == null) return file

  // Apply EXIF rotation and overwrite temp file
  val rotated = if (rotation != 0) {
    val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
    Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
      .also { if (it != cropped) cropped.recycle() }
  } else cropped

  FileOutputStream(file).use { fos ->
    rotated.compress(Bitmap.CompressFormat.JPEG, 95, fos)
  }
  rotated.recycle()
  return file
}

@Composable
private fun GridOverlay(faceLabel: String) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val side = minOf(w, h) * 0.74f
    val left = (w - side) / 2f
    val top = (h - side) / 2f
    val line = 2.dp.toPx()
    val alpha = 0.65f

    // Border
    drawRect(
      color = Color.White.copy(alpha = alpha),
      topLeft = Offset(left, top),
      size = androidx.compose.ui.geometry.Size(side, side),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = line),
    )
    // Grid lines
    for (i in 1..2) {
      val x = left + side * i / 3f
      val y = top + side * i / 3f
      drawLine(Color.White.copy(alpha = alpha), Offset(x, top), Offset(x, top + side), strokeWidth = line)
      drawLine(Color.White.copy(alpha = alpha), Offset(left, y), Offset(left + side, y), strokeWidth = line)
    }
  }
}
