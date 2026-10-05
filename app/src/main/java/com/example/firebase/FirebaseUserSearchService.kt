package com.example.firebase

import android.util.Log
import com.example.firebase.model.FirebaseUserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * Service for searching real users in Firebase Realtime Database by exact PID.
 * Guaranteed: No fake/demo/mock users or results.
 */
object FirebaseUserSearchService {
  private const val TAG = "UserSearchService"

  /**
   * Searches for a real user by their unique numeric PID in Firebase Realtime Database.
   * Performs an exact lookup in /pids/{pid} and /users query.
   * Calls onResult with the found FirebaseUserProfile, or null if no user matches.
   */
  fun searchUserByPid(
    targetPid: String,
    onResult: (FirebaseUserProfile?) -> Unit
  ) {
    val cleanPid = targetPid.trim().filter { it.isDigit() }
    if (cleanPid.isBlank()) {
      onResult(null)
      return
    }

    // Step 1: Check fast index /pids/{cleanPid} -> uid
    val pidIndexRef = FirebaseDatabaseManager.getReference("pids").child(cleanPid)
    pidIndexRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val uid = snapshot.getValue(String::class.java)
        if (!uid.isNullOrBlank()) {
          // Fetch user by uid
          fetchUserByUid(uid, cleanPid, onResult)
        } else {
          // Step 2: Fallback query on /users ordered by pid or userId
          queryUsersNodeByPid(cleanPid, onResult)
        }
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "PID index search error: ${error.message}. Trying direct query...")
        queryUsersNodeByPid(cleanPid, onResult)
      }
    })
  }

  private fun fetchUserByUid(
    uid: String,
    expectedPid: String,
    onResult: (FirebaseUserProfile?) -> Unit
  ) {
    val userRef = FirebaseDatabaseManager.getReference("users").child(uid)
    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val profile = snapshot.getValue(FirebaseUserProfile::class.java)
        if (profile != null) {
          profile.uid = uid
          if (profile.pid.isBlank()) {
            profile.pid = if (profile.userId.isNotBlank()) profile.userId else expectedPid
          }
          if (profile.userId.isBlank()) {
            profile.userId = profile.pid
          }
          onResult(profile)
        } else {
          onResult(null)
        }
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Error fetching user by UID: ${error.message}")
        onResult(null)
      }
    })
  }

  private fun queryUsersNodeByPid(
    cleanPid: String,
    onResult: (FirebaseUserProfile?) -> Unit
  ) {
    val usersRef = FirebaseDatabaseManager.getReference("users")

    // Try querying by "pid"
    usersRef.orderByChild("pid").equalTo(cleanPid)
      .addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (snapshot.exists() && snapshot.childrenCount > 0) {
            val userChild = snapshot.children.first()
            val profile = userChild.getValue(FirebaseUserProfile::class.java)
            if (profile != null) {
              profile.uid = userChild.key ?: profile.uid
              if (profile.pid.isBlank()) profile.pid = cleanPid
              if (profile.userId.isBlank()) profile.userId = cleanPid
              // Index it for future fast O(1) lookups
              indexPid(cleanPid, profile.uid)
              onResult(profile)
              return
            }
          }

          // Try querying by "userId"
          usersRef.orderByChild("userId").equalTo(cleanPid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
              override fun onDataChange(snapshot2: DataSnapshot) {
                if (snapshot2.exists() && snapshot2.childrenCount > 0) {
                  val userChild = snapshot2.children.first()
                  val profile = userChild.getValue(FirebaseUserProfile::class.java)
                  if (profile != null) {
                    profile.uid = userChild.key ?: profile.uid
                    profile.pid = cleanPid
                    if (profile.userId.isBlank()) profile.userId = cleanPid
                    indexPid(cleanPid, profile.uid)
                    onResult(profile)
                    return
                  }
                }
                // Not found anywhere
                onResult(null)
              }

              override fun onCancelled(error2: DatabaseError) {
                Log.w(TAG, "Error querying users by userId: ${error2.message}")
                onResult(null)
              }
            })
        }

        override fun onCancelled(error: DatabaseError) {
          Log.w(TAG, "Error querying users by pid: ${error.message}")
          onResult(null)
        }
      })
  }

  /**
   * Registers a PID -> UID mapping in /pids/{pid} for atomic fast lookup.
   */
  fun indexPid(pid: String, uid: String) {
    if (pid.isNotBlank() && uid.isNotBlank()) {
      try {
        FirebaseDatabaseManager.getReference("pids").child(pid).setValue(uid)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to index PID $pid: ${e.message}")
      }
    }
  }
}
