package com.example.agora

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.ContextCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Agora Voice Service managing voice channel connection, audio publishing,
 * microphone mute/unmute, speakerphone, and event callbacks.
 */
object AgoraVoiceService {
  private const val TAG = "AgoraVoiceService"
  const val AGORA_APP_ID = "d445b636ff23404d9972913f12258472"

  private var rtcEngine: RtcEngine? = null

  private val _isJoined = MutableStateFlow(false)
  val isJoined: StateFlow<Boolean> = _isJoined.asStateFlow()

  private val _channelName = MutableStateFlow<String?>(null)
  val channelName: StateFlow<String?> = _channelName.asStateFlow()

  private val _isLocalMicMuted = MutableStateFlow(true)
  val isLocalMicMuted: StateFlow<Boolean> = _isLocalMicMuted.asStateFlow()

  private val _isSpeakerphoneOn = MutableStateFlow(true)
  val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

  // Track remote user speaking status or UIDs
  private val _speakingUsers = MutableStateFlow<Set<Int>>(emptySet())
  val speakingUsers: StateFlow<Set<Int>> = _speakingUsers.asStateFlow()

  private val _isLocalSpeaking = MutableStateFlow(false)
  val isLocalSpeaking: StateFlow<Boolean> = _isLocalSpeaking.asStateFlow()

  private var localUid: Int = 0

  private val _remoteUsersInChannel = MutableStateFlow<Set<Int>>(emptySet())
  val remoteUsersInChannel: StateFlow<Set<Int>> = _remoteUsersInChannel.asStateFlow()

  private val rtcEventHandler = object : IRtcEngineEventHandler() {
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
      Log.d(TAG, "Successfully joined Agora voice channel: $channel with uid: $uid")
      _isJoined.value = true
      _channelName.value = channel
      localUid = uid
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
      Log.d(TAG, "Remote user joined Agora voice channel: $uid")
      _remoteUsersInChannel.value = _remoteUsersInChannel.value + uid
    }

    override fun onUserOffline(uid: Int, reason: Int) {
      Log.d(TAG, "Remote user left Agora voice channel: $uid, reason: $reason")
      _remoteUsersInChannel.value = _remoteUsersInChannel.value - uid
      _speakingUsers.value = _speakingUsers.value - uid
    }

    override fun onAudioVolumeIndication(
      speakers: Array<out AudioVolumeInfo>?,
      totalVolume: Int
    ) {
      speakers?.let { list ->
        val activeSpeakers = mutableSetOf<Int>()
        var selfSpeaking = false
        for (info in list) {
          if (info.volume > 10) {
            if (info.uid == 0 || info.uid == localUid) {
              selfSpeaking = true
            } else {
              activeSpeakers.add(info.uid)
            }
          }
        }
        _isLocalSpeaking.value = selfSpeaking
        _speakingUsers.value = activeSpeakers
      }
    }

    override fun onError(err: Int) {
      when (err) {
        Constants.ERR_INVALID_TOKEN -> { // Error 110: Token is invalid or App Certificate required
          Log.w(
            TAG,
            "Agora RTC Token mode required (Error 110). Voice room running in local/preview mode until token server or App Certificate is configured."
          )
        }
        Constants.ERR_TOKEN_EXPIRED -> { // Error 109: Token expired
          Log.w(TAG, "Agora RTC Token expired (Error 109).")
        }
        Constants.ERR_NOT_READY, Constants.ERR_NOT_INITIALIZED -> {
          Log.w(TAG, "Agora RTC not ready yet (Error $err).")
        }
        else -> {
          Log.e(TAG, "Agora RTC Error code: $err")
        }
      }
    }
  }

  /**
   * Pre-configures internal SQLite caching tables used by Agora's Media Content Center (MCC).
   * Ensures the android_metadata table has expected columns (key, value, expired) to prevent
   * SQLiteLog: (1) no such column: key in "select key, value, expired from android_metadata"
   */
  private fun ensureMetadataTableCompatibility(context: Context) {
    val dbNames = listOf("agora_cache.db", "songcodejsonoption_v3.db", "musiccontentcenter_v4.db")
    for (name in dbNames) {
      try {
        val dbFile = context.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT, key TEXT, value TEXT, expired INTEGER)"
          )
          val cursor = db.rawQuery("PRAGMA table_info(android_metadata)", null)
          val existingCols = mutableSetOf<String>()
          cursor.use {
            while (it.moveToNext()) {
              val colName = it.getString(1)
              existingCols.add(colName)
            }
          }
          if (!existingCols.contains("key")) {
            try { db.execSQL("ALTER TABLE android_metadata ADD COLUMN key TEXT") } catch (_: Exception) {}
          }
          if (!existingCols.contains("value")) {
            try { db.execSQL("ALTER TABLE android_metadata ADD COLUMN value TEXT") } catch (_: Exception) {}
          }
          if (!existingCols.contains("expired")) {
            try { db.execSQL("ALTER TABLE android_metadata ADD COLUMN expired INTEGER") } catch (_: Exception) {}
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Notice on metadata table setup for $name: ${e.message}")
      }
    }
  }

  /**
   * Initializes RtcEngine instance if not already initialized.
   */
  fun initialize(context: Context) {
    if (rtcEngine != null) return

    try {
      // 1. Ensure SQLite compatibility to prevent android_metadata query errors
      ensureMetadataTableCompatibility(context)

      val config = RtcEngineConfig().apply {
        mContext = context.applicationContext
        mAppId = AGORA_APP_ID
        mEventHandler = rtcEventHandler
        mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
      }

      rtcEngine = RtcEngine.create(config).apply {
        // Voice chat optimization
        enableAudio()
        // Default to Audience and disable local audio recording until user takes a seat with mic permission.
        // This avoids AudioFlinger record track creation failure when RECORD_AUDIO permission is not yet granted.
        enableLocalAudio(false)
        muteLocalAudioStream(true)
        setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
        enableAudioVolumeIndication(200, 3, true)
        setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_STANDARD, Constants.AUDIO_SCENARIO_GAME_STREAMING)
        setEnableSpeakerphone(true)
      }
      Log.d(TAG, "Agora RtcEngine initialized successfully with App ID: $AGORA_APP_ID")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize Agora RtcEngine: ${e.message}", e)
    }
  }

  /**
   * Joins a voice channel for the room.
   * @param channelId Unique identifier for the audio room/party board.
   * @param uid 0 for auto-assigned Agora UID, or a specific integer uid.
   * @param isBroadcaster true if user has taken a seat/microphone, false if in audience.
   */
  fun joinChannel(
    context: Context,
    channelId: String,
    uid: Int = 0,
    isBroadcaster: Boolean = false
  ) {
    initialize(context)
    val engine = rtcEngine ?: return

    try {
      val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED

      val canBroadcast = isBroadcaster && hasMicPermission
      val role = if (canBroadcast) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
      engine.setClientRole(role)

      if (canBroadcast) {
        engine.enableLocalAudio(!_isLocalMicMuted.value)
        engine.muteLocalAudioStream(_isLocalMicMuted.value)
      } else {
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
      }

      // Clean channel name: Agora channel names accept letters, numbers, and certain symbols
      val safeChannel = channelId.replace(Regex("[^a-zA-Z0-9_]"), "_").take(64)

      // Join channel with null token (App ID mode or test token)
      engine.joinChannel(null, safeChannel, "", uid)
      Log.d(TAG, "Joining Agora channel: $safeChannel with role: $role")
    } catch (e: Exception) {
      Log.e(TAG, "Error joining Agora channel: ${e.message}", e)
    }
  }

  /**
   * Updates user role dynamically (e.g. taking seat = Broadcaster, leaving seat = Audience).
   */
  fun setClientRole(isBroadcaster: Boolean, context: Context? = null) {
    rtcEngine?.let { engine ->
      val hasMicPermission = if (context != null) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
      } else false

      val canBroadcast = isBroadcaster && hasMicPermission
      val role = if (canBroadcast) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
      engine.setClientRole(role)

      if (canBroadcast) {
        engine.enableLocalAudio(!_isLocalMicMuted.value)
        engine.muteLocalAudioStream(_isLocalMicMuted.value)
      } else {
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
      }
    }
  }

  /**
   * Toggles local microphone mute/unmute safely with permission checks.
   */
  fun setLocalMicMuted(muted: Boolean, context: Context? = null) {
    _isLocalMicMuted.value = muted
    rtcEngine?.let { engine ->
      val hasMicPermission = if (context != null) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
      } else false

      if (!muted && hasMicPermission) {
        engine.enableLocalAudio(true)
        engine.muteLocalAudioStream(false)
      } else {
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
      }
      Log.d(TAG, "Agora local microphone muted: $muted")
    }
  }

  /**
   * Mutes or unmutes playback of all remote audio streams.
   */
  fun setLocalPlaybackMuted(muted: Boolean, context: Context? = null) {
    rtcEngine?.muteAllRemoteAudioStreams(muted)
  }

  /**
   * Toggles speakerphone on/off.
   */
  fun setSpeakerphoneOn(enabled: Boolean) {
    rtcEngine?.let { engine ->
      engine.setEnableSpeakerphone(enabled)
      _isSpeakerphoneOn.value = enabled
      Log.d(TAG, "Agora speakerphone enabled: $enabled")
    }
  }

  /**
   * Returns whether the local user is currently speaking.
   */
  fun isLocalUserSpeaking(): Boolean = _isLocalSpeaking.value

  /**
   * Leaves the current voice channel.
   */
  fun leaveChannel() {
    try {
      rtcEngine?.let { engine ->
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
        engine.leaveChannel()
      }
      _isJoined.value = false
      _channelName.value = null
      _speakingUsers.value = emptySet()
      _remoteUsersInChannel.value = emptySet()
      Log.d(TAG, "Left Agora channel")
    } catch (e: Exception) {
      Log.e(TAG, "Error leaving Agora channel: ${e.message}", e)
    }
  }

  /**
   * Releases RtcEngine resources.
   */
  fun destroy() {
    leaveChannel()
    try {
      RtcEngine.destroy()
      rtcEngine = null
    } catch (e: Exception) {
      Log.e(TAG, "Error destroying RtcEngine: ${e.message}", e)
    }
  }
}

