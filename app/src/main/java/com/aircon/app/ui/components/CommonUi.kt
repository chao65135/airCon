package com.aircon.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aircon.app.domain.aqi.AqiLevel

/** AQI 数值对应的官方色标（HJ 633 配套色卡）。 */
fun aqiColor(value: Int): Color = Color(AqiLevel.from(value).colorArgb.toInt())

/**
 * AQI 文字用色。
 *
 * 官方色标饱和度高（如「良」的纯黄 #FFFF00），直接作为浅色卡片上的文字时对比度
 * 仅约 1.1:1，几乎不可读。这里按背景明暗分别给出压暗 / 提亮版本
 * （浅色约达 WCAG AA，深色下因 AQI 数值为大字号至少满足 3:1），
 * 仅用于文字与图标；色块、柱状图继续使用 [aqiColor]。
 */
@Composable
fun aqiTextColor(value: Int): Color {
    val level = AqiLevel.from(value)
    val darkSurface = MaterialTheme.colorScheme.surfaceVariant.luminance() < 0.5f
    return if (darkSurface) aqiTextColorDark(level) else aqiTextColorLight(level)
}

private fun aqiTextColorLight(level: AqiLevel): Color = when (level) {
    AqiLevel.EXCELLENT -> Color(0xFF146614)
    AqiLevel.GOOD -> Color(0xFF6E6E00)
    AqiLevel.LIGHT_POLLUTION -> Color(0xFFB34700)
    AqiLevel.MODERATE_POLLUTION -> Color(0xFFB30000)
    AqiLevel.HEAVY_POLLUTION -> Color(0xFF6B2A70)
    AqiLevel.SEVERE_POLLUTION -> Color(0xFF5C0018)
}

private fun aqiTextColorDark(level: AqiLevel): Color = when (level) {
    AqiLevel.EXCELLENT -> Color(0xFF7BE87B)
    AqiLevel.GOOD -> Color(0xFFE8E85C)
    AqiLevel.LIGHT_POLLUTION -> Color(0xFFFFB86B)
    AqiLevel.MODERATE_POLLUTION -> Color(0xFFFF8A8A)
    AqiLevel.HEAVY_POLLUTION -> Color(0xFFD79BDC)
    AqiLevel.SEVERE_POLLUTION -> Color(0xFFFF8FA6)
}

/** 区块标题。 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

/** 提示卡片：错误或一般提示。 */
@Composable
fun NoticeCard(text: String, isError: Boolean = false, modifier: Modifier = Modifier) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** 指标块：上数值、下标签。 */
@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 一行「名称 — 数值」。 */
@Composable
fun LabeledRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
