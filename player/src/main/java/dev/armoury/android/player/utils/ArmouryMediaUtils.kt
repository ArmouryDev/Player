package dev.armoury.android.player.utils

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util.toLowerInvariant
import dev.armoury.android.player.R
import dev.armoury.android.player.data.VideoSpeedModel
import dev.armoury.android.player.data.VideoTrackModel

object ArmouryMediaUtils {

    val autoQualityTrack: VideoTrackModel by lazy {
        VideoTrackModel(
            titleRes = R.string.quality_auto,
            width = Int.MAX_VALUE,
            groupIndex = -1,
            trackIndex = -1
        )
    }

    val defaultSpeedModel: VideoSpeedModel by lazy {
        VideoSpeedModel(
            title = R.string.speed_normal,
            value = 1.0f
        )
    }

    val speedOptions: List<VideoSpeedModel> by lazy {
        listOf(
            VideoSpeedModel(
                title = R.string.speed_0_5,
                value = 0.5f
            ),
            VideoSpeedModel(
                title = R.string.speed_0_75,
                value = 0.75f
            ),
            VideoSpeedModel(
                title = R.string.speed_normal,
                value = 1f
            ),
            VideoSpeedModel(
                title = R.string.speed_1_25,
                value = 1.25f
            ),
            VideoSpeedModel(
                title = R.string.speed_1_5,
                value = 1.5f
            ),
            VideoSpeedModel(
                title = R.string.speed_1_75,
                value = 1.75f
            ),
            VideoSpeedModel(
                title = R.string.speed_2,
                value = 2f
            )
        )
    }

    fun getRendererIndex(
        trackedGroup: MappingTrackSelector.MappedTrackInfo,
        trackType: Int
    ): Int? {
        for (i in 0 until trackedGroup.rendererCount) {
            val trackGroups = trackedGroup.getTrackGroups(i)
            if (
                trackGroups.length != 0 &&
                trackedGroup.getRendererType(i) == trackType
            ) {
                return i
            }
        }
        return null
    }

    fun buildMediaSource(
        url: String,
        userAgent: String
    ): MediaSource {
        val uri = Uri.parse(url)
        when (getMediaType(uri)) {
            C.TYPE_HLS -> return HlsMediaSource
                .Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
            C.TYPE_OTHER -> return ProgressiveMediaSource
                .Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
            else -> throw Throwable("We are not going to handle this type of video!")
        }
    }

    fun getMediaType(url: String) = getMediaType(Uri.parse(url))

    private fun getMediaType(uri: Uri) : Int {
        val fileName = toLowerInvariant(uri.path ?: "") // TODO
        return when {
            fileName.endsWith(".mpd") or fileName.endsWith("mpd") -> C.TYPE_DASH
            fileName.endsWith(".m3u8") or fileName.endsWith("m3u8") -> C.TYPE_HLS
            fileName.matches(Regex.fromLiteral(".*\\.ism(l)?(/manifest(\\(.+\\))?)?")) -> C.TYPE_SS
            else -> C.TYPE_OTHER
        }
    }

    fun getVideoTrackList(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?
    ): List<VideoTrackModel>? {
        return mappedTrackInfo?.let {
            val rendererIndex = getRendererIndex(mappedTrackInfo, C.TRACK_TYPE_VIDEO)
            return rendererIndex?.let {
                val videoTracks = ArrayList<VideoTrackModel>()
                val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
                val trackGroupCount = trackGroups.length
                for (i in 0 until trackGroupCount) {
                    val currentGroup = trackGroups[i]
                    val itemsCount = currentGroup.length
                    for (j in 0 until itemsCount) {
                        if (mappedTrackInfo.getTrackSupport(rendererIndex, i, j) ==
                            RendererCapabilities.FORMAT_HANDLED
                        ) {
                            val format = currentGroup.getFormat(j)
                            videoTracks.add(
                                VideoTrackModel(
                                    title = "${format.height}p",
                                    groupIndex = i,
                                    trackIndex = j,
                                    width = format.width
                                )
                            )
                        }
                    }
                }
                videoTracks.add(0, autoQualityTrack)
                videoTracks.sortWith(Comparator { trackInfoModel, t1 -> t1.width - trackInfoModel.width })
                videoTracks
            }
        }
    }
}