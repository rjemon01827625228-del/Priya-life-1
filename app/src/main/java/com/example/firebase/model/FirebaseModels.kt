package com.example.firebase.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Firebase Realtime Database POJO model for User Profile.
 * Supports default no-arg constructor required by Firebase serialization.
 */
@IgnoreExtraProperties
data class FirebaseUserProfile(
  var uid: String = "",
  var userId: String = "",
  var pid: String = "",
  var name: String = "",
  var username: String = "",
  var photoURL: String? = null,
  var profileImageUri: String? = null,
  var phoneNumber: String = "",
  var email: String = "",
  var gender: String = "Male",
  var age: Int = 18,
  var dateOfBirth: String = "2006-01-01",
  var birthDate: String = "2006-01-01",
  var country: String = "Bangladesh",
  var countryFlag: String = "🇧🇩",
  var backgroundImageUri: String? = null,
  var bio: String = "",
  var friendCount: String = "0",
  var followCount: String = "0",
  var followersCount: String = "0",
  var visitorsCount: String = "0",
  var vipBadge: String = "None",
  var levelBadge: String = "Lv.1",
  var authProvider: String = "GUEST",
  var authId: String = "",
  var isCpConnected: Boolean = false,
  var cpPartnerName: String? = null,
  var cpPartnerAvatarUri: String? = null,
  var isAgencyJoined: Boolean = false,
  var isAgencyOwner: Boolean = false,
  var agencyName: String? = null,
  var agencyLogoUri: String? = null,
  var agencyLevel: String = "Lv.1",
  var coinBalance: Long = 640_000L,
  var userLevel: Int = 1,
  var totalRecharge: Long = 0L,
  var vipLevel: Int = 0,
  var svipLevel: Int = 0,
  var frameId: String? = null,
  var frameUrl: String? = null,
  var isVipEnabled: Boolean = true,
  var isVipHidden: Boolean = false,
  var isPanelAuthorized: Boolean = false,
  var createdAt: Long = System.currentTimeMillis(),
  var updatedAt: Long = System.currentTimeMillis(),
  var lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Firebase Realtime Database POJO model for Live Broadcast / Voice Party Rooms.
 */
@IgnoreExtraProperties
data class FirebaseLiveRoom(
  var roomId: String = "",
  var hostUserId: String = "",
  var hostPID: String = "",
  var hostName: String = "",
  var hostAvatarUri: String? = null,
  var hostAvatarRes: Int = 0,
  var roomTitle: String = "",
  var coverUri: String? = null,
  var countryCode: String = "BD",
  var countryName: String = "Bangladesh",
  var flagEmoji: String = "🇧🇩",
  var category: String = "Explore",
  var startTimeMs: Long = 0L,
  var viewerCount: Int = 1,
  var heartScore: Long = 0L,
  var isLocked: Boolean = false,
  var active: Boolean = true,
  var lastActivityTimestamp: Long = System.currentTimeMillis()
)

/**
 * Firebase Realtime Database POJO model for Room / Global Chat Messages.
 */
@IgnoreExtraProperties
data class FirebaseChatMessage(
  var messageId: String = "",
  var roomId: String = "",
  var senderUserId: String = "",
  var senderName: String = "",
  var senderAvatarUri: String? = null,
  var senderLevel: Int = 1,
  var senderVipLevel: Int = 0,
  var content: String = "",
  var timestamp: Long = System.currentTimeMillis()
)

/**
 * Firebase Realtime Database POJO model for Party Board Seats.
 */
@IgnoreExtraProperties
data class FirebasePartySeat(
  var seatNumber: Int = 1,
  var isOccupied: Boolean = false,
  var isLocked: Boolean = false,
  var userId: String? = null,
  var userName: String? = null,
  var userAvatarUri: String? = null,
  var userLevel: Int = 1,
  var userGender: String = "Male",
  var userCountry: String = "Bangladesh",
  var userCountryFlag: String = "🇧🇩",
  var userFrameId: String? = null,
  var userFrameUrl: String? = null,
  var isSpeaking: Boolean = false,
  var isMuted: Boolean = false
)

/**
 * Firebase Realtime Database POJO model for Room Shared Music.
 */
@IgnoreExtraProperties
data class FirebaseRoomMusic(
  var title: String = "",
  var audioUrl: String = "",
  var isPlaying: Boolean = false,
  var position: Long = 0L,
  var updatedAt: Long = 0L,
  var startedBy: String = ""
)

/**
 * Firebase Realtime Database POJO model for Full Party Board Room.
 */
@IgnoreExtraProperties
data class FirebasePartyRoomData(
  var roomId: String = "",
  var roomTitle: String = "Party Board",
  var hostUid: String = "",
  var hostPID: String = "",
  var hostName: String = "",
  var hostAvatarUri: String? = null,
  var hostUserId: String = "",
  var coverUri: String? = null,
  var country: String = "Bangladesh",
  var countryCode: String = "BD",
  var flagEmoji: String = "🇧🇩",
  var isLocked: Boolean = false,
  var seatCount: Int = 8,
  var seats: Map<String, FirebasePartySeat> = emptyMap(),
  var music: FirebaseRoomMusic? = null,
  var viewerCount: Int = 1,
  var coinPool: Long = 0L,
  var active: Boolean = true,
  var lastActivityTimestamp: Long = System.currentTimeMillis()
)

/**
 * Firebase Realtime Database POJO model for Party Room Gift Events.
 */
@IgnoreExtraProperties
data class FirebaseGiftEvent(
  var id: String = "",
  var senderUid: String = "",
  var senderName: String = "",
  var senderAvatarUri: String? = null,
  var giftName: String = "",
  var giftEmoji: String = "",
  var coinCost: Long = 0L,
  var timestamp: Long = System.currentTimeMillis()
)

/**
 * Global Shared Room Registry POJO model.
 * Authoritative global index under /rooms/{roomId}
 */
@IgnoreExtraProperties
data class FirebaseSharedRoom(
  var roomId: String = "",
  var ownerPID: String = "",
  var ownerUid: String = "",
  var ownerName: String = "",
  var ownerAvatar: String? = null,
  var roomType: String = "LIVE", // "LIVE" or "PARTY"
  var roomName: String = "",
  var roomPicture: String? = null,
  var roomStatus: String = "ACTIVE", // "ACTIVE" or "CLOSED"
  var seatCount: Int = 1,
  var lockStatus: Boolean = false,
  var active: Boolean = true,
  var country: String = "Bangladesh",
  var countryCode: String = "BD",
  var flagEmoji: String = "🇧🇩",
  var category: String = "Explore",
  var viewerCount: Int = 1,
  var heartScore: Long = 0L,
  var createdAt: Long = System.currentTimeMillis(),
  var updatedAt: Long = System.currentTimeMillis()
)

