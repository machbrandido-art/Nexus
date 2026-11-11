package com.nexus.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.BuildConfig
import com.nexus.app.R
import com.nexus.app.data.datastore.Density
import com.nexus.app.data.datastore.ThemeMode
import com.nexus.app.ui.theme.SeedOption
import com.nexus.app.ui.theme.SeedPalette
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamic by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val seed by viewModel.seedColorArgb.collectAsStateWithLifecycle()
    val density by viewModel.density.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
            ).forEach { (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        RadioButton(
                            selected = theme == mode,
                            onClick = { viewModel.setThemeMode(mode) }
                        )
                    },
                    modifier = Modifier
                        .selectable(selected = theme == mode) { viewModel.setThemeMode(mode) }
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_desc)) },
                trailingContent = {
                    Switch(checked = dynamic, onCheckedChange = viewModel::setDynamicColor)
                }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(if (dynamic) 0.5f else 1f)
            ) {
                Text(
                    text = stringResource(R.string.settings_seed_color),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_seed_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(SeedPalette, key = { it.argb }) { option ->
                        SeedSwatch(
                            option = option,
                            selected = option.argb == seed,
                            enabled = !dynamic,
                            onClick = { viewModel.setSeedColor(option.argb) }
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Density") },
                supportingContent = {
                    Text(if (density == Density.COMPACT) "Compact" else "Comfortable")
                },
                trailingContent = {
                    Switch(
                        checked = density == Density.COMPACT,
                        onCheckedChange = {
                            viewModel.setDensity(if (it) Density.COMPACT else Density.COMFORTABLE)
                        }
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }
}

@Composable
private fun SeedSwatch(
    option: SeedOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(option.name) },
        leadingIcon = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(option.argb), CircleShape)
            )
        }
    )
}