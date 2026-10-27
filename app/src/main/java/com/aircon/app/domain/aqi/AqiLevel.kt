package com.aircon.app.domain.aqi

/**
 * 中国 AQI 等级划分（HJ 633-2026）。
 *
 * 颜色取自生态环境部《环境空气质量指数技术规定》配套色标。
 */
enum class AqiLevel(
    val level: Int,
    val category: String,
    val colorArgb: Long,
    val advice: String,
    val sensitiveAdvice: String,
) {
    EXCELLENT(
        1, "优", 0xFF00E400,
        "空气质量令人满意，基本无空气污染，各类人群可正常活动。",
        "各类人群可正常进行户外活动。",
    ),
    GOOD(
        2, "良", 0xFFFFFF00,
        "空气质量可接受，但某些污染物可能对极少数异常敏感人群有较弱影响。",
        "极少数异常敏感人群应减少户外活动。",
    ),
    LIGHT_POLLUTION(
        3, "轻度污染", 0xFFFF7E00,
        "敏感人群症状有轻度加剧，健康人群出现刺激症状。",
        "儿童、老年人及心血管与呼吸系统疾病患者应减少长时间、高强度户外锻炼。",
    ),
    MODERATE_POLLUTION(
        4, "中度污染", 0xFFFF0000,
        "进一步加剧敏感人群症状，健康人群可能出现心血管与呼吸系统影响。",
        "敏感人群应避免长时间、高强度户外锻炼，一般人群适量减少户外运动。",
    ),
    HEAVY_POLLUTION(
        5, "重度污染", 0xFF8F3F97,
        "心肺疾病患者症状显著加剧，运动耐受力降低，一般人群普遍出现症状。",
        "敏感人群应停留在室内、停止户外活动，一般人群减少户外活动。",
    ),
    SEVERE_POLLUTION(
        6, "严重污染", 0xFF7E0023,
        "健康人群运动耐受力降低，有明显强烈症状，一般人群应避免户外活动。",
        "人群应当留在室内避免体力消耗，一般人群应避免户外活动。",
    ),
    ;

    companion object {
        /** 按 AQI 数值取等级。 */
        fun from(aqi: Int): AqiLevel = when {
            aqi <= 50 -> EXCELLENT
            aqi <= 100 -> GOOD
            aqi <= 150 -> LIGHT_POLLUTION
            aqi <= 200 -> MODERATE_POLLUTION
            aqi <= 300 -> HEAVY_POLLUTION
            else -> SEVERE_POLLUTION
        }
    }
}
