package com.expiryx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class ThemedAppCompatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
    }
}
