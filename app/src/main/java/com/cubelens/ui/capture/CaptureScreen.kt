package com.cubelens.ui.capture

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.cubelens.R
import com.cubelens.camera.ColorCalibration
import com.cubelens.camera.ColorDetector
import com.cubelens.camera.centerGridCrop
import com.cubelens.camera.toRgbaBitmap
import com.cubelens.model.CubeColor
import com.cubelens.model.CubeFace
import com.cubelens.ui.util.BitmapUtils
import com.cubelens.ui.util.CubeColorUi
import com.cubelens.viewmodel.CaptureViewModel
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
  viewModel: CaptureViewModel,
  onReview: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onManualInput: () -> Unit = {},
  modifier: Modifier = Modifier,
  lensFacing: Int = CameraSelector.LENS_FACING_BACK,
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val colorCalibration by viewModel.colorCalibration.collectAsStateWithLifecycle()

  var hasCameraPermission by remember { mutableStateOf(false) }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted -> hasCameraPermission = granted },
  )

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    val bitmap = BitmapUtils.decodeFromUri(context, uri) ?: run {
      Toast.makeText(context, context.getString(R.string.capture_gallery_failed), Toast.LENGTH_SHORT).show()
      return@rememberLauncherForActivityResult
    }
    viewModel.setCapturedFaceBitmap(state.currentFace, bitmap, null)
  }

  LaunchedEffect(Unit) {
    permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.capture_title, state.currentFace.label)) },
        actions = {
          TextButton(onClick = onNavigateToSettings) {
            Text(stringResource(R.string.menu_settings))
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .then(modifier)
        .padding(innerPadding),
    ) {
      if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.capture_camera_required))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
              Text(stringResource(R.string.capture_grant_permission))
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
              .height(200.dp)
              .padding(horizontal = 12.dp, vertical = 6.dp),
            scans = state.scans,
            highlightFace = state.currentFace,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          enabled = !state.isProcessing,
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.capture_gallery))
        }
        OutlinedButton(
          onClick = onManualInput,
          enabled = !state.isProcessing,
          modifier = Modifier.weight(1f),
        ) {
          Text(stringResource(R.string.capture_manual_input))
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        CameraPreviewWithCapture(
          faceLabel = state.currentFace.label,
          enabled = !state.isProcessing,
          lensFacing = lensFacing,
          calibration = colorCalibration,
          onPhotoSaved = { path ->
            val bmp = BitmapUtils.decodeUpright(path) ?: return@CameraPreviewWithCapture
            viewModel.setCapturedFaceBitmap(state.currentFace, bmp, path)
          },
          onPhotoError = {
            Toast.makeText(context, context.getString(R.string.capture_photo_failed), Toast.LENGTH_SHORT).show()
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
            Text(stringResource(R.string.capture_prev))
          }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = stringResource(R.string.capture_progress, state.scans.size),
            style = MaterialTheme.typography.bodyMedium,
          )
          state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        Button(
          onClick = onReview,
          enabled = state.isComplete && !state.isProcessing,
        ) {
          Text(stringResource(R.string.capture_review))
        }
      }
    }
  }
}

@Composable
private fun CameraPreviewWithCapture(
  faceLabel: String,
  enabled: Boolean,
  lensFacing: Int,
  calibration: ColorCalibration,
  onPhotoSaved: (String) -> Unit,
  onPhotoError: () -> Unit = {},
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

  val imageAnalysis = remember {
    ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
      .build()
  }

  val colorDetector = remember { ColorDetector() }
  val analyzeExecutor = remember { Executors.newSingleThreadExecutor() }
  var previewColors by remember { mutableStateOf<List<CubeColor>?>(null) }

  DisposableEffect(Unit) {
    onDispose { analyzeExecutor.shutdown() }
  }

  LaunchedEffect(lensFacing) {
    val provider = ProcessCameraProvider.getInstance(context).get()
    provider.unbindAll()
    val preview = Preview.Builder().build().apply {
      setSurfaceProvider(previewView.surfaceProvider)
    }
    provider.bindToLifecycle(
      lifecycleOwner,
      CameraSelector.Builder().requireLensFacing(lensFacing).build(),
      preview,
      imageCapture,
      imageAnalysis,
    )
  }

  LaunchedEffect(enabled, lensFacing, calibration) {
    if (!enabled) {
      imageAnalysis.clearAnalyzer()
      previewColors = null
      return@LaunchedEffect
    }
    var lastAnalyzeAt = 0L
    imageAnalysis.setAnalyzer(analyzeExecutor) { proxy ->
      val now = System.currentTimeMillis()
      if (now - lastAnalyzeAt < 350) {
        proxy.close()
        return@setAnalyzer
      }
      lastAnalyzeAt = now
      try {
        val frame = proxy.toRgbaBitmap() ?: return@setAnalyzer
        val cropped = centerGridCrop(frame)
        val colors = colorDetector.detectFace(CubeFace.U, cropped, null, calibration).colors
        ContextCompat.getMainExecutor(context).execute {
          previewColors = colors
        }
      } catch (_: Throwable) {
        // Ignore preview analysis errors
      } finally {
        proxy.close()
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    GridOverlay(faceLabel = faceLabel, previewColors = previewColors)

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
            takePhoto(context, imageCapture, executor, previewW, previewH, onPhotoSaved, onPhotoError)
          } else {
            takePhoto(context, imageCapture, executor, 0, 0, onPhotoSaved, onPhotoError)
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
  onPhotoError: () -> Unit,
) {
  val outDir = File(context.cacheDir, "cubelens").apply { mkdirs() }
  val file = File(outDir, "face_${System.currentTimeMillis()}.jpg")
  val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
  imageCapture.takePicture(
    outputOptions,
    executor,
    object : ImageCapture.OnImageSavedCallback {
      override fun onError(exception: ImageCaptureException) {
        onPhotoError()
      }
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
private fun GridOverlay(faceLabel: String, previewColors: List<CubeColor>? = null) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val side = minOf(w, h) * 0.74f
    val left = (w - side) / 2f
    val top = (h - side) / 2f
    val line = 2.dp.toPx()
    val alpha = 0.65f

    drawRect(
      color = Color.White.copy(alpha = alpha),
      topLeft = Offset(left, top),
      size = androidx.compose.ui.geometry.Size(side, side),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = line),
    )
    for (i in 1..2) {
      val x = left + side * i / 3f
      val y = top + side * i / 3f
      drawLine(Color.White.copy(alpha = alpha), Offset(x, top), Offset(x, top + side), strokeWidth = line)
      drawLine(Color.White.copy(alpha = alpha), Offset(left, y), Offset(left + side, y), strokeWidth = line)
    }

    previewColors?.takeIf { it.size == 9 }?.let { colors ->
      val dotR = side / 12f
      for (row in 0..2) {
        for (col in 0..2) {
          val cx = left + side * (col + 0.5f) / 3f
          val cy = top + side * (row + 0.5f) / 3f
          drawCircle(
            color = CubeColorUi.swatch(colors[row * 3 + col]).copy(alpha = 0.88f),
            radius = dotR,
            center = Offset(cx, cy),
          )
        }
      }
    }
  }
}
