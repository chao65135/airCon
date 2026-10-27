package com.aircon.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aircon.app.R
import com.aircon.app.di.AppContainer
import com.aircon.app.ui.cities.CitiesScreen
import com.aircon.app.ui.settings.SettingsScreen
import com.aircon.app.ui.settings.SettingsViewModel
import com.aircon.app.ui.weather.AirQualityScreen
import com.aircon.app.ui.weather.WeatherScreen
import com.aircon.app.ui.weather.WeatherViewModel
import com.aircon.app.work.RefreshScheduler

/** 导航路由。 */
object Routes {
    const val HOME = "home"
    const val AIR = "air"
    const val CITIES = "cities"
    const val SETTINGS = "settings"
}

/** 底部导航项。 */
private data class BottomItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val BOTTOM_ITEMS = listOf(
    BottomItem(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    BottomItem(Routes.AIR, R.string.nav_air_quality, Icons.Default.Air),
    BottomItem(Routes.CITIES, R.string.nav_cities, Icons.Default.LocationOn),
    BottomItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

/**
 * 应用主界面：顶栏 + 底部导航 + 四个页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirConApp(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val weatherViewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModel.factory(
            repository = container.weatherRepository,
            settingsStore = container.settingsStore,
            citySearchRepository = container.citySearchRepository,
        ),
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsStore),
    )

    val state by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val cities by weatherViewModel.cities.collectAsStateWithLifecycle()
    val searchResults by weatherViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by weatherViewModel.isSearching.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = titleOf(currentRoute)) },
                actions = {
                    if (currentRoute == Routes.HOME || currentRoute == Routes.AIR) {
                        IconButton(onClick = { weatherViewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                BOTTOM_ITEMS.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            // 清掉上一页残留的输入焦点，避免新页面 LazyColumn 自动滚动到首个可聚焦项。
                            focusManager.clearFocus()
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = null) },
                        label = { Text(text = stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                WeatherScreen(state = state)
            }
            composable(Routes.AIR) {
                AirQualityScreen(state = state)
            }
            composable(Routes.CITIES) {
                CitiesScreen(
                    cities = cities,
                    selectedCityId = state.city?.id,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onSearch = weatherViewModel::searchCities,
                    onAddCity = { city -> weatherViewModel.addCity(city) },
                    onSelectCity = { city -> weatherViewModel.selectCity(city) },
                    onRemoveCity = { city -> weatherViewModel.removeCity(city.id) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onUseCelsiusChange = settingsViewModel::setUseCelsius,
                    onAqiStandardChange = settingsViewModel::setAqiStandard,
                    onPrimarySourceChange = settingsViewModel::setPrimarySource,
                    onRefreshIntervalChange = { minutes ->
                        settingsViewModel.setRefreshIntervalMinutes(minutes)
                        // 周期任务间隔需要同步给 WorkManager 才会真正生效。
                        RefreshScheduler.schedule(context, minutes)
                    },
                    onSaveQWeather = settingsViewModel::saveQWeather,
                )
            }
        }
    }
}

/** 当前路由对应的标题。 */
@Composable
private fun titleOf(route: String): String = when (route) {
    Routes.AIR -> stringResource(R.string.nav_air_quality)
    Routes.CITIES -> stringResource(R.string.label_cities)
    Routes.SETTINGS -> stringResource(R.string.label_settings)
    else -> stringResource(R.string.app_name)
}

