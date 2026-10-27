package com.pricescope.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.pricescope.app.BuildConfig
import com.pricescope.app.R
import com.pricescope.app.core.AppSettings
import com.pricescope.app.core.Countries
import com.pricescope.app.designsystem.Dimens
import com.pricescope.app.designsystem.PsCard
import com.pricescope.app.designsystem.PsListRow
import com.pricescope.app.designsystem.PsSearchField
import java.util.Locale

@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.heightIn(min = Dimens.MinTouch)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        content()
    }
}

@Composable
fun SettingsScreen(settings: AppSettings, onOpen: (Screen) -> Unit) {
    val notSelected = stringResource(R.string.not_selected)
    val languageValue = currentLanguageLabel()
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        PsCard(Modifier.fillMaxWidth()) {
            PsListRow(
                title = stringResource(R.string.setting_country),
                trailing = Countries.nameOf(settings.countryCode) ?: notSelected,
                onClick = { onOpen(Screen.Country) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            PsListRow(
                title = stringResource(R.string.setting_currency),
                trailing = settings.effectiveCurrency ?: notSelected,
                onClick = { onOpen(Screen.Currency) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            PsListRow(
                title = stringResource(R.string.setting_language),
                trailing = languageValue,
                onClick = { onOpen(Screen.Language) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            PsListRow(
                title = stringResource(R.string.setting_theme),
                trailing = themeLabel(settings.themeMode),
                onClick = { onOpen(Screen.Theme) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            PsListRow(
                title = stringResource(R.string.setting_ads),
                trailing = when (settings.adsPersonalised) {
                    true -> stringResource(R.string.ads_personalised)
                    false -> stringResource(R.string.ads_non_personalised)
                    null -> notSelected
                },
                onClick = { onOpen(Screen.Ads) }
            )
        }
        Text(
            stringResource(R.string.version_label, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CountryPickerScreen(settings: AppSettings, onBack: () -> Unit) {
    val countries = remember { Countries.all() }
    var query by rememberSaveable { mutableStateOf("") }
    val deviceCountry = remember { Locale.getDefault().country.takeIf { it.length == 2 } }
    val filtered = remember(query, countries) {
        if (query.isBlank()) countries
        else countries.filter {
            it.name.contains(query.trim(), ignoreCase = true) || it.code.equals(query.trim(), ignoreCase = true)
        }
    }
    SubScreen(stringResource(R.string.setting_country), onBack) {
        Column(Modifier.padding(horizontal = Dimens.ScreenPadding)) {
            PsSearchField(query, { query = it }, stringResource(R.string.search_countries))
            Text(
                stringResource(R.string.country_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            if (query.isBlank() && deviceCountry != null && Countries.nameOf(deviceCountry) != null) {
                item {
                    PsListRow(
                        title = stringResource(R.string.use_device_country),
                        subtitle = Countries.nameOf(deviceCountry),
                        onClick = { settings.setCountry(deviceCountry); onBack() }
                    )
                }
            }
            if (filtered.isEmpty()) {
                item { Text(stringResource(R.string.no_results), modifier = Modifier.padding(Dimens.ScreenPadding)) }
            }
            items(filtered, key = { it.code }) { c ->
                PsListRow(
                    title = c.name,
                    trailing = c.currencyCode,
                    selected = c.code == settings.countryCode,
                    onClick = { settings.setCountry(c.code); onBack() }
                )
            }
        }
    }
}

@Composable
fun CurrencyPickerScreen(settings: AppSettings, onBack: () -> Unit) {
    val currencies = remember { Countries.currencies() }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, currencies) {
        if (query.isBlank()) currencies
        else currencies.filter {
            it.name.contains(query.trim(), ignoreCase = true) || it.code.contains(query.trim(), ignoreCase = true)
        }
    }
    SubScreen(stringResource(R.string.setting_currency), onBack) {
        Column(Modifier.padding(horizontal = Dimens.ScreenPadding)) {
            PsSearchField(query, { query = it }, stringResource(R.string.search_currencies))
            Text(
                stringResource(R.string.currency_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                PsListRow(
                    title = stringResource(R.string.currency_country_default),
                    trailing = Countries.currencyOf(settings.countryCode),
                    selected = settings.currencyCode == null,
                    onClick = { settings.setCurrency(null); onBack() }
                )
            }
            if (filtered.isEmpty()) {
                item { Text(stringResource(R.string.no_results), modifier = Modifier.padding(Dimens.ScreenPadding)) }
            }
            items(filtered, key = { it.code }) { c ->
                PsListRow(
                    title = c.name,
                    trailing = c.code,
                    selected = c.code == settings.currencyCode,
                    onClick = { settings.setCurrency(c.code); onBack() }
                )
            }
        }
    }
}

private data class LanguageOption(val tag: String, val nativeName: String?)

private val languageOptions = listOf(
    LanguageOption("", null),
    LanguageOption("en", "English"),
    LanguageOption("ar", "العربية"),
    LanguageOption("fr", "Français"),
    LanguageOption("es", "Español"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("zh", "简体中文")
)

private fun currentLanguageTag(): String =
    AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.language ?: ""

@Composable
private fun currentLanguageLabel(): String {
    val tag = currentLanguageTag()
    return languageOptions.firstOrNull { it.tag == tag }?.nativeName
        ?: stringResource(R.string.language_system)
}

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val current = remember { currentLanguageTag() }
    SubScreen(stringResource(R.string.setting_language), onBack) {
        Text(
            stringResource(R.string.language_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
        )
        LazyColumn {
            items(languageOptions, key = { it.tag }) { option ->
                PsListRow(
                    title = option.nativeName ?: stringResource(R.string.language_system),
                    selected = option.tag == current,
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(
                            if (option.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                            else LocaleListCompat.forLanguageTags(option.tag)
                        )
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun themeLabel(mode: String): String = when (mode) {
    AppSettings.THEME_LIGHT -> stringResource(R.string.theme_light)
    AppSettings.THEME_DARK -> stringResource(R.string.theme_dark)
    else -> stringResource(R.string.theme_system)
}

@Composable
fun ThemeScreen(settings: AppSettings, onBack: () -> Unit) {
    SubScreen(stringResource(R.string.setting_theme), onBack) {
        listOf(AppSettings.THEME_SYSTEM, AppSettings.THEME_LIGHT, AppSettings.THEME_DARK).forEach { mode ->
            PsListRow(
                title = themeLabel(mode),
                selected = settings.themeMode == mode,
                onClick = { settings.setTheme(mode); onBack() }
            )
        }
    }
}

@Composable
fun AdsScreen(settings: AppSettings, onBack: () -> Unit) {
    SubScreen(stringResource(R.string.setting_ads), onBack) {
        Text(
            stringResource(R.string.ads_consent_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
        )
        PsListRow(
            title = stringResource(R.string.ads_personalised),
            selected = settings.adsPersonalised == true,
            onClick = { settings.setAdsPersonalised(true); onBack() }
        )
        PsListRow(
            title = stringResource(R.string.ads_non_personalised),
            selected = settings.adsPersonalised == false,
            onClick = { settings.setAdsPersonalised(false); onBack() }
        )
    }
}
