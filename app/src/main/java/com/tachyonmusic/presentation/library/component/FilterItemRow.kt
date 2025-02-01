package com.tachyonmusic.presentation.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.tachyonmusic.app.R
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.presentation.theme.Theme

@Composable
fun FilterItemRow(
    filterPlaybackType: PlaybackType,
    modifier: Modifier = Modifier,
    onFilterSongs: () -> Unit,
    onFilterRemixes: () -> Unit,
    onFilterPlaylists: () -> Unit
) {
    Row(
        modifier = modifier
            .shadow(Theme.shadow.small, shape = Theme.shapes.extraLarge)
            .clip(Theme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(
                start = Theme.padding.medium,
                top = Theme.padding.extraSmall,
                end = Theme.padding.medium,
                bottom = Theme.padding.extraSmall
            )
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.width(2.dp))
        FilterItem(
            R.string.songs, selected = filterPlaybackType is PlaybackType.Song,
            modifier = Modifier.weight(1f),
            onClick = onFilterSongs
        )

        Spacer(modifier = Modifier.width(16.dp))
        FilterItem(
            R.string.remixes,
            selected = filterPlaybackType is PlaybackType.Remix,
            modifier = Modifier.weight(1f),
            onClick = onFilterRemixes
        )

        Spacer(modifier = Modifier.width(16.dp))
        FilterItem(
            R.string.playlists,
            selected = filterPlaybackType is PlaybackType.Playlist,
            modifier = Modifier.weight(1f),
            onClick = onFilterPlaylists
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}