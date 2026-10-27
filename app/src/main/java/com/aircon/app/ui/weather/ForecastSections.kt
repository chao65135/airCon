package com.aircon.app.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aircon.app.R
import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.AqiStandard
import com.aircon.app.domain.model.DailyForecast
import com.aircon.app.domain.model.HourlyForecast
import com.aircon.app.domain.weather.WeatherCodeMapper
import com.aircon.app.ui.components.LabeledRow
import com.aircon.app.ui.components.SectionTitle
import com.aircon.app.ui.components.aqiColor
import com.aircon.app.ui.components.aqiTextColor
import com.aircon.app.util.TimeUtils
import java.util.TimeZone
import kotlin.math.roundToInt

/** 空气质量卡片：AQI 数值、等级色块、首要污染物与敏感人群建议。 */
@Composable
fun AqiCard(
    air: AirQuality,
    standardLabel: String,
    modifier: Modifier = Modifier,
) {
    val index = air.index
    val color = aqiColor(index.value)
    val textColor = aqiTextColor(index.value)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_air_quality),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = index.value.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = index.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = index.value.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = standardLabel.ifBlank { stringResource(R.string.aqi_standard_cn) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (index.standard == AqiStandard.CN) {
                Text(
                    text = stringResource(R.string.aqi_realtime_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }

            index.primaryPollutantName?.let { pollutant ->
                Spacer(Modifier.height(10.dp))
                LabeledRow(
                    label = stringResource(R.string.label_primary_pollutant),
                    value = pollutant,
                )
            }

            val advice = index.healthAdvice
            val sensitiveAdvice = index.sensitivePopulationAdvice
            if (!advice.isNullOrBlank() || !sensitiveAdvice.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_aqi_advice),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    advice?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                    sensitiveAdvice?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/** 逐小时预报：横向滚动。 */
@Composable
fun HourlySection(
    items: List<HourlyForecast>,
    useCelsius: Boolean,
    zone: TimeZone,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_hourly))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { hourly ->
                Column(
                    modifier = Modifier.width(58.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = TimeUtils.formatHour(hourly.epochMillis, zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = WeatherCodeMapper.description(hourly.weatherCode),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tempText(hourly.temperatureC, useCelsius),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (hourly.precipitationProbability > 0.0) {
                        Text(
                            text = "${hourly.precipitationProbability.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}


/** 未来数日预报。 */
@Composable
fun DailySection(
    items: List<DailyForecast>,
    useCelsius: Boolean,
    zone: TimeZone,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_daily, items.size))
        items.forEachIndexed { index, day ->
            DailyRow(
                day = day,
                dayLabel = when (index) {
                    0 -> stringResource(R.string.label_today)
                    1 -> stringResource(R.string.label_tomorrow)
                    else -> TimeUtils.formatWeekday(day.epochMillis, zone)
                },
                useCelsius = useCelsius,
            )
        }
    }
}

@Composable
private fun DailyRow(
    day: DailyForecast,
    dayLabel: String,
    useCelsius: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = WeatherCodeMapper.description(day.weatherCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (day.precipitationProbability > 0.0) {
            Text(
                text = "${day.precipitationProbability.roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Text(
            text = tempText(day.minTempC, useCelsius) + " / " + tempText(day.maxTempC, useCelsius),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

