package com.expiryx.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : ThemedAppCompatActivity() {

    private var googleSignInClient: GoogleSignInClient? = null
    private lateinit var progressBar: ProgressBar
    
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to get FirebaseAuth instance", e)
            null
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken.isNullOrBlank()) {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_google_sign_in_failed, "Missing ID token"),
                        Toast.LENGTH_LONG
                    ).show()
                    return@registerForActivityResult
                }
                firebaseAuthWithGoogle(idToken)
            } catch (e: ApiException) {
                setLoading(false)
                Log.e("LoginActivity", "Google sign in failed code=${e.statusCode}", e)
                val msg = when(e.statusCode) {
                    7 -> "Network Error"
                    10 -> "Developer Error (Check SHA-1 in Firebase)"
                    12500 -> "Sign-in Failed (Check configuration)"
                    12501 -> "Sign-in Cancelled"
                    else -> e.message ?: "Unknown error"
                }
                Toast.makeText(this, "Google sign in failed: $msg", Toast.LENGTH_LONG).show()
            }
        } else {
            setLoading(false)
            Log.w("LoginActivity", "Sign-in result not OK: ${result.resultCode}")
            if (result.resultCode != RESULT_CANCELED) {
                Toast.makeText(this, "Sign-in failed (result=${result.resultCode})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forceLogin = intent.getBooleanExtra("force_login", false)
        
        // 1. If user is already logged in, always go to Main (unless we're explicitly trying to change accounts)
        if (AccountManager.isLoggedIn() && !forceLogin) {
            navigateToMain()
            return
        }
        
        // 2. If NOT logged in, but they've already "passed" the welcome screen as a guest, go to Main
        // But if they clicked "Manage" or "Sign In" from settings (forceLogin=true), we show the page
        if (!AccountManager.isLoggedIn() && AccountManager.isWelcomeScreenPassed(this) && !forceLogin) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)
        progressBar = findViewById(R.id.progressBarLogin)

        if (auth == null) {
            Log.e("LoginActivity", "FirebaseAuth is null!")
            Toast.makeText(this, "Firebase initialization error", Toast.LENGTH_LONG).show()
        }

        // Configure Google Sign In
        val gso = try {
            Log.d("LoginActivity", "Configuring GoogleSignInOptions")
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to configure GoogleSignInOptions. Check google-services.json.", e)
            null
        }

        if (gso == null) {
            Log.e("LoginActivity", "GSO is null, sign-in will not work")
        }

        googleSignInClient = gso?.let { GoogleSignIn.getClient(this, it) }

        findViewById<Button>(R.id.buttonGoogleSignIn).setOnClickListener {
            Log.d("LoginActivity", "Google Sign-In button clicked")
            googleSignInClient?.let {
                Log.d("LoginActivity", "Launching sign-in intent")
                setLoading(true)
                signInLauncher.launch(it.signInIntent)
            } ?: run {
                Log.e("LoginActivity", "googleSignInClient is null")
                Toast.makeText(this, "Google Sign-In is currently unavailable.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.buttonContinue).setOnClickListener {
            AccountManager.setWelcomeScreenPassed(this, true)
            navigateToMain()
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.buttonGoogleSignIn).isEnabled = !loading
        findViewById<Button>(R.id.buttonContinue).isEnabled = !loading
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val currentAuth = auth
        if (currentAuth == null) {
            setLoading(false)
            Toast.makeText(this, getString(R.string.error_firebase_unavailable), Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        currentAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    AccountManager.setWelcomeScreenPassed(this, true)
                    AccountManager.startSync(this)
                    navigateToMain()
                } else {
                    setLoading(false)
                    Log.e("LoginActivity", "Firebase auth failed", task.exception)
                    Toast.makeText(this, getString(R.string.error_auth_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        Log.d("LoginActivity", "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
