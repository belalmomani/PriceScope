package com.pricescope.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.pricescope.app.core.AppSettings
import com.pricescope.app.designsystem.PriceScopeTheme
import com.pricescope.app.ui.AppRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = AppSettings(applicationContext)
        setContent {
            PriceScopeTheme(themeMode = settings.themeMode) {
                AppRoot(settings)
            }
        }
    }
}
