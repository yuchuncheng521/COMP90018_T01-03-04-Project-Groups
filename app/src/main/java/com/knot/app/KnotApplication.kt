package com.knot.app

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application entry point. Initializes Firebase once for the whole app.
 *
 * NOTE: To actually connect to a Firebase project you need to:
 *   1. Create a project at https://console.firebase.google.com
 *   2. Add an Android app with applicationId "com.knot.app"
 *   3. Download google-services.json and place it in the /app module folder
 *   4. Enable Email/Password sign-in under Authentication > Sign-in method
 *   5. Create a Firestore database (test mode is fine for development)
 *
 * Until google-services.json is added, the app will still compile and run,
 * but AuthRepository/FirestoreRepository calls will fail -- the repositories
 * in this base project fall back to in-memory sample data so every screen
 * is still browsable without a configured backend.
 */
class KnotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
    }
}
