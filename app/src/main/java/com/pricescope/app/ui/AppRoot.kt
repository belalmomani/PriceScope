package com.pricescope.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pricescope.app.R
import com.pricescope.app.core.AppSettings

enum class Screen { Home, Settings, Country, Currency, Language, Theme }

@Composable
fun AppRoot(settings: AppSettings) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var previous by rememberSaveable { mutableStateOf(Screen.Home) }
    val isTop = screen == Screen.Home || screen == Screen.Settings

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
            }
        }
    }
}
