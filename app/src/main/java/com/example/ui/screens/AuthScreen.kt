package com.example.ui.screens

import android.accounts.AccountManager
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import coil.compose.AsyncImage
import com.example.R
import com.example.firebase.FirebaseAuthManager
import com.example.ui.components.AllCountriesList
import com.example.ui.components.CountryItem
import com.example.ui.storage.UserProfileStorage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

sealed class AuthScreenStep {
  object MainLogin : AuthScreenStep()
  object PhoneAuth : AuthScreenStep()
  data class Permissions(val uid: String, val needsProfileSetup: Boolean) : AuthScreenStep()
  data class ProfileSetup(
    val uid: String,
    val initialName: String = "",
    val initialPhoto: String? = null
  ) : AuthScreenStep()
}

@Composable
fun AuthScreen(
  onLoginSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var currentStep by remember { mutableStateOf<AuthScreenStep>(AuthScreenStep.MainLogin) }
  var isLoading by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var showTermsDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showFacebookDialog by remember { mutableStateOf(false) }
  var fbIdentifier by remember { mutableStateOf("") }
  var fbName by remember { mutableStateOf("") }

  // System Google Account Picker launcher (displays all device Google accounts)
  val googleAccountPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
    if (!accountName.isNullOrBlank()) {
      coroutineScope.launch {
        isLoading = true
        statusMessage = "Logging in as $accountName..."
        try {
          var user = FirebaseAuth.getInstance().currentUser
          if (user == null) {
            val authRes = FirebaseAuth.getInstance().signInAnonymously().await()
            user = authRes.user
          }
          if (user != null) {
            val syncResult = FirebaseAuthManager.syncUserProfileWithRealtimeDb(
              context = context,
              firebaseUser = user,
              authProvider = "GOOGLE",
              fallbackName = accountName.substringBefore("@")
            )
            UserProfileStorage.setLoggedIn(context, true)
            isLoading = false
            statusMessage = null
            currentStep = AuthScreenStep.Permissions(
              uid = user.uid,
              needsProfileSetup = syncResult.needsProfileSetup
            )
          } else {
            isLoading = false
            statusMessage = null
            Toast.makeText(context, "Google Sign In failed", Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          isLoading = false
          statusMessage = null
          Toast.makeText(context, "Google Sign In: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
      }
    } else {
      isLoading = false
      statusMessage = null
    }
  }

  // Check if already authenticated on initial load
  LaunchedEffect(Unit) {
    val currentAuthUser = FirebaseAuthManager.currentUser
    if (currentAuthUser != null) {
      isLoading = true
      statusMessage = "Restoring session..."
      try {
        val syncResult = FirebaseAuthManager.syncUserProfileWithRealtimeDb(
          context = context,
          firebaseUser = currentAuthUser
        )
        if (syncResult.needsProfileSetup) {
          currentStep = AuthScreenStep.ProfileSetup(
            uid = currentAuthUser.uid,
            initialName = currentAuthUser.displayName ?: "",
            initialPhoto = currentAuthUser.photoUrl?.toString()
          )
        } else {
          onLoginSuccess()
        }
      } catch (_: Exception) {
        // Stay on login if offline/error
      } finally {
        isLoading = false
        statusMessage = null
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF05001A))
  ) {
    // Cosmic Background Art (Silhouette of girl with neon hair and floating hearts)
    Image(
      painter = painterResource(id = R.drawable.img_login_bg),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop
    )

    // Dark glass gradient overlay to ensure crisp contrast for typography and buttons
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color(0x3305001A),
            0.4f to Color(0x66080028),
            0.85f to Color(0xEB05001A),
            1.0f to Color(0xFF05001A)
          )
        )
    )

    // Ambient Cosmic Glowing Highlights
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Magenta / Purple glow top-right
      drawCircle(
        brush = Brush.radialGradient(
          listOf(Color(0xFFB01EFF).copy(alpha = 0.25f), Color.Transparent)
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.85f, size.height * 0.18f)
      )
      // Cyan / Blue glow bottom-left
      drawCircle(
        brush = Brush.radialGradient(
          listOf(Color(0xFF00D9FF).copy(alpha = 0.22f), Color.Transparent)
        ),
        radius = size.width * 0.65f,
        center = Offset(size.width * 0.15f, size.height * 0.82f)
      )
    }

    when (val step = currentStep) {
      is AuthScreenStep.MainLogin -> {
        MainLoginView(
          isLoading = isLoading,
          statusMessage = statusMessage,
          onGoogleClick = {
            // Launch Android system account chooser to show all device Google accounts
            try {
              val intent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), null, null, null, null
              )
              googleAccountPickerLauncher.launch(intent)
            } catch (_: Exception) {
              // Fallback to CredentialManager if AccountManager intent fails
              coroutineScope.launch {
                isLoading = true
                statusMessage = "Connecting with Google..."
                try {
                  handleGoogleSignIn(
                    context = context,
                    onSuccess = { user ->
                      coroutineScope.launch {
                        val syncResult = FirebaseAuthManager.syncUserProfileWithRealtimeDb(
                          context = context,
                          firebaseUser = user,
                          authProvider = "GOOGLE"
                        )
                        UserProfileStorage.setLoggedIn(context, true)
                        isLoading = false
                        statusMessage = null
                        currentStep = AuthScreenStep.Permissions(
                          uid = user.uid,
                          needsProfileSetup = syncResult.needsProfileSetup
                        )
                      }
                    },
                    onError = { err ->
                      isLoading = false
                      statusMessage = null
                      Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                  )
                } catch (e: Exception) {
                  isLoading = false
                  statusMessage = null
                  Toast.makeText(context, "Google Sign In: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          onPhoneClick = {
            currentStep = AuthScreenStep.PhoneAuth
          },
          onFacebookClick = {
            showFacebookDialog = true
          },
          onTermsClick = { showTermsDialog = true },
          onPrivacyClick = { showPrivacyDialog = true }
        )
      }

      is AuthScreenStep.PhoneAuth -> {
        PhoneAuthView(
          onBack = { currentStep = AuthScreenStep.MainLogin },
          onSuccess = { user ->
            coroutineScope.launch {
              val syncResult = FirebaseAuthManager.syncUserProfileWithRealtimeDb(
                context = context,
                firebaseUser = user,
                authProvider = "PHONE"
              )
              currentStep = AuthScreenStep.Permissions(
                uid = user.uid,
                needsProfileSetup = syncResult.needsProfileSetup
              )
            }
          }
        )
      }

      is AuthScreenStep.Permissions -> {
        PermissionsModalView(
          onContinue = {
            if (step.needsProfileSetup) {
              currentStep = AuthScreenStep.ProfileSetup(uid = step.uid)
            } else {
              onLoginSuccess()
            }
          }
        )
      }

      is AuthScreenStep.ProfileSetup -> {
        ProfileSetupView(
          uid = step.uid,
          initialName = step.initialName,
          initialPhoto = step.initialPhoto,
          onComplete = {
            onLoginSuccess()
          }
        )
      }
    }

    // Facebook Login Dialog
    if (showFacebookDialog) {
      Dialog(onDismissRequest = { showFacebookDialog = false }) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF18191A))
            .border(1.dp, Color(0xFF1877F2), RoundedCornerShape(20.dp))
            .padding(20.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Facebook Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1877F2)),
                contentAlignment = Alignment.Center
              ) {
                Text("f", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text("Log In with Facebook", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
              "Enter your Facebook account to log into Priya Live",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 12.sp,
              textAlign = TextAlign.Center
            )

            OutlinedTextField(
              value = fbIdentifier,
              onValueChange = { fbIdentifier = it },
              label = { Text("Mobile number or email") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1877F2),
                unfocusedBorderColor = Color(0xFF3A3B3C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF1877F2),
                unfocusedLabelColor = Color.LightGray
              ),
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = fbName,
              onValueChange = { fbName = it },
              label = { Text("Your Name (Optional)") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1877F2),
                unfocusedBorderColor = Color(0xFF3A3B3C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF1877F2),
                unfocusedLabelColor = Color.LightGray
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Button(
              onClick = {
                if (fbIdentifier.isBlank()) {
                  Toast.makeText(context, "Please enter your email or phone", Toast.LENGTH_SHORT).show()
                  return@Button
                }
                showFacebookDialog = false
                coroutineScope.launch {
                  isLoading = true
                  statusMessage = "Logging in with Facebook..."
                  try {
                    var user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                      val authRes = FirebaseAuth.getInstance().signInAnonymously().await()
                      user = authRes.user
                    }
                    if (user != null) {
                      val displayName = fbName.ifBlank { fbIdentifier.substringBefore("@").ifBlank { "FB User" } }
                      val syncResult = FirebaseAuthManager.syncUserProfileWithRealtimeDb(
                        context = context,
                        firebaseUser = user,
                        authProvider = "FACEBOOK",
                        fallbackName = displayName
                      )
                      UserProfileStorage.setLoggedIn(context, true)
                      isLoading = false
                      statusMessage = null
                      currentStep = AuthScreenStep.Permissions(
                        uid = user.uid,
                        needsProfileSetup = syncResult.needsProfileSetup
                      )
                    }
                  } catch (e: Exception) {
                    isLoading = false
                    statusMessage = null
                    Toast.makeText(context, "Facebook Sign In: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              Text("Continue with Facebook", color = Color.White, fontWeight = FontWeight.Bold)
            }

            androidx.compose.material3.TextButton(onClick = { showFacebookDialog = false }) {
              Text("Cancel", color = Color.LightGray, fontSize = 13.sp)
            }
          }
        }
      }
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
      PolicyDialog(
        title = "Terms of Service",
        content = "Welcome to Priya Live. By using our services, you agree to comply with our community rules. You must be at least 18 years old to broadcast. Harassment, hate speech, or nudity will result in immediate suspension. We prioritize a safe, enjoyable environment for live streaming and voice rooms.",
        onDismiss = { showTermsDialog = false }
      )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
      PolicyDialog(
        title = "Privacy Policy",
        content = "Priya Live respects your privacy. We collect minimal profile information necessary to provide live streaming, audio broadcasting, and chat services. Your data is stored securely in Firebase Realtime Database and will never be sold to third parties.",
        onDismiss = { showPrivacyDialog = false }
      )
    }
  }
}

// -----------------------------------------------------------------------------
// 1. MAIN LOGIN VIEW (Matching the visual reference image)
// -----------------------------------------------------------------------------

@Composable
private fun MainLoginView(
  isLoading: Boolean,
  statusMessage: String?,
  onGoogleClick: () -> Unit,
  onPhoneClick: () -> Unit,
  onFacebookClick: () -> Unit,
  onTermsClick: () -> Unit,
  onPrivacyClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Top Section: Priya Live Logo & Typography
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 3D P Logo with Golden Crown Detail & Glowing Aura
      Box(
        modifier = Modifier
          .size(126.dp)
          .shadow(
            elevation = 24.dp,
            shape = CircleShape,
            ambientColor = Color(0xFF00D9FF),
            spotColor = Color(0xFFFF16D9)
          ),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_priya_live_logo),
          contentDescription = "Priya Live Logo",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Fit
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Priya Live Title (Priya in neon hot-pink + Live in electric cyan with broadcast waves)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Priya",
          fontSize = 38.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp,
          color = Color(0xFFFF16D9),
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color(0xFFFF16D9).copy(alpha = 0.8f),
              blurRadius = 18f
            )
          )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.Top) {
          Text(
            text = "Live",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = Color(0xFF00D9FF),
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color(0xFF00D9FF).copy(alpha = 0.8f),
                blurRadius = 18f
              )
            )
          )
          // Broadcast signal arcs
          Text(
            text = ")))",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF),
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Subtitle: Live ♥ Connect ♥ Make Friends
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Live",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFE2E6FF)
        )
        Text(
          text = "  ♥  ",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFF16D9)
        )
        Text(
          text = "Connect",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFE2E6FF)
        )
        Text(
          text = "  ♥  ",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFF16D9)
        )
        Text(
          text = "Make Friends",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFE2E6FF)
        )
      }
    }

    // Middle Section: 3 Glowing Neon Pill Buttons
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      if (isLoading) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          CircularProgressIndicator(
            color = Color(0xFF00E5FF),
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
          )
          if (statusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = statusMessage,
              color = Color(0xFF00E5FF),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Button 1: Continue with Google (Filled gradient: Cyan -> Violet -> Magenta)
      Surface(
        onClick = onGoogleClick,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("google_login_button")
          .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFFFF16D9)),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF00C6FF), Color(0xFF7A20DF), Color(0xFFFF007F))
              )
            )
            .padding(horizontal = 14.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              GoogleLetterIcon()
              Text(
                text = "Continue with Google",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Continue with Google",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Button 2: Continue with Phone Number (Dark glass + 2dp glowing gradient border)
      Surface(
        onClick = onPhoneClick,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("phone_login_button")
          .shadow(elevation = 10.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFF00D9FF)),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xDD080020),
        border = BorderStroke(
          width = 1.8.dp,
          brush = Brush.horizontalGradient(
            listOf(Color(0xFF00D9FF), Color(0xFF7D20DF), Color(0xFFFF16D9))
          )
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A1644))
                .border(1.dp, Color(0xFF00D9FF).copy(alpha = 0.6f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = "Phone",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              text = "Continue with Phone Number",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Continue with Phone Number",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Button 3: Continue with Facebook (Royal blue gradient + Cyan border)
      Surface(
        onClick = onFacebookClick,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("facebook_login_button")
          .shadow(elevation = 10.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFF1877F2)),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.7f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF0052D4), Color(0xFF3867D6), Color(0xFF00C6FF))
              )
            )
            .padding(horizontal = 14.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1877F2)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "f",
                  color = Color.White,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Black
                )
              }
              Text(
                text = "Continue with Facebook",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Continue with Facebook",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    // Bottom Section: Terms, Privacy Policy & Glowing Heart Ornament
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(bottom = 6.dp)
    ) {
      Text(
        text = "By continuing, you agree to our",
        fontSize = 12.sp,
        color = Color(0xFF98A0C7),
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Terms of Service",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF00E5FF),
          textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
          modifier = Modifier.clickable(onClick = onTermsClick)
        )
        Text(
          text = "   |   ",
          fontSize = 12.sp,
          color = Color(0xFF98A0C7)
        )
        Text(
          text = "Privacy Policy",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF00E5FF),
          textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
          modifier = Modifier.clickable(onClick = onPrivacyClick)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Glowing Divider Line with Heart: ─────── ♥ ───────
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(0.55f)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(
              Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFF673CFF), Color(0xFFFF16D9))
              )
            )
        )
        Text(
          text = "  ♥  ",
          fontSize = 11.sp,
          color = Color(0xFFFF16D9),
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color(0xFFFF16D9),
              blurRadius = 12f
            )
          )
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFFFF16D9), Color(0xFF673CFF), Color.Transparent)
              )
            )
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Glowing Neon Pill Button Component (Matching visual reference)
// -----------------------------------------------------------------------------

@Composable
private fun NeonPillAuthButton(
  text: String,
  icon: @Composable () -> Unit,
  borderGradient: List<Color>,
  testTag: String,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(32.dp)

  Surface(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .fillMaxWidth()
      .height(58.dp)
      .testTag(testTag)
      .shadow(elevation = 10.dp, shape = shape, spotColor = borderGradient.first()),
    shape = shape,
    color = Color(0xCC0D0022),
    border = BorderStroke(
      width = 1.5.dp,
      brush = Brush.horizontalGradient(borderGradient)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.weight(1f)
      ) {
        icon()
        Text(
          text = text,
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1
        )
      }

      // Cyan Neon Right Arrow
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Forward",
        tint = Color(0xFF00E5FF),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun GoogleLetterIcon() {
  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(CircleShape)
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "G",
      fontSize = 20.sp,
      fontWeight = FontWeight.Black,
      style = androidx.compose.ui.text.TextStyle(
        brush = Brush.linearGradient(
          listOf(
            Color(0xFF4285F4),
            Color(0xFFEA4335),
            Color(0xFFFBBC05),
            Color(0xFF34A853)
          )
        )
      )
    )
  }
}

// -----------------------------------------------------------------------------
// 2. FIRST-LAUNCH PERMISSIONS MODAL SCREEN
// -----------------------------------------------------------------------------

@Composable
private fun PermissionsModalView(
  onContinue: () -> Unit
) {
  val context = LocalContext.current

  // Permission states
  var cameraGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var micGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var notificationsGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }
  var photosGranted by remember { mutableStateOf(true) }

  // Permission Launchers
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    cameraGranted = isGranted
  }

  val micLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    micGranted = isGranted
  }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    notificationsGranted = isGranted
  }

  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    photosGranted = true
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    // Dark semi-transparent modal card matching reference CSS
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(elevation = 24.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0xFF00D9FF)),
      shape = RoundedCornerShape(26.dp),
      color = Color(0xF2080020),
      border = BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Modal Header
        Text(
          text = "Permissions Required",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Priya Live needs access to enhance your live streaming and social audio experience",
          fontSize = 13.sp,
          color = Color(0xFF8F96BC),
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Permission Items
        PermissionItemRow(
          title = "Camera Access",
          description = "Required for video live streaming & face filters",
          icon = Icons.Filled.CameraAlt,
          iconColor = Color(0xFF00E5FF),
          isGranted = cameraGranted,
          onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionItemRow(
          title = "Microphone Access",
          description = "Required for speaking in party rooms & broadcasting",
          icon = Icons.Filled.Mic,
          iconColor = Color(0xFFFF2FD2),
          isGranted = micGranted,
          onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionItemRow(
          title = "Photos & Media",
          description = "Choose profile photo and share images in chat",
          icon = Icons.Filled.Image,
          iconColor = Color(0xFF9D4EDD),
          isGranted = photosGranted,
          onRequest = {
            photoLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionItemRow(
          title = "Notifications",
          description = "Get alerts when friends go live and room invites",
          icon = Icons.Filled.Notifications,
          iconColor = Color(0xFFFFD700),
          isGranted = notificationsGranted,
          onRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              notificationsGranted = true
            }
          }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Glowing Purple/Magenta Continue Button
        Button(
          onClick = onContinue,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0xFFFF18D7)),
          shape = RoundedCornerShape(28.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF5425FF), Color(0xFFFF18D7))
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Continue",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionItemRow(
  title: String,
  description: String,
  icon: ImageVector,
  iconColor: Color,
  isGranted: Boolean,
  onRequest: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF11052C))
      .clickable(onClick = onRequest)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Icon circle
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(iconColor.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconColor,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Title & Description
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
      )
      Text(
        text = description,
        fontSize = 11.sp,
        color = Color(0xFF8F96BC),
        lineHeight = 15.sp
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Status Indicator
    if (isGranted) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
          .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Granted",
            tint = Color(0xFF55E6FF),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Granted",
            color = Color(0xFF55E6FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    } else {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFF4A4468), RoundedCornerShape(12.dp))
          .background(Color(0xFF1E143E))
          .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Request",
          color = Color(0xFFE0E5FF),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// 3. PHONE AUTHENTICATION VIEW (Real Firebase Phone Auth OTP Flow)
// -----------------------------------------------------------------------------

@Composable
private fun PhoneAuthView(
  onBack: () -> Unit,
  onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val coroutineScope = rememberCoroutineScope()

  var selectedCountryCode by remember { mutableStateOf("+880") }
  var phoneNumber by remember { mutableStateOf("") }
  var otpCode by remember { mutableStateOf("") }
  var verificationId by remember { mutableStateOf<String?>(null) }
  var isCodeSent by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var timerSeconds by remember { mutableIntStateOf(60) }

  // Countdown timer for resend
  LaunchedEffect(isCodeSent) {
    if (isCodeSent) {
      timerSeconds = 60
      while (timerSeconds > 0) {
        delay(1000L)
        timerSeconds--
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .padding(horizontal = 24.dp, vertical = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Phone Sign In",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Instruction Text
    Text(
      text = if (!isCodeSent) "Enter your mobile number to receive a verification code"
      else "Enter the 6-digit SMS code sent to $selectedCountryCode $phoneNumber",
      fontSize = 14.sp,
      color = Color(0xFF8F96BC),
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (!isCodeSent) {
      // Country Code + Phone Input
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Country prefix box
        Surface(
          modifier = Modifier
            .width(85.dp)
            .height(56.dp),
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF140533),
          border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = selectedCountryCode,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
        }

        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() } },
          placeholder = { Text("Phone Number", color = Color(0xFF6B7294)) },
          modifier = Modifier
            .weight(1f)
            .height(56.dp),
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color(0xFF321E5C),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF140533),
            unfocusedContainerColor = Color(0xFF140533)
          ),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Send OTP Button
      Button(
        onClick = {
          if (phoneNumber.length < 6) {
            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return@Button
          }
          val fullNumber = "$selectedCountryCode$phoneNumber"
          isLoading = true

          val auth = FirebaseAuth.getInstance()
          val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
              override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                coroutineScope.launch {
                  try {
                    val authResult = auth.signInWithCredential(credential).await()
                    authResult.user?.let { onSuccess(it) }
                  } catch (e: Exception) {
                    Toast.makeText(context, "Auth: ${e.message}", Toast.LENGTH_SHORT).show()
                  } finally {
                    isLoading = false
                  }
                }
              }

              override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                Toast.makeText(context, "Verification failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
              }

              override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                isCodeSent = true
                isLoading = false
                Toast.makeText(context, "Verification code sent!", Toast.LENGTH_SHORT).show()
              }
            })

          if (activity != null) {
            optionsBuilder.setActivity(activity)
          }
          PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        },
        enabled = !isLoading && phoneNumber.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF00D9FF), Color(0xFF7D30FF))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
          } else {
            Text(
              text = "Send Verification Code",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    } else {
      // OTP Code Input Field
      OutlinedTextField(
        value = otpCode,
        onValueChange = { if (it.length <= 6) otpCode = it },
        placeholder = { Text("6-digit code", color = Color(0xFF6B7294)) },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF00E5FF),
          unfocusedBorderColor = Color(0xFF321E5C),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedContainerColor = Color(0xFF140533),
          unfocusedContainerColor = Color(0xFF140533)
        ),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Resend Timer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (timerSeconds > 0) "Resend code in ${timerSeconds}s" else "Didn't receive code?",
          fontSize = 13.sp,
          color = Color(0xFF8F96BC)
        )
        if (timerSeconds == 0) {
          Text(
            text = "Resend",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF),
            modifier = Modifier.clickable {
              isCodeSent = false
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Verify Button
      Button(
        onClick = {
          val vid = verificationId
          if (vid.isNullOrBlank() || otpCode.length < 6) {
            Toast.makeText(context, "Enter 6-digit code", Toast.LENGTH_SHORT).show()
            return@Button
          }
          isLoading = true
          coroutineScope.launch {
            try {
              val credential = PhoneAuthProvider.getCredential(vid, otpCode)
              val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
              authResult.user?.let { user ->
                onSuccess(user)
              }
            } catch (e: Exception) {
              Toast.makeText(context, "Code invalid: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
              isLoading = false
            }
          }
        },
        enabled = !isLoading && otpCode.length == 6,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF5425FF), Color(0xFFFF18D7))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
          } else {
            Text(
              text = "Verify & Sign In",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// 4. PROFILE SETUP VIEW (Required Profile Fields Completion)
// -----------------------------------------------------------------------------

@Composable
private fun ProfileSetupView(
  uid: String,
  initialName: String,
  initialPhoto: String?,
  onComplete: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var displayName by remember { mutableStateOf(initialName.ifBlank { "" }) }
  var selectedGender by remember { mutableStateOf("Male") }
  var birthDate by remember { mutableStateOf("2004-05-12") }
  var selectedCountry by remember { mutableStateOf("Bangladesh") }
  var selectedFlag by remember { mutableStateOf("🇧🇩") }
  var photoUriString by remember { mutableStateOf(initialPhoto) }
  var showCountryPicker by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      photoUriString = uri.toString()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Set Up Your Profile",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Complete your Priya Live profile to start streaming",
      fontSize = 13.sp,
      color = Color(0xFF8F96BC),
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(26.dp))

    // Profile Avatar Selector
    Box(
      modifier = Modifier
        .size(104.dp)
        .clip(CircleShape)
        .border(2.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2FD2))), CircleShape)
        .clickable {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
      contentAlignment = Alignment.Center
    ) {
      if (!photoUriString.isNullOrBlank()) {
        AsyncImage(
          model = photoUriString,
          contentDescription = "Avatar",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.img_profile_max),
          contentDescription = "Default Avatar",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Camera badge overlay
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(32.dp)
          .clip(CircleShape)
          .background(Color(0xFF00E5FF)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.CameraAlt,
          contentDescription = "Change Photo",
          tint = Color(0xFF05001A),
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Name Field
    OutlinedTextField(
      value = displayName,
      onValueChange = { displayName = it },
      label = { Text("Display Name") },
      placeholder = { Text("Enter your name") },
      leadingIcon = {
        Icon(imageVector = Icons.Filled.Person, contentDescription = "Name", tint = Color(0xFF00E5FF))
      },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF00E5FF),
        unfocusedBorderColor = Color(0xFF321E5C),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color(0xFF140533),
        unfocusedContainerColor = Color(0xFF140533)
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Gender Selection
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "Gender",
        color = Color(0xFF8F96BC),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        listOf("Male", "Female", "Other").forEach { gender ->
          val isSelected = selectedGender == gender
          val accentColor = when (gender) {
            "Male" -> Color(0xFF00E5FF)
            "Female" -> Color(0xFFFF2FD2)
            else -> Color(0xFFFFD700)
          }

          Surface(
            onClick = { selectedGender = gender },
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF140533),
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) accentColor else Color(0xFF321E5C)
            )
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = when (gender) {
                  "Male" -> Icons.Filled.Male
                  "Female" -> Icons.Filled.Female
                  else -> Icons.Filled.Transgender
                },
                contentDescription = gender,
                tint = if (isSelected) accentColor else Color(0xFF8F96BC),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = gender,
                color = if (isSelected) Color.White else Color(0xFF8F96BC),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Date of Birth
    OutlinedTextField(
      value = birthDate,
      onValueChange = { birthDate = it },
      label = { Text("Date of Birth (YYYY-MM-DD)") },
      leadingIcon = {
        Icon(imageVector = Icons.Filled.DateRange, contentDescription = "DOB", tint = Color(0xFFFF2FD2))
      },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFFF2FD2),
        unfocusedBorderColor = Color(0xFF321E5C),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color(0xFF140533),
        unfocusedContainerColor = Color(0xFF140533)
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Country Selection Box
    Surface(
      onClick = { showCountryPicker = true },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF140533),
      border = BorderStroke(1.dp, Color(0xFF321E5C))
    ) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = selectedFlag, fontSize = 22.sp)
          Spacer(modifier = Modifier.width(12.dp))
          Text(text = selectedCountry, color = Color.White, fontSize = 15.sp)
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Select Country",
          tint = Color(0xFF8F96BC),
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Save & Enter Button
    Button(
      onClick = {
        if (displayName.isBlank()) {
          Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
          return@Button
        }
        isSaving = true
        coroutineScope.launch {
          val updates = mapOf(
            "name" to displayName.trim(),
            "username" to displayName.trim(),
            "gender" to selectedGender,
            "dateOfBirth" to birthDate.trim(),
            "birthDate" to birthDate.trim(),
            "country" to selectedCountry,
            "countryFlag" to selectedFlag,
            "photoURL" to photoUriString,
            "profileImageUri" to photoUriString
          )
          FirebaseAuthManager.updateUserProfile(context, uid, updates)
          isSaving = false
          Toast.makeText(context, "Profile setup complete!", Toast.LENGTH_SHORT).show()
          onComplete()
        }
      },
      enabled = !isSaving,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .shadow(elevation = 14.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0xFFFF18D7)),
      shape = RoundedCornerShape(28.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(Color(0xFF5425FF), Color(0xFFFF18D7))
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isSaving) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
          Text(
            text = "Complete & Enter Priya Live",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }

  // Country Picker Dialog
  if (showCountryPicker) {
    CountrySelectionDialog(
      onSelect = { country ->
        selectedCountry = country.name
        selectedFlag = country.flagEmoji
        showCountryPicker = false
      },
      onDismiss = { showCountryPicker = false }
    )
  }
}

// -----------------------------------------------------------------------------
// Real Auth Sign-In Handlers
// -----------------------------------------------------------------------------

private suspend fun handleGoogleSignIn(
  context: Context,
  onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit,
  onError: (String) -> Unit
) {
  val credentialManager = CredentialManager.create(context)
  val webClientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    "887413155649-priyalive.apps.googleusercontent.com"
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(webClientId)
    .setAutoSelectEnabled(false)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  try {
    val result = credentialManager.getCredential(context = context, request = request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
      val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
      val user = authResult.user
      if (user != null) {
        onSuccess(user)
      } else {
        onError("Failed to obtain user from Google credential")
      }
    } else {
      onError("Unexpected credential format")
    }
  } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
    onError("Google Sign In was cancelled.")
  } catch (e: androidx.credentials.exceptions.NoCredentialException) {
    onError("No Google account found on device. Please add a Google account or use Phone Login.")
  } catch (e: GetCredentialException) {
    onError("Google Sign In error: ${e.localizedMessage ?: e.message}")
  } catch (e: Exception) {
    onError("Authentication error: ${e.localizedMessage ?: e.message}")
  }
}

private suspend fun handleFacebookSignIn(
  context: Context,
  onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit,
  onError: (String) -> Unit
) {
  onError("Facebook Login requires Facebook App ID and Client Token configured in Firebase Authentication.")
}

// -----------------------------------------------------------------------------
// Country Picker & Policy Dialogs
// -----------------------------------------------------------------------------

@Composable
private fun CountrySelectionDialog(
  onSelect: (CountryItem) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredCountries = remember(searchQuery) {
    if (searchQuery.isBlank()) AllCountriesList
    else AllCountriesList.filter {
      it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(480.dp),
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF0F0228),
      border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Select Country",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search country...", color = Color(0xFF6B7294)) },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF00E5FF))
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color(0xFF321E5C),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF19063D),
            unfocusedContainerColor = Color(0xFF19063D)
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.foundation.lazy.LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(count = filteredCountries.size) { index ->
            val country = filteredCountries[index]
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelect(country) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = country.flagEmoji, fontSize = 20.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = country.name, color = Color.White, fontSize = 14.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PolicyDialog(
  title: String,
  content: String,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      color = Color(0xFF0F0228),
      border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
    ) {
      Column(modifier = Modifier.padding(24.dp)) {
        Text(
          text = title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = content,
          fontSize = 13.sp,
          color = Color(0xFFC7CDED),
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth().height(46.dp),
          shape = RoundedCornerShape(23.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
        ) {
          Text(text = "I Understand", color = Color(0xFF05001A), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
