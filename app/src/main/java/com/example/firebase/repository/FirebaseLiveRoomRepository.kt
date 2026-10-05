package com.example.firebase.repository

import com.example.firebase.FirebaseDatabaseManager
import com.example.firebase.model.FirebaseLiveRoom
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Repository for managing live / voice party rooms in Firebase Realtime Database.
 * Path: /live_rooms/{roomId}
 */
class FirebaseLiveRoomRepository : FirebaseRepository<FirebaseLiveRoom, String> {

  private val roomsRef = FirebaseDatabaseManager.getReference("live_rooms")

  override suspend fun get(id: String): FirebaseResult<FirebaseLiveRoom?> =
    suspendCancellableCoroutine { continuation ->
      roomsRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val room = snapshot.getValue(FirebaseLiveRoom::class.java)
          continuation.resume(FirebaseResult.Success(room))
        }

        override fun onCancelled(error: DatabaseError) {
          continuation.resume(
            FirebaseResult.Error(
              error.toException(),
              "Failed to get live room: ${error.message}"
            )
          )
        }
      })
    }

  override fun observe(id: String): Flow<FirebaseResult<FirebaseLiveRoom?>> = callbackFlow {
    trySend(FirebaseResult.Loading)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val room = snapshot.getValue(FirebaseLiveRoom::class.java)
        trySend(FirebaseResult.Success(room))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(
          FirebaseResult.Error(
            error.toException(),
            "Error observing room: ${error.message}"
          )
        )
      }
    }

    val roomRef = roomsRef.child(id)
    roomRef.addValueEventListener(listener)
    awaitClose { roomRef.removeEventListener(listener) }
  }

  override fun observeAll(): Flow<FirebaseResult<List<FirebaseLiveRoom>>> = callbackFlow {
    trySend(FirebaseResult.Loading)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val list = mutableListOf<FirebaseLiveRoom>()
        for (child in snapshot.children) {
          child.getValue(FirebaseLiveRoom::class.java)?.let {
            if (it.active) list.add(it)
          }
        }
        trySend(FirebaseResult.Success(list))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(
          FirebaseResult.Error(
            error.toException(),
            "Error observing live rooms: ${error.message}"
          )
        )
      }
    }

    roomsRef.addValueEventListener(listener)
    awaitClose { roomsRef.removeEventListener(listener) }
  }

  override suspend fun save(id: String, item: FirebaseLiveRoom): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      item.roomId = id
      item.lastActivityTimestamp = System.currentTimeMillis()
      roomsRef.child(id).setValue(item)
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to save live room: ${ex.localizedMessage}"
            )
          )
        }
    }

  override suspend fun update(id: String, values: Map<String, Any?>): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      val updatedMap = values.toMutableMap()
      updatedMap["lastActivityTimestamp"] = System.currentTimeMillis()
      roomsRef.child(id).updateChildren(updatedMap)
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to update live room: ${ex.localizedMessage}"
            )
          )
        }
    }

  override suspend fun delete(id: String): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      roomsRef.child(id).removeValue()
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to delete live room: ${ex.localizedMessage}"
            )
          )
        }
    }

  /**
   * Helper to close/deactivate room without physically deleting history.
   */
  suspend fun closeRoom(id: String): FirebaseResult<Unit> {
    return update(id, mapOf("active" to false))
  }
}
