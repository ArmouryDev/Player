package dev.armoury.android.player.data

import dev.armoury.android.player.utils.ArmouryMediaUtils

sealed class VideoTrackModel {

    data class Quality(
        override val groupIndex: Int,
        override val trackIndex: Int,
        override val title: CharSequence? = null,
        override val titleRes: Int? = null,
        val width: Int,
        val height: Int,
        override val default: Boolean = false
    ) : VideoTrackModel() {
        override val id: String = "$groupIndex-$trackIndex"
    }

    data class Audio(
        override val title: CharSequence,
        override val groupIndex: Int,
        override val trackIndex: Int,
        override val default: Boolean = false
    ) : VideoTrackModel() {
        override val id: String = "$groupIndex-$trackIndex"
    }

    data class Subtitle(
        override val title: CharSequence? = null,
        override val titleRes: Int? = null,
        override val groupIndex: Int,
        override val trackIndex: Int,
        override val default: Boolean = false
    ) : VideoTrackModel() {
        override val id: String = "$groupIndex-$trackIndex"
    }

    open val title: CharSequence? = null
    open val titleRes: Int? = null
    abstract val groupIndex: Int
    abstract val trackIndex: Int
    abstract val id: String
    abstract val default: Boolean

}

fun VideoTrackModel.Quality?.isAutoQuality() =
    if (this == null) false
    else this == ArmouryMediaUtils.autoQualityTrack

fun VideoTrackModel.Subtitle?.isNoSubTitle() =
    if (this == null) false
    else this == ArmouryMediaUtils.noSubtitleTrack
