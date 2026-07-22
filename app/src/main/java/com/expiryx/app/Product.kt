package com.expiryx.app

import android.os.Parcelable
import androidx.room.ColumnInfo // Ensure this import is present
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

import java.util.UUID

@Parcelize
@Entity(tableName = "product_table")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Int = 1,
    val expirationDate: Long? = null,
    val brand: String? = null,
    @ColumnInfo(name = "weight") val weight: Int? = null, // Explicit ColumnInfo
    val weightUnit: String = "g", // "g" for grams or "ml" for milliliters
    val imageUri: String? = null,
    val reminderDays: Int = 3,
    val isFavorite: Boolean = false,
    val barcode: String? = null, // Barcode from scanning or image upload
    val isSnoozed: Boolean = false, // Whether notifications for this item are muted
    val dateAdded: Long = System.currentTimeMillis(), // When product was first created
    val dateModified: Long? = null, // When product was last updated (null for new products)
) : Parcelable
// Trivial change to ensure recompilation