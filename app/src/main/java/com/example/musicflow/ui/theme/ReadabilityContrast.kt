package com.example.musicflow.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x relative luminance and contrast ratio for opaque design tokens. */
internal object ReadabilityContrast {
    fun ratio(foreground: Color, background: Color): Double {
        require(foreground.alpha == 1f && background.alpha == 1f) {
            "Contrast must be measured on opaque composited colors"
        }
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red.toDouble()) +
            0.7152 * channel(color.green.toDouble()) +
            0.0722 * channel(color.blue.toDouble())

    private fun channel(c: Double) =
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}
