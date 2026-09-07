# Knot — base Android app

A starter Jetpack Compose project with 3 main screens (Groups, Activities, Account Settings)
behind a login/sign-up flow, matching the MVP described in the COMP90018 project plan
(private groups, weekly prompts, and a P2P proximity alert placeholder).

## What's included

- **Login / Sign up** — email + password screens wired to Firebase Auth (`AuthRepository`).
- **Groups** — lists every active group the signed-in user is a member of.
- **Activities** — lists weekly prompts/quests to complete, with a **placeholder P2P alert
  banner** pinned to the top of the list (simulates the BLE "you're near a group member"
  trigger from the project plan — see `TODO(sensors)` comments for where to wire in the real
  BLE scan service).
- **Account Settings** — profile header, notification/privacy toggles, log out.
- Bottom navigation between the 3 main screens, Material 3 theming with a soft/neutral
  palette (per the project plan's UI Appeal guidelines).

## ⚠️ Backend note

This base code stubs the backend with **Firebase Auth + Firestore**, since that's what was
asked for. Your submitted project plan (`Mobile Computing.pdf`) actually specifies
**Supabase** (Supabase Auth + Supabase Realtime + Postgres) for Connectivity and Technical
Depth. If you want the grading criteria to line up with what you build, swap
`AuthRepository` / `GroupsRepository` / `ActivitiesRepository` in `data/` for Supabase's
Kotlin client (`io.github.jan-tennert.supabase`) — the ViewModels and UI don't need to
change, since they only depend on the repository's public functions.

## Project structure

```
app/src/main/java/com/knot/app/
├── MainActivity.kt              # sets content, applies theme
├── KnotApplication.kt           # Firebase init
├── model/                       # Group, ActivityItem, UserAccount
├── data/                        # AuthRepository, GroupsRepository, ActivitiesRepository
├── navigation/                  # Screen routes + NavHost (auth graph + main graph)
├── ui/
│   ├── auth/                    # LoginScreen, SignUpScreen, AuthViewModel
│   ├── groups/                  # GroupsScreen, GroupsViewModel
│   ├── activities/              # ActivitiesScreen, ActivitiesViewModel
│   ├── settings/                # AccountSettingsScreen, AccountSettingsViewModel
│   ├── components/              # BottomNavBar, P2PAlertBanner
│   └── theme/                   # Color, Type, Theme (Material 3)
```

Every repository falls back to in-memory sample data when there's no `google-services.json`
configured, so **the app is fully browsable and demoable with zero backend setup**.

## Getting it running

1. Open the `KnotApp` folder in Android Studio (Koala or newer) and let it sync Gradle.
2. Just want to click around the UI? Run it as-is — sample data fills every screen.
3. Want real accounts/data? Set up Firebase:
   - Create a project at https://console.firebase.google.com
   - Add an Android app with application ID `com.knot.app`
   - Download `google-services.json` and place it in the `app/` folder
   - In the Firebase console, enable **Email/Password** under Authentication → Sign-in method
   - Create a Firestore database (test mode is fine while developing)
4. Build & run on an emulator or device (minSdk 26).

## Next steps for the full MVP

- Replace the placeholder P2P alert (`ActivitiesRepository.getP2pAlert()`) with a real BLE
  foreground scan service (Nearby Connections API, per the project plan).
- Add camera/mic/GPS capture flows for answering a weekly prompt.
- Add the shared timeline screen (chronological feed of a group's memories).
- Add group creation / invite flow (the "New group" FAB on the Groups screen is currently a
  no-op hook — `onCreateGroupClick`).
