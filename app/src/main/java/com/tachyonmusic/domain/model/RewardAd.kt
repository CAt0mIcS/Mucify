package com.tachyonmusic.domain.model

import androidx.activity.ComponentActivity

interface RewardAd {
    val type: Type?

    fun load()
    fun unload()
    fun show(activity: ComponentActivity, onRewardGranted: (Type, Int) -> Unit)

    sealed class Type(val amount: Int) {
        class NewRemixes(numGrantedRemixes: Int) : Type(numGrantedRemixes)
        data object Invalid: Type(-1)
    }
}