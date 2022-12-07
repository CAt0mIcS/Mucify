package com.tachyonmusic.core.domain

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

interface Artwork : Parcelable {
    @Composable
    fun Image(contentDescription: String?, modifier: Modifier)

    override fun describeContents() = 0
}