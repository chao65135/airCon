package com.aircon.app.ui.weather

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aircon.app.R
import com.aircon.app.domain.model.CurrentWeather
import com.aircon.app.domain.weather.WeatherCodeMapper
import com.aircon.app.ui.components.Metric
import com.aircon.app.ui.components.NoticeCard
import com.aircon.app.ui.theme.Temperature
import com.aircon.app.util.TimeUtils
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * 首页：实况 + 空气质量 + 逐小时 + 未来数日。
 *
 * 不内建 Scaffold，由外层 AirConApp 统一提供顶栏与底部导航。
 */
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    modifier: Modifier = Modifier,
) {
    val snapshot = state.snapshot
    val zone = remember(snapshot?.city?.timezone) {
        zoneOf(snapshot?.city?.timezone)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.isLoading && snapshot == null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.msg_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                NoticeCard(
                    text = stringResource(R.string.msg_load_failed, message),
                    isError = true,
                )
            }
        }
        if (state.isOffline) {
            item { NoticeCard(text = stringResource(R.string.msg_offline_cache)) }
        }
        state.fallbackMessage?.let { message ->
            item { NoticeCard(text = message) }
        }

        if (snapshot == null) {
            if (!state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.label_no_city),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.label_add_first_city),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                CurrentWeatherCard(
                    current = snapshot.current,
                    cityName = snapshot.city.displayName,
                    weatherText = WeatherCodeMapper.description(snapshot.current.weatherCode),
                    useCelsius = state.useCelsius,
                )
            }
            snapshot.airQuality?.let { air ->
                item { AqiCard(air = air, standardLabel = state.aqiStandardLabel) }
            }
            if (snapshot.hourly.isNotEmpty()) {
                item {
                    HourlySection(
                        items = snapshot.hourly.take(24),
                        useCelsius = state.useCelsius,
                        zone = zone,
                    )
                }
            }
            if (snapshot.daily.isNotEmpty()) {
                item {
                    DailySection(
                        items = snapshot.daily,
                        useCelsius = state.useCelsius,
                        zone = zone,
                    )
                }
            }
            item {
                AttributionFooter(
                    sourceName = snapshot.sourceName,
                    attribution = state.attribution,
                    fetchedAtMillis = snapshot.fetchedAtMillis,
                    zone = zone,
                )
            }
        }
    }
}

/** 实况主卡片。 */
@Composable
private fun CurrentWeatherCard(
    current: CurrentWeather,
    cityName: String,
    weatherText: String,
    useCelsius: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = cityName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = tempText(current.temperatureC, useCelsius),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = weatherText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_feels_like) + " " +
                    tempText(current.feelsLikeC, useCelsius),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            // 指标按 3 列网格排布，数量不整除时用等宽 Spacer 补齐，避免行内分布不均。
            val metrics = buildList {
                add(
                    stringResource(R.string.label_humidity) to
                        "${(current.humidity * 100).roundToInt()}%",
                )
                add(
                    stringResource(R.string.label_wind) to
                        "${current.windSpeedKmh.roundToInt()} ${stringResource(R.string.unit_kmh)}",
                )
                add(
                    WeatherCodeMapper.windDirection(current.windDirectionDeg) to
                        WeatherCodeMapper.windScaleDescription(current.windSpeedKmh),
                )
                current.pressureHpa?.let {
                    add(
                        stringResource(R.string.label_pressure) to
                            "${it.roundToInt()} ${stringResource(R.string.unit_hpa)}",
                    )
                }
                current.uvIndex?.let {
                    add(stringResource(R.string.label_uv_index) to it.roundToInt().toString())
                }
                current.visibilityMeters?.let {
                    add(
                        stringResource(R.string.label_visibility) to
                            String.format(Locale.getDefault(), "%.1f", it / 1000.0) + " " +
                            stringResource(R.string.unit_km),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.chunked(3).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowMetrics.forEach { (label, value) ->
                            Metric(label = label, value = value, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** 数据来源署名，按许可要求必须展示。 */
@Composable
private fun AttributionFooter(
    sourceName: String,
    attribution: String?,
    fetchedAtMillis: Long,
    zone: TimeZone,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = attribution ?: sourceName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (fetchedAtMillis > 0L) {
            Text(
                text = stringResource(
                    R.string.label_updated_at,
                    TimeUtils.formatDateTime(fetchedAtMillis, zone),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 摄氏/华氏温度展示文本。 */
internal fun tempText(celsius: Double, useCelsius: Boolean): String =
    "${Temperature.convert(celsius, useCelsius).roundToInt()}${Temperature.symbol(useCelsius)}"

/** 城市时区，缺失时退回设备时区。 */
internal fun zoneOf(timezone: String?): TimeZone =
    if (timezone.isNullOrBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(timezone)

