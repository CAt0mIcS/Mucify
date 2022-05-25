package com.daton.media.ext

import android.support.v4.media.MediaBrowserCompat

inline val MediaBrowserCompat.MediaItem.artist: String
    get() = description.artist

inline val MediaBrowserCompat.MediaItem.title: String
    get() = description.title as String


