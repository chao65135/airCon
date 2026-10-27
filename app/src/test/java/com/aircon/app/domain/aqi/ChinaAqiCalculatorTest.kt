package com.aircon.app.domain.aqi

import com.aircon.app.domain.model.AqiStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 中国 AQI（HJ 633—2026）计算测试。
 *
 * 重点覆盖分段限值的边界值，这是最容易出现「差一档」错误的地方。
 */
class ChinaAqiCalculatorTest {

    private fun pm25(value: Double) =
        ChinaAqiCalculator.Concentrations(pm25_24h = value)

    private fun pm10(value: Double) =
        ChinaAqiCalculator.Concentrations(pm10_24h = value)

    // ---------- PM2.5 24h ----------

    @Test
    fun pm25_boundary_values_map_to_expected_iaqi() {
        assertEquals(0, ChinaAqiCalculator.aqiOf(pm25(0.0)))
        assertEquals(50, ChinaAqiCalculator.aqiOf(pm25(35.0)))
        assertEquals(100, ChinaAqiCalculator.aqiOf(pm25(60.0)))
        assertEquals(150, ChinaAqiCalculator.aqiOf(pm25(115.0)))
        assertEquals(200, ChinaAqiCalculator.aqiOf(pm25(150.0)))
        assertEquals(300, ChinaAqiCalculator.aqiOf(pm25(250.0)))
        assertEquals(400, ChinaAqiCalculator.aqiOf(pm25(350.0)))
        assertEquals(500, ChinaAqiCalculator.aqiOf(pm25(500.0)))
    }

    @Test
    fun pm25_beyond_upper_bound_is_capped_at_500() {
        assertEquals(500, ChinaAqiCalculator.aqiOf(pm25(900.0)))
    }

    @Test
    fun pm25_interpolates_linearly_inside_a_segment() {
        // 35~60 对应 50~100，中点为 47.5 -> 75
        assertEquals(75, ChinaAqiCalculator.aqiOf(pm25(47.5)))
        // 150~250 对应 200~300，中点为 200 -> 250
        assertEquals(250, ChinaAqiCalculator.aqiOf(pm25(200.0)))
    }

    // ---------- PM10 24h ----------

    @Test
    fun pm10_boundary_values_map_to_expected_iaqi() {
        assertEquals(50, ChinaAqiCalculator.aqiOf(pm10(50.0)))
        assertEquals(100, ChinaAqiCalculator.aqiOf(pm10(120.0)))
        assertEquals(150, ChinaAqiCalculator.aqiOf(pm10(250.0)))
        assertEquals(200, ChinaAqiCalculator.aqiOf(pm10(350.0)))
        assertEquals(300, ChinaAqiCalculator.aqiOf(pm10(420.0)))
        assertEquals(400, ChinaAqiCalculator.aqiOf(pm10(500.0)))
        assertEquals(500, ChinaAqiCalculator.aqiOf(pm10(600.0)))
    }

    // ---------- 气态污染物 1h ----------

    @Test
    fun no2_one_hour_limits() {
        assertEquals(
            50,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(no2_1h = 100.0)),
        )
        assertEquals(
            100,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(no2_1h = 200.0)),
        )
    }
}

/** 气态污染物与综合计算的补充用例。 */
class ChinaAqiCalculatorGasTest {

    @Test
    fun so2_one_hour_limits_stop_at_200() {
        assertEquals(
            50,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(so2_1h = 150.0)),
        )
        assertEquals(
            100,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(so2_1h = 500.0)),
        )
        assertEquals(
            200,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(so2_1h = 800.0)),
        )
        // 超出最高分段时取最后一个分段的 IAQI 上限
        assertEquals(
            200,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(so2_1h = 1200.0)),
        )
    }

    @Test
    fun co_uses_milligram_per_cubic_meter() {
        assertEquals(
            50,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(co_1h = 5.0)),
        )
        assertEquals(
            100,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(co_1h = 10.0)),
        )
        assertEquals(
            150,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(co_1h = 35.0)),
        )
    }

    @Test
    fun o3_eight_hour_limits_and_cap() {
        assertEquals(
            50,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(o3_8h = 100.0)),
        )
        assertEquals(
            100,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(o3_8h = 160.0)),
        )
        assertEquals(
            200,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(o3_8h = 265.0)),
        )
        assertEquals(
            300,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(o3_8h = 800.0)),
        )
        // 高于 800 μg/m³ 时按规范直接记为 300
        assertEquals(
            300,
            ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations(o3_8h = 801.0)),
        )
    }

    @Test
    fun aqi_is_the_maximum_of_sub_indexes() {
        val aqi = ChinaAqiCalculator.aqiOf(
            ChinaAqiCalculator.Concentrations(
                pm25_24h = 60.0,   // IAQI 100
                pm10_24h = 250.0,  // IAQI 150
                no2_1h = 100.0,    // IAQI 50
                o3_8h = 100.0,     // IAQI 50
            ),
        )
        assertEquals(150, aqi)
    }

    @Test
    fun sub_indexes_are_sorted_descending_and_expose_primary_pollutant() {
        val subIndexes = ChinaAqiCalculator.computeSubIndexes(
            ChinaAqiCalculator.Concentrations(pm25_24h = 115.0, pm10_24h = 120.0),
        )
        assertEquals(2, subIndexes.size)
        assertEquals("pm25", subIndexes.first().code)
        assertEquals(150, subIndexes.first().iaqi)
        assertEquals("PM2.5", subIndexes.first().name)
    }

    @Test
    fun empty_concentrations_produce_no_aqi() {
        assertNull(
            ChinaAqiCalculator.buildAirQuality(ChinaAqiCalculator.Concentrations()),
        )
        assertEquals(0, ChinaAqiCalculator.aqiOf(ChinaAqiCalculator.Concentrations()))
    }

    @Test
    fun build_air_quality_attaches_level_category_and_advice() {
        val air = ChinaAqiCalculator.buildAirQuality(
            concentrations = ChinaAqiCalculator.Concentrations(pm25_24h = 115.0),
            updatedAtMillis = 1_700_000_000_000L,
        )
        assertNotNull(air)
        val index = requireNotNull(air).index
        assertEquals(150, index.value)
        assertEquals(3, index.level)
        assertEquals("轻度污染", index.category)
        assertEquals(AqiStandard.CN, index.standard)
        assertEquals("pm25", index.primaryPollutantCode)
        assertTrue(index.healthAdvice?.isNotBlank() == true)
        assertTrue(index.sensitivePopulationAdvice?.isNotBlank() == true)
        assertEquals(1_700_000_000_000L, air.updatedAtMillis)
    }
}

