package com.example.firebase.repository

import kotlinx.coroutines.flow.Flow

/**
 * Generic Result wrapper for Firebase Realtime Database repository operations.
 */
sealed class FirebaseResult<out T> {
  data class Success<out T>(val data: T) : FirebaseResult<T>()
  data class Error(val exception: Throwable, val message: String = exception.localizedMessage ?: "Unknown error") : FirebaseResult<Nothing>()
  object Loading : FirebaseResult<Nothing>()
}

/**
 * Base generic interface for Firebase Realtime Database CRUD operations.
 */
interface FirebaseRepository<T, ID> {
  suspend fun get(id: ID): FirebaseResult<T?>
  fun observe(id: ID): Flow<FirebaseResult<T?>>
  fun observeAll(): Flow<FirebaseResult<List<T>>>
  suspend fun save(id: ID, item: T): FirebaseResult<Unit>
  suspend fun update(id: ID, values: Map<String, Any?>): FirebaseResult<Unit>
  suspend fun delete(id: ID): FirebaseResult<Unit>
}
