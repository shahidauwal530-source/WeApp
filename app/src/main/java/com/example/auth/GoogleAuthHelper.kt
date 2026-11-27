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
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
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

    private fun resolveWebClientId(context: Context): String = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (_: Throwable) {
        ""
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
        val clientId = resolveWebClientId(context)
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
        val clientId = resolveWebClientId(context)
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
                    onAuthError("Unexpected credential type returned by Google Sign-In.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credential in CredentialManager; launching Firebase OAuth Google popup fallback.", e)
                signInWithFirebaseGooglePopup(
                    activity = activity,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    onAuthCancelled = onAuthCancelled
                )
            } catch (e: GetCredentialUnsupportedException) {
                Log.w(TAG, "CredentialManager unsupported on device; launching Firebase OAuth Google popup fallback.", e)
                signInWithFirebaseGooglePopup(
                    activity = activity,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    onAuthCancelled = onAuthCancelled
                )
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.w(TAG, "CredentialManager provider unavailable; launching Firebase OAuth Google popup fallback.", e)
                signInWithFirebaseGooglePopup(
                    activity = activity,
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    onAuthCancelled = onAuthCancelled
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
            }
        }
    }

    private suspend fun signInWithFirebaseGooglePopup(
        activity: Activity,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        try {
            val auth = Firebase.auth
            val pendingTask = auth.pendingAuthResult
            if (pendingTask != null) {
                pendingTask.await()
                onAuthSuccess()
                return
            }
            val provider = OAuthProvider.newBuilder("google.com")
                .addCustomParameter("prompt", "select_account")
                .build()
            auth.startActivityForSignInWithProvider(activity, provider).await()
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthWebException) {
            Log.w(TAG, "Firebase Google OAuth popup cancelled or closed: ${e.errorCode}", e)
            if (e.errorCode.contains("CANCELED", ignoreCase = true) ||
                e.errorCode.contains("CANCELLED", ignoreCase = true) ||
                e.message?.contains("cancel", ignoreCase = true) == true
            ) {
                onAuthCancelled()
            } else {
                onAuthError(e.localizedMessage ?: "Google Sign-In was cancelled or failed.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase Google OAuth popup failed", e)
            if (e.message?.contains("cancel", ignoreCase = true) == true) {
                onAuthCancelled()
            } else {
                onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
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
