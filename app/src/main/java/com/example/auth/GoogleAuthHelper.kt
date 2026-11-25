package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "Auth"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        val hasFirebase = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
        if (!hasFirebase) {
            onUnauthenticated()
            return
        }
        val alreadySignedIn = try {
            Firebase.auth.currentUser != null
        } catch (_: Throwable) {
            false
        }
        if (alreadySignedIn) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id).trim()
        } catch (_: Throwable) {
            ""
        }
        if (clientId.isEmpty()) {
            onUnauthenticated()
            return
        }
        val request = try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
        } catch (_: Throwable) {
            onUnauthenticated()
            return
        }
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Throwable) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val hasFirebase = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
        if (!hasFirebase) {
            onAuthError("Firebase is not configured for this build.")
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id).trim()
        } catch (_: Throwable) {
            ""
        }
        if (clientId.isEmpty()) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Activity context required for Google Sign-In")
            return
        }
        val request = try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
        } catch (e: Throwable) {
            onAuthError(e.localizedMessage ?: "Unable to prepare Google Sign-In")
            return
        }
        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Throwable) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    try {
                        Firebase.auth.signOut()
                    } catch (_: Throwable) {
                    }
                    onSignOutComplete()
                }
            }
        }
    }
}
