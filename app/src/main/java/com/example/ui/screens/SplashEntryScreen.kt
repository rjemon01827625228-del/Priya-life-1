package com.example.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sin

/**
 * Premium Full-Screen Entry / Splash Screen.
 *
 * Guaranteed Behaviors:
 * 1. FULL-SCREEN EDGE-TO-EDGE:
 *    Uses a TextureView with CENTER_CROP transformation matrix matching the device
 *    display dimensions seamlessly, eliminating letterboxing, bars, or empty gaps.
 * 2. SINGLE LOGO & SINGLE APP NAME:
 *    When splash_video.mp4 is active, NO duplicate logo or text overlays are rendered.
 *    The animation plays exactly what is inside the original video without duplicating
 *    the app logo or app name.
 * 3. SILENT & SMOOTH PLAYBACK:
 *    Zero-volume muted audio, hardware-accelerated video decoding with TextureView surface,
 *    smooth fade out transition into the application.
 */
@Composable
fun SplashEntryScreen(
  onAnimationFinished: () -> Unit
) {
  val context = LocalContext.current
  val screenAlpha = remember { Animatable(1f) }

  // Check if raw video resource is present
  val videoRawId = remember {
    context.resources.getIdentifier("splash_video", "raw", context.packageName)
  }
  val hasVideo = (videoRawId != 0)

  // Secondary fallback animation drivers (only used if video resource is absent)
  val logoScale = remember { Animatable(0.4f) }
  val logoAlpha = remember { Animatable(0f) }
  val textAlpha = remember { Animatable(0f) }
  val spinnerAlpha = remember { Animatable(0f) }

  val infiniteTransition = rememberInfiniteTransition(label = "splash_fallback_anim")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28318f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  LaunchedEffect(Unit) {
    if (hasVideo) {
      // Allow video to play its complete 6-second sequence, then smoothly fade out
      delay(5500)
      screenAlpha.animateTo(0f, animationSpec = tween(600))
      onAnimationFinished()
    } else {
      // Fallback choreographed timeline
      delay(700)
      logoAlpha.animateTo(1f, animationSpec = tween(700))
      logoScale.animateTo(1f, animationSpec = tween(800))
      delay(400)
      textAlpha.animateTo(1f, animationSpec = tween(600))
      delay(300)
      spinnerAlpha.animateTo(1f, animationSpec = tween(500))
      delay(2200)
      screenAlpha.animateTo(0f, animationSpec = tween(600))
      onAnimationFinished()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .alpha(screenAlpha.value)
      .background(Color(0xFF030308))
      .testTag("splash_entry_screen"),
    contentAlignment = Alignment.Center
  ) {
    if (hasVideo) {
      // 1. FULL-SCREEN HARDWARE-ACCELERATED VIDEO (NO DUPLICATE LOGO, NO DUPLICATE NAME)
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          val frameLayout = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
          }

          val textureView = TextureView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT
            )
          }

          var mediaPlayer: MediaPlayer? = null

          fun adjustAspectRatio(videoWidth: Int, videoHeight: Int, viewWidth: Int, viewHeight: Int) {
            if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
            val scaleX: Float
            val scaleY: Float
            // Center crop to fill entire screen without letterboxing or black gaps
            val scale = max(
              viewWidth.toFloat() / videoWidth.toFloat(),
              viewHeight.toFloat() / videoHeight.toFloat()
            )
            scaleX = (videoWidth.toFloat() * scale) / viewWidth.toFloat()
            scaleY = (videoHeight.toFloat() * scale) / viewHeight.toFloat()

            val matrix = Matrix()
            val px = viewWidth / 2f
            val py = viewHeight / 2f
            matrix.setScale(scaleX, scaleY, px, py)
            textureView.setTransform(matrix)
          }

          textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
              val s = Surface(surface)
              try {
                mediaPlayer = MediaPlayer().apply {
                  setSurface(s)
                  val uri = Uri.parse("android.resource://${ctx.packageName}/$videoRawId")
                  setDataSource(ctx, uri)
                  // Muted / Silent as requested
                  setVolume(0f, 0f)
                  isLooping = false
                  setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                    adjustAspectRatio(vWidth, vHeight, textureView.width, textureView.height)
                  }
                  setOnPreparedListener { mp ->
                    adjustAspectRatio(mp.videoWidth, mp.videoHeight, textureView.width, textureView.height)
                    mp.start()
                  }
                  prepareAsync()
                }
              } catch (_: Exception) {}
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
              mediaPlayer?.let { mp ->
                adjustAspectRatio(mp.videoWidth, mp.videoHeight, width, height)
              }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
              try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
              } catch (_: Exception) {}
              return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
          }

          frameLayout.addView(textureView)
          frameLayout
        }
      )
    } else {
      // 2. FALLBACK ONLY IF VIDEO IS ABSENT: SHOW LOGO & APP NAME ONCE
      val streamResId = remember {
        context.resources.getIdentifier("splash_neon_stream", "drawable", context.packageName)
      }
      if (streamResId != 0) {
        Image(
          painter = painterResource(id = streamResId),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Procedural neon ribbons
      Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cyanPath = Path().apply {
          moveTo(0f, height * 0.15f)
          cubicTo(
            width * 0.2f, height * (0.12f + 0.03f * sin(wavePhase)),
            width * 0.5f, height * (0.24f + 0.04f * sin(wavePhase + 1f)),
            width * 1.05f, height * 0.18f
          )
        }
        drawPath(
          path = cyanPath,
          color = Color(0xFF00E5FF).copy(alpha = 0.75f),
          style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )

        val magentaPath = Path().apply {
          moveTo(width * 0.05f, height * 0.22f)
          cubicTo(
            width * 0.35f, height * (0.19f + 0.04f * sin(wavePhase + 2f)),
            width * 0.65f, height * (0.28f + 0.03f * sin(wavePhase + 3f)),
            width * 1.02f, height * 0.26f
          )
        }
        drawPath(
          path = magentaPath,
          color = Color(0xFFFF2A8D).copy(alpha = 0.85f),
          style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.weight(0.7f))

        val pLogoResId = remember {
          val customSplash = context.resources.getIdentifier("splash_p_neon_logo", "drawable", context.packageName)
          if (customSplash != 0) customSplash
          else context.resources.getIdentifier("img_priya_live_logo", "drawable", context.packageName)
        }

        Box(
          modifier = Modifier
            .size(190.dp)
            .scale(logoScale.value)
            .alpha(logoAlpha.value),
          contentAlignment = Alignment.Center
        ) {
          if (pLogoResId != 0) {
            Image(
              painter = painterResource(id = pLogoResId),
              contentDescription = "Priya live Logo",
              modifier = Modifier
                .size(165.dp)
                .clip(RoundedCornerShape(38.dp)),
              contentScale = ContentScale.Crop
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
          text = "Priya live",
          color = Color.White,
          fontSize = 34.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp,
          modifier = Modifier
            .alpha(textAlpha.value)
            .testTag("splash_title_text")
        )

        Spacer(modifier = Modifier.weight(0.4f))

        Box(
          modifier = Modifier
            .size(46.dp)
            .alpha(spinnerAlpha.value)
            .testTag("splash_spinner"),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = Color(0xFFFF2A8D),
            strokeWidth = 3.5.dp,
            trackColor = Color(0xFF00E5FF).copy(alpha = 0.25f)
          )
        }

        Spacer(modifier = Modifier.height(56.dp))
      }
    }
  }
}
