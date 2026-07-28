package com.yassernull.shappky.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yassernull.shappky.core.preferences.KEY_THEME
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME

private val ShappkyTypography = Typography(
  titleLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.2).sp,
  ),
  titleMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.8.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.2.sp,
  ),
)

@Suppress("DEPRECATION")
@Composable
fun AppTheme(withBackground: Boolean = true, content: @Composable () -> Unit) {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  val appTheme = sharedPreferences.getString(KEY_THEME, "dark") ?: "dark"
  val dynamicColors = sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, false)

  val colorScheme = when {
    dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      when (appTheme) {
        "white" -> dynamicLightColorScheme(context)
        "black" -> dynamicDarkColorScheme(context).copy(
          surface = ObsidianBlack,
          background = ObsidianBlack,
          surfaceContainer = ObsidianBlack,
          surfaceContainerHigh = ObsidianBlack,
          surfaceContainerHighest = ObsidianBlack,
          surfaceContainerLow = ObsidianBlack,
          surfaceContainerLowest = ObsidianBlack,
        )
        else -> dynamicDarkColorScheme(context)
      }
    }
    else -> {
      when (appTheme) {
        "white" -> lightColorScheme(
          primary = MaroonDeep,
          onPrimary = White,
          primaryContainer = MaroonContainerLight,
          onPrimaryContainer = Color(0xFF3A0E16),
          secondary = MaroonDeep,
          onSecondary = White,
          secondaryContainer = Color(0xFFF3E0E3),
          onSecondaryContainer = Color(0xFF3A0E16),
          tertiary = SelectionAccentDeep,
          onTertiary = White,
          surface = SoftPaper,
          background = SoftPaper,
          onSurface = SoftInk,
          onSurfaceVariant = MutedRose,
          surfaceVariant = Color(0xFFEDE4E6),
          outline = Color(0xFFD0C2C5),
          outlineVariant = Color(0xFFE6DBDD),
        )
        "black" -> darkColorScheme(
          primary = MaroonPrimary,
          onPrimary = White,
          primaryContainer = MaroonContainerDark,
          onPrimaryContainer = MaroonSoft,
          secondary = MaroonPrimary,
          onSecondary = White,
          secondaryContainer = Color(0xFF241014),
          onSecondaryContainer = MaroonSoft,
          tertiary = SelectionAccent,
          onTertiary = Color(0xFF00382E),
          surface = ObsidianBlack,
          background = ObsidianBlack,
          surfaceVariant = Color(0xFF161012),
          onSurface = DarkOnSurface,
          onSurfaceVariant = MutedRoseDark,
          outline = Color(0xFF4A3C40),
          outlineVariant = Color(0xFF2A2225),
        )
        else -> darkColorScheme(
          primary = MaroonPrimary,
          onPrimary = White,
          primaryContainer = MaroonContainerDark,
          onPrimaryContainer = MaroonSoft,
          secondary = MaroonPrimary,
          onSecondary = White,
          secondaryContainer = ObsidianElevated,
          onSecondaryContainer = MaroonSoft,
          tertiary = SelectionAccent,
          onTertiary = Color(0xFF00382E),
          surface = Obsidian,
          background = Obsidian,
          surfaceVariant = ObsidianElevated,
          onSurface = DarkOnSurface,
          onSurfaceVariant = MutedRoseDark,
          outline = Color(0xFF4A3C40),
          outlineVariant = Color(0xFF2A2225),
        )
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = ShappkyTypography,
  ) {
    if (withBackground) {
      Surface(color = MaterialTheme.colorScheme.surface, content = content)
    } else {
      content()
    }
  }
}
