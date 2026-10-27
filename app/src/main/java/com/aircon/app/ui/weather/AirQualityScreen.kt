package com.aircon.app.ui.weather

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.PollutantReading
import com.aircon.app.ui.components.NoticeCard
import com.aircon.app.ui.components.SectionTitle
import com.aircon.app.ui.components.aqiColor
import com.aircon.app.ui.components.aqiTextColor
import com.aircon.app.util.TimeUtils
import java.util.TimeZone
import kotlin.math.roundToInt

/** 空气质量详情：AQI 评价、污染物浓度、逐小时 AQI 趋势。 */
@Composable
fun AirQualityScreen(
    state: WeatherUiState,
    modifier: Modifier = Modifier,
) {
    val air = state.snapshot?.airQuality
    val zone = remember(state.snapshot?.city?.timezone) {
        zoneOf(state.snapshot?.city?.timezone)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (air == null) {
            item { NoticeCard(text = stringResource(R.string.msg_aqi_unavailable)) }
        } else {
            item { AqiCard(air = air, standardLabel = state.aqiStandardLabel) }
            if (air.hourly.isNotEmpty()) {
                item { HourlyAqiSection(air = air, zone = zone) }
            }
            item { PollutantSection(pollutants = air.pollutants) }
            air.sourceNote?.takeIf { it.isNotBlank() }?.let { note ->
                item { NoticeCard(text = note) }
            }
        }
    }
}

/** 逐小时 AQI 趋势（以色块高度近似）。 */
@Composable
private fun HourlyAqiSection(air: AirQuality, zone: TimeZone) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_hourly))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(air.hourly) { point ->
                Column(
                    modifier = Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = point.value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = aqiTextColor(point.value),
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height((8 + point.value.coerceIn(0, 500) / 8).dp)
                            .background(
                                aqiColor(point.value).copy(alpha = 0.85f),
                                RoundedCornerShape(6.dp),
                            ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = TimeUtils.formatHour(point.epochMillis, zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 污染物浓度列表。 */
@Composable
private fun PollutantSection(pollutants: List<PollutantReading>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SectionTitle(text = stringResource(R.string.section_pollutants))
            Text(
                text = stringResource(R.string.label_pollutants_hourly),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (pollutants.isEmpty()) {
            NoticeCard(text = stringResource(R.string.msg_pollutants_unavailable))
            return
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                pollutants.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${item.value.roundToInt()} ${item.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

