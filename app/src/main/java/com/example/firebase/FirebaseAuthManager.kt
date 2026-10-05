package com.example.firebase

import android.content.Context
import android.util.Log
import com.example.firebase.model.FirebaseUserProfile
import com.example.ui.storage.UserProfileStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.random.Random

data class UserSyncResult(
  val profile: FirebaseUserProfile,
  val isNewUser: Boolean,
  val needsProfileSetup: Boolean
)

object FirebaseAuthManager {
  private const val TAG = "FirebaseAuthManager"

  val auth: FirebaseAuth
    get() = FirebaseAuth.getInstance()

  val currentUser: FirebaseUser?
    get() = try {
      auth.currentUser
    } catch (_: Exception) {
      null
    }

  fun isUserAuthenticated(): Boolean {
    return currentUser != null
  }

  fun signOut(context: Context) {
    try {
      auth.signOut()
    } catch (e: Exception) {
      Log.w(TAG, "Error signing out of FirebaseAuth: ${e.message}")
    }
    UserProfileStorage.logout(context)
  }

  /**
   * Generates a permanent unique 7-digit numeric application User ID.
   * Checks Realtime Database to guarantee uniqueness across all users.
   */
  suspend fun generateUniqueApplicationUserId(): String = withContext(Dispatchers.IO) {
    val usersRef = FirebaseDatabaseManager.getReference("users")

    for (attempt in 0..10) {
      // Generate a 7-digit ID starting with 7, 8, or 9
      val prefix = (7..9).random()
      val suffix = Random.nextInt(100_000, 999_999)
      val candidateId = "$prefix$suffix"

      val isUnique = suspendCancellableCoroutine<Boolean> { continuation ->
        // Check reservation in user_ids node
        val reservationRef = FirebaseDatabaseManager.getReference("user_ids").child(candidateId)
        reservationRef.addListenerForSingleValueEvent(object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
              continuation.resume(false)
            } else {
              continuation.resume(true)
            }
          }

          override fun onCancelled(error: DatabaseError) {
            continuation.resume(true)
          }
        })
      }

      if (isUnique) {
        // Reserve the ID
        try {
          FirebaseDatabaseManager.getReference("user_ids").child(candidateId).setValue(true)
        } catch (_: Exception) {}
        return@withContext candidateId
      }
    }

    // Fallback based on time if needed
    val fallback = "7" + (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
    fallback
  }

  /**
   * Loads or creates user in Realtime Database under users/{firebaseAuthUid}.
   * Preserves existing data, permanent User ID, and checks if required profile fields are present.
   */
  suspend fun syncUserProfileWithRealtimeDb(
    context: Context,
    firebaseUser: FirebaseUser,
    authProvider: String = "FIREBASE",
    fallbackName: String = "",
    fallbackPhone: String = "",
    fallbackPhoto: String? = null
  ): UserSyncResult = withContext(Dispatchers.IO) {
    val uid = firebaseUser.uid
    val usersRef = FirebaseDatabaseManager.getReference("users").child(uid)

    val existingSnapshot = suspendCancellableCoroutine<DataSnapshot?> { cont ->
      usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          cont.resume(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
          Log.w(TAG, "Error fetching user record from RTDB: ${error.message}")
          cont.resume(null)
        }
      })
    }

    if (existingSnapshot != null && existingSnapshot.exists() && existingSnapshot.childrenCount > 0) {
      // Existing user: Load profile, preserve existing User ID & all existing info
      val profile = existingSnapshot.getValue(FirebaseUserProfile::class.java) ?: FirebaseUserProfile()
      profile.uid = uid

      // Ensure permanent userId and pid exist
      if (profile.userId.isBlank()) {
        val newUserId = generateUniqueApplicationUserId()
        profile.userId = newUserId
        profile.pid = newUserId
        usersRef.child("userId").setValue(newUserId)
        usersRef.child("pid").setValue(newUserId)
      } else if (profile.pid.isBlank()) {
        profile.pid = profile.userId
        usersRef.child("pid").setValue(profile.userId)
      }
      FirebaseUserSearchService.indexPid(profile.pid, uid)

      // Check if required fields are missing
      val needsSetup = profile.name.isBlank() && profile.username.isBlank() ||
        profile.gender.isBlank() ||
        profile.country.isBlank()

      // Synchronize with local storage cache
      syncToLocalStorage(context, profile)

      UserSyncResult(
        profile = profile,
        isNewUser = false,
        needsProfileSetup = needsSetup
      )
    } else {
      // New user: create record with unique User ID and required initial fields
      val newUserId = generateUniqueApplicationUserId()
      val resolvedName = firebaseUser.displayName?.ifBlank { null }
        ?: fallbackName.ifBlank { "User_$newUserId" }
      val resolvedPhoto = firebaseUser.photoUrl?.toString() ?: fallbackPhoto
      val resolvedEmail = firebaseUser.email ?: ""
      val resolvedPhone = firebaseUser.phoneNumber?.ifBlank { null } ?: fallbackPhone
      val now = System.currentTimeMillis()

      val newProfile = FirebaseUserProfile(
        uid = uid,
        userId = newUserId,
        pid = newUserId,
        name = resolvedName,
        username = resolvedName,
        photoURL = resolvedPhoto,
        profileImageUri = resolvedPhoto,
        phoneNumber = resolvedPhone,
        email = resolvedEmail,
        gender = "", // Left blank so profile setup will collect it
        age = 18,
        dateOfBirth = "",
        birthDate = "",
        country = "",
        countryFlag = "🇧🇩",
        bio = "Live • Connect • Make Friends on Priya Live",
        authProvider = authProvider,
        authId = firebaseUser.uid,
        friendCount = "0",
        followCount = "0",
        followersCount = "0",
        visitorsCount = "0",
        vipBadge = "None",
        levelBadge = "Lv.1",
        coinBalance = 0L,
        userLevel = 1,
        totalRecharge = 0L,
        vipLevel = 0,
        isVipEnabled = true,
        isVipHidden = false,
        createdAt = now,
        updatedAt = now,
        lastUpdatedTimestamp = now
      )

      // Save initial record to Realtime Database
      suspendCancellableCoroutine<Unit> { cont ->
        usersRef.setValue(newProfile)
          .addOnSuccessListener {
            FirebaseUserSearchService.indexPid(newUserId, uid)
            cont.resume(Unit)
          }
          .addOnFailureListener { ex ->
            Log.w(TAG, "Failed to save initial user record: ${ex.message}")
            cont.resume(Unit)
          }
      }

      // Sync with local cache
      syncToLocalStorage(context, newProfile)

      UserSyncResult(
        profile = newProfile,
        isNewUser = true,
        needsProfileSetup = true
      )
    }
  }

  /**
   * Incremental update to user profile at users/{uid}
   * Only updates provided fields and sets updatedAt timestamp.
   */
  suspend fun updateUserProfile(
    context: Context,
    uid: String,
    updates: Map<String, Any?>
  ): Boolean = withContext(Dispatchers.IO) {
    if (uid.isBlank()) return@withContext false
    val usersRef = FirebaseDatabaseManager.getReference("users").child(uid)
    val map = updates.toMutableMap()
    val now = System.currentTimeMillis()
    map["updatedAt"] = now
    map["lastUpdatedTimestamp"] = now

    // Keep name/username and photoURL/profileImageUri in sync
    if (map.containsKey("name")) {
      map["username"] = map["name"]
    }
    if (map.containsKey("photoURL")) {
      map["profileImageUri"] = map["photoURL"]
    }
    if (map.containsKey("dateOfBirth")) {
      map["birthDate"] = map["dateOfBirth"]
    }

    suspendCancellableCoroutine { cont ->
      usersRef.updateChildren(map)
        .addOnSuccessListener {
          // Update local cache
          updateLocalStorageFields(context, uid, map)
          cont.resume(true)
        }
        .addOnFailureListener { ex ->
          Log.e(TAG, "Failed incremental profile update: ${ex.message}")
          // Still update local cache so user is not blocked
          updateLocalStorageFields(context, uid, map)
          cont.resume(false)
        }
    }
  }

  private fun syncToLocalStorage(context: Context, profile: FirebaseUserProfile) {
    try {
      UserProfileStorage.setActiveUserId(context, profile.userId)
      val prefs = context.getSharedPreferences("cyber_user_profile_prefs", Context.MODE_PRIVATE)
      val prefix = "user_${profile.userId}_"
      val editor = prefs.edit()
      editor.putString(prefix + "username", profile.name.ifBlank { profile.username })
      editor.putString(prefix + "gender", profile.gender)
      editor.putInt(prefix + "age", profile.age)
      editor.putString(prefix + "birth_date", profile.dateOfBirth.ifBlank { profile.birthDate })
      editor.putString(prefix + "country", profile.country)
      editor.putString(prefix + "flag", profile.countryFlag)
      editor.putString(prefix + "profile_uri", profile.photoURL ?: profile.profileImageUri)
      editor.putString(prefix + "bio", profile.bio)
      editor.putString(prefix + "auth_provider", profile.authProvider)
      editor.putString(prefix + "auth_id", profile.authId)
      editor.putString(prefix + "phone_number", profile.phoneNumber)
      editor.putString(prefix + "email", profile.email)
      editor.putString(prefix + "firebase_uid", profile.uid)
      editor.apply()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to sync to local storage: ${e.message}")
    }
  }

  private fun updateLocalStorageFields(context: Context, uid: String, fields: Map<String, Any?>) {
    try {
      val activeUserId = UserProfileStorage.getActiveUserId(context)
      val prefs = context.getSharedPreferences("cyber_user_profile_prefs", Context.MODE_PRIVATE)
      val prefix = "user_${activeUserId}_"
      val editor = prefs.edit()

      fields["name"]?.let { editor.putString(prefix + "username", it.toString()) }
      fields["username"]?.let { editor.putString(prefix + "username", it.toString()) }
      fields["gender"]?.let { editor.putString(prefix + "gender", it.toString()) }
      fields["country"]?.let { editor.putString(prefix + "country", it.toString()) }
      fields["countryFlag"]?.let { editor.putString(prefix + "flag", it.toString()) }
      fields["dateOfBirth"]?.let { editor.putString(prefix + "birth_date", it.toString()) }
      fields["photoURL"]?.let { editor.putString(prefix + "profile_uri", it.toString()) }
      fields["profileImageUri"]?.let { editor.putString(prefix + "profile_uri", it.toString()) }
      fields["bio"]?.let { editor.putString(prefix + "bio", it.toString()) }

      editor.apply()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update local storage: ${e.message}")
    }
  }

  /**
   * Attaches a real-time listener to users/{uid} for continuous profile synchronization.
   */
  fun observeUserProfile(
    context: Context,
    uid: String,
    onUpdate: (FirebaseUserProfile) -> Unit
  ): ValueEventListener {
    val usersRef = FirebaseDatabaseManager.getReference("users").child(uid)
    val listener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        if (snapshot.exists()) {
          val profile = snapshot.getValue(FirebaseUserProfile::class.java)
          if (profile != null) {
            profile.uid = uid
            syncToLocalStorage(context, profile)
            onUpdate(profile)
          }
        }
      }

      override fun onCancelled(error: DatabaseError) {
        Log.w(TAG, "Profile observation cancelled: ${error.message}")
      }
    }
    usersRef.addValueEventListener(listener)
    return listener
  }

  /**
   * Removes real-time profile listener.
   */
  fun removeUserProfileObserver(uid: String, listener: ValueEventListener) {
    try {
      FirebaseDatabaseManager.getReference("users").child(uid).removeEventListener(listener)
    } catch (_: Exception) {}
  }
}
