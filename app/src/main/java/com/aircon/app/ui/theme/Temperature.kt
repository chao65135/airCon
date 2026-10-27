package com.aircon.app.ui.theme

import com.aircon.app.domain.model.AqiStandard

/**
 * 温度单位转换工具。
 */
object Temperature {

    /**
     * 摄氏度转华氏度。
     */
    fun cToF(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

    /**
     * 按设置转换温度。
     *
     * @param celsius 摄氏度
     * @param useCelsius 是否使用摄氏度
     * @return 转换后的温度值
     */
    fun convert(celsius: Double, useCelsius: Boolean): Double =
        if (useCelsius) celsius else cToF(celsius)

    /**
     * 温度单位符号。
     */
    fun symbol(useCelsius: Boolean): String = if (useCelsius) "°C" else "°F"

    /**
     * 风速单位转换：km/h → m/s。
     */
    fun kmhToMs(kmh: Double): Double = kmh / 3.6
}

/**
 * AQI 标准的展示名称。
 */
fun AqiStandard.displayName(): String = when (this) {
    AqiStandard.CN -> "中国 AQI"
    AqiStandard.US -> "US EPA AQI"
    AqiStandard.EU -> "欧洲 AQI"
}
