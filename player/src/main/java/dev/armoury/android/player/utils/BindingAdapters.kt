package dev.armoury.android.player.utils

import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.exoplayer2.ui.PlayerView
import dev.armoury.android.player.data.PlayerState
import dev.armoury.android.widget.MessageView

/*
 * Message View
 */

/**
 * Show/hide the player base on the state of the player
 */
@BindingAdapter("playerVisibility")
fun PlayerView.customizePlayerVisibility(state: PlayerState) {
    visibility = when (state) {
        is PlayerState.Pause,
        is PlayerState.Playing -> View.VISIBLE
        else -> View.INVISIBLE
    }
}

/**
 * Show or hide the loading indicator related views
 */
@BindingAdapter("showPlayerLoading")
fun View.setPlayerLoadingVisibility(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}


/**
 * Update the state of the message view base on the
 * [dev.armoury.android.player.data.PlayerState]
 */
@BindingAdapter("playerState")
fun MessageView.setMessageViewState(state: PlayerState?) {
    visibility = when (state) {
        is PlayerState.Error.General,
        is PlayerState.Error.PaymentRequired,
        is PlayerState.Error.Finished,
        is PlayerState.Error.Playing,
        is PlayerState.Error.AuthenticationTimeout,
        is PlayerState.Error.ComingSoonPassed,
        is PlayerState.Error.ForbiddenAccess,
        is PlayerState.Error.NoInternet,
        is PlayerState.Error.Unauthorized -> {
            updateState(messageModel = (state as PlayerState.Error).messageModel)
            View.VISIBLE
        }
        else -> {
            View.GONE
        }
    }
}
/*
 * End of the message view
 */