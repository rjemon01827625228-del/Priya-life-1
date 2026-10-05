package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AppNavTab
import com.example.ui.components.AudioPartyActiveRoomDialog
import com.example.ui.components.ChatDetailDialog
import com.example.ui.components.CyberBackground
import com.example.ui.components.LiveInteractiveDialog
import com.example.ui.components.LiveRoomDialog
import com.example.ui.components.NeonBottomNav
import com.example.ui.components.PostCreateDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatConversation
import com.example.ui.screens.DynamicAudioPartyRoom
import com.example.ui.screens.LiveStreamItem
import com.example.ui.screens.LiveStreamingScreen
import com.example.ui.screens.MessageInboxScreen
import com.example.ui.screens.ProfileMiScreen
import com.example.ui.screens.SplashEntryScreen
import com.example.ui.screens.SquareFeedScreen
import com.example.ui.screens.VoicePartyScreen
import com.example.ui.storage.ActiveLiveRoom
import com.example.ui.storage.LiveRoomStorage
import com.example.firebase.FirebaseDatabaseManager
import com.example.ui.storage.SavedAudioRoom
import com.example.ui.storage.UserProfileStorage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    FirebaseDatabaseManager.initialize(this)
    setContent {
      MyApplicationTheme {
        CyberPartyApp(
          onOpenWhatsApp = { phone ->
            try {
              val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phone")
              }
              startActivity(intent)
            } catch (_: Exception) {
              Toast.makeText(this, "Contact WhatsApp: $phone", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }
    }
  }
}

@Composable
fun CyberPartyApp(
  onOpenWhatsApp: (String) -> Unit
) {
  val context = LocalContext.current
  var showSplashScreen by remember { mutableStateOf(true) }

  if (showSplashScreen) {
    SplashEntryScreen(
      onAnimationFinished = {
        showSplashScreen = false
      }
    )
    return
  }

  var isLoggedIn by remember {
    mutableStateOf(
      UserProfileStorage.isLoggedIn(context) || com.example.firebase.FirebaseAuthManager.isUserAuthenticated()
    )
  }

  androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
    if (isLoggedIn) {
      val user = com.example.firebase.FirebaseAuthManager.currentUser
      if (user != null) {
        com.example.firebase.FirebaseAuthManager.syncUserProfileWithRealtimeDb(context, user)
      }
    }
  }

  if (!isLoggedIn) {
    AuthScreen(
      onLoginSuccess = {
        UserProfileStorage.setLoggedIn(context, true)
        isLoggedIn = true
      }
    )
    return
  }

  var currentTab by remember { mutableStateOf(AppNavTab.LIVE) }
  var showPostDialog by remember { mutableStateOf(false) }

  // Dialog states for rich interactivity
  var selectedStreamItem by remember { mutableStateOf<LiveStreamItem?>(null) }
  var selectedPartyRoom by remember { mutableStateOf<DynamicAudioPartyRoom?>(null) }
  var selectedChat by remember { mutableStateOf<ChatConversation?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  CyberBackground {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        NeonBottomNav(
          currentTab = currentTab,
          onTabSelected = { tab ->
            currentTab = tab
          }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = innerPadding.calculateBottomPadding())
      ) {
        AnimatedContent(
          targetState = currentTab,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "screen_transition"
        ) { tab ->
          when (tab) {
            AppNavTab.LIVE -> {
              LiveStreamingScreen(
                onStreamClick = { item -> selectedStreamItem = item },
                onGoLiveClick = {
                  selectedStreamItem = LiveStreamItem(
                    id = "host",
                    streamerName = "You (Live)",
                    heartCount = "0",
                    imageRes = R.drawable.img_profile_max
                  )
                },
                onWhatsAppClick = {
                  onOpenWhatsApp("+8801818022519")
                }
              )
            }

            AppNavTab.PARTY -> {
              VoicePartyScreen(
                onRoomClick = { room -> selectedPartyRoom = room },
                onWhatsAppClick = {
                  onOpenWhatsApp("+8801818022519")
                }
              )
            }

            AppNavTab.DISCOVER -> {
              SquareFeedScreen(
                onAddNewPost = { showPostDialog = true }
              )
            }

            AppNavTab.CHAT -> {
              MessageInboxScreen(
                onChatClick = { chat -> selectedChat = chat },
                onHelplineClick = {
                  selectedChat = ChatConversation(
                    id = "official",
                    name = "OFFICIAL HELPLINE",
                    avatarRes = R.drawable.img_streamer_girl,
                    flagEmoji = "🎧"
                  )
                }
              )
            }

            AppNavTab.ME -> {
              ProfileMiScreen(
                onSettingsItemClick = { item ->
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar("Navigating to $item")
                  }
                },
                onCoinsClick = {
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar("Coin Balance: $0.00. Recharge options opening...")
                  }
                },
                onLogout = {
                  com.example.firebase.FirebaseAuthManager.signOut(context)
                  isLoggedIn = false
                },
                onBack = {
                  currentTab = AppNavTab.LIVE
                }
              )
            }
          }
        }
      }
    }

    // Interactive Modals
    if (showPostDialog) {
      PostCreateDialog(
        onDismiss = { showPostDialog = false },
        onActionComplete = { msg ->
          coroutineScope.launch {
            snackbarHostState.showSnackbar(msg)
          }
        }
      )
    }

    selectedStreamItem?.let { stream ->
      val activeUserId = remember { UserProfileStorage.getActiveUserId(context) }
      val liveRoom = stream.rawRoom ?: LiveRoomStorage.getRoomById(context, stream.id) ?: ActiveLiveRoom(
        roomId = stream.id,
        hostUserId = if (stream.id == "user_live" || stream.id == "host") activeUserId else "streamer_${stream.id}",
        hostName = stream.streamerName,
        hostAvatarUri = stream.coverUri,
        hostAvatarRes = stream.imageRes,
        roomTitle = "${stream.streamerName}'s Live Room",
        coverUri = stream.coverUri,
        countryCode = stream.countryCode,
        countryName = "Bangladesh",
        flagEmoji = stream.countryFlag,
        category = stream.category,
        startTimeMs = System.currentTimeMillis(),
        viewerCount = 1,
        heartScore = stream.heartScoreNum
      )

      LiveRoomDialog(
        room = liveRoom,
        isHost = (liveRoom.hostUserId == activeUserId),
        onExitRoom = { selectedStreamItem = null },
        onEscapeRoom = { selectedStreamItem = null },
        onOpenUserProfile = { /* Profile inspect */ }
      )
    }

    selectedPartyRoom?.let { room ->
      AudioPartyActiveRoomDialog(
        room = SavedAudioRoom(
          id = room.id,
          title = room.title,
          coverUriString = room.hostAvatarUri,
          country = room.country,
          flagEmoji = room.flagEmoji,
          creatorUserId = room.hostUserId
        ),
        onDismiss = { selectedPartyRoom = null }
      )
    }

    selectedChat?.let { chat ->
      ChatDetailDialog(
        chat = chat,
        onDismiss = { selectedChat = null }
      )
    }
  }
}
