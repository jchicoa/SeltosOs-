package com.example.media

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.MediaItemEntry
import com.example.telemetry.SeltosTelemetryRepository
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateLabel
import com.example.ui.theme.TelemetryWhite

/**
 * Hardware-accelerated Media3 ExoPlayer supporting .mp4, .mkv, .avi, .m3u8 IPTV,
 * left/right swipe gestures for brightness & volume, PiP, and continuous background audio.
 */
@OptIn(UnstableApi::class)
@Composable
fun UnrestrictedVideoPlayer(
    currentTitle: String,
    currentUri: String,
    playlist: List<MediaItemEntry>,
    brightness: Float,
    volume: Float,
    backgroundPlaybackEnabled: Boolean,
    compactMode: Boolean = false,
    onSelectMedia: (MediaItemEntry) -> Unit,
    onAddCustomMedia: (String, String) -> Unit,
    onLoadM3uText: (String) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var showPlaylistDrawer by remember { mutableStateOf(!compactMode) }
    var isPlaying by remember { mutableStateOf(false) }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var containerWidthPx by remember { mutableStateOf(1) }

    val exoPlayer = remember(context) {
        SeltosMediaService.sharedPlayer ?: ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
    }

    LaunchedEffect(currentUri) {
        if (currentUri.isNotBlank()) {
            val existingUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
            if (existingUri != currentUri) {
                val item = MediaItem.Builder()
                    .setUri(currentUri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(currentTitle).build())
                    .build()
                exoPlayer.setMediaItem(item)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer, backgroundPlaybackEnabled) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                SeltosTelemetryRepository.setMediaNowPlaying(currentTitle, playing)
            }
        }
        exoPlayer.addListener(listener)
        isPlaying = exoPlayer.isPlaying
        onDispose {
            exoPlayer.removeListener(listener)
            // Keep service player alive if background playback is enabled
            if (!backgroundPlaybackEnabled && SeltosMediaService.sharedPlayer == null) {
                exoPlayer.pause()
            }
        }
    }

    val videoFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "USB / Local Video"
            if (fileName.endsWith(".m3u", true) || fileName.endsWith(".m3u8", true)) {
                val text = runCatching {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                }.getOrNull()
                if (!text.isNullOrBlank()) {
                    onLoadM3uText(text)
                    return@let
                }
            }
            onAddCustomMedia(fileName, it.toString())
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Video Surface + Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Top Media Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldActive.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EN RUTA",
                                color = EmeraldActive,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentTitle,
                            color = TelemetryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            videoFileLauncher.launch(
                                arrayOf("video/*", "application/x-mpegURL", "audio/x-mpegurl", "*/*")
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("btn_pick_local_video")
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Local or USB Video", tint = ElectricCyan)
                    }
                    IconButton(
                        onClick = { showAddStreamDialog = true },
                        modifier = Modifier.size(48.dp).testTag("btn_add_iptv_url")
                    ) {
                        Icon(Icons.Rounded.AddLink, contentDescription = "Add Stream or M3U8 URL", tint = ElectricCyan)
                    }
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.size(48.dp).testTag("btn_video_pip")
                    ) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = TelemetryWhite)
                    }
                    IconButton(
                        onClick = { showPlaylistDrawer = !showPlaylistDrawer },
                        modifier = Modifier.size(48.dp).testTag("btn_toggle_playlist")
                    ) {
                        Icon(
                            Icons.Rounded.PlaylistPlay,
                            contentDescription = "Toggle Playlist",
                            tint = if (showPlaylistDrawer) ElectricCyan else SlateLabel
                        )
                    }
                }
            }

            // Video Player Viewport with Left-Brightness / Right-Volume Gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .onSizeChanged { containerWidthPx = it.width.coerceAtLeast(1) }
                    .pointerInput(brightness, volume) {
                        detectVerticalDragGestures(
                            onDragEnd = { gestureHudLabel = null },
                            onDragCancel = { gestureHudLabel = null }
                        ) { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / 450f
                            if (change.position.x < containerWidthPx * 0.5f) {
                                val nextB = (brightness + delta).coerceIn(0.1f, 1f)
                                onBrightnessChange(nextB)
                                gestureHudLabel = "BRIGHTNESS ${(nextB * 100).toInt()}%"
                            } else {
                                val nextV = (volume + delta).coerceIn(0f, 1f)
                                onVolumeChange(nextV)
                                gestureHudLabel = "VOLUME ${(nextV * 100).toInt()}%"
                            }
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Gesture HUD Indicator
                this@Column.AnimatedVisibility(
                    visible = gestureHudLabel != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xDD0A0B0E))
                            .border(1.dp, ElectricCyan, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (gestureHudLabel?.startsWith("BRIGHT") == true) {
                                Icons.Rounded.Brightness6
                            } else {
                                Icons.Rounded.VolumeUp
                            },
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = gestureHudLabel.orEmpty(),
                            color = TelemetryWhite,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Automotive Oversized Playback Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { exoPlayer.seekBack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = TelemetryWhite)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("btn_play_pause_video"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = ObsidianBg
                        )
                    }
                    IconButton(
                        onClick = { exoPlayer.seekForward() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = TelemetryWhite)
                    }
                }
            }
        }

        // Collapsible IPTV & Local Playlist Drawer
        if (showPlaylistDrawer && !compactMode) {
            Column(
                modifier = Modifier
                    .width(235.dp)
                    .fillMaxHeight()
                    .background(CarbonSurface)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "PLAYLIST",
                    color = ElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(playlist, key = { it.id }) { item ->
                        val selected = item.uri == currentUri
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) ElectricCyan.copy(alpha = 0.14f) else CarbonElevated)
                                .border(
                                    1.dp,
                                    if (selected) ElectricCyan else CarbonBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectMedia(item) }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = item.title,
                                color = TelemetryWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.group} • ${item.type.name}",
                                color = if (selected) ElectricCyan else SlateLabel,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddStreamDialog) {
        var streamTitle by remember { mutableStateOf("") }
        var streamUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddStreamDialog = false },
            containerColor = CarbonSurface,
            title = { Text("Add IPTV / Video Stream (.m3u8, .mp4, .mkv)", color = TelemetryWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        label = { Text("Channel / Video Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("Stream URL or Paste #EXTM3U Playlist") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (streamUrl.contains("#EXTM3U", ignoreCase = true) || streamUrl.contains("#EXTINF", ignoreCase = true)) {
                            onLoadM3uText(streamUrl)
                        } else {
                            onAddCustomMedia(streamTitle, streamUrl)
                        }
                        showAddStreamDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg)
                ) {
                    Text("Load Stream")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStreamDialog = false }) {
                    Text("Cancel", color = SlateLabel)
                }
            }
        )
    }
}
