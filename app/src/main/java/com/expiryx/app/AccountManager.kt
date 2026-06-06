package com.expiryx.app

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object AccountManager {
    private const val TAG = "AccountManager"
    
    private var productsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    // Flag to prevent local changes triggered by remote sync from being pushed back
    @Volatile
    var isApplyingRemoteChange = false

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(ProductApplication.instance).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                Log.w(TAG, "Firebase Auth skipped: FirebaseApp not initialized.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth initialization error", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(ProductApplication.instance).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                Log.w(TAG, "Firestore skipped: FirebaseApp not initialized.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization error", e)
            null
        }
    }

    fun isLoggedIn(): Boolean = auth?.currentUser != null

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    fun getUserId(): String? = auth?.currentUser?.uid

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        stopSync()
        auth?.signOut()
        setWelcomeScreenPassed(context, false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun stopSync() {
        productsListener?.remove()
        historyListener?.remove()
        productsListener = null
        historyListener = null
        Log.d(TAG, "Cloud sync listeners stopped.")
    }

    fun isWelcomeScreenPassed(context: Context): Boolean {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("welcomeScreenPassed", false)
    }

    fun setWelcomeScreenPassed(context: Context, passed: Boolean) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("welcomeScreenPassed", passed).apply()
    }
    
    fun startSync(context: Context) {
        val userId = getUserId() ?: return
        val currentFirestore = firestore ?: return
        val repo = (context.applicationContext as ProductApplication).repository
        
        if (!Prefs.isSyncEnabled(context)) {
            stopSync()
            return
        }

        if (productsListener != null) {
            Log.d(TAG, "Sync already active.")
            return
        }

        Log.d(TAG, "Starting modern real-time sync for: $userId")

        // 1. PRODUCTS LISTENER (Incremental Pull)
        productsListener = currentFirestore.collection("users").document(userId)
            .collection("products")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Products listen error", e)
                    return@addSnapshotListener
                }
                
                snapshots?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        processProductChanges(it.documentChanges, repo)
                    }
                }
            }

        // 2. HISTORY LISTENER (Incremental Pull)
        historyListener = currentFirestore.collection("users").document(userId)
            .collection("history")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "History listen error", e)
                    return@addSnapshotListener
                }
                
                snapshots?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        processHistoryChanges(it.documentChanges, repo)
                    }
                }
            }
    }

    private suspend fun processProductChanges(changes: List<DocumentChange>, repo: ProductRepository) {
        isApplyingRemoteChange = true
        try {
            val localProducts = repo.getAllProductsNow()
            for (dc in changes) {
                val cloudProduct = dc.document.toObject(Product::class.java)
                if (cloudProduct.uuid.isBlank()) continue

                when (dc.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        val local = localProducts.find { it.uuid == cloudProduct.uuid }
                        if (local == null) {
                            repo.insertProductLocallyOnly(cloudProduct.copy(id = 0))
                        } else if ((cloudProduct.dateModified ?: 0) > (local.dateModified ?: 0)) {
                            // Only update if the cloud version is newer
                            repo.updateProductLocallyOnly(cloudProduct.copy(id = local.id))
                        }
                    }
                    DocumentChange.Type.REMOVED -> {
                        localProducts.find { it.uuid == cloudProduct.uuid }?.let {
                            repo.deleteProductLocallyOnly(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing remote product changes", e)
        } finally {
            isApplyingRemoteChange = false
        }
    }

    private suspend fun processHistoryChanges(changes: List<DocumentChange>, repo: ProductRepository) {
        isApplyingRemoteChange = true
        try {
            val localHistory = repo.getAllHistoryNow()
            for (dc in changes) {
                val cloudItem = dc.document.toObject(History::class.java)
                if (cloudItem.uuid.isBlank()) continue

                when (dc.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        val local = localHistory.find { it.uuid == cloudItem.uuid }
                        if (local == null) {
                            repo.insertHistoryLocallyOnly(cloudItem.copy(id = 0))
                        }
                    }
                    DocumentChange.Type.REMOVED -> {
                        localHistory.find { it.uuid == cloudItem.uuid }?.let {
                            repo.deleteHistoryEntryLocallyOnly(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing remote history changes", e)
        } finally {
            isApplyingRemoteChange = false
        }
    }

    /**
     * PUSH: Individual product update to Firestore.
     */
    fun pushProductToCloud(product: Product) {
        if (isApplyingRemoteChange) return
        val userId = getUserId() ?: return
        val db = firestore ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .collection("products").document(product.uuid)
                    .set(product, SetOptions.merge())
                    .await()
                Log.d(TAG, "Pushed product to cloud: ${product.uuid}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push product ${product.uuid}", e)
            }
        }
    }

    fun deleteProductFromCloud(productUuid: String) {
        if (isApplyingRemoteChange) return
        val userId = getUserId() ?: return
        val db = firestore ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .collection("products").document(productUuid)
                    .delete()
                    .await()
                Log.d(TAG, "Deleted product from cloud: $productUuid")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete product $productUuid", e)
            }
        }
    }

    fun pushHistoryToCloud(history: History) {
        if (isApplyingRemoteChange) return
        val userId = getUserId() ?: return
        val db = firestore ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .collection("history").document(history.uuid)
                    .set(history, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push history ${history.uuid}", e)
            }
        }
    }

    fun deleteHistoryFromCloud(historyUuid: String) {
        if (isApplyingRemoteChange) return
        val userId = getUserId() ?: return
        val db = firestore ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .collection("history").document(historyUuid)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete history $historyUuid", e)
            }
        }
    }

    fun deleteCloudData(onComplete: (Boolean) -> Unit) {
        val userId = getUserId() ?: return
        val db = firestore ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val products = db.collection("users").document(userId).collection("products").get().await()
                val history = db.collection("users").document(userId).collection("history").get().await()
                
                val batch = db.batch()
                products.forEach { batch.delete(it.reference) }
                history.forEach { batch.delete(it.reference) }
                batch.commit().await()
                
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Full cloud wipe failed", e)
                onComplete(false)
            }
        }
    }
}
