package com.example.firebase.repository

import com.example.firebase.FirebaseDatabaseManager
import com.example.firebase.model.FirebaseChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Repository for managing live room chat messages in Firebase Realtime Database.
 * Path: /room_messages/{roomId}/{messageId}
 */
class FirebaseChatRepository {

  private val rootRef = FirebaseDatabaseManager.getReference("room_messages")

  /**
   * Sends a new chat message to a room.
   */
  suspend fun sendMessage(roomId: String, message: FirebaseChatMessage): FirebaseResult<String> =
    suspendCancellableCoroutine { continuation ->
      val roomChatRef = rootRef.child(roomId)
      val newMsgRef = roomChatRef.push()
      val msgId = newMsgRef.key ?: System.currentTimeMillis().toString()

      message.messageId = msgId
      message.roomId = roomId
      message.timestamp = System.currentTimeMillis()

      newMsgRef.setValue(message)
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(msgId))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to send message: ${ex.localizedMessage}"
            )
          )
        }
    }

  /**
   * Observes recent chat messages in a live room (e.g. limit to last 50).
   */
  fun observeMessages(roomId: String, limit: Int = 50): Flow<FirebaseResult<List<FirebaseChatMessage>>> =
    callbackFlow {
      trySend(FirebaseResult.Loading)
      val query = rootRef.child(roomId).limitToLast(limit)

      val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val list = mutableListOf<FirebaseChatMessage>()
          for (child in snapshot.children) {
            child.getValue(FirebaseChatMessage::class.java)?.let { list.add(it) }
          }
          trySend(FirebaseResult.Success(list))
        }

        override fun onCancelled(error: DatabaseError) {
          trySend(
            FirebaseResult.Error(
              error.toException(),
              "Error observing room messages: ${error.message}"
            )
          )
        }
      }

      query.addValueEventListener(listener)
      awaitClose { query.removeEventListener(listener) }
    }
}
