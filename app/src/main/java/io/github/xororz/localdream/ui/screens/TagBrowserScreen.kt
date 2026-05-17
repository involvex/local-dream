package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.TagAutocompleteRepository
import io.github.xororz.localdream.data.TagEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagBrowserScreen(
    onInsertTag: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TagAutocompleteRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TagEntry>>(emptyList()) }
    var categories by remember { mutableStateOf<Map<Int, List<TagEntry>>>(emptyMap()) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var favoriteTags by remember { mutableStateOf(repository.getFavorites()) }

    LaunchedEffect(Unit) {
        categories = repository.getEntriesByCategory()
        isLoading = false
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults = repository.searchEntries(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_browser_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_tags)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchQuery.isNotBlank()) {
                TagList(
                    tags = searchResults,
                    favoriteTags = favoriteTags,
                    onTagClick = onInsertTag,
                    onToggleFavorite = { tag ->
                        repository.toggleFavorite(tag)
                        favoriteTags = repository.getFavorites()
                    }
                )
            } else {
                val categoryIds = categories.keys.toList()
                if (categoryIds.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = categoryIds.indexOf(selectedCategory).coerceAtLeast(0),
                        edgePadding = 16.dp
                    ) {
                        categoryIds.forEach { id ->
                            Tab(
                                selected = selectedCategory == id,
                                onClick = { selectedCategory = id },
                                text = { Text(repository.getCategoryName(id)) }
                            )
                        }
                    }

                    TagList(
                        tags = categories[selectedCategory] ?: emptyList(),
                        favoriteTags = favoriteTags,
                        onTagClick = onInsertTag,
                        onToggleFavorite = { tag ->
                            repository.toggleFavorite(tag)
                            favoriteTags = repository.getFavorites()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagList(
    tags: List<TagEntry>,
    favoriteTags: Set<String>,
    onTagClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tags) { tag ->
            TagItem(
                tag = tag,
                isFavorite = favoriteTags.contains(tag.english),
                onTagClick = { onTagClick(tag.english) },
                onToggleFavorite = { onToggleFavorite(tag.english) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun TagItem(
    tag: TagEntry,
    isFavorite: Boolean,
    onTagClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTagClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tag.english, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            tag.translation?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatPostCount(tag.postCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatPostCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
