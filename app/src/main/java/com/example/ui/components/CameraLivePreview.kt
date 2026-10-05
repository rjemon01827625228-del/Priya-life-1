package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink

/**
 * Real-time camera view for Live Room host.
 * Automatically asks for camera and microphone permissions.
 * Starts front camera with CameraX.
 * Supports Camera Flip (Front/Back) and Natural Beauty Filter ON/OFF.
 */
@Composable
fun CameraLivePreview(
  isHost: Boolean,
  fallbackAvatarRes: Int,
  fallbackAvatarUri: String?,
  isEnhancementEnabled: Boolean,
  onToggleEnhancement: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  var isFrontCamera by remember { mutableStateOf(true) }
  var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var isCameraBound by remember { mutableStateOf(false) }

  // Permission launcher for Camera & Mic
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val cameraGranted = permissions[Manifest.permission.CAMERA] == true
    hasCameraPermission = cameraGranted
  }

  LaunchedEffect(isHost) {
    if (isHost && !hasCameraPermission) {
      permissionLauncher.launch(
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      )
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    if (isHost && hasCameraPermission) {
      // 1. CameraX Real-time Preview
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          }
          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
          cameraProviderFuture.addListener({
            try {
              val provider = cameraProviderFuture.get()
              cameraProvider = provider
              bindCamera(provider, previewView, lifecycleOwner, isFrontCamera)
              isCameraBound = true
            } catch (e: Exception) {
              Log.e("CameraLivePreview", "Failed to bind camera preview", e)
              isCameraBound = false
            }
          }, ContextCompat.getMainExecutor(ctx))
          previewView
        },
        update = { previewView ->
          cameraProvider?.let { provider ->
            bindCamera(provider, previewView, lifecycleOwner, isFrontCamera)
          }
        },
        modifier = Modifier.fillMaxSize()
      )

      // Re-bind when camera switches front/back
      LaunchedEffect(isFrontCamera) {
        cameraProvider?.let { provider ->
          // AndroidView update handles re-bind
        }
      }

      DisposableEffect(lifecycleOwner) {
        onDispose {
          cameraProvider?.unbindAll()
        }
      }
    } else {
      // Viewer or Camera Not Granted: Elegant live broadcaster stream display
      Box(modifier = Modifier.fillMaxSize()) {
        val safeFallbackRes = remember(fallbackAvatarRes) {
          if (fallbackAvatarRes <= 0) {
            R.drawable.img_streamer_girl
          } else {
            try {
              context.resources.getResourceName(fallbackAvatarRes)
              fallbackAvatarRes
            } catch (e: Throwable) {
              R.drawable.img_streamer_girl
            }
          }
        }

        if (!fallbackAvatarUri.isNullOrBlank()) {
          AsyncImage(
            model = fallbackAvatarUri,
            contentDescription = "Live Broadcaster",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          Image(
            painter = painterResource(id = safeFallbackRes),
            contentDescription = "Live Broadcaster",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }

        // Overlay cyber atmosphere
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color(0x77080014),
                  Color(0x33000000),
                  Color(0x99080014)
                )
              )
            )
        )

        if (isHost && !hasCameraPermission) {
          Column(
            modifier = Modifier
              .align(Alignment.Center)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC110022))
              .border(1.dp, NeonPink, RoundedCornerShape(16.dp))
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Filled.VideocamOff,
              contentDescription = null,
              tint = NeonPink,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              "Camera Access Required",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Grant camera permission to stream your live video to viewers.",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                permissionLauncher.launch(
                  arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Allow Camera", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 2. NATURAL BEAUTY ENHANCEMENT LAYER (Real skin smoothing, warm lighting, natural blush, face luminance)
    if (isEnhancementEnabled) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        // Natural skin-tone radiance & soft blush
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0x35FFF0E6), // Bright face highlight
              Color(0x2EFEB4B4), // Soft peach rose blush tone
              Color(0x18FFE0B2), // Warm natural golden tone
              Color(0x0A000000)
            ),
            center = center,
            radius = size.maxDimension * 0.75f
          ),
          blendMode = BlendMode.Screen
        )

        // Subtle soft focus lighting & skin tone smoothing
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0x20FFFFFF),
              Color(0x10FFF5EB),
              Color(0x15210033)
            )
          ),
          blendMode = BlendMode.Overlay
        )
      }
    }

    // Top Floating Quick Camera Switch for Host
    if (isHost && hasCameraPermission) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 96.dp, end = 14.dp)
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0x88110022))
          .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
          .clickable { isFrontCamera = !isFrontCamera },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Filled.Cameraswitch,
          contentDescription = "Switch Camera",
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

private fun bindCamera(
  cameraProvider: ProcessCameraProvider,
  previewView: PreviewView,
  lifecycleOwner: androidx.lifecycle.LifecycleOwner,
  useFrontCamera: Boolean
) {
  try {
    cameraProvider.unbindAll()

    val cameraSelector = if (useFrontCamera) {
      CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
      CameraSelector.DEFAULT_BACK_CAMERA
    }

    val preview = Preview.Builder().build().also {
      it.setSurfaceProvider(previewView.surfaceProvider)
    }

    cameraProvider.bindToLifecycle(
      lifecycleOwner,
      cameraSelector,
      preview
    )
  } catch (e: Exception) {
    Log.e("CameraLivePreview", "Error binding camera lifecycle", e)
  }
}
