package dev.laraib.khidki.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val TealPrimaryLight = Color(0xFF006A60)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFF70F7E5)
val TealOnContainerLight = Color(0xFF00201C)

val TealPrimaryDark = Color(0xFF53DBC9)
val TealOnPrimaryDark = Color(0xFF003731)
val TealContainerDark = Color(0xFF005048)
val TealOnContainerDark = Color(0xFF70F7E5)

val SlateSecondaryLight = Color(0xFF4A635F)
val SlateSecondaryDark = Color(0xFFB1CCC7)

val IndigoTertiaryLight = Color(0xFF456179)
val IndigoTertiaryDark = Color(0xFFAECBE6)

val SurfaceLight = Color(0xFFF4FBF7)
val SurfaceDark = Color(0xFF191C1B)

val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBackgroundDark = Color(0xFF222625)

val StatusGreen = Color(0xFF1B873F)
val StatusGreenBg = Color(0xFFE8F5E9)
val StatusAmber = Color(0xFFD97706)
val StatusAmberBg = Color(0xFFFFFBEB)
val StatusRed = Color(0xFFDC2626)
val StatusRedBg = Color(0xFFFEF2F2)
val StatusBlue = Color(0xFF2563EB)
val StatusBlueBg = Color(0xFFEFF6FF)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContainerLight,
    secondary = SlateSecondaryLight,
    tertiary = IndigoTertiaryLight,
    surface = SurfaceLight,
    background = SurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealOnContainerDark,
    secondary = SlateSecondaryDark,
    tertiary = IndigoTertiaryDark,
    surface = SurfaceDark,
    background = SurfaceDark,
)

val KhidkiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun KhidkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = KhidkiShapes,
        content = content,
    )
}
