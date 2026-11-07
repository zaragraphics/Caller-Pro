
package com.callerpro.app

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object UserProfileRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        user: FirebaseUser,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userData = hashMapOf(
            "uid" to user.uid,
            "name" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "provider" to "google",
            "status" to "active",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(user.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(
                    error.localizedMessage
                        ?: "Unable to save user profile."
                )
            }
    }
}
