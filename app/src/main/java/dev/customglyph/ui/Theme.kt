package dev.customglyph.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.customglyph.R

internal val Ink = Color(0xff000000)
internal val Panel = Color(0xff1c1c1e)
internal val Line = Color(0xff38383b)
internal val Muted = Color(0xffa2a2a8)
internal val Paper = Color(0xffefedf1)
internal val Red = Color(0xffec353b)
internal val ActionRed = Color(0xffc51f27)
internal val Mono = FontFamily(Font(R.font.jetbrains_mono_regular))

@OptIn(ExperimentalTextApi::class)
internal val Dot =
    FontFamily(
        Font(
            R.font.doto,
            weight = FontWeight.Black,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(900),
                    FontVariation.Setting("ROND", 100f),
                ),
        )
    )

@Composable
internal fun GlyphTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val type =
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = Mono),
            displayMedium = base.displayMedium.copy(fontFamily = Mono),
            displaySmall = base.displaySmall.copy(fontFamily = Mono),
            headlineLarge = base.headlineLarge.copy(fontFamily = Mono),
            headlineMedium = base.headlineMedium.copy(fontFamily = Mono),
            headlineSmall = base.headlineSmall.copy(fontFamily = Mono),
            titleLarge = base.titleLarge.copy(fontFamily = Mono),
            titleMedium = base.titleMedium.copy(fontFamily = Mono),
            titleSmall = base.titleSmall.copy(fontFamily = Mono),
            bodyLarge =
                base.bodyLarge.copy(
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                ),
            bodyMedium = base.bodyMedium.copy(fontFamily = Mono),
            bodySmall = base.bodySmall.copy(fontFamily = Mono),
            labelLarge = base.labelLarge.copy(fontFamily = Mono),
            labelMedium = base.labelMedium.copy(fontFamily = Mono),
            labelSmall = base.labelSmall.copy(fontFamily = Mono),
        )
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Paper,
                onPrimary = Ink,
                secondary = Red,
                onSecondary = Color.White,
                background = Ink,
                onBackground = Paper,
                surface = Panel,
                onSurface = Paper,
                surfaceVariant = Line,
                onSurfaceVariant = Muted,
                outline = Line,
                error = Red,
                primaryContainer = Line,
                onPrimaryContainer = Paper,
                secondaryContainer = Line,
                onSecondaryContainer = Paper,
                tertiary = Red,
                onTertiary = Color.White,
                tertiaryContainer = Line,
                onTertiaryContainer = Paper,
                surfaceContainer = Panel,
                surfaceContainerLow = Color(0xff121212),
                surfaceContainerLowest = Ink,
                surfaceContainerHigh = Color(0xff242424),
                surfaceContainerHighest = Line,
                surfaceBright = Line,
                surfaceDim = Ink,
                surfaceTint = Paper,
            ),
        typography = type,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(24.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(32.dp),
            ),
        content = content,
    )
}
