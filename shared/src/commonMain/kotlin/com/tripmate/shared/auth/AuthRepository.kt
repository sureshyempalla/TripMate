package com.tripmate.shared.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/**
 * There's no login UI yet, but the Firestore security rules require
 * `request.auth != null` on every read/write and check `ownerId` against
 * `request.auth.uid` — so every device needs a real Firebase Auth identity
 * before it can touch Firestore. This signs each device in anonymously and
 * hands back its stable uid; swapping in real accounts later only changes
 * this class; the ViewModels that consume the uid don't change.
 */
class AuthRepository {
    /** The signed-in uid, signing in anonymously first if no session exists yet. */
    suspend fun ensureSignedIn(): String {
        Firebase.auth.currentUser?.let { return it.uid }
        return requireNotNull(Firebase.auth.signInAnonymously().user) {
            "Anonymous sign-in returned no user"
        }.uid
    }
}
