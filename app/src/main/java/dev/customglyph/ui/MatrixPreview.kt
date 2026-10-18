package dev.customglyph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.customglyph.model.DeviceProfile
import kotlin.math.*

@Composable
fun MatrixPreview(
    pixels: IntArray,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    profile: DeviceProfile = DeviceProfile.PHONE_3,
    onGesture:
        (
            panX: Float, panY: Float, zoom: Float, rotation: Float, anchorX: Float, anchorY: Float,
        ) -> Unit =
        { _, _, _, _, _, _ ->
        },
) {
    val gesture by rememberUpdatedState(onGesture)
    val gestures =
        if (editable)
            Modifier.pointerInput(profile) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val grid = minOf(size.width, size.height) * .82f
                    val pitch = grid / profile.size
                    gesture(
                        pan.x / pitch,
                        pan.y / pitch,
                        zoom,
                        rotation,
                        (centroid.x - size.width / 2f) / pitch,
                        (centroid.y - size.height / 2f) / pitch,
                    )
                }
            }
        else Modifier
    Canvas(
        modifier.aspectRatio(1f).then(gestures).semantics {
            contentDescription =
                "Glyph Matrix preview, ${profile.size} by ${profile.size} grid, ${profile.ledCount} LEDs. ${pixels.count { it>0 }} lit pixels."
        }
    ) {
        val width = size.minDimension
        val c = center
        drawCircle(Panel, width * .5f, c)
        val pitch = width * .82f / profile.size
        val origin = c - Offset(pitch * profile.size / 2f, pitch * profile.size / 2f)
        for (y in 0 until profile.size) for (x in 0 until profile.size) {
            if (!profile.contains(x, y)) continue
            val v = pixels.getOrElse(y * profile.size + x) { 0 } / 255f
            val point = origin + Offset((x + .5f) * pitch, (y + .5f) * pitch)
            val led = pitch * profile.aperture
            val base = .17f
            val light = base + (1 - base) * v
            drawRoundRect(
                Color(light, light, light),
                point - Offset(led / 2, led / 2),
                Size(led, led),
                CornerRadius(led * .12f),
            )
        }
        // Four alignment indices sit outside the hardware aperture.
        if (editable)
            for (angle in listOf(0.0, PI / 2, PI, PI * 1.5)) {
                val v = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(
                    Color(0xff616166),
                    c + v * (width * .48f),
                    c + v * (width * .49f),
                    1.dp.toPx(),
                )
            }
    }
}
