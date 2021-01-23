package dev.armoury.android.player.viewmodel

import android.app.Application
import android.os.Handler
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import dev.armoury.android.data.ArmouryUiAction
import dev.armoury.android.lifecycle.SingleLiveEvent
import dev.armoury.android.widget.MessageView
import dev.armoury.android.widget.data.MessageModel
import dev.armoury.android.player.R
import dev.armoury.android.data.ErrorModel
import dev.armoury.android.player.data.*
import dev.armoury.android.player.utils.ArmouryMediaUtils
import dev.armoury.android.viewmodel.ArmouryViewModel


//  TODO : We should moved the player out of this library later.
abstract class ArmouryPlayerViewModel<UI: ArmouryUiAction>(applicationContext: Application) :
    ArmouryViewModel<UI>(applicationContext) {

    protected var videoUrl: String? = null

    protected val _state = MutableLiveData<PlayerState>(PlayerState.Idle)
    val state: LiveData<PlayerState>
        get() = _state

    protected val _enablePlayerController = MutableLiveData<Boolean>(true)
    val enablePlayerController: LiveData<Boolean>
        get() = _enablePlayerController

    private val _controllerVisibility: SingleLiveEvent<Int> = SingleLiveEvent(View.INVISIBLE)
    val controllerVisibility: LiveData<Int>
        get() = _controllerVisibility

    protected val _playerUiActions = SingleLiveEvent<PlayerUiActions?>(value = null)
    val playerUiActions: LiveData<PlayerUiActions?>
        get() = _playerUiActions

    protected val selectedSpeed = SingleLiveEvent(ArmouryMediaUtils.defaultSpeedModel)

    protected val selectedQuality = SingleLiveEvent(ArmouryMediaUtils.autoQualityTrack)

    protected val selectedSubtitle = SingleLiveEvent(ArmouryMediaUtils.noSubtitleTrack)

    protected val selectedAudio = SingleLiveEvent<VideoTrackModel.Audio?>(null)

    val showComingSoon: LiveData<Boolean> = Transformations.map(_state) {
        _state.value is PlayerState.Error.ComingSoon
    }

    /**
     * We need to save the last position of the player
     * if we are playing a video file and resuming
     */
    private var playerLastPosition: Long? = null

    //  TODO Can we merge this variable to the PlayerStates or not
    val stopPlayer : LiveData<Boolean> = Transformations.map(state) {
        state.value is PlayerState.NeedToPrepare || state.value is PlayerState.Error
    }

    val controllerVisibilityListener by lazy {
        PlayerControlView.VisibilityListener { visibility ->
            _controllerVisibility.value = visibility
        }
    }

    protected val hasTimeShift = MutableLiveData<Boolean>()

    /**
     * Playback Report Related
     */
    protected abstract fun needReportPlayback(): Boolean

    private var isReporting = false
    private val playbackReportHandler = Handler()
    private val playbackReportRunnable: Runnable by lazy {
        Runnable {
            sendReportPlaybackIsRequested()
            playbackReportHandler.postDelayed(
                playbackReportRunnable,
                getPlaybackReportInterval()
            )
        }
    }

    protected open fun getPlaybackReportInterval() : Long {
        TODO (reason = "You should override this function if you are going to have the report playback feature")
    }

    protected open fun sendReportPlaybackIsRequested() {
        TODO (reason = "You should override this function if you are going to have the report playback feature")
    }

    private fun stopReporting() {
        isReporting = false
        playbackReportHandler.removeCallbacks(playbackReportRunnable)
    }

    protected abstract fun getReportPlaybackRequestCode() : Int?

    protected open fun isSeriousReportPlaybackError(errorModel: ErrorModel): Boolean {
        TODO (reason = "You should override this function if you are going to have the report playback feature")
    }

    private fun startReporting() {
        if (isReporting) return
        isReporting = true
        playbackReportHandler.postDelayed(
            playbackReportRunnable,
            getPlaybackReportInterval()
        )
    }
    /**
     * End of the playback report related
     */

    val adaptiveTrackSelectionFactory by lazy {
        DefaultTrackSelector(applicationContext)
    }

    private val qualityRendererIndex: Int by lazy {
        adaptiveTrackSelectionFactory.currentMappedTrackInfo?.let {
            ArmouryMediaUtils.getRendererIndex(
                it,
                C.TRACK_TYPE_VIDEO
            )
        } ?: -1
    }

    private val subtitleRendererIndex: Int by lazy {
        adaptiveTrackSelectionFactory.currentMappedTrackInfo?.let {
            ArmouryMediaUtils.getRendererIndex(
                    it,
                    C.TRACK_TYPE_TEXT
            )
        } ?: -1
    }

    private val audioRendererIndex: Int by lazy {
        adaptiveTrackSelectionFactory.currentMappedTrackInfo?.let {
            ArmouryMediaUtils.getRendererIndex(
                    it,
                    C.TRACK_TYPE_AUDIO
            )
        } ?: -1
    }

    val timeRelatedViewsVisibility: LiveData<Int> = Transformations.map(hasTimeShift) {
        when (it) {
            false -> View.INVISIBLE
            else -> View.VISIBLE
        }
    }

    private val playbackState = MutableLiveData<Int>(Player.STATE_IDLE)

    //  TODO : The state of the playbackstate should be considered as well
    val showLoadingIndicator: LiveData<Boolean> = Transformations.map(state) {
        when (it) {
            is PlayerState.Preparing,
            is PlayerState.Fetching -> true
            else -> false
        }
    }

    //  TODO : Should be checked later
    val showBanner: LiveData<Boolean> = Transformations.map(playbackState) {
        when (it) {
            Player.STATE_BUFFERING,
            Player.STATE_READY -> false
            else -> true
        }
    }

    private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = e.sourceException
        while (cause != null) {
            if (cause is BehindLiveWindowException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    fun onPlaybackStateChanged(playWhenReady: Boolean, playbackState: Int) {
        this.playbackState.value = playbackState
        when (playbackState) {
            Player.STATE_IDLE -> {
                stopReporting()
            }
            Player.STATE_READY -> {
                _state.value = if(playWhenReady) PlayerState.Playing.VideoFile else PlayerState.Pause
                //  TODO Should be checked
                if (needReportPlayback()) {
                    when (playWhenReady) {
                        true -> startReporting()
                        false -> stopReporting()
                    }
                }
            }
            Player.STATE_ENDED -> {
                _state.value = PlayerState.Done
                stopReporting()
            }
            Player.STATE_BUFFERING -> {
                //  Nothing
            }
        }
    }

    fun onPlayerError(error: ExoPlaybackException?) {
        error?.let {
            when {
                isBehindLiveWindow(it) -> {
                    videoUrl?.let { url ->
                        _state.value = PlayerState.Preparing.Video(url)
                    }
                }
                else -> {
                    _messageModel.value = MessageModel(
                        state = MessageView.States.ERROR,
                        descriptionTextRes = R.string.message_error_playing_video,
                        buttonTextRes = R.string.button_retry
                    )
                    _state.value = PlayerState.Error.Playing(
                        MessageModel(
                            state = MessageView.States.ERROR,
                            descriptionTextRes = R.string.message_error_playing_video,
                            buttonTextRes = R.string.button_retry
                        )
                    )
                }
            }
        }
    }

    protected fun onSpeedSelected(selectedSpeed: VideoSpeedModel) {
        if (this.selectedSpeed.value == selectedSpeed) return
        this.selectedSpeed.value = selectedSpeed
        _playerUiActions.value = PlayerUiActions.UpdatePlayerParam(
            playerParam = PlaybackParameters(selectedSpeed.value, selectedSpeed.value)
        )
    }

    protected fun onQualitySelected(selectedTrack: VideoTrackModel.Quality) {
        if (this.selectedQuality.value == selectedTrack) return
            this.selectedQuality.value = selectedTrack
            val parametersBuilder = DefaultTrackSelector.ParametersBuilder()
            parametersBuilder.setRendererDisabled(qualityRendererIndex, false)
            if (selectedTrack.isAutoQuality()) {
                parametersBuilder.clearSelectionOverrides(qualityRendererIndex)
            } else {
                val override = DefaultTrackSelector.SelectionOverride(
                    selectedTrack.groupIndex,
                    selectedTrack.trackIndex
                )
                adaptiveTrackSelectionFactory.currentMappedTrackInfo?.getTrackGroups(qualityRendererIndex)?.let { array ->
                    parametersBuilder.setSelectionOverride(
                        qualityRendererIndex,
                        array,
                        override
                    )
                }
            }
            adaptiveTrackSelectionFactory.parameters = parametersBuilder.build()
    }

    protected fun onAudioSelected(selectedTrack: VideoTrackModel.Audio) {
        if (this.selectedAudio.value == selectedTrack) return
        this.selectedAudio.value = selectedTrack
        val parametersBuilder = DefaultTrackSelector.ParametersBuilder(applicationContext)
        parametersBuilder.setRendererDisabled(audioRendererIndex, false)

        val override = DefaultTrackSelector.SelectionOverride(
                selectedTrack.groupIndex,
                selectedTrack.trackIndex
        )

        adaptiveTrackSelectionFactory.currentMappedTrackInfo?.getTrackGroups(audioRendererIndex)?.let { array ->
            parametersBuilder.setSelectionOverride(
                    audioRendererIndex,
                    array,
                    override
            )
        }

        adaptiveTrackSelectionFactory.parameters = parametersBuilder.build()
    }

    protected fun onSubtitleSelected(selectedTrack: VideoTrackModel.Subtitle) {
        if (this.selectedSubtitle.value == selectedTrack) return
        this.selectedSubtitle.value = selectedTrack
        val parametersBuilder = DefaultTrackSelector.ParametersBuilder(applicationContext)
        if (selectedTrack.isNoSubTitle()) {
            parametersBuilder.clearSelectionOverrides(subtitleRendererIndex)
            parametersBuilder.setRendererDisabled(subtitleRendererIndex, true)
        } else {
            parametersBuilder.setRendererDisabled(subtitleRendererIndex, false)
            val override = DefaultTrackSelector.SelectionOverride(
                    selectedTrack.groupIndex,
                    selectedTrack.trackIndex
            )
            adaptiveTrackSelectionFactory.currentMappedTrackInfo?.getTrackGroups(subtitleRendererIndex)?.let { array ->
                parametersBuilder.setSelectionOverride(
                        subtitleRendererIndex,
                        array,
                        override
                )
            }
        }
        adaptiveTrackSelectionFactory.parameters = parametersBuilder.build()
    }

    fun onViewClicked(id: Int) {
        when (id) {
            R.id.exo_settings -> {
                handleClickSettingButton()
            }
            R.id.exo_toggle_full_screen -> {
                _playerUiActions.value = PlayerUiActions.ToggleFullScreen
            }
        }
    }

    protected open fun handleClickSettingButton() {
        TODO ("Should be implemented")
    }

    protected fun onSpeedOptionSelected() {
        _playerUiActions.value = PlayerUiActions.ShowSpeedPicker(
            currentSpeedModel = selectedSpeed.value!!
        )
    }

    //  TODO Performance Improvement needed
    protected fun onQualityOptionSelected() {
        _playerUiActions.value = PlayerUiActions.ShowQualityPicker(
            currentQuality = selectedQuality.value!!,
            availableQualityTracks = ArmouryMediaUtils.getVideoTrackList(adaptiveTrackSelectionFactory.currentMappedTrackInfo) ?: ArrayList()
        )
    }

    fun onFragmentStopped(playerPosition : Long?) {
        _state.value = PlayerState.Pause
        if (hasTimeShift.value == true) {
            playerLastPosition = playerPosition
        }
    }

    fun onFragmentStarted() {
        if (_state.value == PlayerState.Pause) {
            videoUrl?.let {
                _state.value = PlayerState.Preparing.Video(url = it, requestedPosition = playerLastPosition)
            }
        }
    }

    override fun onCleared() {
        if (needReportPlayback()) stopReporting()
        super.onCleared()
    }

    override fun handleErrorInChild(errorModel: ErrorModel) {
        when (errorModel.requestCode) {
            getReportPlaybackRequestCode() -> if (isSeriousReportPlaybackError(errorModel = errorModel)) onSeriousErrorOccurred(errorModel)
            else -> onSeriousErrorOccurred(errorModel)
        }
    }

    protected fun onSeriousErrorOccurred(errorModel: ErrorModel) {
        stopReporting()
        _state.value = PlayerState.Error.Playing(messageModel = errorModel.messageModel)
    }
}
