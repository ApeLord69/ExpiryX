package com.expiryx.app

import androidx.lifecycle.LiveData

class ProductRepository(
    private val productDao: ProductDao,
    private val historyDao: HistoryDao
) {
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    val allHistory: LiveData<List<History>> = historyDao.getAllHistory()

    suspend fun insertProduct(product: Product) {
        val updatedProduct = product.copy(dateModified = System.currentTimeMillis())
        productDao.insert(updatedProduct)
        AccountManager.pushProductToCloud(updatedProduct)
    }

    suspend fun update(product: Product) {
        val updatedProduct = product.copy(dateModified = System.currentTimeMillis())
        productDao.update(updatedProduct)
        AccountManager.pushProductToCloud(updatedProduct)
    }

    suspend fun insertHistory(history: History) {
        historyDao.insert(history)
        AccountManager.pushHistoryToCloud(history)
    }

    suspend fun deleteProduct(product: Product) {
        val historyEntry = History(
            productUuid = product.uuid,
            productName = product.name,
            expirationDate = product.expirationDate,
            quantity = product.quantity,
            weight = product.weight,
            weightUnit = product.weightUnit,
            brand = product.brand,
            imageUri = product.imageUri,
            isFavorite = product.isFavorite,
            action = "Deleted",
            timestamp = System.currentTimeMillis(),
            barcode = product.barcode, 
            dateAdded = product.dateAdded, 
            dateModified = System.currentTimeMillis()
        )
        
        productDao.delete(product)
        historyDao.insert(historyEntry)
        
        AccountManager.deleteProductFromCloud(product.uuid)
        AccountManager.pushHistoryToCloud(historyEntry)
    }

    suspend fun markAsUsed(product: Product) {
        val historyEntry = History(
            productUuid = product.uuid,
            productName = product.name,
            expirationDate = product.expirationDate,
            quantity = product.quantity,
            weight = product.weight,
            weightUnit = product.weightUnit,
            brand = product.brand,
            imageUri = product.imageUri,
            isFavorite = product.isFavorite,
            action = "Used",
            timestamp = System.currentTimeMillis(),
            barcode = product.barcode, 
            dateAdded = product.dateAdded, 
            dateModified = System.currentTimeMillis()
        )
        
        productDao.delete(product)
        historyDao.insert(historyEntry)
        
        AccountManager.deleteProductFromCloud(product.uuid)
        AccountManager.pushHistoryToCloud(historyEntry)
    }

    suspend fun archiveExpiredProducts() {
        val now = System.currentTimeMillis()
        val all = productDao.getAllProductsNow()
        for (p in all) {
            val expiry = p.expirationDate ?: continue
            if (now - expiry >= (7L * 24 * 60 * 60 * 1000)) {
                val historyEntry = History(
                    productUuid = p.uuid,
                    productName = p.name,
                    expirationDate = p.expirationDate,
                    quantity = p.quantity,
                    weight = p.weight,
                    weightUnit = p.weightUnit,
                    brand = p.brand,
                    imageUri = p.imageUri,
                    isFavorite = p.isFavorite,
                    action = "Expired",
                    timestamp = now,
                    barcode = p.barcode, 
                    dateAdded = p.dateAdded, 
                    dateModified = now
                )
                productDao.delete(p)
                historyDao.insert(historyEntry)
                
                AccountManager.deleteProductFromCloud(p.uuid)
                AccountManager.pushHistoryToCloud(historyEntry)
            }
        }
    }

    suspend fun getAllProductsNow(): List<Product> = productDao.getAllProductsNow()
    suspend fun getAllHistoryNow(): List<History> = historyDao.getAllHistoryNow()
    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    suspend fun clearAllProducts() = productDao.clearAllProducts()
    suspend fun clearAllHistory() = historyDao.clearAllHistory()

    // Sync helpers to avoid loops
    suspend fun insertProductLocallyOnly(product: Product) = productDao.insert(product)
    suspend fun updateProductLocallyOnly(product: Product) = productDao.update(product)
    suspend fun deleteProductLocallyOnly(product: Product) = productDao.delete(product)
    
    suspend fun insertHistoryLocallyOnly(history: History) = historyDao.insert(history)
    suspend fun deleteHistoryEntryLocallyOnly(history: History) = historyDao.deleteById(history.id)

    suspend fun deleteHistoryEntry(history: History) {
        historyDao.deleteById(history.id)
        AccountManager.deleteHistoryFromCloud(history.uuid)
    }

    suspend fun restoreFromHistory(history: History) {
        val product = Product(
            id = 0, 
            uuid = history.productUuid ?: java.util.UUID.randomUUID().toString(),
            name = history.productName,
            expirationDate = history.expirationDate,
            quantity = history.quantity,
            weight = history.weight,
            weightUnit = history.weightUnit,
            brand = history.brand,
            imageUri = history.imageUri,
            isFavorite = history.isFavorite,
            barcode = history.barcode, 
            dateAdded = history.dateAdded, 
            dateModified = System.currentTimeMillis()
        )
        productDao.insert(product)
        historyDao.deleteById(history.id)
        
        AccountManager.pushProductToCloud(product)
        AccountManager.deleteHistoryFromCloud(history.uuid)
    }

    suspend fun restoreWithNewExpiry(history: History, newExpiry: Long) {
        val product = Product(
            id = 0, 
            uuid = history.productUuid ?: java.util.UUID.randomUUID().toString(),
            name = history.productName,
            expirationDate = newExpiry,
            quantity = history.quantity,
            weight = history.weight,
            weightUnit = history.weightUnit,
            brand = history.brand,
            imageUri = history.imageUri,
            isFavorite = history.isFavorite,
            barcode = history.barcode, 
            dateAdded = history.dateAdded, 
            dateModified = System.currentTimeMillis()
        )
        productDao.insert(product)
        historyDao.deleteById(history.id)
        
        AccountManager.pushProductToCloud(product)
        AccountManager.deleteHistoryFromCloud(history.uuid)
    }
}
