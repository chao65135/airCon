package com.aircon.app.ui.cities

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aircon.app.R
import com.aircon.app.domain.model.City
import com.aircon.app.ui.components.NoticeCard
import com.aircon.app.ui.components.SectionTitle

/**
 * 城市管理：搜索并添加城市、切换当前城市、移除城市。
 */
@Composable
fun CitiesScreen(
    cities: List<City>,
    selectedCityId: String?,
    searchResults: List<City>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onAddCity: (City) -> Unit,
    onSelectCity: (City) -> Unit,
    onRemoveCity: (City) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.hint_city_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onSearch("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (searching) {
                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.label_searching),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item { NoticeCard(text = stringResource(R.string.label_no_search_result)) }
                } else {
                    item { SectionTitle(text = stringResource(R.string.action_add_city)) }
                    items(searchResults, key = { it.id }) { city ->
                        SearchResultRow(
                            city = city,
                            alreadyAdded = cities.any { it.id == city.id },
                            onAdd = { onAddCity(city) },
                        )
                    }
                }
            } else {
                item { SectionTitle(text = stringResource(R.string.label_saved_cities)) }
                if (cities.isEmpty()) {
                    item {
                        NoticeCard(text = stringResource(R.string.label_add_first_city))
                    }
                }
                items(cities, key = { it.id }) { city ->
                    SavedCityRow(
                        city = city,
                        isSelected = city.id == selectedCityId,
                        onSelect = { onSelectCity(city) },
                        onRemove = { onRemoveCity(city) },
                    )
                }
            }
        }
    }
}

/** 搜索结果行，右侧为添加按钮。 */
@Composable
private fun SearchResultRow(
    city: City,
    alreadyAdded: Boolean,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (city.displayName.isNotBlank()) {
                    Text(
                        text = city.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onAdd, enabled = !alreadyAdded) {
                Icon(
                    imageVector = if (alreadyAdded) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_city),
                )
            }
        }
    }
}

/** 已保存城市行，点击切换，右侧可移除。 */
@Composable
private fun SavedCityRow(
    city: City,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isSelected) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.label_current_city),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (city.admin.isNotBlank() || city.country.isNotBlank()) {
                    Text(
                        text = city.displayName,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.label_remove_city, city.name),
                )
            }
        }
    }
}

