package com.nuvetrix.wishplay.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.domain.model.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Web client ID from Firebase Console → Authentication → Google → Web SDK config
// This is the OAuth 2.0 client ID for your web application, NOT the Android one.
// Replace this with the actual value from your Firebase project.
private const val WEB_CLIENT_ID =
    "821356552132-o9janc3riml5nfsamptnp0g3itv0717j.apps.googleusercontent.com"

@Singleton
class AuthRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val wishlistDao: WishlistDao,
    private val syncRepository: SyncRepository,
    @ApplicationContext private val context: Context
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    /** Current signed-in Firebase user, null if guest. */
    val firebaseUser: FirebaseUser? get() = firebaseAuth.currentUser

    val currentUser: Flow<AuthUser> = combine(
        userPreferences.userId,
        userPreferences.userEmail,
        userPreferences.userName,
        userPreferences.userAvatarUrl,
        userPreferences.userRole,
        userPreferences.isPro
    ) { args: Array<Any?> ->
        val id = args[0] as String?
        val email = args[1] as String?
        val name = args[2] as String?
        val avatar = args[3] as String?
        val rawRole = (args[4] as? String) ?: "guest"
        val isPro = (args[5] as? Boolean) ?: false

        val effectiveEmail = firebaseAuth.currentUser?.email ?: email ?: ""
        val isAdmin = effectiveEmail.equals("hyathis.x@gmail.com", ignoreCase = true) ||
            effectiveEmail.equals("mdliad.se@gmail.com", ignoreCase = true) ||
            rawRole == "admin"
        val role = if (isAdmin) "admin" else rawRole

        if (role == "guest" || (id.isNullOrBlank() && firebaseAuth.currentUser == null)) {
            AuthUser(
                id = "",
                email = "List stored on this phone only",
                name = "Guest",
                avatarUrl = null,
                role = "guest",
                isPro = false
            )
        } else {
            AuthUser(
                id = id ?: firebaseAuth.currentUser?.uid ?: "",
                email = effectiveEmail,
                name = name ?: firebaseAuth.currentUser?.displayName ?: "You",
                avatarUrl = avatar ?: firebaseAuth.currentUser?.photoUrl?.toString(),
                role = role,
                isPro = isPro || role == "pro" || isAdmin
            )
        }
    }

    /**
     * Signs the user in with Google using Android Credential Manager.
     * Obtains a Google ID token, exchanges it for a Firebase credential,
     * then upserts the Firestore profile and persists the session locally.
     *
     * [activityContext] must be an Activity context to show the account picker.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> =
        withContext(Dispatchers.Main) {
            try {
                // 1. Build a Google credential request
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 2. Launch Credential Manager picker
                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                val credential = result.credential
                if (credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    return@withContext Result.failure(Exception("Unexpected credential type"))
                }

                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken

                // 3. Exchange Google ID token for a Firebase credential
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth
                    .signInWithCredential(firebaseCredential)
                    .await()

                val fbUser = authResult.user
                    ?: return@withContext Result.failure(Exception("Firebase sign-in returned null user"))

                // 4. Persist user profile to Firestore (upsert — merge: true)
                withContext(Dispatchers.IO) {
                    upsertFirestoreProfile(fbUser)
                }

                val email = fbUser.email ?: ""
                val isAdmin = email.equals("hyathis.x@gmail.com", ignoreCase = true) ||
                    email.equals("mdliad.se@gmail.com", ignoreCase = true) ||
                    withContext(Dispatchers.IO) { checkIsAdminFromFirestore(fbUser.uid) }
                val isPro = isAdmin || withContext(Dispatchers.IO) { fetchIsProFromFirestore(fbUser.uid) }
                val role = if (isAdmin) "admin" else if (isPro) "pro" else "free"

                // 6. Save to local prefs
                userPreferences.setUserData(
                    id = fbUser.uid,
                    email = email,
                    name = fbUser.displayName ?: "You",
                    avatarUrl = fbUser.photoUrl?.toString(),
                    role = role,
                    isProUser = isPro
                )

                // 7. Merge guest wishlist into cloud account
                withContext(Dispatchers.IO) {
                    syncRepository.mergeGuestListOnSignIn(fbUser.uid)
                }

                val user = AuthUser(
                    id = fbUser.uid,
                    email = email,
                    name = fbUser.displayName ?: "You",
                    avatarUrl = fbUser.photoUrl?.toString(),
                    role = role,
                    isPro = isPro
                )
                Result.success(user)

            } catch (e: GetCredentialCancellationException) {
                Result.failure(Exception("Sign-in cancelled"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Signs the user out of both Firebase Auth and Credential Manager,
     * then clears the local session.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            userPreferences.clearUserData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes the user's Firestore data and Firebase Auth account,
     * then wipes the local Room database and preferences.
     */
    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fbUser = firebaseAuth.currentUser
                ?: return@withContext Result.failure(Exception("Not signed in"))

            // Delete Firestore sub-collections
            deleteFirestoreUserData(fbUser.uid)

            // Delete Firebase Auth user
            fbUser.delete().await()

            // Clear local DB
            wishlistDao.clearAllWishlist()
            userPreferences.clearUserData()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────── Firestore helpers ───────────────────────────

    private suspend fun upsertFirestoreProfile(user: FirebaseUser) {
        val profileData = mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("users")
            .document(user.uid)
            .set(profileData, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    private suspend fun fetchIsProFromFirestore(uid: String): Boolean {
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("entitlements")
                .document("pro")
                .get()
                .await()
            val revokedAt = doc.getTimestamp("revokedAt")
            doc.exists() && revokedAt == null
        } catch (_: Exception) {
            // If Firestore is unreachable, fall back to cached value
            userPreferences.isPro.first()
        }
    }

    private suspend fun checkIsAdminFromFirestore(uid: String): Boolean {
        return try {
            val doc = firestore.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun deleteFirestoreUserData(uid: String) {
        // Delete wishlist sub-collection items
        val wishlistRef = firestore.collection("users").document(uid).collection("wishlist")
        val items = wishlistRef.get().await()
        for (doc in items.documents) {
            doc.reference.delete().await()
        }
        // Delete entitlements
        val entRef = firestore.collection("users").document(uid).collection("entitlements")
        val ents = entRef.get().await()
        for (doc in ents.documents) {
            doc.reference.delete().await()
        }
        // Delete profile doc
        firestore.collection("users").document(uid).delete().await()
    }
}
