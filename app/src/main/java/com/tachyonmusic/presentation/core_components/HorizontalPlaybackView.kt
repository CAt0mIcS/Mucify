package com.tachyonmusic.presentation.core_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tachyonmusic.core.domain.Artwork
import com.tachyonmusic.presentation.theme.Theme

@Composable
fun HorizontalPlaybackView(
    displayTitle: String,
    displaySubtitle: String,
    artwork: Artwork,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    dropDownMenuExpanded: Boolean = false,
    onOptionsMenuClicked: () -> Unit = {},
    dropDownMenuContent: (@Composable ColumnScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(Theme.shadow.extraSmall, shape = Theme.shapes.medium)
            .background(
                if (isEnabled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = Theme.shapes.medium
            )
//            .border(BorderStroke(1.dp, Theme.colors.border), shape = Theme.shapes.medium)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(modifier = Modifier.weight(1f)) {
            artwork.Image(
                contentDescription = "Album Artwork",
                modifier = Modifier
                    .padding(Theme.padding.extraSmall)
                    .size(50.dp, 50.dp)
                    .clip(Theme.shapes.medium)
            )

            Column(
                modifier = Modifier
                    .padding(start = Theme.padding.extremelySmall)
                    .align(Alignment.CenterVertically)
            ) {
                AnimatedText(
                    modifier = Modifier
                        .padding(top = Theme.padding.small),
                    text = displayTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer
                )

                AnimatedText(
                    modifier = Modifier
                        .padding(
                            start = Theme.padding.small,
                            bottom = Theme.padding.small
                        ),
                    text = displaySubtitle,
                    fontSize = 14.sp,
                    gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }


        if (dropDownMenuContent != null) {
            IconButton(
                onClick = onOptionsMenuClicked,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Playback options")

                DropdownMenu(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    expanded = dropDownMenuExpanded,
                    onDismissRequest = onOptionsMenuClicked,
                    content = dropDownMenuContent
                )
            }
        }
    }
}
