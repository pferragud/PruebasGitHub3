package com.pmdm.pmdm08_pruegagithub3

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {

        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            db.collection("users").document(userEmail).set(
                hashMapOf("token" to token)
            )
        } else {
            // Si no tenemos email, no podemos registrar el token
        }
    }
}