package com.michaem.android.onetap

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

	private val parentView: View by lazy {
		findViewById<View>(android.R.id.content)
	}

	private lateinit var oneTapClient: SignInClient
	private lateinit var signInRequest: BeginSignInRequest

	private var showOneTapUI = true
	private var onlyAuthorizedAccounts = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		oneTapClient = Identity.getSignInClient(this)
		signInRequest = BeginSignInRequest.builder()
			.setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
				.setSupported(true)
				.build())
			.setGoogleIdTokenRequestOptions(
				BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
					.setSupported(true)
					// Your server's client ID, not your Android client ID.
					.setServerClientId(getString(R.string.your_web_client_id))
					// Only show accounts previously used to sign in.
					.setFilterByAuthorizedAccounts(onlyAuthorizedAccounts)
					.build())
			.build()

		findViewById<Button>(R.id.signInButton).setOnClickListener { signIn() }

		findViewById<Button>(R.id.signOutButton).setOnClickListener { signOut() }

		findViewById<Button>(R.id.signUpButton).setOnClickListener { signUp() }
	}

	private fun signUp() {
		onlyAuthorizedAccounts = false
		beginSignIn()
	}

	private fun signOut() {
		oneTapClient
			.signOut()
			.addOnSuccessListener {
				Log.d(TAG, "Success sign out")
				Snackbar.make(parentView, "Sign out success", Snackbar.LENGTH_LONG).show()
			}
			.addOnFailureListener { e ->
				Log.d(TAG, e.localizedMessage)
				Snackbar.make(parentView, "Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
			}

	}

	private fun signIn() {
		onlyAuthorizedAccounts = true
		beginSignIn()
	}

	private fun beginSignIn() {
		oneTapClient
			.beginSignIn(signInRequest)
			.addOnSuccessListener(this) { result ->
				try {
					startIntentSenderForResult(
						result.pendingIntent.intentSender, REQ_ONE_TAP,
						null, 0, 0, 0, null)
				} catch (e: IntentSender.SendIntentException) {
					Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
				}
			}
			.addOnFailureListener(this) { e ->
				// No saved credentials found. Launch the One Tap sign-up flow, or
				// do nothing and continue presenting the signed-out UI.
				Log.d(TAG, e.localizedMessage)
				Snackbar.make(parentView, "Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
			}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		when (requestCode) {
			REQ_ONE_TAP -> {
				try {
					val credential = oneTapClient.getSignInCredentialFromIntent(data)
					val idToken = credential.googleIdToken
					val username = credential.id
					val password = credential.password
					when {
						idToken != null -> {
							// Got an ID token from Google. Use it to authenticate
							// with your backend.
							Log.d(TAG, "Got ID token: $idToken")
						}
						username != null -> {
							Log.d(TAG, "Got name: $username")
						}
						password != null -> {
							// Got a saved username and password. Use them to authenticate
							// with your backend.
							Log.d(TAG, "Got password: $password")
						}
						else -> {
							// Shouldn't happen.
							Log.d(TAG, "No ID token or password!")
						}
					}
				} catch (e: ApiException) {
					when (e.statusCode) {
						CommonStatusCodes.CANCELED -> {
							Log.d(TAG, "One-tap dialog was closed.")
							// Don't re-prompt the user.
							showOneTapUI = false
						}
						CommonStatusCodes.NETWORK_ERROR -> {
							Log.d(TAG, "One-tap encountered a network error.")
							// Try again or just ignore.
						}
						else -> {
							Log.d(TAG, "Couldn't get credential from result." +
									" (${e.localizedMessage})")
						}
					}
				}
			}
		}
	}

	private companion object {
		const val REQ_ONE_TAP = 12345
		const val TAG = "AndroidOneTap"

	}
}