package com.pricescope.app.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pricescope.app.BuildConfig
import com.startapp.sdk.adsbase.StartAppSDK

/**
 * Single place that touches the Start.io SDK. It is initialised only after the user has
 * answered the ads prompt and only for tiers that show ads. Any SDK failure is swallowed:
 * ads must never crash the app or block navigation.
 */
object AdsManager {
    // Start.io application id supplied for this app.
    private const val START_IO_APP_ID = "209437292"

    @Volatile private var initialized = false

    var ready by mutableStateOf(false)
        private set

    fun enable(context: Context, personalised: Boolean) {
        ready = runCatching {
            val app = context.applicationContext
            if (!initialized) {
                if (BuildConfig.DEBUG) StartAppSDK.setTestAdsEnabled(true)
                StartAppSDK.init(app, START_IO_APP_ID, false) // false = no return ads
                initialized = true
            }
            // Required consent flag ("pas"); re-sent every time the choice changes.
            StartAppSDK.setUserConsent(app, "pas", System.currentTimeMillis(), personalised)
            true
        }.getOrDefault(false)
    }
}
