package com.pricescope.app.designsystem

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.pricescope.app.R
import com.startapp.sdk.ads.banner.Banner

/** A clearly labelled banner. If the SDK cannot create it, nothing is shown. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.ad_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx -> runCatching<View> { Banner(ctx) }.getOrElse { View(ctx) } }
        )
    }
}
