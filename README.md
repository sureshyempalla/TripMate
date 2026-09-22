# TripMate

A cross-platform trip-planning app: itineraries, activities, places, and a
"next up" push notification during a trip. Shared business logic in Kotlin
Multiplatform (KMM); fully native UI on each platform (Jetpack Compose on
Android, SwiftUI on iOS).

## Why KMM here

Everything that isn't UI — trip/activity models, the Firestore↔SQLDelight
sync layer, screen state (ViewModels) — lives once in `/shared` and is
compiled into both an Android library and an iOS framework. The two apps
stay native for feel and platform APIs (notifications, permissions), while
avoiding writing the same sync and validation logic twice. The one place
this doesn't fully share: on-device notification *delivery* APIs
(`UNUserNotificationCenter` vs Android's notification manager) are
platform-specific by nature, bridged via Kotlin `expect`/`actual`.

## Architecture

```
TripMate/
├── shared/            KMM module: models, SQLDelight cache, Firestore sync,
│                       ViewModels (TripList / TripDetail / AddActivity)
├── androidApp/         Jetpack Compose UI, Firebase Messaging
├── iosApp/             SwiftUI UI (XcodeGen project spec), Firebase Messaging
└── firebase/           Firestore rules/indexes + a scheduled Cloud Function
                         that sends the "next activity" push
```

**Data flow:** Firestore is the source of truth. `FirestoreTripRepository`
attaches snapshot listeners and writes every change into the local
SQLDelight database; screens observe *only* SQLDelight via `Flow`s, so the
UI updates instantly from cache (including offline) and again when a
Firestore snapshot arrives — never both, and never a race between them.

**Offline mode is real, not just incidental.** Firestore's mobile SDKs
persist reads/writes to disk and queue writes automatically when there's no
connection — `TripMateApplication.kt` (Android) and `AppDelegate.swift`
(iOS) both explicitly raise that cache to unlimited size so a whole trip's
itinerary survives rather than being evicted under the 100MB default.
`TripRepository.syncStatus` reads each snapshot's `metadata.isFromCache`
and surfaces it as `SyncStatus.OFFLINE`/`SYNCED`, which both apps render as
a plain, non-alarming banner ("You're offline — showing your saved
itinerary") on the Trips and Timeline screens. This is a deliberate gap-fill
against the competition: TripIt doesn't message offline state at all, and
Wanderlog locks full offline behind its paid tier — here it's on by
default and honest about what's happening.

**Notifications:** a scheduled Cloud Function (`firebase/functions/index.js`)
runs every 5 minutes, finds activities starting within their
`reminderLeadMinutes` window, and sends an FCM push to every device token
registered for that trip's owner. This was chosen over on-device scheduled
notifications so a reminder still fires if the app was killed, the phone
rebooted, or the activity was edited from a different device — the
trade-off is it needs the Cloud Function deployed and billing enabled
(Cloud Scheduler requires the Blaze plan).

## UX notes

- **Timeline, not a calendar grid.** Activities render as chronological
  day-sections — closer to a packing list you scroll through than a grid
  you have to parse.
- **"Next Up" banner** pinned above the trip list whenever something is
  coming up — the answer to the question people actually open a trip app
  to ask.
- **Single-form Add Activity**, not a wizard — every field (name, place,
  time, notes) is something people fill in in a few seconds, often
  standing in a ticket line.
- Warm "travel journal" palette (terracotta + deep teal) instead of a
  stock Material blue, defined once in `androidApp/.../ui/theme/Theme.kt`
  and mirrored in iOS via named colors (`Terracotta`, `DeepTeal` — add
  these to `iosApp/TripMate/Assets.xcassets` before first run).

## What's stubbed vs. real

This is a scaffold meant to compile and run end-to-end for the core loop
(create activity → see it in the timeline → get a push near the time),
not a finished app. Left as clearly-marked TODOs:

- **Auth.** Both apps currently pass a hardcoded `"current_user"` id.
  Wire up Firebase Auth (email or a social provider) and thread the real
  uid through `TripListViewModel`/`KoinHelper` calls.
- **Trip creation.** The "+" button on the trip list is a no-op — add a
  create-trip sheet/screen calling `TripRepository.createTrip`.
- **Date/time and place pickers.** `AddActivityScreen`/`AddActivityView`
  have clearly-marked stub buttons/fields where a real
  date-time picker and a places-autocomplete field (e.g. Google Places)
  belong.
- **Device token registration.** Both `TripMateMessagingService.onNewToken`
  (Android) and `AppDelegate.messaging(_:didReceiveRegistrationToken:)`
  (iOS) log the FCM token with a `TODO` instead of writing it to
  `users/{uid}.deviceTokens` in Firestore — the Cloud Function reads from
  there, so pushes won't actually arrive until this is wired up.
- **iOS Kotlin/Swift bridging.** `ViewModelObservers.swift` assumes Kotlin
  `Flow`s are consumable with `for await` from Swift. That works out of
  the box with [SKIE](https://skie.touchlab.co/) (recommended — add the
  Touchlab SKIE Gradle plugin to `shared/build.gradle.kts`) or with a
  small hand-rolled `Flow -> AsyncSequence` wrapper if you'd rather not
  add SKIE. Either way, treat the exact bridged type names
  (`Kotlinx_datetimeLocalDate`, `SyncStatus.offline` vs `.OFFLINE`, etc.)
  as something to confirm against Xcode's generated interface on first
  build, not as guaranteed exact — Kotlin/Swift interop naming has shifted
  across Kotlin versions.
- **Firestore metadata-change delivery.** `SyncStatus` is derived from each
  snapshot's `metadata.isFromCache`. GitLive's `.snapshots` flow may need
  the "include metadata changes" variant of that call (naming varies by
  version — check `dev.gitlive.firebase.firestore.Query` in the pinned
  release) to transition promptly the moment connectivity returns, rather
  than waiting for the next real data change. Flagged inline in
  `FirestoreTripRepository.kt`.

## Running it

### Prerequisites
- Android Studio (Koala+) with the Kotlin Multiplatform plugin
- Xcode 15+, and [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
- A Firebase project (Firestore + Cloud Messaging enabled, Blaze plan for
  the scheduled Cloud Function)
- Node 20 + the Firebase CLI (`npm i -g firebase-tools`) for deploying functions

### 1. Firebase setup
1. Create a Firebase project, add an Android app (package
   `com.tripmate.android`) and an iOS app (bundle id `com.tripmate.ios`).
2. Download `google-services.json` into `androidApp/` and
   `GoogleService-Info.plist` into `iosApp/TripMate/`.
3. From `/firebase`: `firebase deploy --only firestore:rules,firestore:indexes,functions`

### 2. Android
Open the repo root in Android Studio and run the `androidApp` configuration.

### 3. iOS
```
cd iosApp
xcodegen generate
open TripMate.xcodeproj
```
Add `Terracotta` and `DeepTeal` colors to `Assets.xcassets`, then build and
run. The `preBuildScripts` step in `project.yml` builds the `shared`
framework automatically via Gradle before each Xcode build.

## Next steps
Auth, trip creation, and the two picker stubs above are the fastest path
to a genuinely usable first version; device-token registration is what
makes the push notifications real rather than theoretical.
