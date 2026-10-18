@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.customglyph.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.customglyph.model.DeviceProfile
import kotlin.math.*

@Composable
internal fun EditorWorkspace(
    editor: EditorViewModel.Editor,
    vm: EditorViewModel,
    onChoose: () -> Unit,
    onPreview: () -> Unit,
    onSettings: () -> Unit,
    onExport: () -> Unit,
    exportMenu: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val wide = maxWidth > 650.dp
        val height = maxHeight
        if (!wide && LocalDensity.current.fontScale > 1.3f) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactPreview(
                    editor,
                    vm,
                    onChoose,
                    onPreview,
                    onSettings,
                    onExport,
                    exportMenu,
                    Modifier.fillMaxWidth().height((height * .36f).coerceIn(150.dp, 240.dp)),
                )
                AdjustmentsPanel(editor, vm, Modifier.weight(1f).fillMaxWidth())
            }
        } else if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                CompactPreview(
                    editor,
                    vm,
                    onChoose,
                    onPreview,
                    onSettings,
                    onExport,
                    exportMenu,
                    Modifier.weight(1f).fillMaxHeight(),
                )
                AdjustmentsPanel(editor, vm, Modifier.weight(1f).fillMaxHeight())
            }
        } else
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    DeviceSelector(editor.draft.profile, vm, onSettings)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        EditableMatrix(
                            editor,
                            vm,
                            (height * (if (editor.frames > 1) .30f else .38f)).coerceIn(
                                156.dp,
                                286.dp,
                            ),
                            Modifier.weight(1f),
                        )
                        if (editor.frames > 1)
                            Column(
                                Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircleAction(
                                        "Preview",
                                        Icons.Outlined.Visibility,
                                        onPreview,
                                        light = true,
                                    )
                                    CircleAction(
                                        "Center",
                                        Icons.Outlined.CenterFocusStrong,
                                        { vm.update { it.copy(x = 0f, y = 0f) } },
                                        enabled = !editor.busy,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircleAction(
                                        "Reset",
                                        Icons.Outlined.RestartAlt,
                                        vm::reset,
                                        enabled = !editor.busy,
                                    )
                                    Box {
                                        CircleAction(
                                            "Export",
                                            Icons.Outlined.IosShare,
                                            onExport,
                                            enabled = !editor.busy,
                                        )
                                        exportMenu()
                                    }
                                }
                            }
                        else
                            Column(
                                Modifier.padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircleAction(
                                    "Preview",
                                    Icons.Outlined.Visibility,
                                    onPreview,
                                    light = true,
                                )
                                CircleAction(
                                    "Center",
                                    Icons.Outlined.CenterFocusStrong,
                                    { vm.update { it.copy(x = 0f, y = 0f) } },
                                    enabled = !editor.busy,
                                )
                                CircleAction(
                                    "Reset",
                                    Icons.Outlined.RestartAlt,
                                    vm::reset,
                                    enabled = !editor.busy,
                                )
                            }
                    }
                    AnimationControls(editor, vm)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Surface(
                            onClick = onChoose,
                            enabled = !editor.busy,
                            color = Panel,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(22.dp))
                                Text(
                                    "Choose Image",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                        Text(
                            editor.draft.name,
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, start = 18.dp, end = 18.dp),
                        )
                    }
                    if (editor.frames <= 1)
                        Box {
                            CircleAction(
                                "Export",
                                Icons.Outlined.IosShare,
                                onExport,
                                enabled = !editor.busy,
                            )
                            exportMenu()
                        }
                }
                editor.error?.let { Text(it, color = Red, fontSize = 11.sp, maxLines = 3) }
                AdjustmentsPanel(editor, vm, Modifier.weight(1f).fillMaxWidth())
            }
    }
}

@Composable
private fun EditableMatrix(
    editor: EditorViewModel.Editor,
    vm: EditorViewModel,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        MatrixPreview(
            editor.pixels,
            Modifier.size(diameter),
            editable = !editor.busy,
            profile = editor.draft.profile,
        ) { x, y, zoom, rotation, anchorX, anchorY ->
            vm.update { a ->
                val scale = (a.scale * zoom).coerceIn(.2f, 4f)
                val ratio = scale / a.scale
                val radians = rotation * PI / 180
                val dx = a.x - anchorX
                val dy = a.y - anchorY
                a.copy(
                    scale = scale,
                    rotation = a.rotation + rotation,
                    x = anchorX + (dx * cos(radians) - dy * sin(radians)).toFloat() * ratio + x,
                    y = anchorY + (dx * sin(radians) + dy * cos(radians)).toFloat() * ratio + y,
                )
            }
        }
        if (editor.busy) CircularProgressIndicator(Modifier.size(30.dp), color = Red)
    }
}

@Composable
private fun AnimationControls(editor: EditorViewModel.Editor, vm: EditorViewModel) {
    if (editor.frames > 1)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.setPlaying(!editor.playing) }) {
                Icon(
                    if (editor.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    if (editor.playing) "Pause animation" else "Play animation",
                )
            }
            val colors =
                SliderDefaults.colors(
                    thumbColor = Paper,
                    activeTrackColor = Paper,
                    inactiveTrackColor = Line,
                )
            Slider(
                editor.frameIndex.toFloat(),
                { vm.seek(it.roundToInt()) },
                valueRange = 0f..(editor.frames - 1).toFloat(),
                colors = colors,
                thumb = { Box(Modifier.size(14.dp).background(Paper, CircleShape)) },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = colors,
                        modifier = Modifier.height(4.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 3.dp,
                    )
                },
                modifier =
                    Modifier.weight(1f).height(40.dp).semantics {
                        contentDescription = "Animation frame"
                    },
            )
            Text(
                "${editor.frameIndex + 1}/${editor.frames}",
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 10.dp).widthIn(min = 32.dp),
            )
        }
}

@Composable
private fun DeviceSelector(
    profile: DeviceProfile,
    vm: EditorViewModel,
    onSettings: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
) {
    var devices by remember { mutableStateOf(false) }
    val status by vm.glyph.status.collectAsStateWithLifecycle()
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TextButton(
                onClick = { devices = true },
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(profile.title, fontSize = 11.sp)
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
            }
            DropdownMenu(devices, { devices = false }) {
                DeviceProfile.entries.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.title) },
                        onClick = {
                            vm.setProfile(device)
                            devices = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (actions != null) actions()
        else
            TextButton(onClick = onSettings, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Box(Modifier.size(5.dp).background(if (status.error) Red else Muted, CircleShape))
                Text(
                    if (status.error) status.title
                    else "${profile.size} × ${profile.size} / ${profile.ledCount}",
                    fontSize = 9.sp,
                    color = if (status.error) Red else Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 7.dp).widthIn(max = 164.dp),
                )
            }
    }
}

@Composable
private fun CompactPreview(
    editor: EditorViewModel.Editor,
    vm: EditorViewModel,
    onChoose: () -> Unit,
    onPreview: () -> Unit,
    onSettings: () -> Unit,
    onExport: () -> Unit,
    exportMenu: @Composable () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        DeviceSelector(editor.draft.profile, vm, onSettings) {
            var actions by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { actions = true }) {
                    Text("Actions", fontSize = 11.sp)
                    Icon(Icons.Outlined.MoreHoriz, null, Modifier.padding(start = 6.dp).size(18.dp))
                }
                DropdownMenu(actions, { actions = false }) {
                    listOf(
                            "Choose Image" to onChoose,
                            "Preview" to onPreview,
                            "Center" to { vm.update { it.copy(x = 0f, y = 0f) } },
                            "Reset" to vm::reset,
                            "Export / Share" to onExport,
                        )
                        .forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                enabled = !editor.busy,
                                onClick = {
                                    actions = false
                                    action()
                                },
                            )
                        }
                }
                exportMenu()
            }
        }
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            EditableMatrix(editor, vm, minOf(maxHeight, maxWidth, 280.dp))
        }
        AnimationControls(editor, vm)
        editor.error?.let { Text(it, color = Red, fontSize = 10.sp, maxLines = 2) }
    }
}
