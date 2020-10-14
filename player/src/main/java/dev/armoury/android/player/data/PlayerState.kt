package dev.armoury.android.player.data

import dev.armoury.android.widget.data.MessageModel

sealed class PlayerState {

    object Idle : PlayerState()

    sealed class Fetching : PlayerState() {

        object UrlAccess : Fetching()

        object Vast : Fetching()

    }

    object NeedToPrepare : PlayerState()

    sealed class Preparing : PlayerState() {

        class Video(
            override val url: String,
            override val requestedPosition: Long? = null
        ) : Preparing()

        class Vast(
            override val url: String,
            override val requestedPosition: Long? = null
        ) : Preparing()

        abstract val url: String

        abstract val requestedPosition: Long?

    }

    sealed class Playing : PlayerState() {

        object VideoFile : Playing()

        class VAS(var showSkipButton: Boolean = false) : Playing()

    }

    object Pause: PlayerState()

    class DisplayingVas(var showSkipButton: Boolean = false) : PlayerState()

    sealed class Error() : PlayerState() {

        /**
         * When the user needs to be signed in to watch a video file, but he/she's not
         * signed in or his/her authentication is failed
         */
        data class Unauthorized(override val messageModel: MessageModel) : Error()

        /**
         * When the user needs to pay to become able to watch this video
         */
        data class PaymentRequired(override val messageModel: MessageModel) : Error()

        /**
         * When the user is not able to access the current video file
         * i.e. His/her ip is out of the authorized ip range
         */
        data class ForbiddenAccess(override val messageModel: MessageModel) : Error()

        /**
         * When he/she is not authorized anymore to have access to the video file
         * i.e. In some services more than n users can not watch available video files
         * simultaneously.
         */
        data class AuthenticationTimeout(override val messageModel: MessageModel) : Error()

        /**
         * The video file is not started yet. Mostly, in live streaming is going to happen
         */
        data class ComingSoon(override val messageModel: MessageModel) : Error()

        /**
         * When the star-time of a video file is passed but it's not started yet
         */
        data class ComingSoonPassed(override val messageModel: MessageModel) : Error()

        /**
         * When player is not able to play the video file
         */
        data class Playing(override val messageModel: MessageModel) : Error()

        /**
         * The streaming is finished
         */
        data class Finished(override val messageModel: MessageModel) : Error()

        /**
         * There is no internet connection available
         */
        data class NoInternet(override val messageModel: MessageModel) : Error()

        /**
         * All other situations that are not considered above
         */
        data class General(override val messageModel: MessageModel) : Error()

        abstract val messageModel: MessageModel
    }

    object Done : PlayerState()

    object Stop : PlayerState()
}