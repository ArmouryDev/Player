package dev.armoury.android.player.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import dev.armoury.android.player.R
import dev.armoury.android.player.data.PlayerState
import dev.armoury.android.player.data.PlayerUiActions
import dev.armoury.android.player.data.VideoSpeedModel
import dev.armoury.android.player.data.VideoTrackModel
import dev.armoury.android.player.utils.ArmouryMediaUtils
import dev.armoury.android.player.viewmodel.ArmouryPlayerViewModel
import dev.armoury.android.player.widgets.AppTimeBar
import dev.armoury.android.player.widgets.PlayerTimeTextView
import dev.armoury.android.ui.ArmouryFragment
import dev.armoury.android.utils.isPortrait

abstract class ArmouryPlayerFragment<T : ViewDataBinding, V : ArmouryPlayerViewModel> :
    ArmouryFragment<T, V>() {

    private var exoPlayer: SimpleExoPlayer? = null
    private var toggleFullScreenButton: AppCompatImageView? = null

    private val playerUiActionObserver: Observer<PlayerUiActions?> by lazy {
        Observer<PlayerUiActions?> { action ->
            when (action) {
                is PlayerUiActions.PreparePlayer -> {
                    preparePlayer(
                        url = action.videoFileUrl,
                        requestedPosition = action.requestedPosition
                    )
                }
                is PlayerUiActions.ShowQualityPicker -> {
                    showQualityPicker(
                        availableQualityTracks = action.availableQualityTracks,
                        selectedQuality = action.currentQuality
                    )
                }
                is PlayerUiActions.ShowSpeedPicker -> {
                    showSpeedPicker(selectedSpeedModel = action.currentSpeedModel)
                }
                is PlayerUiActions.UpdatePlayerParam -> {
                    exoPlayer?.setPlaybackParameters(action.playerParam)
                }
                //  TODO Should be checked later
                is PlayerUiActions.ToggleFullScreen -> {
                    activity.apply {
                        requestedOrientation =
                            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        onScreenRotated(requestedOrientation)
                    }
                }
            }
        }
    }

    private val playerStateObserver: Observer<PlayerState?> by lazy {
        Observer<PlayerState?> { state ->
            when (state) {
                is PlayerState.Preparing -> preparePlayer(
                    state.url,
                    state.requestedPosition
                )
                is PlayerState.Stop -> stopPlayer()
                is PlayerState.Error -> stopPlayer()
            }
        }
    }

    //  TODO : Find a better way!?
    //  TODO : Maybe customize exo player
    private val timeRelatedViewsVisibilityObserver: Observer<Int?> by lazy {
        Observer<Int?> {
            viewDataBinding.root.findViewById<AppTimeBar>(R.id.exo_progress)?.setForceVisibility(it)
            viewDataBinding.root.findViewById<PlayerTimeTextView>(R.id.exo_position)
                ?.setForceVisibility(it)
            viewDataBinding.root.findViewById<PlayerTimeTextView>(R.id.exo_time_slash)
                ?.setForceVisibility(it)
            viewDataBinding.root.findViewById<PlayerTimeTextView>(R.id.exo_duration)
                ?.setForceVisibility(it)
        }
    }

    private val playerEventListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            viewModel.onPlaybackStateChanged(
                playWhenReady = playWhenReady,
                playbackState = playbackState
            )
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            viewModel.onPlayerError(error)
        }

    }

    private val bandwidthMeter: DefaultBandwidthMeter by lazy {
        DefaultBandwidthMeter.Builder(activity).build()
    }

    override fun startObserving() {
        super.startObserving()
        viewModel.playerUiActions.observe(this, playerUiActionObserver)
        viewModel.state.observe(this, playerStateObserver)
        viewModel.timeRelatedViewsVisibility.observe(this, timeRelatedViewsVisibilityObserver)
        //  TODO
        viewModel.stopPlayer.observe(this, Observer {
            if (it == true) stopPlayer()
        })
    }

    protected abstract fun showQualityPicker(
        availableQualityTracks: List<VideoTrackModel>,
        selectedQuality: VideoTrackModel
    )

    abstract fun showSpeedPicker(selectedSpeedModel: VideoSpeedModel)

    override fun doOtherTasks() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewDataBinding.root.findViewById<AppCompatImageView>(R.id.exo_toggle_full_screen)?.apply {
            setOnClickListener {
                viewModel.onViewClicked(this.id)
            }
            toggleFullScreenButton = this
        }
        viewDataBinding.root.findViewById<View>(R.id.exo_settings)?.apply {
            setOnClickListener {
                viewModel.onViewClicked(this.id)
            }
        }

        // Make the screen on when using watching a video
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onFragmentStopped(exoPlayer?.currentPosition)
        stopPlayer()
    }

    override fun onStart() {
        super.onStart()
        /**
         * We can use onRestart here, but as we are going to change this page to
         * a fragment, we are calling this function in onStart
         */
        viewModel.onFragmentStarted()
    }

    private fun preparePlayer(url: String, requestedPosition: Long? = null) {
        exoPlayer = SimpleExoPlayer
            .Builder(activity, DefaultRenderersFactory(activity))
            .setTrackSelector(viewModel.adaptiveTrackSelectionFactory)
            .setLoadControl(DefaultLoadControl())
            .setBandwidthMeter(bandwidthMeter)
            .build() // TODO
        exoPlayer?.apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            playWhenReady = true
            addListener(playerEventListener)
            setPlayer(this)
            prepare(ArmouryMediaUtils.buildMediaSource(url = url, userAgent = getPlayerAgentName()))
            requestedPosition?.let { seekTo(it) }
        }
    }

    abstract fun setPlayer(simpleExoPlayer: SimpleExoPlayer)

    private fun stopPlayer() {
        exoPlayer?.apply {
            playWhenReady = false
            stop()
            release()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateToggleFullScreenButton()
    }

    protected open fun onScreenRotated(rotation: Int) {

    }

    private fun updateToggleFullScreenButton() {
        toggleFullScreenButton?.setImageResource(
            if (activity.isPortrait()) {
                R.drawable.ic_fullscreen
            } else {
                R.drawable.ic_fullscreen_exit
            }
        )
    }

    //  It's going to return the application name in most cases
    protected abstract fun getPlayerAgentName(): String

}