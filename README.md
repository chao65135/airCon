# AirCon · 空气质量与天气

一款用 Kotlin + Jetpack Compose 编写的 Android 天气应用，重点在空气质量：多污染物浓度、按国标反算 AQI、逐小时趋势。

<p align="left">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.06.00-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-24-green">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-36-green">
</p>

## 功能

| 页面 | 内容 |
|---|---|
| 首页 | 实况（温度、体感、湿度、风速、风向风力、气压、紫外线、能见度）、24 小时逐小时、7 天预报 |
| 空气质量 | AQI 主卡（等级 / 首要污染物 / 健康建议）、六项污染物浓度、逐小时 AQI 趋势 |
| 城市 | 关键字搜索（Open-Meteo Geocoding）、添加 / 删除、切换当前城市，本地持久化 |
| 设置 | AQI 标准（中式 / 美式）、温度单位、主数据源、后台刷新间隔、和风天气凭据（可选） |

## 数据来源

- **Open-Meteo**（默认，免 Key）：`forecast` 天气接口 + CAMS 空气质量接口
- **和风天气**（可选）：在设置页填入 API Host / Key 或 JWT 后作为主数据源
- 本地缓存：**Room**（天气与空气质量快照、城市列表）
- 用户设置：**DataStore Preferences**
- 后台定时刷新：**WorkManager**（间隔跟随设置，最小 15 分钟）

## AQI 口径说明

中国实时报（HJ 633）并非直接取瞬时浓度：

- PM2.5 / PM10 → **24 小时滑动平均**
- O₃ → **8 小时滑动平均**
- SO₂ / NO₂ / CO → 1 小时值
- 各项 IAQI 取最大值为总 AQI，最大值对应的污染物即「首要污染物」

所以首页 AQI 82「良」与某时刻 PM2.5 瞬时浓度折算出的数值不一致是正常现象，界面上的「实时报口径」文案已作说明。美式 EPA 标准可在设置中切换。

## 构建与运行

环境要求：JDK 17+、Android SDK Platform 36、Gradle 由 Wrapper 自带。

```bash
git clone git@github.com:chao65135/airCon.git
cd airCon

# 指定本机 SDK 路径（该文件已被 .gitignore 忽略）
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# 编译 + 安装到已连接的设备/模拟器
./gradlew :app:installDebug

# 启动（debug 变体包名带 .debug 后缀）
adb shell am start -n com.aircon.app.debug/com.aircon.app.MainActivity
```

也可以用 Android Studio 直接打开工程后点击 Run。

运行单元测试：

```bash
./gradlew :app:testDebugUnitTest   # 含 AQI 计算国标/美标用例
```

> 首次启动需要联网拉取天气数据；无网络时使用 Room 中的最近一次缓存。

## 技术栈

| 分类 | 选型 |
|---|---|
| 语言 / 构建 | Kotlin 2.1.20、AGP 8.13.0、KSP、Version Catalog |
| UI | Jetpack Compose（BOM 2025.06.00）、Material 3、Navigation Compose |
| 架构 | 单 Activity + ViewModel + StateFlow + 手动 DI（`AppContainer`） |
| 网络 | Retrofit 2.11 + kotlinx.serialization + OkHttp Logging |
| 存储 | Room 2.7.1（导出 schema）、DataStore Preferences |
| 后台 | WorkManager 2.10 |
| 测试 | JUnit4 + kotlinx-coroutines-test |

## 目录结构

```
app/src/main/java/com/aircon/app/
├── data/            # 远端接口、数据源、仓库、Room、DataStore
│   ├── remote/      # Open-Meteo、和风天气 的 Retrofit 接口与 DTO
│   ├── source/      # WeatherDataSource 抽象及两个实现
│   └── local/       # Room 实体、DAO、数据库
├── domain/          # 领域模型、AQI 计算（ChinaAqiCalculator）、天气码映射
├── ui/              # Compose 页面：weather / cities / settings / components / theme
└── util/            # 时间工具等
```

## 说明

- `local.properties`、`build/`、`.gradle/`、`.kotlin/` 均不入库。
- 和风天气 Key/JWT 仅存于本机 DataStore，请勿提交到仓库。
- 项目当前为个人练习项目，未发布正式版本。
