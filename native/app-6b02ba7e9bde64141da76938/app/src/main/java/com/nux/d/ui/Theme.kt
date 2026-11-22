package com.nux.d.ui

import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.nux.d.data.AppSpec

fun parseHex(value: String, fallback: Color): Color {
    return try {
        if (value.startsWith("#") && (value.length == 7 || value.length == 9)) {
            Color(AndroidColor.parseColor(value))
        } else {
            fallback
        }
    } catch (error: Exception) {
        fallback
    }
}

class NuxColors(spec: AppSpec) {
    val isDark: Boolean = spec.dark
    val primary: Color = parseHex(spec.primary, Color(0xFF0A84FF))
    val background: Color = parseHex(
        spec.theme.optString("background", "#FFFFFF"),
        if (spec.dark) Color(0xFF121212) else Color(0xFFFFFFFF)
    )
    val surface: Color = parseHex(
        spec.theme.optString("surface", "#F4F4F4"),
        if (spec.dark) Color(0xFF1F1F1F) else Color(0xFFF4F4F4)
    )
    val text: Color = parseHex(
        spec.theme.optString("text", "#0D0D0D"),
        if (spec.dark) Color(0xFFFFFFFF) else Color(0xFF0D0D0D)
    )
    val muted: Color = if (spec.dark) Color(0xFFA0A0A0) else Color(0xFF666666)
}

@Composable
fun NuxTheme(spec: AppSpec, content: @Composable () -> Unit) {
    val colors = remember(spec) { NuxColors(spec) }
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = Color(0xFFFFFFFF),
            onBackground = colors.text,
            onSurface = colors.text
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = Color(0xFFFFFFFF),
            onBackground = colors.text,
            onSurface = colors.text
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
