# TripMate — working notes for Claude

This file exists so a new Claude session (or a teammate's) doesn't have to
rediscover the architecture and its gotchas by grepping the whole repo every
time. Read this before making changes; update it when a pattern changes.

## Stack

- KMM shared module (`shared/`) + Android app (`androidApp/`) + iOS app
  (`iosApp/`, Swift — not yet actively built this cycle).
- Firestore is the source of truth. SQLDelight (`shared/src/commonMain/sqldelight/`)
  is a local read cache. **UI never reads Firestore directly** — it reads
  SQLDelight-backed `Flow`s, which a Firestore snapshot listener keeps fresh.
- Jetpack Compose Material3 on Android. Koin for DI. kotlinx-datetime for dates.

## Architecture: layered MVVM + Repository, feature-first, no use-case layer

Model → `.sq` (schema + queries) → mapping (`toDomain()`, colocated with the
repository that uses it) → Repository interface + Firestore-backed impl →
ViewModel → Compose screen → Koin wiring → nav route.

We deliberately do **not** use a full Clean-Architecture use-case/interactor
layer — for CRUD-shaped screens like these it's ceremony that costs more
tokens/files to read and write than it saves. Add a use-case class only if a
specific feature grows real multi-step orchestration logic worth isolating.

**One repository interface per feature**, not one growing god-interface:
- `TripRepository` — trips
- `ActivityRepository` — activities (depends on `TripRepository` for
  `observeNextActivity`, the one place activity and trip data cross)
- `PackingRepository`, `BudgetRepository` (expenses), `DocumentRepository`
- `SyncCoordinator` — the one cross-cutting piece: owns the Firestore
  snapshot listeners for every collection and writes them into SQLDelight.
  Exposes `syncStatus` and `startSync`/`stopSync`. Lives separately because
  sync is a session lifecycle concern, not any one feature's.

Each feature's Firestore-backed repository impl (`Firestore<Feature>Repository.kt`)
carries its own `<Entity>.toDomain()` mapping function — mappings are
colocated with their repository, not centralized in one mapping file, so
touching one feature only touches one small file.

Each domain model lives in its own file under `shared/.../model/`
(`Trip.kt`, `Activity.kt`, `PackingItem.kt`, `Expense.kt`, `TripDocument.kt`)
— don't add a new model to an existing file "just this once"; give it its
own file, even if small.

## Recipe: adding a new feature (e.g. a new trip sub-resource)

Miss any of the last two steps and the feature will crash on-device even
though it compiles — both have already caused real crashes in this repo.

1. **Model**: new file in `shared/.../model/`.
2. **SQLDelight**: new `.sq` file in `shared/.../sqldelight/com/tripmate/db/`
   (table + `selectXByTrip` / `upsertX` / `deleteX` queries). Booleans are
   `INTEGER`, converted manually (no ColumnAdapter in this codebase).
3. **Bump the schema version**: add a new `N.sqm` migration file with the
   new table's `CREATE TABLE`/`CREATE INDEX` statements (see `2.sqm` for the
   pattern). **Do not skip this.** SQLDelight's schema version only bumps
   when a `.sqm` file is added — without one, `AndroidSqliteDriver` sees the
   version unchanged on an already-installed app and never creates the new
   table, so every read/write against it crashes with "no such table" on any
   device that already had the app installed. Adding the table to the `.sq`
   file alone is only enough for a brand-new install.
4. **Repository**: new `<Feature>Repository` interface + `Firestore<Feature>Repository`
   impl in `shared/.../data/`, with its own `toDomain()` mapping in the same
   file. Add the collection name to `FirestoreCollections`.
5. **Sync**: add a listener block for the new collection in
   `FirestoreSyncCoordinator.startSync()`, following the existing
   trip-ids-keyed pattern.
6. **Firestore rules**: add a `match /<collection>/{id}` block to
   `firebase/firestore.rules` mirroring `activities`' `ownsTrip(...)` check.
   **Do not skip this.** Without a matching rule, every write to that
   collection is denied by default and the app crashes with an uncaught
   `PERMISSION_DENIED`. Rules must be deployed (Firebase console → Firestore
   Database → Rules, or `firebase deploy --only firestore:rules`) — nothing
   in this repo does that automatically.
7. **ViewModel**: takes the narrow `<Feature>Repository`, not `TripRepository`.
8. **Koin**: register the repository as a `single<X>` and the ViewModel as a
   `factory` in `SharedModule.kt`. Add a matching static helper in
   `KoinHelper.kt` for Swift/iOS interop (Kotlin generics/default params
   don't bridge to Swift, so these are thin concretely-typed wrappers).
9. **Screen + nav**: Compose screen in `androidApp/.../ui/screens/`, a nav
   route in `MainActivity.kt`, and an entry point (usually an icon on the
   relevant screen's header).

## Known gotchas (already bitten us once each)

- **SQLDelight schema version** — see step 3 above.
- **Firestore security rules** — see step 6 above.
- Booleans in SQLDelight are `INTEGER` (`1L`/`0L`), converted by hand in
  `toDomain()`/upsert calls.
- `AskUserQuestion`-style tools cap at 4 options.
- Computer-use access to Android Studio is granted at tier "click" only
  (screenshot + left-click, no typing/keys into the IDE) since it's an IDE —
  use `device_bash` (real shell on the Mac) for all file edits, and the IDE's
  Run button + Logcat panel only for building/verifying.
- The device_bash environment is a separate sandboxed Linux VM with a
  restricted network allowlist — it cannot run `./gradlew` (blocked from
  downloading the Gradle distribution) and cannot reach `npm`/`firebase-tools`
  registries reliably. Builds and Firestore rule deploys have to go through
  the actual Android Studio / Firebase console on the user's machine.

## Not yet done (tracked, not forgotten)

- `TripRepository.deleteTrip()` doesn't cascade-delete a trip's activities/
  packing items/expenses/documents in Firestore.
- Shared/collaborative trips (multi-user access) needs an auth model change
  (currently anonymous-only sign-in) — scope this with the user before
  implementing.
