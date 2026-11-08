package com.callerpro.app

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task

class GoogleAuthManager(private val activity: Activity) {

    private val firebaseAuth: FirebaseAuth =
        FirebaseAuth.getInstance()

    private val googleSignInClient =
        GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(
                    activity.getString(R.string.default_web_client_id)
                )
                .requestEmail()
                .build()
        )

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?): Task<AuthResult> {
        Log.d(TAG, "Google account result received")

        val accountTask =
            GoogleSignIn.getSignedInAccountFromIntent(data)

        val account: GoogleSignInAccount = try {
            accountTask.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            Log.e(
                TAG,
                "GOOGLE_ACCOUNT_RESULT_FAILED: statusCode=${e.statusCode}, statusMessage=${e.statusMessage}",
                e
            )
            throw IllegalStateException(
                "Google account selection failed. " +
                    "Status code: ${e.statusCode}; " +
                    "Status message: ${e.statusMessage ?: "Not provided"}",
                e
            )
        }

        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            Log.e(TAG, "GOOGLE_ACCOUNT_RESULT_FAILED: ID token is null or blank")
            throw IllegalStateException(
                "Google account was selected, but Google returned no ID token."
            )
        }

        Log.d(TAG, "Google account result succeeded; starting Firebase credential sign-in")

        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        return firebaseAuth.signInWithCredential(credential).addOnFailureListener { error ->
            Log.e(TAG, "FIREBASE_CREDENTIAL_SIGN_IN_FAILED", error)
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            firebaseAuth.signOut()
            onComplete()
        }
    }

    private companion object {
        const val TAG = "CallerProGoogleAuth"
    }
}
