package com.example.firebase

import android.util.Log
import com.example.firebase.model.FirebaseLiveRoom
import com.example.firebase.model.FirebasePartyRoomData
import com.example.firebase.model.FirebasePartySeat
import com.example.firebase.model.FirebaseSharedRoom
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * Authoritative Global Room Registry & Real-Time Discovery Manager for Priya Live.
 * Synchronizes room creation, room updates, room closures, and real-time room discovery
 * across all connected users and devices via Firebase Realtime Database.
 *
 * Paths:
 * - Shared authoritative index: /rooms/{roomId}
 * - Live broadcast rooms:       /live_rooms/{roomId}
 * - Party voice board rooms:    /party_rooms/{roomId}
 */
object FirebaseGlobalRoomManager {
  private const val TAG = "GlobalRoomManager"

  val roomsRegistryRef get() = FirebaseDatabaseManager.getReference("rooms")
  val liveRoomsRef get() = FirebaseDatabaseManager.getReference("live_rooms")
  val partyRoomsRef get() = FirebaseDatabaseManager.getReference("party_rooms")

  /**
   * Publishes or updates a Live Room to both the live room path and the shared room registry.
   */
  fun publishLiveRoom(
    room: FirebaseLiveRoom,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    val roomId = room.roomId.ifBlank { "live_${room.hostUserId}_${System.currentTimeMillis()}" }
    room.roomId = roomId
    room.active = true
    room.lastActivityTimestamp = System.currentTimeMillis()

    // 1. Write to live_rooms/{roomId}
    liveRoomsRef.child(roomId).setValue(room)
      .addOnSuccessListener {
        // 2. Write to authoritative shared registry /rooms/{roomId}
        val sharedEntry = FirebaseSharedRoom(
          roomId = roomId,
          ownerPID = room.hostPID.ifBlank { room.hostUserId },
          ownerUid = room.hostUserId,
          ownerName = room.hostName,
          ownerAvatar = room.hostAvatarUri,
          roomType = "LIVE",
          roomName = room.roomTitle,
          roomPicture = room.coverUri ?: room.hostAvatarUri,
          roomStatus = "ACTIVE",
          seatCount = 1,
          lockStatus = room.isLocked,
          active = true,
          country = room.countryName,
          countryCode = room.countryCode,
          flagEmoji = room.flagEmoji,
          category = room.category,
          viewerCount = room.viewerCount,
          heartScore = room.heartScore,
          createdAt = if (room.startTimeMs > 0) room.startTimeMs else System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        roomsRegistryRef.child(roomId).setValue(sharedEntry)
          .addOnSuccessListener {
            Log.d(TAG, "Successfully published Live Room $roomId to global registry")
            onComplete?.invoke(true)
          }
          .addOnFailureListener { ex ->
            Log.w(TAG, "Saved live_room but failed writing to /rooms/$roomId: ${ex.message}")
            onComplete?.invoke(true)
          }
      }
      .addOnFailureListener { ex ->
        Log.e(TAG, "Failed publishing Live Room $roomId: ${ex.message}")
        onComplete?.invoke(false)
      }
  }

  /**
   * Closes a Live Room and marks it CLOSED/inactive in the global registry.
   */
  fun closeLiveRoom(
    roomId: String,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    if (roomId.isBlank()) {
      onComplete?.invoke(true)
      return
    }

    val updates = mapOf<String, Any>(
      "active" to false,
      "lastActivityTimestamp" to System.currentTimeMillis()
    )

    liveRoomsRef.child(roomId).updateChildren(updates)
      .addOnCompleteListener {
        val registryUpdates = mapOf<String, Any>(
          "active" to false,
          "roomStatus" to "CLOSED",
          "updatedAt" to System.currentTimeMillis()
        )
        roomsRegistryRef.child(roomId).updateChildren(registryUpdates)
          .addOnCompleteListener {
            Log.d(TAG, "Live Room $roomId marked as CLOSED in global registry")
            onComplete?.invoke(true)
          }
      }
  }

  /**
   * Publishes or updates a Party Voice Room to both the party room path and the shared room registry.
   */
  fun publishPartyRoom(
    room: FirebasePartyRoomData,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    val roomId = room.roomId.ifBlank { "party_room_${room.hostUserId}" }
    room.roomId = roomId
    room.active = true
    room.lastActivityTimestamp = System.currentTimeMillis()

    // 1. Write to party_rooms/{roomId}
    partyRoomsRef.child(roomId).setValue(room)
      .addOnSuccessListener {
        // 2. Write to authoritative shared registry /rooms/{roomId}
        val sharedEntry = FirebaseSharedRoom(
          roomId = roomId,
          ownerPID = room.hostPID.ifBlank { room.hostUserId },
          ownerUid = room.hostUid.ifBlank { room.hostUserId },
          ownerName = room.hostName,
          ownerAvatar = room.hostAvatarUri,
          roomType = "PARTY",
          roomName = room.roomTitle,
          roomPicture = room.coverUri ?: room.hostAvatarUri,
          roomStatus = "ACTIVE",
          seatCount = room.seatCount,
          lockStatus = room.isLocked,
          active = true,
          country = room.country,
          countryCode = room.countryCode,
          flagEmoji = room.flagEmoji,
          category = "Explore",
          viewerCount = room.viewerCount,
          heartScore = room.coinPool,
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        roomsRegistryRef.child(roomId).setValue(sharedEntry)
          .addOnSuccessListener {
            Log.d(TAG, "Successfully published Party Room $roomId to global registry")
            onComplete?.invoke(true)
          }
          .addOnFailureListener { ex ->
            Log.w(TAG, "Saved party_room but failed writing to /rooms/$roomId: ${ex.message}")
            onComplete?.invoke(true)
          }
      }
      .addOnFailureListener { ex ->
        Log.e(TAG, "Failed publishing Party Room $roomId: ${ex.message}")
        onComplete?.invoke(false)
      }
  }

  /**
   * Closes a Party Room and marks it CLOSED/inactive in the global registry.
   */
  fun closePartyRoom(
    roomId: String,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    if (roomId.isBlank()) {
      onComplete?.invoke(true)
      return
    }

    val updates = mapOf<String, Any>(
      "active" to false,
      "lastActivityTimestamp" to System.currentTimeMillis()
    )

    partyRoomsRef.child(roomId).updateChildren(updates)
      .addOnCompleteListener {
        val registryUpdates = mapOf<String, Any>(
          "active" to false,
          "roomStatus" to "CLOSED",
          "updatedAt" to System.currentTimeMillis()
        )
        roomsRegistryRef.child(roomId).updateChildren(registryUpdates)
          .addOnCompleteListener {
            Log.d(TAG, "Party Room $roomId marked as CLOSED in global registry")
            onComplete?.invoke(true)
          }
      }
  }

  /**
   * Observes all active Live Rooms in real time across the entire application.
   */
  fun observeAllLiveRooms(onRoomsUpdated: (List<FirebaseLiveRoom>) -> Unit): ValueEventListener {
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val roomsList = mutableListOf<FirebaseLiveRoom>()
        for (child in snapshot.children) {
          val room = child.getValue(FirebaseLiveRoom::class.java)
          if (room != null && room.active) {
            room.roomId = child.key ?: room.roomId
            roomsList.add(room)
          }
        }
        onRoomsUpdated(roomsList)
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Error observing all live rooms: ${error.message}")
      }
    }
    liveRoomsRef.addValueEventListener(listener)
    return listener
  }

  /**
   * Observes all active Party Rooms in real time across the entire application.
   */
  fun observeAllPartyRooms(onRoomsUpdated: (List<FirebasePartyRoomData>) -> Unit): ValueEventListener {
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val roomsList = mutableListOf<FirebasePartyRoomData>()
        for (child in snapshot.children) {
          val room = child.getValue(FirebasePartyRoomData::class.java)
          if (room != null && room.active) {
            room.roomId = child.key ?: room.roomId

            // Carefully parse seats
            val seatsSnapshot = child.child("seats")
            val parsedSeats = mutableMapOf<String, FirebasePartySeat>()
            for (seatChild in seatsSnapshot.children) {
              val seat = seatChild.getValue(FirebasePartySeat::class.java)
              if (seat != null) {
                val key = seatChild.key ?: "seat_${seat.seatNumber}"
                parsedSeats[key] = seat
              }
            }
            room.seats = parsedSeats

            roomsList.add(room)
          }
        }
        onRoomsUpdated(roomsList)
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Error observing all party rooms: ${error.message}")
      }
    }
    partyRoomsRef.addValueEventListener(listener)
    return listener
  }

  fun removeLiveRoomObserver(listener: ValueEventListener) {
    try {
      liveRoomsRef.removeEventListener(listener)
    } catch (_: Exception) {}
  }

  fun removePartyRoomObserver(listener: ValueEventListener) {
    try {
      partyRoomsRef.removeEventListener(listener)
    } catch (_: Exception) {}
  }
}
