package it.fast4x.rimusic.utils

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.PauseBetweenSongs
import it.fast4x.rimusic.enums.PlayerTimelineType
import it.fast4x.rimusic.models.ui.UiMedia
import it.fast4x.rimusic.service.PlayerService
import it.fast4x.rimusic.ui.components.SeekBar
import it.fast4x.rimusic.ui.components.SeekBarColored
import it.fast4x.rimusic.ui.components.SeekBarCustom
import it.fast4x.rimusic.ui.components.SeekBarWaved
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.collapsedPlayerProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun GetSeekBar(
    position: Long,
    duration: Long,
    mediaId: String,
    media: UiMedia,
){
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    val (colorPalette, typography) = LocalAppearance.current
    val playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.Default)
    var scrubbingPosition by remember(mediaId) {
        mutableStateOf<Long?>(null)
    }
    val scope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(position.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    val pauseBetweenSongs by rememberPreference(pauseBetweenSongsKey, PauseBetweenSongs.`0`)

    val compositionLaunched = isCompositionLaunched()
    LaunchedEffect(mediaId) {
        if (compositionLaunched) animatedPosition.animateTo(0f)
    }
    LaunchedEffect(position) {
        if (!isSeeking && !animatedPosition.isRunning)
            animatedPosition.animateTo(
                position.toFloat(), tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {

        /*
        val visualizerData = remember {
            mutableStateOf(VisualizerData())
        }
        val amp = getAmplitudes(
            binder = binder,
            context = context,
            visualizerData = visualizerData
        )
        //println("mediaItem Controls amplitudes ${amp}")

        AudioWaveform(
            amplitudes = amp,
            progress = position.toFloat(),
            onProgressChange = {},
            amplitudeType = AmplitudeType.Avg,
            waveformBrush = SolidColor(colorPalette.text),
            progressBrush = SolidColor(colorPalette.favoritesIcon)

        )
         */

        if (playerTimelineType != PlayerTimelineType.Default
            && playerTimelineType != PlayerTimelineType.Wavy
            //&& playerTimelineType != PlayerTimelineType.ColoredBar
            )
            SeekBarCustom(
                type = playerTimelineType,
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette.collapsedPlayerProgressBar,
                backgroundColor = colorPalette.textSecondary,
                shape = RoundedCornerShape(8.dp)
            )

        if (playerTimelineType == PlayerTimelineType.Default)
            SeekBar(
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette.collapsedPlayerProgressBar,
                backgroundColor = colorPalette.textSecondary,
                shape = RoundedCornerShape(8.dp),
            )

        if (playerTimelineType == PlayerTimelineType.Wavy) {
            SeekBarWaved(
                position = { animatedPosition.value },
                range = 0f..media.duration.toFloat(),
                onSeekStarted = {
                    scrubbingPosition = it.toLong()

                    //isSeeking = true
                    scope.launch {
                        animatedPosition.animateTo(it)
                    }

                },
                onSeek = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0F, duration.toFloat())
                            ?.toLong()
                    } else {
                        null
                    }

                    if (media.duration != C.TIME_UNSET) {
                        //isSeeking = true
                        scope.launch {
                            animatedPosition.snapTo(
                                animatedPosition.value.plus(delta)
                                    .coerceIn(0f, media.duration.toFloat())
                            )
                        }
                    }

                },
                onSeekFinished = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                    /*
                isSeeking = false
                animatedPosition.let {
                    binder.player.seekTo(it.targetValue.toLong())
                }
                 */
                },
                color = colorPalette.collapsedPlayerProgressBar,
                isActive = binder.player.isPlaying,
                backgroundColor = colorPalette.textSecondary,
                shape = RoundedCornerShape(8.dp)
            )
        }


        /*
        if (playerTimelineType == PlayerTimelineType.ColoredBar)
            SeekBarColored(
                alphaType = false,
                value = scrubbingPosition ?: position,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = {
                    scrubbingPosition = it
                },
                onDrag = { delta ->
                    scrubbingPosition = if (duration != C.TIME_UNSET) {
                        scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                    } else {
                        null
                    }
                },
                onDragEnd = {
                    scrubbingPosition?.let(binder.player::seekTo)
                    scrubbingPosition = null
                },
                color = colorPalette.collapsedPlayerProgressBar,
                backgroundColor = colorPalette.textSecondary,
                shape = RoundedCornerShape(8.dp),
            )
         */

    }

    Spacer(
        modifier = Modifier
            .height(8.dp)
    )


    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {
        BasicText(
            text = formatAsDuration(scrubbingPosition ?: position),
            style = typography.xxs.semiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (duration != C.TIME_UNSET) {
            val positionAndDuration = binder.player.positionAndDurationState()
            var timeRemaining by remember { mutableIntStateOf(0) }
            timeRemaining =
                positionAndDuration.value.second.toInt() - positionAndDuration.value.first.toInt()
            var paused by remember { mutableStateOf(false) }

            if (pauseBetweenSongs != PauseBetweenSongs.`0`)
                LaunchedEffect(timeRemaining) {
                    if (
                    //formatAsDuration(timeRemaining.toLong()) == "0:00"
                        timeRemaining.toLong() < 500
                    ) {
                        paused = true
                        binder.player.pause()
                        delay(pauseBetweenSongs.number)
                        //binder.player.seekTo(position+2000)
                        binder.player.play()
                        paused = false
                    }
                }

            if (!paused) {

                if (showRemainingSongTime)
                    BasicText(
                        text = "-${formatAsDuration(timeRemaining.toLong())}",
                        style = typography.xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                    )

                /*
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                ) {
                    Image(
                        painter = painterResource(R.drawable.time),
                        colorFilter = ColorFilter.tint(colorPalette.accent),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(horizontal = 5.dp),
                        contentDescription = "Background Image",
                        contentScale = ContentScale.Fit
                    )
                    BasicText(
                        text = " ${formatAsTime(totalPlayTimes)}",
                        style = typography.xxs.semiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                 */

            } else {
                Image(
                    painter = painterResource(R.drawable.pause),
                    colorFilter = ColorFilter.tint(colorPalette.accent),
                    modifier = Modifier
                        .size(20.dp),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Fit
                )
            }

            /*
            BasicText(
                text = "-${formatAsDuration(timeRemaining.toLong())} / ${formatAsDuration(duration)}",
                style = typography.xxs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
             */

            BasicText(
                text = formatAsDuration(duration),
                style = typography.xxs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

        }
    }


}