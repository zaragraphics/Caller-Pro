
package com.callerpro.app

import android.app.Activity
import android.content.Intent
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
        val accountTask =
            GoogleSignIn.getSignedInAccountFromIntent(data)

        val account: GoogleSignInAccount =
            accountTask.getResult(ApiException::class.java)

        val credential = GoogleAuthProvider.getCredential(
            account.idToken,
            null
        )

        return firebaseAuth.signInWithCredential(credential)
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            firebaseAuth.signOut()
            onComplete()
        }
    }
}
