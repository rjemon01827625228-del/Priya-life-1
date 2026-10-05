package com.example.firebase.repository

import com.example.firebase.FirebaseDatabaseManager
import com.example.firebase.model.FirebaseUserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Repository for managing user profile operations against Firebase Realtime Database.
 * Path: /users/{userId}
 */
class FirebaseUserRepository : FirebaseRepository<FirebaseUserProfile, String> {

  private val usersRef = FirebaseDatabaseManager.getReference("users")

  override suspend fun get(id: String): FirebaseResult<FirebaseUserProfile?> =
    suspendCancellableCoroutine { continuation ->
      usersRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val profile = snapshot.getValue(FirebaseUserProfile::class.java)
          continuation.resume(FirebaseResult.Success(profile))
        }

        override fun onCancelled(error: DatabaseError) {
          continuation.resume(
            FirebaseResult.Error(
              error.toException(),
              "Failed to get user profile: ${error.message}"
            )
          )
        }
      })
    }

  override fun observe(id: String): Flow<FirebaseResult<FirebaseUserProfile?>> = callbackFlow {
    trySend(FirebaseResult.Loading)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val profile = snapshot.getValue(FirebaseUserProfile::class.java)
        trySend(FirebaseResult.Success(profile))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(
          FirebaseResult.Error(
            error.toException(),
            "Error observing user profile: ${error.message}"
          )
        )
      }
    }

    val userRef = usersRef.child(id)
    userRef.addValueEventListener(listener)
    awaitClose { userRef.removeEventListener(listener) }
  }

  override fun observeAll(): Flow<FirebaseResult<List<FirebaseUserProfile>>> = callbackFlow {
    trySend(FirebaseResult.Loading)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val list = mutableListOf<FirebaseUserProfile>()
        for (child in snapshot.children) {
          child.getValue(FirebaseUserProfile::class.java)?.let { list.add(it) }
        }
        trySend(FirebaseResult.Success(list))
      }

      override fun onCancelled(error: DatabaseError) {
        trySend(
          FirebaseResult.Error(
            error.toException(),
            "Error observing users list: ${error.message}"
          )
        )
      }
    }

    usersRef.addValueEventListener(listener)
    awaitClose { usersRef.removeEventListener(listener) }
  }

  override suspend fun save(id: String, item: FirebaseUserProfile): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      item.userId = id
      item.lastUpdatedTimestamp = System.currentTimeMillis()
      usersRef.child(id).setValue(item)
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to save user profile: ${ex.localizedMessage}"
            )
          )
        }
    }

  override suspend fun update(id: String, values: Map<String, Any?>): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      val updatedMap = values.toMutableMap()
      updatedMap["lastUpdatedTimestamp"] = System.currentTimeMillis()
      usersRef.child(id).updateChildren(updatedMap)
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to update user profile: ${ex.localizedMessage}"
            )
          )
        }
    }

  override suspend fun delete(id: String): FirebaseResult<Unit> =
    suspendCancellableCoroutine { continuation ->
      usersRef.child(id).removeValue()
        .addOnSuccessListener {
          continuation.resume(FirebaseResult.Success(Unit))
        }
        .addOnFailureListener { ex ->
          continuation.resume(
            FirebaseResult.Error(
              ex,
              "Failed to delete user: ${ex.localizedMessage}"
            )
          )
        }
    }
}
