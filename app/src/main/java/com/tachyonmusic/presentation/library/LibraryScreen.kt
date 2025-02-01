package com.tachyonmusic.presentation.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tachyonmusic.app.R
import com.tachyonmusic.core.data.constants.PlaceholderArtwork
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.playback_layers.SortOrder
import com.tachyonmusic.presentation.BottomNavigationItem
import com.tachyonmusic.presentation.core_components.HorizontalPlaybackView
import com.tachyonmusic.presentation.core_components.SwipeDelete
import com.tachyonmusic.presentation.entry.SwipingStates
import com.tachyonmusic.presentation.library.component.FilterItem
import com.tachyonmusic.presentation.library.component.FilterItemRow
import com.tachyonmusic.presentation.library.search.PlaybackSearchScreen
import com.tachyonmusic.presentation.theme.Theme
import com.tachyonmusic.presentation.util.AdmobNativeAppInstallAd
import com.tachyonmusic.presentation.util.asString
import com.tachyonmusic.util.cycle
import com.tachyonmusic.util.debounce
import com.tachyonmusic.util.delay
import com.tachyonmusic.util.ms
import com.tachyonmusic.util.sec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object LibraryScreen :
    BottomNavigationItem(R.string.btmNav_library, R.drawable.ic_library, "library") {

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    operator fun invoke(
        draggable: AnchoredDraggableState<SwipingStates>,
        navController: NavController,
        viewModel: LibraryViewModel = hiltViewModel()
    ) {
        val scope = rememberCoroutineScope()
        val playbackItems by viewModel.items.collectAsState()

        val listState = rememberLazyListState()
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .debounce(200.ms)
                .collect { firstVisibleIndex ->
                    viewModel.loadArtwork(
                        kotlin.math.max(
                            firstVisibleIndex - (listState.layoutInfo.visibleItemsInfo.size + 8), 0
                        )..firstVisibleIndex + listState.layoutInfo.visibleItemsInfo.size + 8,
                        playbackItems
                    )
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Theme.padding.medium,
                    top = Theme.padding.medium,
                    end = Theme.padding.medium
                ), contentPadding = PaddingValues(bottom = Theme.padding.small)
        ) {
            item {
                val filterPlaybackType by viewModel.filterType.collectAsState()
                FilterItemRow(
                    filterPlaybackType,
                    onFilterSongs = viewModel::onFilterSongs,
                    onFilterRemixes = viewModel::onFilterRemixes,
                    onFilterPlaylists = viewModel::onFilterPlaylists
                )
            }

            item {
                val filterPlaybackType by viewModel.filterType.collectAsState()
                val availableSortTypes by viewModel.availableSortTypes.collectAsState()
                var sortOptionsExpanded by rememberSaveable { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .padding(vertical = Theme.padding.small)
                ) {
                    var iconBounds by remember { mutableStateOf<Rect?>(null) }
                    ExposedDropdownMenuBox(
                        modifier = Modifier
                            .clip(Theme.shapes.extraLarge)
                            .weight(1f)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        // Observe pointer events in the Initial pass
                                        val event =
                                            awaitPointerEvent(PointerEventPass.Initial)
                                        event.changes.forEach { change ->
                                            // If the pointer just went down and is above the IconButton, consume it
                                            if (iconBounds?.contains(change.position) == true && change.changedToDown()) {
                                                viewModel.flipSortOrder()
                                                change.consumeDownChange()
                                            }
                                        }
                                    }
                                }
                            },
                        expanded = sortOptionsExpanded,
                        onExpandedChange = {
                            sortOptionsExpanded = !sortOptionsExpanded
                        }
                    ) {
                        val iconAndTextColor by animateColorAsState(
                            if (sortOptionsExpanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground,
                            tween(Theme.animation.short)
                        )

                        val underlineColor by animateColorAsState(
                            if (sortOptionsExpanded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.inversePrimary
                        )

                        val sortParams by viewModel.sortParams.collectAsState()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                modifier = Modifier.menuAnchor(),
                                value = sortParams.type.asString(filterPlaybackType),
                                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = iconAndTextColor,
                                    unfocusedTextColor = iconAndTextColor,
                                    focusedTrailingIconColor = iconAndTextColor,
                                    unfocusedTrailingIconColor = iconAndTextColor,
                                    focusedLeadingIconColor = iconAndTextColor,
                                    unfocusedLeadingIconColor = iconAndTextColor,

                                    focusedIndicatorColor = underlineColor,
                                    unfocusedIndicatorColor = underlineColor,

                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortOptionsExpanded)
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            // Handled in the pointerInput MotionEvent capture
                                        },
                                        modifier = Modifier.onGloballyPositioned {
                                            iconBounds = it.boundsInParent()
                                        }
                                    ) {
                                        val iconRotation by animateFloatAsState(
                                            targetValue = when (sortParams.order) {
                                                SortOrder.Ascending -> 0f
                                                SortOrder.Descending -> 180f
                                            }
                                        )

                                        Icon(
                                            painter = painterResource(R.drawable.ic_sort),
                                            contentDescription = "Open Sorting Options",
                                            tint = iconAndTextColor,
                                            modifier = Modifier.rotate(iconRotation)
                                        )
                                    }
                                }
                            )
                        }

                        ExposedDropdownMenu(
                            expanded = sortOptionsExpanded,
                            onDismissRequest = { sortOptionsExpanded = false }
                        ) {
                            availableSortTypes.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(it.asString(filterPlaybackType))
                                    },
                                    onClick = {
                                        viewModel.onSortTypeChanged(it)
                                        sortOptionsExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = Theme.padding.small),
                        onClick = {
                            navController.navigate(
                                PlaybackSearchScreen.route(mapOf("playbackType" to filterPlaybackType.toString()))
                            )
                        } // TODO: Correct animation like in contacts app
                    ) {
                        Icon(
                            Icons.Default.Search,
                            "Search Playbacks",
                            modifier = Modifier.scale(1.3f)
                        )
                    }
                }
            }


            items(playbackItems, key = { it.mediaId.toString() }) { playback ->

                val contentModifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Theme.padding.extraSmall)
                val nativeAds by viewModel.nativeAppInstallAdCache.collectAsState()

                if (playback.playbackType is PlaybackType.Ad.NativeAppInstall && nativeAds.isNotEmpty()) {
                    AdmobNativeAppInstallAd(
                        contentModifier,
                        nativeAds.cycle(playback.mediaId.source.toIntOrNull() ?: 0)
                    )
                } else if (playback.playbackType is PlaybackType.Playback) {
                    val updatedPlayback by rememberUpdatedState(playback)
                    var showArtworkSelectionDialog by remember { mutableStateOf(false) }
                    var showMetadataDialog by remember { mutableStateOf(false) }
                    var showDropDownMenu by remember { mutableStateOf(false) }

                    SwipeDelete(
                        shape = Theme.shapes.medium,
                        modifier = contentModifier,
                        onClick = {
                            viewModel.excludePlayback(updatedPlayback)
                        }
                    ) {
                        HorizontalPlaybackView(
                            updatedPlayback.displayTitle,
                            updatedPlayback.displaySubtitle,
                            updatedPlayback.artwork ?: PlaceholderArtwork,
                            isEnabled = updatedPlayback.isPlayable,
                            dropDownMenuExpanded = showDropDownMenu,
                            onOptionsMenuClicked = {
                                showDropDownMenu = !showDropDownMenu
                            },
                            dropDownMenuContent = {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.set_metadata),
                                                contentDescription = stringResource(R.string.set_metadata)
                                            )
                                            Text(stringResource(R.string.set_metadata))
                                        }
                                    },
                                    onClick = {
                                        showMetadataDialog = true
                                        showDropDownMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.photo_library),
                                                contentDescription = stringResource(R.string.select_artwork)
                                            )
                                            Text(stringResource(R.string.select_artwork))
                                        }
                                    },
                                    onClick = {
                                        viewModel.queryArtwork(updatedPlayback)
                                        showArtworkSelectionDialog = true
                                        showDropDownMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        val text =
                                            stringResource(if (updatedPlayback.playbackType is PlaybackType.Song) R.string.hide else R.string.delete)

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (updatedPlayback.playbackType is PlaybackType.Song)
                                                Icon(
                                                    painterResource(R.drawable.visibility_off),
                                                    contentDescription = text
                                                )
                                            else
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = text
                                                )
                                            Text(text)
                                        }
                                    },
                                    onClick = {
                                        viewModel.excludePlayback(updatedPlayback)
                                        showDropDownMenu = false
                                    }
                                )
                            },
                            onClick = {
                                if (updatedPlayback.isPlayable) {
                                    viewModel.onItemClicked(updatedPlayback)
                                    scope.launch {
                                        draggable.animateTo(SwipingStates.EXPANDED)
                                    }
                                }
                            })
                    }

                    if (showArtworkSelectionDialog) {
                        Dialog(
                            onDismissRequest = { showArtworkSelectionDialog = false }
                        ) {
                            val artworks by viewModel.queriedArtwork.collectAsState()
                            val error by viewModel.artworkLoadingError.collectAsState()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Theme.shapes.extraLarge)
                            ) {
                                Column {
                                    var searchQuery by remember { mutableStateOf(updatedPlayback.albumArtworkSearchQuery) }

                                    LaunchedEffect(searchQuery) {
                                        delay(2.sec) // TODO: Proper delay option/...
                                        viewModel.queryArtwork(updatedPlayback, searchQuery)
                                    }

                                    Text(
                                        "Select artwork to assign to playback",
                                        modifier = Modifier.padding(Theme.padding.medium)
                                    )

                                    Text("Search Query")
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it })

                                    if (error != null) {
                                        Text(
                                            error?.asString() ?: "Unknown error occurred",
                                            modifier = Modifier.padding(Theme.padding.medium)
                                        )
                                    }

                                    LazyVerticalGrid(
                                        modifier = Modifier.padding(Theme.padding.medium),
                                        columns = GridCells.Adaptive(100.dp),
                                        contentPadding = PaddingValues(Theme.padding.small)
                                    ) {
                                        items(artworks) { artwork ->
                                            artwork(null,
                                                Modifier
                                                    .size(100.dp)
                                                    .clickable {
                                                        showArtworkSelectionDialog = false
                                                        viewModel.assignArtworkToPlayback(
                                                            artwork,
                                                            updatedPlayback
                                                        )
                                                    })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showMetadataDialog) {
                        Dialog(
                            onDismissRequest = { showMetadataDialog = false }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Theme.shapes.extraLarge)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    var title by remember { mutableStateOf(playback.title) }
                                    var artist by remember { mutableStateOf(playback.artist) }
                                    var name by remember { mutableStateOf(playback.displayTitle) }
                                    var album by remember { mutableStateOf(playback.album) }

                                    val playbackType = playback.mediaId.playbackType

                                    if (playbackType !is PlaybackType.Playlist) {
                                        Text("Title")
                                        TextField(value = title, onValueChange = { title = it })

                                        Text("Artist")
                                        TextField(value = artist, onValueChange = { artist = it })

                                        Text("Album")
                                        TextField(value = album, onValueChange = { album = it })
                                    }
                                    if (playbackType is PlaybackType.Remix || playbackType is PlaybackType.Playlist) {
                                        Text("Name")
                                        TextField(value = name, onValueChange = { name = it })
                                    }

                                    Button(
                                        onClick = {
                                            showMetadataDialog = false
                                            viewModel.updateMetadata(
                                                playback,
                                                title,
                                                artist,
                                                name,
                                                album
                                            )
                                        }
                                    ) {
                                        Text("Confirm")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}