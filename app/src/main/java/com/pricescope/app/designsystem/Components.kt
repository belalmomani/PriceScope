package com.pricescope.app.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun PsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Dimens.CardRadius)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    }
}

@Composable
fun PsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Dimens.ControlRadius),
        modifier = modifier.heightIn(min = Dimens.MinTouch)
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text(text)
    }
}

@Composable
fun PsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.CardRadius),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
    )
}

@Composable
fun PsListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PsEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(Dimens.ScreenPadding * 2)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
