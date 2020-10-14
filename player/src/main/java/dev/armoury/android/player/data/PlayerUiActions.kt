package dev.armoury.android.player.data

import com.google.android.exoplayer2.PlaybackParameters

sealed class PlayerUiActions {

    class ShowSpeedPicker(val currentSpeedModel: VideoSpeedModel) : PlayerUiActions()

    class ShowQualityPicker(
        val availableQualityTracks: List<VideoTrackModel>,
        val currentQuality: VideoTrackModel
    ) : PlayerUiActions()

    class UpdatePlayerParam(val playerParam: PlaybackParameters) : PlayerUiActions()

    class PreparePlayer(val videoFileUrl: String, val requestedPosition: Long? = null) : PlayerUiActions()

    object ToggleFullScreen : PlayerUiActions()

}