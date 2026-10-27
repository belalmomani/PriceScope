package com.pricescope.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pricescope.app.BuildConfig
import com.pricescope.app.R
import com.pricescope.app.core.AdsManager
import com.pricescope.app.designsystem.AdBanner
import com.pricescope.app.core.AppSettings
import com.pricescope.app.core.Countries
import com.pricescope.app.designsystem.Dimens
import com.pricescope.app.designsystem.PsCard
import com.pricescope.app.designsystem.PsEmptyState
import com.pricescope.app.designsystem.PsSearchField

@Composable
fun HomeScreen(settings: AppSettings, onPickCountry: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    val countryName = Countries.nameOf(settings.countryCode)

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding)
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PsCard(onClick = onPickCountry, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
                modifier = Modifier
                    .heightIn(min = Dimens.MinTouch)
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (countryName != null)
                        stringResource(R.string.country_chip_prefix) + ": " + countryName
                    else stringResource(R.string.choose_country),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        PsSearchField(
            value = query,
            onValueChange = { query = it; searched = false },
            hint = stringResource(R.string.search_hint),
            onSearch = { if (query.isNotBlank()) searched = true }
        )

        if (settings.countryCode == null) {
            PsEmptyState(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.select_country_first_title),
                body = stringResource(R.string.select_country_first_body)
            )
        } else if (searched) {
            // No data source is connected yet, so no results are ever invented.
            PsEmptyState(
                icon = Icons.Default.Info,
                title = stringResource(R.string.no_sources_title),
                body = stringResource(R.string.no_sources_body)
            )
        }

        // Debug builds only: lets us verify the Start.io pipeline with TEST ads.
        // Release builds show banners only next to real results, never on empty screens.
        if (BuildConfig.DEBUG && AdsManager.ready) {
            AdBanner()
        }
    }
}
