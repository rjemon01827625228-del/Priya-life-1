package com.example.ui.storage

import android.content.Context
import android.content.SharedPreferences

data class SavedAudioRoom(
  val title: String,
  val coverUriString: String?,
  val country: String,
  val flagEmoji: String,
  val id: String = "",
  val creatorUserId: String = ""
)

object UserRoomStorage {
  private const val PREFS_NAME = "cyber_party_room_prefs"
  private const val KEY_IS_CREATED = "is_audio_room_created"
  private const val KEY_TITLE = "audio_room_title"
  private const val KEY_COVER_URI = "audio_room_cover_uri"
  private const val KEY_COUNTRY = "audio_room_country"
  private const val KEY_FLAG = "audio_room_flag"
  private const val KEY_CREATOR_UID = "audio_room_creator_uid"

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isRoomCreated(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_IS_CREATED, false)
  }

  fun getSavedRoom(context: Context): SavedAudioRoom {
    val prefs = getPrefs(context)
    val title = prefs.getString(KEY_TITLE, "My Neon Party Room") ?: "My Neon Party Room"
    val coverUri = prefs.getString(KEY_COVER_URI, null)
    val country = prefs.getString(KEY_COUNTRY, "Bangladesh") ?: "Bangladesh"
    val flag = prefs.getString(KEY_FLAG, "🇧🇩") ?: "🇧🇩"
    val creatorUid = prefs.getString(KEY_CREATOR_UID, UserProfileStorage.getActiveUserId(context)) ?: ""
    return SavedAudioRoom(
      id = "party_room_$creatorUid",
      title = title,
      coverUriString = coverUri,
      country = country,
      flagEmoji = flag,
      creatorUserId = creatorUid
    )
  }

  fun saveRoom(context: Context, title: String, coverUriString: String?, country: String, flagEmoji: String, creatorUserId: String = "") {
    val uid = if (creatorUserId.isNotBlank()) creatorUserId else UserProfileStorage.getActiveUserId(context)
    val roomId = "party_room_$uid"
    getPrefs(context).edit()
      .putBoolean(KEY_IS_CREATED, true)
      .putString(KEY_TITLE, title.ifBlank { "My Neon Party Room" })
      .putString(KEY_COVER_URI, coverUriString)
      .putString(KEY_COUNTRY, country)
      .putString(KEY_FLAG, flagEmoji)
      .putString(KEY_CREATOR_UID, uid)
      .apply()

    // Publish to Firebase Realtime Database shared registry
    try {
      val userProfile = UserProfileStorage.getProfileForUser(context, uid)
      val initialSeats = mutableMapOf<String, com.example.firebase.model.FirebasePartySeat>()
      for (i in 1..8) {
        initialSeats["seat_$i"] = com.example.firebase.model.FirebasePartySeat(
          seatNumber = i,
          isOccupied = false,
          isLocked = false
        )
      }
      val partyRoom = com.example.firebase.model.FirebasePartyRoomData(
        roomId = roomId,
        roomTitle = title.ifBlank { "My Neon Party Room" },
        hostUid = uid,
        hostPID = uid,
        hostName = userProfile.username.ifBlank { "Host" },
        hostAvatarUri = coverUriString ?: userProfile.profileImageUri,
        hostUserId = uid,
        coverUri = coverUriString,
        country = country,
        countryCode = if (country.length >= 2) country.take(2).uppercase() else "BD",
        flagEmoji = flagEmoji,
        isLocked = false,
        seatCount = 8,
        seats = initialSeats,
        viewerCount = 1,
        active = true,
        lastActivityTimestamp = System.currentTimeMillis()
      )
      com.example.firebase.FirebaseGlobalRoomManager.publishPartyRoom(partyRoom)
    } catch (e: Exception) {
      android.util.Log.w("UserRoomStorage", "Failed to publish party room: ${e.message}")
    }
  }

  fun updateRoom(context: Context, newTitle: String, newCoverUriString: String?) {
    val editor = getPrefs(context).edit()
    if (newTitle.isNotBlank()) {
      editor.putString(KEY_TITLE, newTitle)
    }
    if (newCoverUriString != null) {
      editor.putString(KEY_COVER_URI, newCoverUriString)
    }
    editor.apply()
  }
}
