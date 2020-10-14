package dev.armoury.android.player.data

import dev.armoury.android.player.utils.ArmouryMediaUtils

data class VideoTrackModel(
    val titleRes: Int? = null,
    val title: CharSequence? = null,
    val width: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String = "$groupIndex-$trackIndex"
)

fun VideoTrackModel?.isAutoQuality() =
    if (this == null) false
    else this == ArmouryMediaUtils.autoQualityTrack
