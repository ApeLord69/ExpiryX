package com.expiryx.app

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

import java.util.UUID

@Parcelize
@Entity(tableName = "history_table")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val productUuid: String? = null,
    val productName: String = "",
    val expirationDate: Long? = null,
    val quantity: Int = 1,
    @ColumnInfo(name = "weight") val weight: Int? = null,
    val weightUnit: String = "g", // "g" for grams or "ml" for milliliters
    val brand: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "action") val action: String = "", // "Expired", "Used", "Deleted"
    val timestamp: Long = System.currentTimeMillis(),
    val barcode: String? = null,
    val dateAdded: Long = System.currentTimeMillis(), // Should match Product's dateAdded
    val dateModified: Long? = null,
) : Parcelable