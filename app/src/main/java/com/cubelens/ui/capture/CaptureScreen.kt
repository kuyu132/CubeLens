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
import androidx.core.content.ContextCompat
import com.cubelens.R
import com.cubelens.ui.util.BitmapUtils
import com.cubelens.viewmodel.CaptureViewModel
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
  viewModel: CaptureViewModel,
  onReview: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

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
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = { viewModel.goPrev() },
          enabled = state.currentFaceIndex > 0 && !state.isProcessing,
        ) {
          Text(stringResource(R.string.capture_prev))
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
          takePhoto(context, imageCapture, executor, onPhotoSaved)
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
        onPhotoSaved(file.absolutePath)
      }
    },
  )
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
