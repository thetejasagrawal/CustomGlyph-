package dev.customglyph.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DotText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 28.sp,
    color: Color = LocalContentColor.current,
) {
    Text(
        text,
        modifier,
        fontFamily = Dot,
        fontWeight = FontWeight.Black,
        fontSize = size,
        lineHeight = size * 1.15f,
        letterSpacing = (-.6).sp,
        color = color,
    )
}

@Composable
internal fun CircleAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    light: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "Action press")
    Column(
        modifier
            .width(64.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = if (light) Paper else Panel,
            contentColor = if (light) Ink else Paper,
            modifier = Modifier.size(52.dp).scale(scale),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp)) }
        }
        Text(
            label,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = if (enabled) Paper else Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun SegmentControl(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    light: Boolean = false,
) {
    Surface(
        color = if (light) Color(0xffdcdadf) else Panel,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                val color by
                    animateColorAsState(
                        if (active) {
                            if (light) Ink else Paper
                        } else Color.Transparent,
                        label = "Tab fill",
                    )
                Surface(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    color = color,
                    contentColor = if (active == light) Paper else Ink,
                    modifier =
                        Modifier.weight(1f).heightIn(min = 44.dp).semantics {
                            this.selected = active
                            role = Role.Tab
                        },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color =
                                if (active) {
                                    if (light) Paper else Ink
                                } else {
                                    if (light) Ink else Muted
                                },
                        )
                    }
                }
            }
        }
    }
}

/** A light widget uses a complete palette so native controls keep readable contrast. */
@Composable
internal fun LightWidget(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Ink,
                onPrimary = Paper,
                surface = Paper,
                onSurface = Ink,
                background = Paper,
                onBackground = Ink,
                onSurfaceVariant = Color(0xff55545b),
                surfaceVariant = Color(0xffd9d7dc),
                outline = Color(0xff77757e),
                secondary = ActionRed,
                onSecondary = Color.White,
                primaryContainer = Ink,
                onPrimaryContainer = Paper,
                surfaceTint = Color.Transparent,
            )
    ) {
        Surface(
            modifier,
            color = Paper,
            contentColor = Ink,
            shape = RoundedCornerShape(28.dp),
            content = content,
        )
    }
}
