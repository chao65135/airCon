package com.aircon.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aircon.app.R
import com.aircon.app.data.prefs.AppSettings
import com.aircon.app.data.source.DataSourceIds
import com.aircon.app.domain.model.AqiStandard
import com.aircon.app.ui.components.SectionTitle
import com.aircon.app.ui.theme.displayName

/** 刷新间隔候选值（分钟）。 */
private val REFRESH_INTERVALS = listOf(30, 60, 180)

/**
 * 设置页。
 */
@Composable
fun SettingsScreen(
    settings: AppSettings?,
    onUseCelsiusChange: (Boolean) -> Unit,
    onAqiStandardChange: (AqiStandard) -> Unit,
    onPrimarySourceChange: (String) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    onSaveQWeather: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (settings == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionTitle(text = stringResource(R.string.settings_temperature_unit))
            SettingsCard {
                RadioRow(
                    label = stringResource(R.string.unit_celsius),
                    selected = settings.useCelsius,
                    onClick = { onUseCelsiusChange(true) },
                )
                RadioRow(
                    label = stringResource(R.string.unit_fahrenheit),
                    selected = !settings.useCelsius,
                    onClick = { onUseCelsiusChange(false) },
                )
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.settings_aqi_standard))
            SettingsCard {
                AqiStandard.values().forEach { standard ->
                    RadioRow(
                        label = standard.displayName(),
                        selected = settings.aqiStandard == standard,
                        onClick = { onAqiStandardChange(standard) },
                    )
                }
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.settings_primary_source))
            SettingsCard {
                RadioRow(
                    label = "Open-Meteo",
                    selected = settings.primarySourceId == DataSourceIds.OPEN_METEO,
                    onClick = { onPrimarySourceChange(DataSourceIds.OPEN_METEO) },
                )
                RadioRow(
                    label = stringResource(R.string.settings_qweather_title),
                    selected = settings.primarySourceId == DataSourceIds.QWEATHER,
                    onClick = { onPrimarySourceChange(DataSourceIds.QWEATHER) },
                )
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.settings_refresh_interval))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REFRESH_INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = settings.refreshIntervalMinutes == minutes,
                        onClick = { onRefreshIntervalChange(minutes) },
                        label = {
                            Text(stringResource(R.string.settings_refresh_interval_value, minutes))
                        },
                    )
                }
            }
        }

        item { QWeatherSection(settings = settings, onSave = onSaveQWeather) }

        item {
            SectionTitle(text = stringResource(R.string.section_about))
            SettingsCard {
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** 和风天气凭证配置。 */
@Composable
private fun QWeatherSection(
    settings: AppSettings,
    onSave: (String, String, String) -> Unit,
) {
    var host by remember(settings.qweatherApiHost) { mutableStateOf(settings.qweatherApiHost) }
    var apiKey by remember(settings.qweatherApiKey) { mutableStateOf(settings.qweatherApiKey) }
    var token by remember(settings.qweatherToken) { mutableStateOf(settings.qweatherToken) }
    var saved by remember { mutableStateOf(false) }

    SectionTitle(text = stringResource(R.string.settings_qweather_title))
    SettingsCard {
        Text(
            text = stringResource(R.string.settings_qweather_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = host,
            onValueChange = {
                host = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.settings_qweather_host)) },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.settings_qweather_key)) },
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.settings_qweather_token)) },
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                onSave(host, apiKey, token)
                saved = true
            }) {
                Text(stringResource(R.string.settings_save))
            }
            if (saved) {
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.settings_saved),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 统一的设置分组卡片。 */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            content()
        }
    }
}

/** 单选项行。 */
@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
