# Knot

Knot is an Android application for creating shared memories within private circles. Members can create or join groups, respond to activities with text and multimedia, and revisit contributions through shared timelines and albums. The app also uses nearby-device discovery to encourage in-person interactions between members of the same circle.


## Features

- **Accounts and private circles:** Firebase Authentication for email/password sign-in, with group creation and membership management.
- **Activities and responses:** View activities and contribute text, photos, videos, or audio recordings.
- **Device sensors:** CameraX for photo/video capture, microphone recording, and device location support.
- **Nearby interactions (P2P):** Google Nearby Connections discovers nearby Android devices and checks shared group membership through Firestore before presenting proximity-related interactions. This is proximity discovery, not precise distance measurement.
- **Shared memories:** Group timelines and album views backed by Cloud Firestore; supported timeline views observe response updates using snapshot listeners.
- **Cloud-backed media:** Firebase Storage holds uploaded media, while Firestore stores related response metadata. Android WorkManager handles background media uploads with connectivity constraints and retries.
- **Notifications:** Firebase Cloud Messaging integration for receiving push messages and local notifications for P2P interactions.
- **Privacy features:** Shared group-key encryption is implemented for response text and media URLs stored in Firestore. The media files themselves are not end-to-end encrypted by this mechanism. Encryption may fall back to unencrypted values if the key is unavailable; this should not be treated as a guarantee of end-to-end confidentiality.

## Technology Stack

- Kotlin, Jetpack Compose, Android Jetpack
- CameraX, Android media recording, Google Play services Location
- Google Nearby Connections API
- Firebase Authentication, Cloud Firestore, Firebase Storage, Firebase Cloud Messaging
- Android WorkManager
- Google Tink-based cryptography

## Requirements

- **Android Studio** with Android SDK 34 installed
- **Android 8.0 (API 26)** or newer device/emulator
- Internet access for Firebase-backed functionality
- Google Play services for Nearby Connections and location functionality
- For end-to-end P2P testing: **two physical Android devices** with Bluetooth and relevant permissions enabled

Project build configuration uses Android Gradle Plugin **8.5.2**, Kotlin **1.9.24**, and Gradle wrapper **8.7**. Use a JDK compatible with Android Gradle Plugin 8.5.2 (JDK 17 recommended).

## Open, Build, and Run

1. Extract the submitted `source_code.zip` archive.
2. Open **Android Studio** and select **Open**.
3. Select the extracted project directory containing `settings.gradle.kts`.
4. Wait for Gradle Sync to finish. Android Studio may prompt you to install missing SDK components.
5. Confirm that Firebase is configured as described below.
6. Select an Android device or emulator.
7. Choose **Build > Make Project**, then click **Run** to install and launch the application.
8. Sign up or log in to use the cloud-backed features.


## Firebase Configuration

Knot uses Firebase Authentication, Cloud Firestore, Cloud Storage, and Firebase Cloud Messaging. The Android application ID is com.knot.app.

The project includes app/google-services.json, which connects the Android application to its configured Firebase project.

To test Firebase-dependent features, the associated Firebase backend must remain available, with Email/Password authentication enabled and appropriate Firestore and Storage access permissions.

## Permissions and Device Testing

Knot may request the following permissions, depending on the feature being used:

- Camera — photo and video capture
- Microphone — audio recording and video audio
- Location — location-aware features and relevant nearby-device workflows
- Bluetooth / nearby devices and nearby Wi-Fi — Nearby Connections discovery
- Notifications — local and Firebase push notifications

For a P2P test, install Knot on **two physical Android devices**, sign in using separate accounts that share a group, grant the requested nearby-device permissions, and enable Bluetooth. Nearby discovery depends on device capabilities, permissions, and environmental conditions; an emulator is not a reliable substitute for testing interactions between physical devices.

## Repository Layout

```text
app/
  src/main/java/com/knot/app/
    crypto/           # Device and group key handling
    data/             # Firestore, Storage, and repository logic
    location/         # Location support
    nearby/           # Nearby Connections and foreground service
    notifications/    # FCM and local notifications
    ui/               # Jetpack Compose screens and view models
    navigation/       # App navigation
  src/main/AndroidManifest.xml
  build.gradle.kts
build.gradle.kts
settings.gradle.kts
gradle/wrapper/
```



