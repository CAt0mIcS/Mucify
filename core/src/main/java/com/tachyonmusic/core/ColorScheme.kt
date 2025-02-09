package com.tachyonmusic.core

sealed class ColorScheme(val name: String) {
    abstract val stringRes: Int

    data object System : ColorScheme(SYSTEM_NAME) {
        override val stringRes = R.string.colorscheme_system
    }
    data object White : ColorScheme(WHITE_NAME) {
        override val stringRes = R.string.colorscheme_white
    }
    data object Dark : ColorScheme(DARK_NAME) {
        override val stringRes = R.string.colorscheme_dark
    }

    companion object {
        fun fromString(name: String) = when (name) {
            SYSTEM_NAME -> System
            WHITE_NAME -> White
            DARK_NAME -> Dark
            else -> TODO("Invalid color scheme name $name")
        }

        const val SYSTEM_NAME = "sys"
        const val WHITE_NAME = "white"
        const val DARK_NAME = "dark"
    }
}