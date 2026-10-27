package com.aircon.app.domain.aqi

import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.AqiIndex
import com.aircon.app.domain.model.AqiStandard
import com.aircon.app.domain.model.HourlyAqi
import com.aircon.app.domain.model.PollutantReading
import kotlin.math.roundToInt

/**
 * 中国空气质量指数（AQI）计算器。
 *
 * 依据《环境空气质量指数（AQI）技术规定》HJ 633—2026：
 *
 * ```
 * IAQI_P = (IAQI_Hi - IAQI_Lo) / (BP_Hi - BP_Lo) × (C_P - BP_Lo) + IAQI_Lo
 * AQI    = max{ IAQI_1, IAQI_2, ... IAQI_n }
 * ```
 *
 * 污染物项目浓度限值表（单位：μg/m³，CO 为 mg/m³）：
 *
 * | IAQI | SO2 24h | SO2 1h | NO2 24h | NO2 1h | CO 24h | CO 1h | O3 8h | O3 1h | PM10 24h | PM2.5 24h |
 * |------|---------|--------|---------|--------|--------|--------|-------|-------|-----------|-----------|
 * | 0    | 0       | 0      | 0       | 0      | 0      | 0      | 0     | 0     | 0         | 0         |
 * | 50   | 50      | 150    | 40      | 100    | 2      | 5      | 100   | 160   | 50        | 35        |
 * | 100  | 150     | 500    | 80      | 200    | 4      | 10     | 160   | 200   | 120       | 60        |
 * | 150  | 475     | 650    | 180     | 700    | 14     | 35     | 215   | 300   | 250       | 115       |
 * | 200  | 800     | 800    | 280     | 1200   | 24     | 60     | 265   | 400   | 350       | 150       |
 * | 300  | 1600    | 800    | 565     | 2340   | 36     | 90     | 800   | 800   | 420       | 250       |
 * | 400  | 2100    | 800    | 750     | 3090   | 48     | 120    | 800   | 1000  | 500       | 350       |
 * | 500  | 2620    | 800    | 940     | 3840   | 60     | 150    | 800   | 1200  | 600       | 500       |
 *
 * 说明：
 * - SO2 1h 平均浓度高于 800 μg/m³ 时，IAQI 记为 200。
 * - O3 8h 平均浓度高于 800 μg/m³ 时，IAQI 记为 300。
 *
 * 实时报口径：PM2.5、PM10 取 24 小时平均浓度，O3 取 8 小时滑动平均，
 * SO2、NO2、CO 取 1 小时平均浓度。
 */
object ChinaAqiCalculator {

    /** 污染物的浓度限值分段。 */
    data class Segment(
        val iaqiLow: Int,
        val iaqiHigh: Int,
        val bpLow: Double,
        val bpHigh: Double,
    )

    private val PM25_24H = listOf(
        Segment(0, 50, 0.0, 35.0),
        Segment(50, 100, 35.0, 60.0),
        Segment(100, 150, 60.0, 115.0),
        Segment(150, 200, 115.0, 150.0),
        Segment(200, 300, 150.0, 250.0),
        Segment(300, 400, 250.0, 350.0),
        Segment(400, 500, 350.0, 500.0),
    )

    private val PM10_24H = listOf(
        Segment(0, 50, 0.0, 50.0),
        Segment(50, 100, 50.0, 120.0),
        Segment(100, 150, 120.0, 250.0),
        Segment(150, 200, 250.0, 350.0),
        Segment(200, 300, 350.0, 420.0),
        Segment(300, 400, 420.0, 500.0),
        Segment(400, 500, 500.0, 600.0),
    )

    private val NO2_1H = listOf(
        Segment(0, 50, 0.0, 100.0),
        Segment(50, 100, 100.0, 200.0),
        Segment(100, 150, 200.0, 700.0),
        Segment(150, 200, 700.0, 1200.0),
        Segment(200, 300, 1200.0, 2340.0),
        Segment(300, 400, 2340.0, 3090.0),
        Segment(400, 500, 3090.0, 3840.0),
    )

    private val SO2_1H = listOf(
        Segment(0, 50, 0.0, 150.0),
        Segment(50, 100, 150.0, 500.0),
        Segment(100, 150, 500.0, 650.0),
        Segment(150, 200, 650.0, 800.0),
    )

    private val CO_1H = listOf(
        Segment(0, 50, 0.0, 5.0),
        Segment(50, 100, 5.0, 10.0),
        Segment(100, 150, 10.0, 35.0),
        Segment(150, 200, 35.0, 60.0),
        Segment(200, 300, 60.0, 90.0),
        Segment(300, 400, 90.0, 120.0),
        Segment(400, 500, 120.0, 150.0),
    )

    private val O3_8H = listOf(
        Segment(0, 50, 0.0, 100.0),
        Segment(50, 100, 100.0, 160.0),
        Segment(100, 150, 160.0, 215.0),
        Segment(150, 200, 215.0, 265.0),
        Segment(200, 300, 265.0, 800.0),
    )

    /**
     * 一次 AQI 评价所需的原始浓度。
     *
     * @param pm25_24h PM2.5 的 24 小时平均浓度，μg/m³
     * @param pm10_24h PM10 的 24 小时平均浓度，μg/m³
     * @param no2_1h NO2 的 1 小时平均浓度，μg/m³
     * @param so2_1h SO2 的 1 小时平均浓度，μg/m³
     * @param co_1h CO 的 1 小时平均浓度，mg/m³
     * @param o3_8h O3 的 8 小时滑动平均浓度，μg/m³
     */
    data class Concentrations(
        val pm25_24h: Double? = null,
        val pm10_24h: Double? = null,
        val no2_1h: Double? = null,
        val so2_1h: Double? = null,
        val co_1h: Double? = null,
        val o3_8h: Double? = null,
    )

    /** 单个污染物的分指数。 */
    data class SubIndex(
        val code: String,
        val name: String,
        val iaqi: Int,
        val concentration: Double,
    )

    /**
     * 计算各污染物的分指数。
     *
     * @return 按 IAQI 从大到小排序的分指数列表，AQI 即首元素。
     */
    fun computeSubIndexes(concentrations: Concentrations): List<SubIndex> {
        val result = mutableListOf<SubIndex>()

        concentrations.pm25_24h?.let {
            result += SubIndex("pm25", "PM2.5", iaqi(it, PM25_24H), it)
        }
        concentrations.pm10_24h?.let {
            result += SubIndex("pm10", "PM10", iaqi(it, PM10_24H), it)
        }
        concentrations.no2_1h?.let {
            result += SubIndex("no2", "NO2", iaqi(it, NO2_1H), it)
        }
        concentrations.so2_1h?.let {
            result += SubIndex("so2", "SO2", iaqi(it, SO2_1H), it)
        }
        concentrations.co_1h?.let {
            result += SubIndex("co", "CO", iaqi(it, CO_1H), it)
        }
        concentrations.o3_8h?.let {
            // O3 8h 平均浓度高于 800 μg/m³ 时，IAQI 记为 300。
            val value = if (it > 800.0) 300 else iaqi(it, O3_8H)
            result += SubIndex("o3", "O3", value, it)
        }

        return result.sortedByDescending { it.iaqi }
    }

    /**
     * 按 HJ 633 线性插值计算单个污染物的分指数。
     *
     * @param concentration 浓度值
     * @param segments 浓度限值分段
     */
    fun iaqi(concentration: Double, segments: List<Segment>): Int {
        if (concentration <= 0.0) return 0
        val segment = segments.firstOrNull { concentration <= it.bpHigh }
            ?: return segments.last().iaqiHigh
        if (segment.bpHigh <= segment.bpLow) return segment.iaqiHigh
        val ratio = (segment.iaqiHigh - segment.iaqiLow) / (segment.bpHigh - segment.bpLow)
        val value = ratio * (concentration - segment.bpLow) + segment.iaqiLow
        return value.roundToInt().coerceIn(0, 500)
    }

    /**
     * 由分指数构建 [AqiIndex]。
     */
    fun toAqiIndex(subIndexes: List<SubIndex>): AqiIndex? {
        val primary = subIndexes.firstOrNull() ?: return null
        val level = AqiLevel.from(primary.iaqi)
        return AqiIndex(
            value = primary.iaqi,
            level = level.level,
            category = level.category,
            standard = AqiStandard.CN,
            primaryPollutantCode = primary.code,
            primaryPollutantName = primary.name,
            healthAdvice = level.advice,
            sensitivePopulationAdvice = level.sensitiveAdvice,
        )
    }

    /**
     * 计算当前时刻的中国 AQI，并组装完整的 [AirQuality]。
     *
     * @param concentrations 当前时刻各污染物的浓度
     * @param pollutants 展示用的污染物浓度列表
     * @param hourly 逐小时 AQI 序列（可选）
     * @param updatedAtMillis 数据更新时间
     * @param sourceNote 数据来源说明
     */
    fun buildAirQuality(
        concentrations: Concentrations,
        pollutants: List<PollutantReading> = emptyList(),
        hourly: List<HourlyAqi> = emptyList(),
        updatedAtMillis: Long = 0L,
        sourceNote: String? = null,
    ): AirQuality? {
        val subIndexes = computeSubIndexes(concentrations)
        val index = toAqiIndex(subIndexes) ?: return null
        return AirQuality(
            index = index,
            pollutants = pollutants,
            hourly = hourly,
            updatedAtMillis = updatedAtMillis,
            sourceNote = sourceNote,
        )
    }

    /**
     * 对一组逐小时浓度序列按实时报口径计算逐小时 AQI。
     *
     * @param timestamps 与浓度一一对应的时间戳
     * @param hourlyConcentrations 每个时间点对应的浓度
     */
    fun buildHourlyAqi(
        timestamps: List<Long>,
        hourlyConcentrations: List<Concentrations>,
    ): List<HourlyAqi> {
        if (timestamps.size != hourlyConcentrations.size) return emptyList()
        return timestamps.zip(hourlyConcentrations).mapNotNull { (time, concentrations) ->
            val maxIaqi = computeSubIndexes(concentrations).maxOfOrNull { it.iaqi }
                ?: return@mapNotNull null
            HourlyAqi(epochMillis = time, value = maxIaqi)
        }
    }

    /**
     * 计算一组浓度的 AQI 数值（取各分指数最大值）。
     */
    fun aqiOf(concentrations: Concentrations): Int =
        computeSubIndexes(concentrations).maxOfOrNull { it.iaqi } ?: 0
}
