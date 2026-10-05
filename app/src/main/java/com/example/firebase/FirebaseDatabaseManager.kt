package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

/**
 * Firebase Realtime Database Manager configured with the project database URL:
 * https://priya-live-f9fa2-default-rtdb.firebaseio.com/
 */
object FirebaseDatabaseManager {
  private const val TAG = "FirebaseDbManager"
  const val DATABASE_URL = "https://priya-live-f9fa2-default-rtdb.firebaseio.com/"
  const val PROJECT_ID = "priya-live-f9fa2"

  @Volatile
  private var isInitialized = false

  /**
   * Initializes Firebase if not already initialized by google-services.json,
   * ensuring that FirebaseRealtimeDatabase instance connects to the specified URL.
   */
  fun initialize(context: Context) {
    if (isInitialized) return
    synchronized(this) {
      if (isInitialized) return

      try {
        if (FirebaseApp.getApps(context).isEmpty()) {
          // Initialize Firebase with the project credentials/options
          val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:887413155649:android:c4b9d7e2f1a083652e79fa")
            .setApiKey("AIzaSyFakeKeyForLocalOptionsInitialization12345")
            .setDatabaseUrl(DATABASE_URL)
            .build()
          FirebaseApp.initializeApp(context.applicationContext, options)
          Log.d(TAG, "FirebaseApp initialized with options for project: $PROJECT_ID")
        } else {
          Log.d(TAG, "FirebaseApp already initialized, using default app")
        }

        // Configure database persistence / connection
        val database = getDatabase()
        try {
          database.setPersistenceEnabled(true)
        } catch (ignored: Exception) {
          // Persistence can only be set once before any other database calls
        }

        // Test connectivity ping
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
          override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
            val connected = snapshot.getValue(Boolean::class.java) ?: false
            Log.d(TAG, "Firebase Realtime Database connection status: $connected")
          }

          override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
            Log.w(TAG, "Firebase Realtime Database listener cancelled: ${error.message}")
          }
        })

        isInitialized = true
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
      }
    }
  }

  /**
   * Returns the FirebaseDatabase instance pointing explicitly to the given database URL.
   */
  fun getDatabase(): FirebaseDatabase {
    return FirebaseDatabase.getInstance(DATABASE_URL)
  }

  /**
   * Quick helper to get a DatabaseReference under a specified path.
   */
  fun getReference(path: String) = getDatabase().getReference(path)

  // Singleton Repository instances
  val usersRepository by lazy { com.example.firebase.repository.FirebaseUserRepository() }
  val liveRoomsRepository by lazy { com.example.firebase.repository.FirebaseLiveRoomRepository() }
  val chatRepository by lazy { com.example.firebase.repository.FirebaseChatRepository() }
}
