package com.aircon.app.domain.weather

/**
 * 统一的天气现象编码。
 *
 * 内部使用 WMO 4677 天气码（Open-Meteo 直接返回该标准），
 * 和风天气的中文现象描述则通过关键词转换为同一编码空间，
 * 这样界面层只需处理一套图标与文案。
 */
object WeatherCodeMapper {

    const val CLEAR = 0
    const val MAINLY_CLEAR = 1
    const val PARTLY_CLOUDY = 2
    const val OVERCAST = 3
    const val FOG = 45
    const val DRIZZLE = 51
    const val FREEZING_DRIZZLE = 56
    const val RAIN = 61
    const val FREEZING_RAIN = 66
    const val SNOW = 71
    const val RAIN_SHOWERS = 80
    const val SNOW_SHOWERS = 85
    const val THUNDERSTORM = 95
    const val THUNDERSTORM_HAIL = 96

    /** 天气现象中文描述。 */
    fun description(code: Int): String = when (code) {
        CLEAR -> "晴"
        MAINLY_CLEAR -> "晴间多云"
        PARTLY_CLOUDY -> "多云"
        OVERCAST -> "阴"
        FOG -> "雾"
        DRIZZLE -> "毛毛雨"
        FREEZING_DRIZZLE -> "冻毛毛雨"
        RAIN -> "雨"
        FREEZING_RAIN -> "冻雨"
        SNOW -> "雪"
        RAIN_SHOWERS -> "阵雨"
        SNOW_SHOWERS -> "阵雪"
        THUNDERSTORM -> "雷阵雨"
        THUNDERSTORM_HAIL -> "雷阵雨伴冰雹"
        else -> "未知"
    }

    /**
     * 由和风天气的现象描述（中文）转换为 WMO 编码。
     *
     * 关键词按优先级排列：先判断更具体的现象（雷、冰雹、冻），
     * 再判断降水类型，最后回落到云量描述。
     */
    fun fromChineseText(text: String): Int {
        if (text.isBlank()) return OVERCAST
        return when {
            text.contains("冰雹") || text.contains("雷阵雨伴") -> THUNDERSTORM_HAIL
            text.contains("雷") -> THUNDERSTORM
            text.contains("冻") && (text.contains("雨") || text.contains("毛毛雨")) ->
                if (text.contains("毛毛雨")) FREEZING_DRIZZLE else FREEZING_RAIN
            text.contains("雨夹雪") || text.contains("夹雪") -> SNOW
            text.contains("雪") -> SNOW
            text.contains("阵雨") -> RAIN_SHOWERS
            text.contains("雨") -> if (text.contains("毛毛雨")) DRIZZLE else RAIN
            text.contains("雾") || text.contains("霾") || text.contains("沙尘") ||
                text.contains("浮尘") || text.contains("烟") -> FOG
            text.contains("阴") -> OVERCAST
            text.contains("多云") -> PARTLY_CLOUDY
            text.contains("少云") -> MAINLY_CLEAR
            text.contains("晴") -> CLEAR
            else -> OVERCAST
        }
    }

    /**
     * 风力等级对应的蒲福风级名称。
     */
    fun windScaleDescription(speedKmh: Double): String {
        val scale = when {
            speedKmh < 1 -> 0
            speedKmh < 6 -> 1
            speedKmh < 12 -> 2
            speedKmh < 20 -> 3
            speedKmh < 29 -> 4
            speedKmh < 39 -> 5
            speedKmh < 50 -> 6
            speedKmh < 62 -> 7
            speedKmh < 75 -> 8
            speedKmh < 89 -> 9
            speedKmh < 103 -> 10
            speedKmh < 118 -> 11
            else -> 12
        }
        return if (scale == 0) "无持续风向" else "$scale 级风"
    }

    /**
     * 风向角度转为中文方位。
     */
    fun windDirection(degree: Int): String {
        val names = listOf("北", "东北偏北", "东北", "东北偏东", "东", "东南偏东", "东南", "东南偏南",
            "南", "西南偏南", "西南", "西南偏西", "西", "西北偏西", "西北", "西北偏北")
        val index = ((degree % 360) / 22.5f).toInt().coerceIn(0, 15)
        return "${names[index]}风"
    }
}
