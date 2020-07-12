package com.michaem.android.onetap

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.squareup.picasso.Picasso

class SecondActivity : AppCompatActivity() {

    private lateinit var oneTapClient: SignInClient

    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val profileImage by lazy { findViewById<ImageView>(R.id.profileImage) }
    private val infoText by lazy { findViewById<TextView>(R.id.infoText) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        oneTapClient = Identity.getSignInClient(this)
        val credential = intent.extras?.get(EXTRA_CREDENTIAL) as SignInCredential

        toolbar.setNavigationOnClickListener {
            signOut()
        }

        Picasso.get()
            .load(credential.profilePictureUri)
            .error(R.drawable.ic_android)
            .into(profileImage)

        infoText.text = getString(R.string.user, credential.displayName, credential.id)
    }

    private fun signOut() {
        oneTapClient
            .signOut()
            .addOnSuccessListener {
                Log.d(TAG, "Success sign out")
                finish()
            }
            .addOnFailureListener { e ->
                Log.d(TAG, e.localizedMessage ?: "null")
            }

    }
}
