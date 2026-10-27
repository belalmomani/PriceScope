package com.pricescope.app.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * User choices. Country, display currency and language are independent of each other:
 * changing one never changes another. Currency null means "currency of the chosen country".
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("pricescope_prefs", Context.MODE_PRIVATE)

    var countryCode by mutableStateOf(prefs.getString(KEY_COUNTRY, null))
        private set
    var currencyCode by mutableStateOf(prefs.getString(KEY_CURRENCY, null))
        private set
    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM)
        private set

    /** null = the user has not answered the ads prompt yet. */
    var adsPersonalised by mutableStateOf<Boolean?>(
        if (prefs.contains(KEY_ADS)) prefs.getBoolean(KEY_ADS, false) else null
    )
        private set

    val effectiveCurrency: String?
        get() = currencyCode ?: Countries.currencyOf(countryCode)

    fun setCountry(code: String?) {
        countryCode = code
        save(KEY_COUNTRY, code)
    }

    fun setCurrency(code: String?) {
        currencyCode = code
        save(KEY_CURRENCY, code)
    }

    fun setTheme(mode: String) {
        themeMode = mode
        save(KEY_THEME, mode)
    }

    fun setAdsPersonalised(value: Boolean) {
        adsPersonalised = value
        prefs.edit().putBoolean(KEY_ADS, value).apply()
    }

    private fun save(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val KEY_COUNTRY = "country"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_THEME = "theme"
        private const val KEY_ADS = "ads_personalised"
    }
}
