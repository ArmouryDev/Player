package dev.armoury.android.player.data

data class VideoInfoModel(
    val width : Int?,
    val height : Int?,
    val qualityTracks: List<VideoTrackModel.Quality>?,
    val audioTracks: List<VideoTrackModel.Audio>?,
    val subtitleTracks: List<VideoTrackModel.Subtitle>?
)