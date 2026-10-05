package com.example.firebase

import android.util.Log
import com.example.firebase.model.FirebaseChatMessage
import com.example.firebase.model.FirebaseGiftEvent
import com.example.firebase.model.FirebasePartyRoomData
import com.example.firebase.model.FirebasePartySeat
import com.example.firebase.model.FirebaseRoomMusic
import com.example.firebase.model.FirebaseUserProfile
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

/**
 * Service for managing real-time Party Board state in Firebase Realtime Database:
 * Path: /party_rooms/{roomId}
 */
object FirebasePartyRoomService {
  private const val TAG = "PartyRoomService"
  private val partyRoomsRef get() = FirebaseDatabaseManager.getReference("party_rooms")

  /**
   * Initializes room if not existing, or joins room and returns real-time listener.
   */
  fun joinRoom(
    roomId: String,
    hostUid: String,
    hostName: String,
    hostAvatar: String?,
    hostUserId: String,
    roomTitle: String = "Party Board",
    initialSeatCount: Int = 8,
    onRoomUpdated: (FirebasePartyRoomData) -> Unit
  ): ValueEventListener {
    val roomRef = partyRoomsRef.child(roomId)

    // Check and initialize default 8 seats if newly created
    roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        if (!snapshot.exists()) {
          val initialSeats = mutableMapOf<String, FirebasePartySeat>()
          for (i in 1..initialSeatCount) {
            initialSeats["seat_$i"] = FirebasePartySeat(
              seatNumber = i,
              isOccupied = false,
              isLocked = false
            )
          }

          val newRoom = FirebasePartyRoomData(
            roomId = roomId,
            roomTitle = roomTitle,
            hostUid = hostUid,
            hostName = hostName,
            hostAvatarUri = hostAvatar,
            hostUserId = hostUserId,
            isLocked = false,
            seatCount = initialSeatCount,
            seats = initialSeats,
            viewerCount = 1,
            coinPool = 0L,
            active = true,
            lastActivityTimestamp = System.currentTimeMillis()
          )
          roomRef.setValue(newRoom)
        } else {
          // Increment viewer count
          val currentViewers = snapshot.child("viewerCount").getValue(Int::class.java) ?: 1
          roomRef.child("viewerCount").setValue(currentViewers + 1)
        }
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Failed checking room creation: ${error.message}")
      }
    })

    // Real-time listener for room updates
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        if (!snapshot.exists()) return

        val room = snapshot.getValue(FirebasePartyRoomData::class.java) ?: return
        room.roomId = roomId

        // Parse seats map carefully from snapshot
        val seatsSnapshot = snapshot.child("seats")
        val parsedSeats = mutableMapOf<String, FirebasePartySeat>()
        for (child in seatsSnapshot.children) {
          val seat = child.getValue(FirebasePartySeat::class.java)
          if (seat != null) {
            val key = child.key ?: "seat_${seat.seatNumber}"
            parsedSeats[key] = seat
          }
        }
        room.seats = parsedSeats

        onRoomUpdated(room)
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Room observation error: ${error.message}")
      }
    }

    roomRef.addValueEventListener(listener)
    return listener
  }

  /**
   * Leaves room and cleans up listeners.
   */
  fun leaveRoom(roomId: String, userId: String, listener: ValueEventListener) {
    val roomRef = partyRoomsRef.child(roomId)
    try {
      roomRef.removeEventListener(listener)
    } catch (_: Exception) {}

    // Vacate any seat occupied by this user
    vacateUserFromSeats(roomId, userId)

    // Decrement viewer count
    roomRef.child("viewerCount").addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val current = snapshot.getValue(Int::class.java) ?: 1
        if (current > 1) {
          roomRef.child("viewerCount").setValue(current - 1)
        } else {
          roomRef.child("viewerCount").setValue(1)
        }
      }
      override fun onCancelled(error: DatabaseError) {}
    })
  }

  /**
   * Vacate user from any seat.
   */
  fun vacateUserFromSeats(roomId: String, userId: String, exceptSeatNumber: Int? = null) {
    val seatsRef = partyRoomsRef.child(roomId).child("seats")
    seatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        for (child in snapshot.children) {
          val seat = child.getValue(FirebasePartySeat::class.java)
          if (seat != null && (seat.userId == userId || (userId.isNotBlank() && seat.userId?.endsWith(userId) == true)) && (exceptSeatNumber == null || seat.seatNumber != exceptSeatNumber)) {
            child.ref.updateChildren(
              mapOf(
                "isOccupied" to false,
                "userId" to null,
                "userName" to null,
                "userAvatarUri" to null,
                "userFrameId" to null,
                "userFrameUrl" to null,
                "isSpeaking" to false,
                "isMuted" to false
              )
            )
          }
        }
      }
      override fun onCancelled(error: DatabaseError) {}
    })
  }

  /**
   * User joins a specific seat.
   */
  fun takeSeat(
    roomId: String,
    seatNumber: Int,
    user: FirebaseUserProfile,
    onComplete: (Boolean, String?) -> Unit
  ) {
    val roomRef = partyRoomsRef.child(roomId)
    val seatKey = "seat_$seatNumber"
    val seatRef = roomRef.child("seats").child(seatKey)

    val effectiveUserId = user.userId.ifBlank { user.uid }
    val updates = mapOf(
      "seatNumber" to seatNumber,
      "isOccupied" to true,
      "isLocked" to false,
      "userId" to effectiveUserId,
      "userName" to (user.name.ifBlank { user.username.ifBlank { "User" } }),
      "userAvatarUri" to user.profileImageUri,
      "userFrameId" to user.frameId,
      "userFrameUrl" to user.frameUrl,
      "userLevel" to user.userLevel,
      "userGender" to user.gender,
      "userCountry" to user.country,
      "userCountryFlag" to user.countryFlag,
      "isSpeaking" to false,
      "isMuted" to false
    )

    seatRef.updateChildren(updates).addOnCompleteListener { task ->
      if (task.isSuccessful) {
        onComplete(true, null)
      } else {
        // Fallback: overwrite seat node directly
        seatRef.setValue(
          FirebasePartySeat(
            seatNumber = seatNumber,
            isOccupied = true,
            isLocked = false,
            userId = effectiveUserId,
            userName = user.name.ifBlank { user.username.ifBlank { "User" } },
            userAvatarUri = user.profileImageUri,
            userFrameId = user.frameId,
            userFrameUrl = user.frameUrl,
            userLevel = user.userLevel,
            userGender = user.gender,
            userCountry = user.country,
            userCountryFlag = user.countryFlag,
            isSpeaking = false,
            isMuted = false
          )
        ).addOnCompleteListener { subTask ->
          onComplete(subTask.isSuccessful, subTask.exception?.message)
        }
      }
    }

    // Vacate previous seat for this user if any
    vacateUserFromSeats(roomId, effectiveUserId, exceptSeatNumber = seatNumber)
    if (user.uid.isNotBlank() && user.uid != effectiveUserId) {
      vacateUserFromSeats(roomId, user.uid, exceptSeatNumber = seatNumber)
    }
  }

  /**
   * User vacates a specific seat.
   */
  fun vacateSeat(roomId: String, seatNumber: Int) {
    val seatRef = partyRoomsRef.child(roomId).child("seats").child("seat_$seatNumber")
    seatRef.updateChildren(
      mapOf(
        "isOccupied" to false,
        "userId" to null,
        "userName" to null,
        "userAvatarUri" to null,
        "isSpeaking" to false,
        "isMuted" to false
      )
    )
  }

  /**
   * Toggle speaking state on seat for Agora / audio visualizer.
   */
  fun setSeatSpeaking(roomId: String, seatNumber: Int, isSpeaking: Boolean) {
    val seatRef = partyRoomsRef.child(roomId).child("seats").child("seat_$seatNumber")
    seatRef.child("isSpeaking").setValue(isSpeaking)
  }

  /**
   * Toggle mute state on seat.
   */
  fun setSeatMuted(roomId: String, seatNumber: Int, isMuted: Boolean) {
    val seatRef = partyRoomsRef.child(roomId).child("seats").child("seat_$seatNumber")
    seatRef.updateChildren(
      mapOf(
        "isMuted" to isMuted,
        "isSpeaking" to false
      )
    )
  }

  /**
   * Locks a specific seat.
   */
  fun lockSeat(roomId: String, seatNumber: Int, onComplete: ((Boolean) -> Unit)? = null) {
    val seatRef = partyRoomsRef.child(roomId).child("seats").child("seat_$seatNumber")
    seatRef.updateChildren(
      mapOf(
        "isLocked" to true,
        "isOccupied" to false,
        "userId" to null,
        "userName" to null,
        "userAvatarUri" to null
      )
    ).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Unlocks/Opens a locked seat.
   */
  fun openSeat(roomId: String, seatNumber: Int, onComplete: ((Boolean) -> Unit)? = null) {
    val seatRef = partyRoomsRef.child(roomId).child("seats").child("seat_$seatNumber")
    seatRef.child("isLocked").setValue(false).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Sets Room Lock status.
   */
  fun setRoomLock(roomId: String, isLocked: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
    partyRoomsRef.child(roomId).child("isLocked").setValue(isLocked).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Updates Room Name / Title.
   */
  fun updateRoomName(roomId: String, newName: String, onComplete: ((Boolean) -> Unit)? = null) {
    partyRoomsRef.child(roomId).child("roomTitle").setValue(newName).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Updates Room Wallpaper / Cover Picture.
   */
  fun updateRoomPicture(roomId: String, pictureUri: String, onComplete: ((Boolean) -> Unit)? = null) {
    partyRoomsRef.child(roomId).child("coverUri").setValue(pictureUri).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Changes seat count to 8, 12, or 16.
   * New seats start empty.
   */
  fun updateSeatCount(roomId: String, newCount: Int, onComplete: ((Boolean) -> Unit)? = null) {
    if (newCount !in listOf(8, 12, 16)) {
      onComplete?.invoke(false)
      return
    }

    val roomRef = partyRoomsRef.child(roomId)
    val seatsRef = roomRef.child("seats")

    // Ensure all seats up to newCount exist
    for (i in 1..newCount) {
      val seatKey = "seat_$i"
      seatsRef.child(seatKey).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) {
            seatsRef.child(seatKey).setValue(
              FirebasePartySeat(
                seatNumber = i,
                isOccupied = false,
                isLocked = false
              )
            )
          }
        }
        override fun onCancelled(error: DatabaseError) {}
      })
    }

    roomRef.child("seatCount").setValue(newCount).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Sends a real chat message to party_rooms/{roomId}/messages.
   */
  fun sendMessage(
    roomId: String,
    senderUid: String,
    senderName: String,
    senderAvatar: String?,
    senderLevel: Int,
    text: String,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    val messagesRef = partyRoomsRef.child(roomId).child("messages")
    val msgId = messagesRef.push().key ?: System.currentTimeMillis().toString()
    val msg = FirebaseChatMessage(
      messageId = msgId,
      roomId = roomId,
      senderUserId = senderUid,
      senderName = senderName,
      senderAvatarUri = senderAvatar,
      senderLevel = senderLevel,
      content = text,
      timestamp = System.currentTimeMillis()
    )
    messagesRef.child(msgId).setValue(msg).addOnCompleteListener { task ->
      onComplete?.invoke(task.isSuccessful)
    }
  }

  /**
   * Listens for chat messages in this room.
   */
  fun observeMessages(
    roomId: String,
    onMessage: (FirebaseChatMessage) -> Unit
  ): ChildEventListener {
    val messagesRef = partyRoomsRef.child(roomId).child("messages")
    val listener = object : ChildEventListener {
      override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        val msg = snapshot.getValue(FirebaseChatMessage::class.java)
        if (msg != null) {
          onMessage(msg)
        }
      }
      override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
      override fun onChildRemoved(snapshot: DataSnapshot) {}
      override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
      override fun onCancelled(error: DatabaseError) {}
    }
    messagesRef.limitToLast(50).addChildEventListener(listener)
    return listener
  }

  fun removeMessageObserver(roomId: String, listener: ChildEventListener) {
    try {
      partyRoomsRef.child(roomId).child("messages").removeEventListener(listener)
    } catch (_: Exception) {}
  }

  /**
   * Sends a gift event and increments room coin pool.
   */
  fun sendGiftEvent(
    roomId: String,
    senderUid: String,
    senderName: String,
    senderAvatar: String?,
    giftName: String,
    giftEmoji: String,
    coinCost: Long,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    val giftsRef = partyRoomsRef.child(roomId).child("gifts")
    val giftId = giftsRef.push().key ?: System.currentTimeMillis().toString()
    val event = FirebaseGiftEvent(
      id = giftId,
      senderUid = senderUid,
      senderName = senderName,
      senderAvatarUri = senderAvatar,
      giftName = giftName,
      giftEmoji = giftEmoji,
      coinCost = coinCost,
      timestamp = System.currentTimeMillis()
    )
    giftsRef.child(giftId).setValue(event)

    // Increment room coin pool
    val roomRef = partyRoomsRef.child(roomId)
    roomRef.child("coinPool").addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val current = snapshot.getValue(Long::class.java) ?: 0L
        roomRef.child("coinPool").setValue(current + coinCost)
      }
      override fun onCancelled(error: DatabaseError) {}
    })

    onComplete?.invoke(true)
  }

  /**
   * Observes gift events in real time.
   */
  fun observeGifts(
    roomId: String,
    onGift: (FirebaseGiftEvent) -> Unit
  ): ChildEventListener {
    val giftsRef = partyRoomsRef.child(roomId).child("gifts")
    val listener = object : ChildEventListener {
      override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        val gift = snapshot.getValue(FirebaseGiftEvent::class.java)
        if (gift != null) {
          onGift(gift)
        }
      }
      override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
      override fun onChildRemoved(snapshot: DataSnapshot) {}
      override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
      override fun onCancelled(error: DatabaseError) {}
    }
    giftsRef.limitToLast(20).addChildEventListener(listener)
    return listener
  }

  fun removeGiftObserver(roomId: String, listener: ChildEventListener) {
    try {
      partyRoomsRef.child(roomId).child("gifts").removeEventListener(listener)
    } catch (_: Exception) {}
  }

  /**
   * Synchronizes shared room music state.
   */
  fun updateRoomMusic(roomId: String, music: FirebaseRoomMusic) {
    partyRoomsRef.child(roomId).child("music").setValue(music)
  }
}
