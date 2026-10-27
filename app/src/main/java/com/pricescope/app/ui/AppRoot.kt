package com.pricescope.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pricescope.app.R
import com.pricescope.app.core.AdsManager
import com.pricescope.app.core.AppSettings
import com.pricescope.app.core.Entitlements

enum class Screen { Home, Settings, Country, Currency, Language, Theme, Ads }

@Composable
fun AppRoot(settings: AppSettings) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var previous by rememberSaveable { mutableStateOf(Screen.Home) }
    val isTop = screen == Screen.Home || screen == Screen.Settings
    val context = LocalContext.current
    // Every user is "free" until accounts and billing are connected (see Entitlements).
    val entitlements = remember { Entitlements() }
    val adsChoice = settings.adsPersonalised

    LaunchedEffect(entitlements.adsEnabled, adsChoice) {
        if (entitlements.adsEnabled && adsChoice != null) AdsManager.enable(context, adsChoice)
    }

    if (entitlements.adsEnabled && adsChoice == null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.ads_consent_title)) },
            text = { Text(stringResource(R.string.ads_consent_body)) },
            confirmButton = {
                TextButton(onClick = { settings.setAdsPersonalised(true) }) {
                    Text(stringResource(R.string.ads_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { settings.setAdsPersonalised(false) }) {
                    Text(stringResource(R.string.ads_deny))
                }
            }
        )
    }

    fun open(target: Screen) {
        previous = screen
        screen = target
    }

    BackHandler(enabled = !isTop) { screen = previous }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTop) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = screen == Screen.Home,
                        onClick = { screen = Screen.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_home)) }
                    )
                    NavigationBarItem(
                        selected = screen == Screen.Settings,
                        onClick = { screen = Screen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                Screen.Home -> HomeScreen(settings, onPickCountry = { open(Screen.Country) })
                Screen.Settings -> SettingsScreen(settings, onOpen = { open(it) })
                Screen.Country -> CountryPickerScreen(settings, onBack = { screen = previous })
                Screen.Currency -> CurrencyPickerScreen(settings, onBack = { screen = previous })
                Screen.Language -> LanguageScreen(onBack = { screen = previous })
                Screen.Theme -> ThemeScreen(settings, onBack = { screen = previous })
                Screen.Ads -> AdsScreen(settings, onBack = { screen = previous })
            }
        }
    }
}
