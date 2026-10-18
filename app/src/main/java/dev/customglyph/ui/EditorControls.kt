@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.customglyph.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun AdjustmentsPanel(
    editor: EditorViewModel.Editor,
    vm: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    val a = editor.draft.adjustments
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LightWidget(modifier) {
        Column(Modifier.fillMaxSize()) {
            SegmentControl(
                listOf("Light", "Move", "Place", "Image"),
                tab,
                { tab = it },
                Modifier.fillMaxWidth().padding(8.dp),
                light = true,
            )
            key(tab) {
                Column(
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 10.dp)
                ) {
                    when (tab) {
                        0 ->
                            PairedAdjustments(
                                first = { compact ->
                                    AdjustmentSlider(
                                        "Brightness",
                                        a.brightness,
                                        0f..1f,
                                        "${(a.brightness * 100).roundToInt()}",
                                        "%",
                                        !editor.busy,
                                        stacked = compact,
                                    ) { v ->
                                        vm.update { it.copy(brightness = v) }
                                    }
                                },
                                second = { compact ->
                                    AdjustmentSlider(
                                        "Contrast",
                                        a.contrast,
                                        .25f..3f,
                                        String.format(Locale.ROOT, "%.2f", a.contrast),
                                        "×",
                                        !editor.busy && !a.silhouette,
                                        stacked = compact,
                                    ) { v ->
                                        vm.update { it.copy(contrast = v) }
                                    }
                                },
                            )
                        1 ->
                            PairedAdjustments(
                                first = { compact ->
                                    AdjustmentSlider(
                                        "Scale",
                                        a.scale,
                                        .2f..4f,
                                        "${(a.scale * 100).roundToInt()}",
                                        "%",
                                        !editor.busy,
                                        stacked = compact,
                                    ) { v ->
                                        vm.update { it.copy(scale = v) }
                                    }
                                },
                                second = { compact ->
                                    AdjustmentSlider(
                                        "Rotation",
                                        a.rotation,
                                        -180f..180f,
                                        "${a.rotation.roundToInt()}",
                                        "°",
                                        !editor.busy,
                                        stacked = compact,
                                    ) { v ->
                                        vm.update { it.copy(rotation = v) }
                                    }
                                },
                            )
                        2 -> {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PositionStepper("X", a.x, Modifier.weight(1f)) { v ->
                                    vm.update { it.copy(x = v) }
                                }
                                PositionStepper("Y", a.y, Modifier.weight(1f)) { v ->
                                    vm.update { it.copy(y = v) }
                                }
                            }
                            Text(
                                "0.25 LED per step",
                                fontSize = 10.sp,
                                color = Color(0xff55545b),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        else -> {
                            SegmentControl(
                                listOf("Tonal", "Silhouette"),
                                if (a.silhouette) 1 else 0,
                                { index -> vm.update { it.copy(silhouette = index == 1) } },
                                Modifier.fillMaxWidth(),
                                light = true,
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Invert tones", fontSize = 11.sp)
                                    Switch(
                                        a.invert,
                                        { v -> vm.update { it.copy(invert = v) } },
                                        enabled = !a.silhouette,
                                        modifier =
                                            Modifier.semantics {
                                                contentDescription = "Invert tones"
                                            },
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Dither", fontSize = 11.sp)
                                    Switch(
                                        a.dither,
                                        { v -> vm.update { it.copy(dither = v) } },
                                        modifier =
                                            Modifier.semantics { contentDescription = "Dither" },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairedAdjustments(
    first: @Composable (Boolean) -> Unit,
    second: @Composable (Boolean) -> Unit,
) {
    if (LocalDensity.current.fontScale <= 1.3f) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f)) { first(true) }
            Column(Modifier.weight(1f)) { second(true) }
        }
    } else {
        first(false)
        second(false)
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    unit: String,
    enabled: Boolean,
    stacked: Boolean = false,
    onChange: (Float) -> Unit,
) {
    if (stacked) {
        Text(label, fontSize = 11.sp, color = if (enabled) Ink else Color(0xff77757e))
        Row(Modifier.padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
            DotText(display, size = 40.sp, color = if (enabled) Ink else Color(0xff77757e))
            Text(unit, fontSize = 12.sp, modifier = Modifier.padding(start = 3.dp, bottom = 5.dp))
        }
    } else
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (enabled) Ink else Color(0xff77757e),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            DotText(display, size = 32.sp, color = if (enabled) Ink else Color(0xff77757e))
            Text(unit, fontSize = 12.sp, modifier = Modifier.padding(start = 3.dp, top = 8.dp))
        }
    val colors =
        SliderDefaults.colors(
            thumbColor = Ink,
            activeTrackColor = Ink,
            inactiveTrackColor = Color(0xffcfccd3),
        )
    Slider(
        value = value.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        enabled = enabled,
        colors = colors,
        thumb = {
            Box(Modifier.size(20.dp).background(if (enabled) Ink else Color.Gray, CircleShape))
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(6.dp),
                enabled = enabled,
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 3.dp,
            )
        },
        modifier = Modifier.fillMaxWidth().height(42.dp).semantics { contentDescription = label },
    )
}

@Composable
private fun PositionStepper(
    axis: String,
    value: Float,
    modifier: Modifier,
    onChange: (Float) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(axis, fontSize = 11.sp)
        DotText(
            String.format(Locale.ROOT, "%.2f", value),
            size = 30.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row {
            IconButton(onClick = { onChange(value - .25f) }) {
                Icon(Icons.Outlined.Remove, "Move $axis minus 0.25 LED", Modifier.size(18.dp))
            }
            IconButton(onClick = { onChange(value + .25f) }) {
                Icon(Icons.Outlined.Add, "Move $axis plus 0.25 LED", Modifier.size(18.dp))
            }
        }
    }
}
