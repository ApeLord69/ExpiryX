package com.expiryx.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ProductApplication : Application() {
    companion object {
        lateinit var instance: ProductApplication
            private set
    }

    val database: ProductDatabase by lazy { ProductDatabase.getDatabase(this) }
    val repository: ProductRepository by lazy {
        ProductRepository(database.productDao(), database.historyDao())
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize Firebase safely
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("ProductApplication", "Firebase initialization failed. Make sure google-services.json is present and the plugin is enabled.", e)
        }

        // Initialize theme based on user preference or system default
        ThemeManager.initializeTheme(this)

        // Initialize Cloud Sync if enabled
        if (AccountManager.isLoggedIn() && Prefs.isSyncEnabled(this)) {
            AccountManager.startSync(this)
        }
    }
}