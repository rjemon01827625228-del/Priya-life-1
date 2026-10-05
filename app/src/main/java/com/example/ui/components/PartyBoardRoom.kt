package com.example.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.agora.AgoraVoiceService
import com.example.firebase.FirebasePartyRoomService
import com.example.firebase.model.FirebaseChatMessage
import com.example.firebase.model.FirebaseGiftEvent
import com.example.firebase.model.FirebasePartyRoomData
import com.example.firebase.model.FirebasePartySeat
import com.example.firebase.model.FirebaseRoomMusic
import com.example.firebase.model.FirebaseUserProfile
import com.example.ui.storage.SavedAudioRoom
import com.example.ui.storage.UserProfileStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visual Color Palettes for the Glowing Seats matching the reference image:
 * No.1: Royal Purple / Violet
 * No.2: Electric Blue / Cobalt
 * No.3: Golden Amber / Orange
 * No.4: Hot Pink / Magenta
 * No.5: Radiant Crimson / Red
 * No.6: Electric Aqua / Sky Cyan
 * No.7: Neon Violet / Purple
 * No.8: Vibrant Emerald Green
 */
data class SeatColorTheme(
  val primaryColor: Color,
  val secondaryColor: Color,
  val glowColor: Color,
  val borderBrush: Brush
)

fun getSeatTheme(seatNumber: Int): SeatColorTheme {
  return when ((seatNumber - 1) % 8) {
    0 -> SeatColorTheme(
      primaryColor = Color(0xFF9C27B0),
      secondaryColor = Color(0xFF6A1B9A),
      glowColor = Color(0xFFD500F9),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFFD500F9), Color(0xFFBA68C8), Color(0xFF8E24AA), Color(0xFFD500F9)))
    )
    1 -> SeatColorTheme(
      primaryColor = Color(0xFF0070F3),
      secondaryColor = Color(0xFF0D47A1),
      glowColor = Color(0xFF00D9FF),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFF00D9FF), Color(0xFF2979FF), Color(0xFF0052CC), Color(0xFF00D9FF)))
    )
    2 -> SeatColorTheme(
      primaryColor = Color(0xFFFF9100),
      secondaryColor = Color(0xFFE65100),
      glowColor = Color(0xFFFFD700),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9100), Color(0xFFFF6D00), Color(0xFFFFD700)))
    )
    3 -> SeatColorTheme(
      primaryColor = Color(0xFFFF1493),
      secondaryColor = Color(0xFF880E4F),
      glowColor = Color(0xFFFF4081),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFFFF4081), Color(0xFFFF007F), Color(0xFFC2185B), Color(0xFFFF4081)))
    )
    4 -> SeatColorTheme(
      primaryColor = Color(0xFFFF1744),
      secondaryColor = Color(0xFFB71C1C),
      glowColor = Color(0xFFFF5252),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFFF5252)))
    )
    5 -> SeatColorTheme(
      primaryColor = Color(0xFF00E5FF),
      secondaryColor = Color(0xFF006064),
      glowColor = Color(0xFF00B0FF),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFF18FFFF), Color(0xFF0091EA), Color(0xFF00E5FF)))
    )
    6 -> SeatColorTheme(
      primaryColor = Color(0xFFA855F7),
      secondaryColor = Color(0xFF4A148C),
      glowColor = Color(0xFFC084FC),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFFC084FC), Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFFC084FC)))
    )
    else -> SeatColorTheme(
      primaryColor = Color(0xFF00E676),
      secondaryColor = Color(0xFF1B5E20),
      glowColor = Color(0xFF10B981),
      borderBrush = Brush.sweepGradient(listOf(Color(0xFF10B981), Color(0xFF00E676), Color(0xFF00C853), Color(0xFF10B981)))
    )
  }
}

/**
 * Animated Floating Gift / Reaction on Party Board
 */
data class ActiveGiftDisplay(
  val id: Long,
  val emoji: String,
  val title: String,
  val sender: String
)

/**
 * FULL-SCREEN INTERACTIVE PARTY BOARD
 * Closely follows the reference image:
 * - Scenic mountain lake twilight balcony background
 * - Top header with Host capsule, user ID, audience count, share, more, exit, and PK button
 * - Center: 8 seats initially (expandable to 12 or 16 maximum) in a 2-row layout
 * - Exact glowing circular seat designs with ||| 🎤 ||| audio wave bars inside empty state
 * - Bottom action bar with exactly 6 options:
 *   Mute | Mic | Message | Gift | Game | Menu
 * - Menu button is at extreme bottom-right, opening a floating dark/neon glassmorphism popup:
 *   ⚙️ Settings, 🎵 Music, 🔒 Room Lock, 🔐 Seat Lock, 🔓 Seat Open
 * - Real Firebase Realtime Database data synchronization: no fake users, no fake chats, no fake coins, no fake games.
 */
@Composable
fun PartyBoardRoom(
  room: SavedAudioRoom,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // ---------------------------------------------------------------------------
  // AUTHENTICATED REAL USER DATA (From Firebase Authentication & Profile Storage)
  // ---------------------------------------------------------------------------
  val currentUserId = remember { UserProfileStorage.getActiveUserId(context) }
  val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
  val userProfile = remember { UserProfileStorage.getProfileForUser(context, currentUserId) }
  var userWalletCoins by remember { mutableLongStateOf(UserProfileStorage.getCoinBalance(context, currentUserId)) }
  val parsedUserLevel = remember(userProfile.levelBadge) {
    userProfile.levelBadge.filter { it.isDigit() }.toIntOrNull() ?: 1
  }

  // Room Identifier
  val roomId = remember(room.id, room.title, room.creatorUserId) {
    if (room.id.isNotBlank()) {
      room.id
    } else if (room.creatorUserId.isNotBlank()) {
      "party_room_${room.creatorUserId}"
    } else {
      "party_${currentUserId}_${room.title.hashCode().toString().replace("-", "")}"
    }
  }

  // Determine if current user is owner (ONLY the host who created/opened this room!)
  val isOwner = remember(room.creatorUserId, currentUserId) {
    if (room.creatorUserId.isNotBlank()) {
      room.creatorUserId == currentUserId || currentFirebaseUser?.uid == room.creatorUserId
    } else {
      true
    }
  }

  // ---------------------------------------------------------------------------
  // REAL-TIME ROOM STATE (From Firebase Realtime Database)
  // ---------------------------------------------------------------------------
  var roomData by remember {
    mutableStateOf(
      FirebasePartyRoomData(
        roomId = roomId,
        roomTitle = room.title.ifBlank { "Party Board" },
        hostUid = currentFirebaseUser?.uid ?: currentUserId,
        hostName = userProfile.username.ifBlank { "Host" },
        hostAvatarUri = userProfile.profileImageUri,
        hostUserId = if (room.creatorUserId.isNotBlank()) room.creatorUserId else currentUserId,
        coverUri = room.coverUriString,
        isLocked = false,
        seatCount = 8,
        viewerCount = 1,
        coinPool = 0L
      )
    )
  }

  // Active seats map (seatNumber -> FirebasePartySeat) initialized with 8 default seats
  var seatsState by remember {
    mutableStateOf(
      (1..8).associateWith { num ->
        FirebasePartySeat(seatNumber = num, isOccupied = false, isLocked = false)
      }
    )
  }

  // Chat & Gifts
  val chatMessages = remember { mutableStateListOf<FirebaseChatMessage>() }
  val activeGifts = remember { mutableStateListOf<ActiveGiftDisplay>() }

  // User mic & mute state
  var isLocalMuted by remember { mutableStateOf(false) } // Audio Mute
  var isLocalMicActive by remember { mutableStateOf(false) } // Microphone On/Off
  val agoraSpeakingUsers by AgoraVoiceService.speakingUsers.collectAsState()
  val isLocalSpeaking by AgoraVoiceService.isLocalSpeaking.collectAsState()

  // Music Player state
  var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
  var isMusicPlaying by remember { mutableStateOf(false) }
  var currentMusicTitle by remember { mutableStateOf<String?>(null) }
  var musicVolume by remember { mutableFloatStateOf(0.85f) }

  // Floating Popups & Dialogs
  var showMenuPanel by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showMusicDialog by remember { mutableStateOf(false) }
  var showSeatLockDialog by remember { mutableStateOf(false) }
  var showSeatOpenDialog by remember { mutableStateOf(false) }
  var showChatDialog by remember { mutableStateOf(false) }
  var showGiftDialog by remember { mutableStateOf(false) }
  var showGameDialog by remember { mutableStateOf(false) }
  var showExitConfirmDialog by remember { mutableStateOf(false) }
  var selectedParticipantProfile by remember { mutableStateOf<PartyParticipant?>(null) }

  // ---------------------------------------------------------------------------
  // PERMISSION LAUNCHERS
  // ---------------------------------------------------------------------------
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      isLocalMicActive = true
      AgoraVoiceService.setClientRole(true, context)
      AgoraVoiceService.setLocalMicMuted(false, context)
      Toast.makeText(context, "Microphone enabled", Toast.LENGTH_SHORT).show()
    } else {
      isLocalMicActive = false
      AgoraVoiceService.setLocalMicMuted(true, context)
      AgoraVoiceService.setClientRole(false, context)
      Toast.makeText(context, "Microphone permission is required for voice", Toast.LENGTH_SHORT).show()
    }
  }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { selectedUri ->
      try {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        var displayName = "Local Audio Track"
        context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1 && cursor.moveToFirst()) {
            displayName = cursor.getString(nameIndex) ?: "Local Audio Track"
          }
        }
        currentMusicTitle = displayName

        val player = MediaPlayer().apply {
          setDataSource(context, selectedUri)
          prepare()
          val vol = if (isLocalMuted) 0f else musicVolume
          setVolume(vol, vol)
          isLooping = true
          start()
        }
        mediaPlayer = player
        isMusicPlaying = true
        showMusicDialog = true

        // Sync music state to Firebase Realtime Database
        FirebasePartyRoomService.updateRoomMusic(
          roomId,
          FirebaseRoomMusic(
            title = displayName,
            isPlaying = true,
            position = 0L,
            updatedAt = System.currentTimeMillis(),
            startedBy = userProfile.username.ifBlank { "User" }
          )
        )
        Toast.makeText(context, "Playing: $displayName", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Could not load audio file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val boardWallpaperPicker = rememberLauncherForActivityResult(
    contract = PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      val uriString = it.toString()
      FirebasePartyRoomService.updateRoomPicture(roomId, uriString) { success ->
        if (success) {
          Toast.makeText(context, "Room picture updated!", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // FIREBASE REALTIME DATABASE INITIALIZATION & LISTENERS
  // ---------------------------------------------------------------------------
  DisposableEffect(roomId) {
    val roomListener = FirebasePartyRoomService.joinRoom(
      roomId = roomId,
      hostUid = currentFirebaseUser?.uid ?: currentUserId,
      hostName = userProfile.username.ifBlank { "Host" },
      hostAvatar = userProfile.profileImageUri,
      hostUserId = currentUserId,
      roomTitle = room.title.ifBlank { "Party Board" },
      initialSeatCount = 8
    ) { updatedRoom ->
      roomData = updatedRoom
      val map = mutableMapOf<Int, FirebasePartySeat>()
      updatedRoom.seats.values.forEach { seat ->
        map[seat.seatNumber] = seat
      }
      seatsState = map
    }

    val chatListener = FirebasePartyRoomService.observeMessages(roomId) { msg ->
      if (chatMessages.none { it.messageId == msg.messageId }) {
        chatMessages.add(msg)
      }
    }

    val giftListener = FirebasePartyRoomService.observeGifts(roomId) { gift ->
      val display = ActiveGiftDisplay(
        id = System.currentTimeMillis() + (0..1000).random(),
        emoji = gift.giftEmoji.ifBlank { "🎁" },
        title = gift.giftName.ifBlank { "Gift" },
        sender = gift.senderName.ifBlank { "Viewer" }
      )
      activeGifts.add(display)
      coroutineScope.launch {
        delay(3200)
        activeGifts.remove(display)
      }
    }

    // Join Agora channel for this party room (audience by default, until mic is enabled)
    AgoraVoiceService.joinChannel(
      context = context,
      channelId = "party_$roomId",
      uid = (currentUserId.filter { it.isDigit() }.toIntOrNull() ?: (1000..9999).random()),
      isBroadcaster = false
    )

    onDispose {
      FirebasePartyRoomService.leaveRoom(roomId, currentUserId, roomListener)
      FirebasePartyRoomService.removeMessageObserver(roomId, chatListener)
      FirebasePartyRoomService.removeGiftObserver(roomId, giftListener)
      mediaPlayer?.stop()
      mediaPlayer?.release()
      mediaPlayer = null
      AgoraVoiceService.leaveChannel()
    }
  }

  // Find user's occupied seat number
  val myOccupiedSeat = remember(seatsState, currentUserId, currentFirebaseUser?.uid) {
    val authUid = currentFirebaseUser?.uid
    seatsState.values.find { it.userId == currentUserId || (authUid != null && it.userId == authUid) }
  }

  // Keep Agora microphone muted state in sync with local mic and mute toggles
  LaunchedEffect(isLocalMicActive, isLocalMuted) {
    val shouldMute = !isLocalMicActive || isLocalMuted
    AgoraVoiceService.setLocalMicMuted(shouldMute, context)
    myOccupiedSeat?.let { seat ->
      FirebasePartyRoomService.setSeatMuted(roomId, seat.seatNumber, shouldMute)
    }
  }

  // Keep speaking indicators synced with Agora volume activity
  LaunchedEffect(agoraSpeakingUsers) {
    myOccupiedSeat?.let { seat ->
      val isSpeaking = AgoraVoiceService.isLocalUserSpeaking() && isLocalMicActive && !isLocalMuted
      FirebasePartyRoomService.setSeatSpeaking(roomId, seat.seatNumber, isSpeaking)
    }
  }

  // ---------------------------------------------------------------------------
  // SEAT INTERACTION
  // ---------------------------------------------------------------------------
  fun handleSeatClick(seatNumber: Int) {
    val seat = seatsState[seatNumber] ?: FirebasePartySeat(seatNumber = seatNumber)

    if (seat.isLocked) {
      Toast.makeText(context, "Seat No.$seatNumber is currently locked", Toast.LENGTH_SHORT).show()
      return
    }

    if (seat.isOccupied) {
      // Tapping an occupied seat: opens that user's full real profile card
      val uid = seat.userId ?: currentUserId
      val fullProf = UserProfileStorage.getProfileForUser(context, uid)
      val participant = PartyParticipant(
        userId = uid,
        username = seat.userName?.ifBlank { fullProf.username } ?: fullProf.username,
        avatarUri = seat.userAvatarUri ?: fullProf.profileImageUri,
        frameId = seat.userFrameId,
        frameUrl = seat.userFrameUrl,
        level = if (seat.userLevel > 0) seat.userLevel else fullProf.userLevel.coerceAtLeast(1),
        gender = if (seat.userGender.isNotBlank()) seat.userGender else fullProf.gender,
        country = if (seat.userCountry.isNotBlank()) seat.userCountry else fullProf.country,
        countryFlag = if (seat.userCountryFlag.isNotBlank()) seat.userCountryFlag else fullProf.countryFlag,
        isHost = uid == roomData.hostUid || uid == roomData.hostUserId || (room.creatorUserId.isNotBlank() && uid == room.creatorUserId),
        isMutedOnSeat = seat.isMuted,
        seatNumber = seat.seatNumber
      )
      selectedParticipantProfile = participant
    } else {
      // Empty seat: Check room and seat lock status first
      if (roomData.isLocked && !isOwner) {
        Toast.makeText(context, "The room is currently locked by the host", Toast.LENGTH_SHORT).show()
        return
      }
      if (seat.isLocked && !isOwner) {
        Toast.makeText(context, "Seat No.$seatNumber is locked by the host", Toast.LENGTH_SHORT).show()
        return
      }

      val equippedFrame = com.example.ui.storage.StoreCenterStorage.getEquippedFrame(context, currentUserId)
      val activeFrameId = equippedFrame?.id
      val activeFrameUrl = equippedFrame?.imageUrl

      // 1. Optimistic Local State Update (Instant UI feedback)
      val newSeat = FirebasePartySeat(
        seatNumber = seatNumber,
        isOccupied = true,
        isLocked = false,
        userId = currentUserId,
        userName = userProfile.username.ifBlank { "User" },
        userAvatarUri = userProfile.profileImageUri,
        userFrameId = activeFrameId,
        userFrameUrl = activeFrameUrl,
        userLevel = parsedUserLevel,
        userGender = userProfile.gender,
        userCountry = userProfile.country,
        userCountryFlag = userProfile.countryFlag,
        isSpeaking = false,
        isMuted = isLocalMuted
      )

      val updatedMap = seatsState.toMutableMap()
      // Vacate current user from any other seat first
      val currentAuthUid = currentFirebaseUser?.uid
      updatedMap.forEach { (num, s) ->
        if (s.userId == currentUserId || (currentAuthUid != null && s.userId == currentAuthUid)) {
          updatedMap[num] = s.copy(
            isOccupied = false,
            userId = null,
            userName = null,
            userAvatarUri = null,
            userFrameId = null,
            userFrameUrl = null,
            isSpeaking = false,
            isMuted = false
          )
        }
      }
      updatedMap[seatNumber] = newSeat
      seatsState = updatedMap

      Toast.makeText(context, "You joined Seat No.$seatNumber!", Toast.LENGTH_SHORT).show()
      // Automatically request mic permission if not yet active
      try {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      } catch (e: Exception) {
        Log.w("PartyBoardRoom", "Mic launcher notice: ${e.message}")
      }

      // 2. Background Sync with Firebase Realtime Database
      val firebaseProfile = FirebaseUserProfile(
        uid = currentFirebaseUser?.uid ?: currentUserId,
        userId = currentUserId,
        name = userProfile.username.ifBlank { "User" },
        username = userProfile.username.ifBlank { "User" },
        profileImageUri = userProfile.profileImageUri,
        frameId = activeFrameId,
        frameUrl = activeFrameUrl,
        userLevel = parsedUserLevel,
        gender = userProfile.gender,
        country = userProfile.country,
        countryFlag = userProfile.countryFlag
      )

      FirebasePartyRoomService.takeSeat(roomId, seatNumber, firebaseProfile) { success, errorMsg ->
        if (!success && errorMsg != null) {
          Log.w("PartyBoardRoom", "takeSeat Firebase sync notice: $errorMsg")
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // MAIN FULL-SCREEN LAYOUT
  // ---------------------------------------------------------------------------
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080014))
  ) {
    // 1. FULLSCREEN SCENIC BACKGROUND (Matches Reference Image)
    if (roomData.coverUri != null) {
      AsyncImage(
        model = roomData.coverUri,
        contentDescription = "Party Board Wallpaper",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.bg_party_scenic),
        contentDescription = "Twilight Lake Balcony Scenic Background",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
    }

    // Atmospheric dark gradient overlay for neon contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0x80040010),
              Color(0x22000000),
              Color(0x11000000),
              Color(0x66000000),
              Color(0xEE050014)
            )
          )
        )
    )

    // Outer Diamond-Style Premium Border Frame (Requirement 6 & 28)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(3.dp)
        .border(
          width = 1.3.dp,
          brush = Brush.sweepGradient(
            listOf(
              Color(0xFF00E5FF).copy(alpha = 0.8f),
              Color(0xFF7C4DFF).copy(alpha = 0.6f),
              Color(0xFFFF007F).copy(alpha = 0.8f),
              Color(0xFF00E5FF).copy(alpha = 0.7f),
              Color(0xFF7C4DFF).copy(alpha = 0.8f),
              Color(0xFF00E5FF).copy(alpha = 0.8f)
            )
          ),
          shape = RoundedCornerShape(22.dp)
        )
    )

    // MAIN CONTENT COLUMN
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      // -----------------------------------------------------------------------
      // TOP HEADER: Board Profile Capsule (Left) and Circular Actions + PX (Right)
      // Strictly follows the layout & styling from the reference image
      // -----------------------------------------------------------------------
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        // TOP-LEFT: BOARD PROFILE CAPSULE (Requirements 3, 7, 8, 26)
        // Square Board Picture + Board Name + Room ID Number
        val hostUid = roomData.hostUserId.ifBlank { currentUserId }
        val hostProf = remember(hostUid) { UserProfileStorage.getProfileForUser(context, hostUid) }
        val hostParticipant = PartyParticipant(
          userId = hostUid,
          username = roomData.hostName.ifBlank { hostProf.username },
          avatarUri = roomData.hostAvatarUri ?: hostProf.profileImageUri,
          level = if (hostUid == currentUserId) parsedUserLevel else hostProf.userLevel.coerceAtLeast(1),
          gender = hostProf.gender,
          country = hostProf.country,
          countryFlag = hostProf.countryFlag,
          isHost = true
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD90E0024))
            .border(
              width = 1.4.dp,
              brush = Brush.horizontalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF007F))
              ),
              shape = RoundedCornerShape(16.dp)
            )
            .clickable {
              selectedParticipantProfile = hostParticipant
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("board_profile_header")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Square Board Profile Picture (Must ALWAYS be Square with slight corner rounding, NEVER circular!)
            Box(
              modifier = Modifier
                .size(44.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF260447))
                .border(1.2.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              if (!roomData.coverUri.isNullOrBlank()) {
                AsyncImage(
                  model = roomData.coverUri,
                  contentDescription = "Board Profile Picture",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else if (!roomData.hostAvatarUri.isNullOrBlank()) {
                AsyncImage(
                  model = roomData.hostAvatarUri,
                  contentDescription = "Board Profile Picture",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.MusicNote,
                  contentDescription = "Board Profile Picture",
                  tint = Color(0xFF00E5FF),
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.widthIn(max = 145.dp)) {
              // Real Board Name
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = roomData.roomTitle.ifBlank { "Cyber Sound Lounge" },
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "👑", fontSize = 11.sp)
              }

              Spacer(modifier = Modifier.height(2.dp))

              // Real Room / Board PID Number & Online Count
              Text(
                text = "PID: ${roomData.hostPID.ifBlank { roomData.hostUserId.ifBlank { roomId.takeLast(6) } }} • ${roomData.viewerCount} Online",
                color = Color(0xFF80D8FF),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // TOP-RIGHT: Circular Glass Actions (< , ··· , ⏻) and Diamond PX Pill Button
        Column(horizontalAlignment = Alignment.End) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Share Button
            HeaderCircularButton(
              icon = Icons.Filled.Share,
              contentDesc = "Share Room",
              onClick = {
                Toast.makeText(context, "Share link for ${roomData.roomTitle} copied!", Toast.LENGTH_SHORT).show()
              }
            )

            // More Options Button
            HeaderCircularButton(
              icon = Icons.Filled.MoreHoriz,
              contentDesc = "More Options",
              onClick = { showMenuPanel = !showMenuPanel }
            )

            // Power / Exit Button (Triggers Exit Confirm Dialog)
            HeaderCircularButton(
              icon = Icons.Filled.PowerSettingsNew,
              contentDesc = "Leave Room",
              glowColor = Color(0xFFFF1744),
              onClick = { showExitConfirmDialog = true }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Diamond PX Currency Pill (Matching reference image)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
                )
              )
              .border(
                1.4.dp,
                Brush.horizontalGradient(listOf(Color(0xFFE040FB), Color(0xFFFFD54F))),
                RoundedCornerShape(16.dp)
              )
              .clickable {
                showGiftDialog = true
              }
              .padding(horizontal = 9.dp, vertical = 4.dp)
              .testTag("diamond_px_pill")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = "💎", fontSize = 11.sp)
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "PX $userWalletCoins",
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
              )
            }
          }
        }
      }

      // Room Locked Notification banner if locked
      if (roomData.isLocked) {
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCCB71C1C))
            .padding(horizontal = 10.dp, vertical = 3.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🔒 Room is currently locked by the Host",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // -----------------------------------------------------------------------
      // CENTER SEAT SYSTEM (Default 8 Seats: No.1 to No.8)
      // Arranged in the two-row layout shown in the reference image:
      // Row 1: No.1, No.2, No.3, No.4
      // Row 2: No.5, No.6, No.7, No.8
      // Expandable: 8 -> 12 -> 16
      // -----------------------------------------------------------------------
      val currentSeatCount = roomData.seatCount.coerceIn(8, 16)
      val seatsList = remember(currentSeatCount, seatsState) {
        (1..currentSeatCount).map { seatNum ->
          seatsState[seatNum] ?: FirebasePartySeat(seatNumber = seatNum)
        }
      }
      val seatRows = remember(seatsList) { seatsList.chunked(4) }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        seatRows.forEach { rowSeats ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            rowSeats.forEach { seat ->
              ReferencePartySeat(
                seat = seat,
                isCurrentUsersSeat = seat.userId == currentUserId,
                isLocalSpeaking = isLocalSpeaking,
                isLocalMicActive = isLocalMicActive,
                onClick = { handleSeatClick(seat.seatNumber) }
              )
            }
            if (rowSeats.size < 4) {
              repeat(4 - rowSeats.size) {
                Spacer(modifier = Modifier.size(72.dp))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // -----------------------------------------------------------------------
      // FLOATING GIFT ANIMATION DISPLAY AREA
      // -----------------------------------------------------------------------
      activeGifts.forEach { gift ->
        key(gift.id) {
          FloatingGiftBanner(gift = gift)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // -----------------------------------------------------------------------
      // BOTTOM ACTION BAR: Mute | Mic | Message | Gift | Game | Menu
      // MUST remain at the bottom of the screen.
      // Menu is at extreme bottom-right.
      // Large rounded glass/neon container, glowing circular icons, icon above label.
      // -----------------------------------------------------------------------
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(bottom = 26.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xCC09001D))
            .border(
              width = 1.8.dp,
              brush = Brush.horizontalGradient(
                listOf(
                  Color(0xFF00E5FF),
                  Color(0xFFE040FB),
                  Color(0xFFFF007F),
                  Color(0xFF7C4DFF),
                  Color(0xFF00E5FF)
                )
              ),
              shape = RoundedCornerShape(36.dp)
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("bottom_action_bar")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // 1. MUTE
            BottomBarItem(
              icon = if (isLocalMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
              label = "Mute",
              orbBrush = if (isLocalMuted) Brush.linearGradient(listOf(Color(0xFFB71C1C), Color(0xFF7F0000)))
              else Brush.radialGradient(listOf(Color(0xFFA855F7), Color(0xFF4A148C))),
              borderColor = if (isLocalMuted) Color(0xFFFF5252) else Color(0xFFD500F9),
              onClick = {
                isLocalMuted = !isLocalMuted
                if (isLocalMuted) {
                  mediaPlayer?.setVolume(0f, 0f)
                  AgoraVoiceService.setSpeakerphoneOn(false)
                  Toast.makeText(context, "Audio Muted", Toast.LENGTH_SHORT).show()
                } else {
                  val vol = musicVolume
                  mediaPlayer?.setVolume(vol, vol)
                  AgoraVoiceService.setSpeakerphoneOn(true)
                  Toast.makeText(context, "Audio Unmuted", Toast.LENGTH_SHORT).show()
                }
              }
            )

            // 2. MIC
            BottomBarItem(
              icon = if (isLocalMicActive && !isLocalMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
              label = "Mic",
              orbBrush = if (isLocalMicActive && !isLocalMuted) Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF0070F3)))
              else Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF263238))),
              borderColor = if (isLocalMicActive && !isLocalMuted) Color(0xFF00E5FF) else Color(0x88FFFFFF),
              onClick = {
                if (isLocalMicActive) {
                  isLocalMicActive = false
                  AgoraVoiceService.setLocalMicMuted(true, context)
                  AgoraVoiceService.setClientRole(false, context)
                  Toast.makeText(context, "Microphone Turned Off", Toast.LENGTH_SHORT).show()
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            )

            // 3. MESSAGE
            BottomBarItem(
              icon = Icons.AutoMirrored.Filled.Chat,
              label = "Message",
              orbBrush = Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF00838F))),
              borderColor = Color(0xFF00E5FF),
              onClick = { showChatDialog = true }
            )

            // 4. GIFT
            BottomBarItem(
              icon = Icons.Filled.CardGiftcard,
              label = "Gift",
              orbBrush = Brush.radialGradient(listOf(Color(0xFFFF007F), Color(0xFFC2185B))),
              borderColor = Color(0xFFFF4081),
              onClick = { showGiftDialog = true }
            )

            // 5. GAME
            BottomBarItem(
              icon = Icons.Filled.SportsEsports,
              label = "Game",
              orbBrush = Brush.radialGradient(listOf(Color(0xFF2979FF), Color(0xFF1565C0))),
              borderColor = Color(0xFF00D9FF),
              onClick = { showGameDialog = true }
            )

            // 6. MENU (Extreme bottom-right) - Visible for all users (Requirement 18)
            BottomBarItem(
              icon = Icons.Filled.Menu,
              label = "Menu",
              orbBrush = Brush.radialGradient(listOf(Color(0xFFE040FB), Color(0xFF7B1FA2))),
              borderColor = Color(0xFFE040FB),
              onClick = { showMenuPanel = !showMenuPanel },
              isHighlighted = showMenuPanel
            )
          }
        }
      }
    }

    // -------------------------------------------------------------------------
    // FLOATING MENU PANEL
    // Appears above the bottom action bar from the bottom-right corner.
    // Clean Material 3 vector icon system (No emojis)
    // -------------------------------------------------------------------------
    if (showMenuPanel) {
      // Backdrop click catcher to dismiss
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { showMenuPanel = false }
      )

      // Floating popup aligned above Menu button
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .navigationBarsPadding()
          .padding(end = 12.dp, bottom = 82.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xF514002B))
          .border(
            width = 1.6.dp,
            brush = Brush.verticalGradient(
              listOf(Color(0xFFE040FB), Color(0xFF00E5FF), Color(0xFFFF007F))
            ),
            shape = RoundedCornerShape(20.dp)
          )
          .padding(10.dp)
          .width(190.dp)
          .testTag("floating_menu_panel")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          // 1. Settings (Authorized for Owner only)
          if (isOwner) {
            MenuOptionRow(
              icon = Icons.Filled.Settings,
              title = "Settings",
              iconTint = Color(0xFFE040FB),
              onClick = {
                showMenuPanel = false
                showSettingsDialog = true
              }
            )
          }

          // 2. Music (Available for authorized music playback)
          MenuOptionRow(
            icon = Icons.Filled.MusicNote,
            title = "Music",
            iconTint = Color(0xFFFFD54F),
            onClick = {
              showMenuPanel = false
              showMusicDialog = true
            }
          )

          // 3. Room Lock / Open (Owner only)
          if (isOwner) {
            MenuOptionRow(
              icon = if (roomData.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
              title = if (roomData.isLocked) "Unlock Room" else "Room Lock",
              iconTint = if (roomData.isLocked) Color(0xFF00E676) else Color(0xFFFF5252),
              onClick = {
                showMenuPanel = false
                val newLockState = !roomData.isLocked
                FirebasePartyRoomService.setRoomLock(roomId, newLockState) { success ->
                  if (success) {
                    Toast.makeText(
                      context,
                      if (newLockState) "Room Locked" else "Room Unlocked",
                      Toast.LENGTH_SHORT
                    ).show()
                  }
                }
              }
            )

            // 4. Seat Lock (Owner only)
            MenuOptionRow(
              icon = Icons.Filled.Lock,
              title = "Seat Lock",
              iconTint = Color(0xFFFF5252),
              onClick = {
                showMenuPanel = false
                showSeatLockDialog = true
              }
            )

            // 5. Seat Open (Owner only)
            MenuOptionRow(
              icon = Icons.Filled.LockOpen,
              title = "Seat Open",
              iconTint = Color(0xFF00E676),
              onClick = {
                showMenuPanel = false
                showSeatOpenDialog = true
              }
            )
          }

          // 6. Mute / Unmute Room Audio (Available to all users)
          MenuOptionRow(
            icon = if (isLocalMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
            title = if (isLocalMuted) "Unmute Audio" else "Mute Audio",
            iconTint = if (isLocalMuted) Color(0xFFFF5252) else Color(0xFF00E5FF),
            onClick = {
              showMenuPanel = false
              isLocalMuted = !isLocalMuted
              AgoraVoiceService.setLocalPlaybackMuted(isLocalMuted, context)
              val vol = if (isLocalMuted) 0f else musicVolume
              mediaPlayer?.setVolume(vol, vol)
              Toast.makeText(
                context,
                if (isLocalMuted) "Room audio muted" else "Room audio unmuted",
                Toast.LENGTH_SHORT
              ).show()
            }
          )
        }
      }
    }

    // -------------------------------------------------------------------------
    // DIALOGS & PANELS
    // -------------------------------------------------------------------------

    // 1. SETTINGS DIALOG (Room Name, Room Picture, Seat Count: 8 -> 12 -> 16)
    if (showSettingsDialog) {
      PartySettingsDialog(
        currentTitle = roomData.roomTitle,
        currentSeatCount = roomData.seatCount,
        isOwner = isOwner,
        onDismiss = { showSettingsDialog = false },
        onChangeTitle = { newTitle ->
          FirebasePartyRoomService.updateRoomName(roomId, newTitle) { success ->
            if (success) Toast.makeText(context, "Room name updated!", Toast.LENGTH_SHORT).show()
          }
        },
        onPickPicture = {
          boardWallpaperPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
        onChangeSeatCount = { count ->
          FirebasePartyRoomService.updateSeatCount(roomId, count) { success ->
            if (success) Toast.makeText(context, "Seats updated to $count seats!", Toast.LENGTH_SHORT).show()
          }
        }
      )
    }

    // 2. REAL PHONE MUSIC DIALOG
    if (showMusicDialog) {
      PartyMusicDialog(
        currentTrackTitle = currentMusicTitle,
        isPlaying = isMusicPlaying,
        volume = musicVolume,
        onDismiss = { showMusicDialog = false },
        onSelectTrack = {
          audioPickerLauncher.launch("audio/*")
        },
        onTogglePlayPause = {
          if (mediaPlayer == null) {
            audioPickerLauncher.launch("audio/*")
          } else {
            if (isMusicPlaying) {
              mediaPlayer?.pause()
              isMusicPlaying = false
            } else {
              mediaPlayer?.start()
              isMusicPlaying = true
            }
          }
        },
        onStop = {
          mediaPlayer?.stop()
          mediaPlayer?.prepare()
          isMusicPlaying = false
        },
        onVolumeChange = { vol ->
          musicVolume = vol
          if (!isLocalMuted) {
            mediaPlayer?.setVolume(vol, vol)
          }
        }
      )
    }

    // 3. SEAT LOCK DIALOG
    if (showSeatLockDialog) {
      SeatLockPickerModal(
        seats = (1..roomData.seatCount).map { seatsState[it] ?: FirebasePartySeat(seatNumber = it) },
        title = "Lock a Seat",
        actionText = "Lock",
        onDismiss = { showSeatLockDialog = false },
        onSelectSeat = { seatNum ->
          FirebasePartyRoomService.lockSeat(roomId, seatNum) { success ->
            if (success) {
              Toast.makeText(context, "Seat No.$seatNum is now locked", Toast.LENGTH_SHORT).show()
              showSeatLockDialog = false
            }
          }
        }
      )
    }

    // 4. SEAT OPEN DIALOG
    if (showSeatOpenDialog) {
      SeatLockPickerModal(
        seats = (1..roomData.seatCount).map { seatsState[it] ?: FirebasePartySeat(seatNumber = it) }.filter { it.isLocked },
        title = "Unlock a Seat",
        actionText = "Open",
        emptyMessage = "No seats are currently locked",
        onDismiss = { showSeatOpenDialog = false },
        onSelectSeat = { seatNum ->
          FirebasePartyRoomService.openSeat(roomId, seatNum) { success ->
            if (success) {
              Toast.makeText(context, "Seat No.$seatNum is now open", Toast.LENGTH_SHORT).show()
              showSeatOpenDialog = false
            }
          }
        }
      )
    }

    // 5. CHAT DIALOG (Message)
    if (showChatDialog) {
      PartyChatDialog(
        messages = chatMessages,
        onDismiss = { showChatDialog = false },
        onSendMessage = { text ->
          FirebasePartyRoomService.sendMessage(
            roomId = roomId,
            senderUid = currentUserId,
            senderName = userProfile.username.ifBlank { "User" },
            senderAvatar = userProfile.profileImageUri,
            senderLevel = parsedUserLevel,
            text = text
          )
        }
      )
    }

    // 6. REAL GIFT DIALOG
    if (showGiftDialog) {
      PartyRealGiftDialog(
        userCoinBalance = userWalletCoins,
        onDismiss = { showGiftDialog = false },
        onSendGift = { name, emoji, cost ->
          if (userWalletCoins < cost) {
            Toast.makeText(
              context,
              "Insufficient coins! You need ${cost - userWalletCoins} more coins.",
              Toast.LENGTH_SHORT
            ).show()
          } else {
            val curCoins = UserProfileStorage.getCoinBalance(context, currentUserId)
            val newBal = (curCoins - cost).coerceAtLeast(0L)
            UserProfileStorage.setCoinBalance(context, newBal, currentUserId)
            userWalletCoins = newBal
            FirebasePartyRoomService.sendGiftEvent(
              roomId = roomId,
              senderUid = currentUserId,
              senderName = userProfile.username.ifBlank { "User" },
              senderAvatar = userProfile.profileImageUri,
              giftName = name,
              giftEmoji = emoji,
              coinCost = cost.toLong()
            )
            Toast.makeText(context, "Sent $name ($emoji)!", Toast.LENGTH_SHORT).show()
            showGiftDialog = false
          }
        }
      )
    }

    // 7. REAL GAME DIALOG (Clean Empty State)
    if (showGameDialog) {
      PartyGameDialog(
        onDismiss = { showGameDialog = false }
      )
    }

    // 8. EXIT CONFIRMATION DIALOG (Keep vs Exit matching reference design)
    if (showExitConfirmDialog) {
      RoomKeepExitDialog(
        onKeep = {
          showExitConfirmDialog = false
          Toast.makeText(context, "Party room kept active", Toast.LENGTH_SHORT).show()
        },
        onExit = {
          showExitConfirmDialog = false
          Toast.makeText(context, "Left party room", Toast.LENGTH_SHORT).show()
          onDismiss()
        },
        onDismiss = { showExitConfirmDialog = false }
      )
    }

    // 9. USER PROFILE DIALOG (Shows real user ID, Level, Gender, Country and Leave Seat button)
    selectedParticipantProfile?.let { participant ->
      PartyUserProfileDialog(
        targetUser = participant,
        currentUserId = currentUserId,
        isViewerOwner = isOwner,
        viewerAdminPermissions = null,
        onDismiss = { selectedParticipantProfile = null },
        onSendMessage = { _ ->
          selectedParticipantProfile = null
          showChatDialog = true
        },
        onRemoveFromSeat = { p ->
          p.seatNumber?.let { seatNum ->
            val updatedMap = seatsState.toMutableMap()
            updatedMap[seatNum] = FirebasePartySeat(seatNumber = seatNum, isOccupied = false, isLocked = false)
            seatsState = updatedMap
            FirebasePartyRoomService.vacateSeat(roomId, seatNum)
            Toast.makeText(context, "${p.username} left Seat No.$seatNum", Toast.LENGTH_SHORT).show()
          }
          selectedParticipantProfile = null
        },
        onMuteParticipant = { p, newMuteState ->
          p.seatNumber?.let { seatNum ->
            val updatedMap = seatsState.toMutableMap()
            seatsState[seatNum]?.let { s ->
              updatedMap[seatNum] = s.copy(isMuted = newMuteState)
              seatsState = updatedMap
            }
            FirebasePartyRoomService.setSeatMuted(roomId, seatNum, newMuteState)
          }
        },
        onKickParticipant = { p ->
          p.seatNumber?.let { seatNum ->
            val updatedMap = seatsState.toMutableMap()
            updatedMap[seatNum] = FirebasePartySeat(seatNumber = seatNum, isOccupied = false, isLocked = false)
            seatsState = updatedMap
            FirebasePartyRoomService.vacateSeat(roomId, seatNum)
          }
          Toast.makeText(context, "${p.username} removed", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }
}

// =============================================================================
// SEAT COMPOSABLE MATCHING THE UPLOADED REFERENCE IMAGE
// Concentric glowing glass circle + ||| 🎤 ||| audio wave bars inside empty seat
// Number pill below: No.1, No.2, etc. with matching neon border
// Real user avatar, name, and speaking wave when occupied
// =============================================================================
@Composable
private fun ReferencePartySeat(
  seat: FirebasePartySeat,
  isCurrentUsersSeat: Boolean,
  isLocalSpeaking: Boolean = false,
  isLocalMicActive: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val theme = getSeatTheme(seat.seatNumber)
  val infiniteTransition = rememberInfiniteTransition(label = "seat_pulse")

  val isActuallySpeaking = seat.isSpeaking || (isCurrentUsersSeat && isLocalSpeaking && isLocalMicActive && !seat.isMuted)

  // Subtle blue audio glow when speaking (Requirement 19: never changes seat size)
  val blueGlowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "blue_glow_alpha"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .width(78.dp) // Fixed dedicated slot width per seat so long names never resize or shift seats!
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick() }
      .testTag("party_seat_${seat.seatNumber}")
  ) {
    // 1. FIXED SIZE CIRCULAR SEAT (Strictly 60.dp, NEVER resizes or scales)
    Box(
      modifier = Modifier.size(60.dp),
      contentAlignment = Alignment.Center
    ) {
      // Subtle Blue Audio Glow Ring (Only active when speaking, strictly contained, never resizes seat)
      if (isActuallySpeaking) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .border(
              width = 2.dp,
              color = Color(0xFF00E5FF).copy(alpha = blueGlowAlpha),
              shape = CircleShape
            )
        )
      }

      // Main concentric glass orb (Fixed size 54.dp)
      Box(
        modifier = Modifier
          .size(54.dp)
          .shadow(
            elevation = if (isActuallySpeaking) 8.dp else 4.dp,
            shape = CircleShape,
            spotColor = if (isActuallySpeaking) Color(0xFF00E5FF) else theme.glowColor
          )
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                theme.primaryColor.copy(alpha = 0.85f),
                theme.secondaryColor.copy(alpha = 0.95f),
                Color(0xFF0D0022)
              )
            )
          )
          .border(
            width = 1.8.dp,
            brush = if (isActuallySpeaking) {
              Brush.sweepGradient(
                listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF00E5FF))
              )
            } else {
              theme.borderBrush
            },
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        if (seat.isLocked) {
          // Locked Seat Indicator
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Seat Locked",
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(22.dp)
          )
        } else if (seat.isOccupied) {
          // REAL OCCUPIED USER (Real photo, real active frame, corner mic badge)
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            AvatarFrameOverlay(
              size = 46.dp,
              frameId = seat.userFrameId,
              frameUrl = seat.userFrameUrl,
              profileImageUri = seat.userAvatarUri
            )

            // Small microphone status badge at the bottom corner of the avatar (Requirement 18)
            Box(
              modifier = Modifier
                .size(15.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(
                  if (seat.isMuted) Color(0xFFD50000) else Color(0xFF00C853)
                )
                .border(1.dp, Color.White, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (seat.isMuted) {
                Icon(
                  imageVector = Icons.Filled.MicOff,
                  contentDescription = "Mic Muted",
                  tint = Color.White,
                  modifier = Modifier.size(8.5.dp)
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.Mic,
                  contentDescription = "Mic Open",
                  tint = Color.White,
                  modifier = Modifier.size(8.5.dp)
                )
              }
            }
          }
        } else {
          // EMPTY SEAT WITH AUDIO WAVE BARS: ||| 🎤 |||
          // Matching the reference image
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            AudioWaveBars(color = Color.White.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Empty Seat Mic",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            AudioWaveBars(color = Color.White.copy(alpha = 0.85f))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // 2. SEAT NUMBER OR USER NAME PILL (Below the seat, as indicated by green circle)
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xCC0D0022))
        .border(
          width = 1.dp,
          brush = if (seat.isOccupied) {
            Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFBD00FF)))
          } else {
            theme.borderBrush
          },
          shape = RoundedCornerShape(10.dp)
        )
        .padding(horizontal = 7.dp, vertical = 2.dp),
      contentAlignment = Alignment.Center
    ) {
      if (seat.isLocked) {
        Text(
          text = "Locked",
          color = Color(0xFFFF5252),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold
        )
      } else if (seat.isOccupied) {
        Text(
          text = seat.userName ?: "User",
          color = if (isCurrentUsersSeat) Color(0xFF00E5FF) else Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      } else {
        Text(
          text = "No.${seat.seatNumber}",
          color = Color.White,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * 3 Subtle Audio Wave Bars ||| inside empty seat circle
 */
@Composable
private fun AudioWaveBars(color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
  ) {
    Box(
      modifier = Modifier
        .width(2.dp)
        .height(8.dp)
        .clip(RoundedCornerShape(1.dp))
        .background(color)
    )
    Box(
      modifier = Modifier
        .width(2.dp)
        .height(13.dp)
        .clip(RoundedCornerShape(1.dp))
        .background(color)
    )
    Box(
      modifier = Modifier
        .width(2.dp)
        .height(8.dp)
        .clip(RoundedCornerShape(1.dp))
        .background(color)
    )
  }
}

// =============================================================================
// BOTTOM BAR ITEM COMPOSABLE
// =============================================================================
@Composable
private fun BottomBarItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  orbBrush: Brush,
  borderColor: Color,
  onClick: () -> Unit,
  isHighlighted: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, label = "press_scale")

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) { onClick() }
      .padding(horizontal = 4.dp, vertical = 2.dp)
      .testTag("bottom_bar_${label.lowercase()}")
  ) {
    // Faceted Glowing Cyber Button (Matching Reference Image)
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(13.dp))
        .background(orbBrush)
        .border(
          width = if (isHighlighted) 2.dp else 1.5.dp,
          color = if (isHighlighted) Color.White else borderColor,
          shape = RoundedCornerShape(13.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(21.dp)
      )
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Label
    Text(
      text = label,
      color = if (isHighlighted) Color(0xFF00E5FF) else Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

// =============================================================================
// MENU OPTION ROW COMPOSABLE (Clean Vector Icon System, No Emojis)
// =============================================================================
@Composable
private fun MenuOptionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  iconTint: Color = Color(0xFF00E5FF),
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 7.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(iconTint.copy(alpha = 0.18f))
        .border(0.8.dp, iconTint.copy(alpha = 0.4f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconTint,
        modifier = Modifier.size(15.dp)
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = title,
      color = Color.White,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

// =============================================================================
// TOP HEADER CIRCULAR ACTION BUTTON
// =============================================================================
@Composable
private fun HeaderCircularButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDesc: String,
  glowColor: Color = Color(0x66FFFFFF),
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(CircleShape)
      .background(Color(0x66000000))
      .border(1.2.dp, glowColor, CircleShape)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDesc,
      tint = Color.White,
      modifier = Modifier.size(18.dp)
    )
  }
}

// =============================================================================
// FLOATING GIFT BANNER ANIMATION
// =============================================================================
@Composable
private fun FloatingGiftBanner(gift: ActiveGiftDisplay) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.horizontalGradient(
          listOf(Color(0xEE880E4F), Color(0xEE4A148C), Color(0xEE006064))
        )
      )
      .border(
        1.5.dp,
        Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF007F))),
        RoundedCornerShape(18.dp)
      )
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(text = gift.emoji, fontSize = 28.sp)
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "${gift.sender} sent ${gift.title}!",
          color = Color(0xFFFFD700),
          fontSize = 13.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = "Party celebration event 🌟",
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 10.sp
        )
      }
    }
  }
}

// =============================================================================
// SETTINGS DIALOG (Real Room Name, Real Room Picture, 8 -> 12 -> 16 Seats)
// =============================================================================
@Composable
private fun PartySettingsDialog(
  currentTitle: String,
  currentSeatCount: Int,
  isOwner: Boolean,
  onDismiss: () -> Unit,
  onChangeTitle: (String) -> Unit,
  onPickPicture: () -> Unit,
  onChangeSeatCount: (Int) -> Unit
) {
  var titleText by remember { mutableStateOf(currentTitle) }
  var selectedSeats by remember { mutableIntStateOf(currentSeatCount) }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF1B0033), Color(0xFF0C001C)))
        )
        .border(
          1.6.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFE040FB))),
          RoundedCornerShape(24.dp)
        )
        .padding(18.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Room Settings",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isOwner) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x33FF5252))
              .padding(8.dp)
          ) {
            Text(
              text = "Only the room owner or admin can edit room settings.",
              color = Color(0xFFFF8A80),
              fontSize = 11.sp
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // 1. Room Name
        Text(text = "Room Name", color = Color(0xFFE0F7FA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = titleText,
          onValueChange = { if (isOwner) titleText = it },
          enabled = isOwner,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color(0x66FFFFFF)
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Room Picture
        Text(text = "Room Wallpaper / Picture", color = Color(0xFFE0F7FA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = { if (isOwner) onPickPicture() },
          enabled = isOwner,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF00E5FF))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Select Picture from Phone", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Seat Count (8 -> 12 -> 16)
        Text(text = "Number of Seats", color = Color(0xFFE0F7FA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(8, 12, 16).forEach { count ->
            val isSelected = selectedSeats == count
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xFF7C4DFF) else Color(0x22FFFFFF))
                .border(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) Color(0xFFE040FB) else Color(0x44FFFFFF),
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = isOwner) {
                  selectedSeats = count
                }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$count Seats",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Save Button
        if (isOwner) {
          Button(
            onClick = {
              if (titleText.isNotBlank()) onChangeTitle(titleText.trim())
              if (selectedSeats != currentSeatCount) onChangeSeatCount(selectedSeats)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// =============================================================================
// REAL PHONE MUSIC DIALOG (Plays real audio file from device, volume, controls)
// =============================================================================
@Composable
private fun PartyMusicDialog(
  currentTrackTitle: String?,
  isPlaying: Boolean,
  volume: Float,
  onDismiss: () -> Unit,
  onSelectTrack: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onStop: () -> Unit,
  onVolumeChange: (Float) -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF1B0033), Color(0xFF0C001C)))
        )
        .border(
          1.6.dp,
          Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF6F00))),
          RoundedCornerShape(24.dp)
        )
        .padding(18.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = null,
              tint = Color(0xFFFFD54F),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Room Music", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display real selected music track
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33FFFFFF))
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = currentTrackTitle ?: "No audio track selected yet",
            color = Color(0xFFFFD54F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Button to pick real audio file from phone
        Button(
          onClick = onSelectTrack,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFB300)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFFFFD54F))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Select Audio File from Phone", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Play, Pause, Stop Controls
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Play / Pause
          IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Color(0xFFFFB300))
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = Color.Black,
              modifier = Modifier.size(28.dp)
            )
          }

          // Stop
          IconButton(
            onClick = onStop,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Color(0x33FFFFFF))
              .border(1.dp, Color(0x66FFFFFF), CircleShape)
          ) {
            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(24.dp))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Volume Slider
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFFFFD54F),
              activeTrackColor = Color(0xFFFFD54F),
              inactiveTrackColor = Color(0x44FFFFFF)
            )
          )
        }
      }
    }
  }
}

// =============================================================================
// SEAT LOCK / SEAT OPEN PICKER MODAL
// =============================================================================
@Composable
private fun SeatLockPickerModal(
  seats: List<FirebasePartySeat>,
  title: String,
  actionText: String,
  emptyMessage: String = "No available seats",
  onDismiss: () -> Unit,
  onSelectSeat: (Int) -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF1B0033), Color(0xFF0C001C)))
        )
        .border(
          1.6.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F))),
          RoundedCornerShape(24.dp)
        )
        .padding(18.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (seats.isEmpty()) {
          Text(
            text = emptyMessage,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        } else {
          val chunked = seats.chunked(4)
          chunked.forEach { rowSeats ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              rowSeats.forEach { seat ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .border(
                      1.dp,
                      if (seat.isLocked) Color(0xFFFF5252) else Color(0xFF00E5FF),
                      RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectSeat(seat.seatNumber) }
                    .padding(vertical = 10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = "No.${seat.seatNumber}",
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = actionText,
                      color = if (actionText == "Lock") Color(0xFFFF5252) else Color(0xFF00E676),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
              if (rowSeats.size < 4) {
                repeat(4 - rowSeats.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }
  }
}

// =============================================================================
// REAL CHAT DIALOG (Message)
// Real messages stored in Firebase Realtime Database
// =============================================================================
@Composable
private fun PartyChatDialog(
  messages: List<FirebaseChatMessage>,
  onDismiss: () -> Unit,
  onSendMessage: (String) -> Unit
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(420.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF140026), Color(0xFF0A0014)))
        )
        .border(
          1.5.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFE040FB))),
          RoundedCornerShape(24.dp)
        )
        .padding(14.dp)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "💬 Room Chat", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages list
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (messages.isEmpty()) {
            item {
              Text(
                text = "No messages yet. Say hello to everyone!",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 16.dp)
              )
            }
          }
          items(messages) { msg ->
            Row(verticalAlignment = Alignment.Top) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .border(1.dp, Color(0xFF00E5FF), CircleShape)
              ) {
                if (msg.senderAvatarUri != null) {
                  AsyncImage(
                    model = msg.senderAvatarUri,
                    contentDescription = msg.senderName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                  )
                } else {
                  Image(
                    painter = painterResource(id = R.drawable.img_profile_max),
                    contentDescription = msg.senderName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                  )
                }
              }

              Spacer(modifier = Modifier.width(6.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = msg.senderName.ifBlank { "User" },
                    color = Color(0xFF80D8FF),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Lv.${msg.senderLevel}",
                    color = Color(0xFFFFD54F),
                    fontSize = 8.5.sp
                  )
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(text = msg.content, color = Color.White, fontSize = 11.sp)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(Color(0x33FFFFFF))
              .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(20.dp))
              .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            BasicTextField(
              value = inputText,
              onValueChange = { inputText = it },
              textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
              cursorBrush = SolidColor(Color(0xFF00E5FF)),
              modifier = Modifier.fillMaxWidth(),
              decorationBox = { inner ->
                if (inputText.isEmpty()) {
                  Text("Say something...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                inner()
              }
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = {
              if (inputText.isNotBlank()) {
                onSendMessage(inputText.trim())
                inputText = ""
              }
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Color(0xFF00E5FF))
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

// =============================================================================
// REAL GIFT DIALOG
// Checks real coin balance, deducts real coins, and triggers real gift event
// =============================================================================
@Composable
private fun PartyRealGiftDialog(
  userCoinBalance: Long,
  onDismiss: () -> Unit,
  onSendGift: (name: String, emoji: String, cost: Int) -> Unit
) {
  val realGifts = listOf(
    Triple("Rose", "🌹", 10),
    Triple("Heart", "💖", 50),
    Triple("Crown", "👑", 100),
    Triple("Rocket", "🚀", 500),
    Triple("Sports Car", "🏎️", 1000),
    Triple("Galaxy Castle", "🏰", 2500)
  )

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF22003D), Color(0xFF0D001C)))
        )
        .border(
          1.6.dp,
          Brush.horizontalGradient(listOf(Color(0xFFFF007F), Color(0xFFFFD700))),
          RoundedCornerShape(24.dp)
        )
        .padding(18.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.CardGiftcard,
              contentDescription = null,
              tint = Color(0xFFFF007F),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Send Gifts", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.WorkspacePremium,
              contentDescription = null,
              tint = Color(0xFFFFD700),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "$userCoinBalance",
              color = Color(0xFFFFD700),
              fontSize = 13.sp,
              fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val chunked = realGifts.chunked(3)
        chunked.forEach { rowGifts ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowGifts.forEach { (name, emoji, cost) ->
              val canAfford = userCoinBalance >= cost
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (canAfford) Color(0x33FFFFFF) else Color(0x11FFFFFF))
                  .border(
                    1.dp,
                    if (canAfford) Color(0x66FF007F) else Color(0x22FFFFFF),
                    RoundedCornerShape(14.dp)
                  )
                  .clickable { onSendGift(name, emoji, cost) }
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = emoji, fontSize = 28.sp)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = name,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "🪙 $cost",
                    color = if (canAfford) Color(0xFFFFD700) else Color(0x88FFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

// =============================================================================
// REAL GAME DIALOG (Clean Empty State - No fake games)
// =============================================================================
@Composable
private fun PartyGameDialog(onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(listOf(Color(0xFF16002E), Color(0xFF080014)))
        )
        .border(
          1.5.dp,
          Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF2979FF))),
          RoundedCornerShape(24.dp)
        )
        .padding(20.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "🎮 Mini-Games", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x3300E5FF)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.SportsEsports,
            contentDescription = "Games",
            tint = Color(0xFF00E5FF),
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "No mini-games currently hosted in this room.",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Interactive games and tournaments will appear here when configured by room administrators.",
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 11.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Got It", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
