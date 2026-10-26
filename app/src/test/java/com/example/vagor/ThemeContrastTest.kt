package com.example.vagor

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

// Every text/background pair the app's widgets use must meet WCAG 2.x AA contrast in both the
// light and dark schemes: 4.5:1 for text, 3:1 for non-text UI such as field underlines.
class ThemeContrastTest {
    private val colors: Map<String, String> by lazy {
        // Unit tests run with the module directory as the working directory.
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/colors.xml"))
        val nodes = document.getElementsByTagName("color")
        (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.trim()
        }
    }

    @Test
    fun textMeetsWcagAa() {
        val pairs = listOf(
            "onPrimary" to "primary",
            "onPrimaryContainer" to "primaryContainer",
            "onSecondaryContainer" to "secondaryContainer",
            "onSurface" to "surface",
            "onBackground" to "background",
            "onSurfaceVariant" to "surface",
            "primary" to "surface",
            "onSurface" to "surfaceContainerHigh",
            "primary" to "surfaceContainerHigh",
            "onSurface" to "surfaceContainerHighest",
            "onError" to "error",
            "error" to "surface",
            // Selected text on the selection highlight.
            "onSurface" to "primaryContainer",
            // Dashboard cards (filled cards).
            "onSurfaceVariant" to "surfaceContainerHighest",
            // Bottom navigation bar labels, active and inactive.
            "onSurface" to "surfaceContainer",
            "onSurfaceVariant" to "surfaceContainer",
            // Drop-down calendar: weekday labels and days; the selected day is onPrimary on primary.
            "onSurfaceVariant" to "surfaceContainerHigh",
        )
        assertPairs(pairs, 4.5)
    }

    @Test
    fun nonTextUiMeetsWcagAa() {
        assertPairs(
            listOf(
                "outline" to "surface",
                "onSurfaceVariant" to "surface",
                "primary" to "surfaceContainerHighest",
                // The blue Add buttons against the screen.
                "primary" to "background",
                // The selected tab's indicator against the bottom navigation bar.
                "primary" to "surfaceContainer",
                // Material's focus rings: the outer ring against what's around a control, and the
                // inner ring against a blue control.
                "secondary" to "background",
                "secondary" to "surfaceContainer",
                "secondary" to "surfaceContainerHighest",
                "onSecondary" to "primary",
                // Drop-down calendar: today's ring and the selected day's circle against the
                // calendar; its focus ring is onSurface (checked as text above).
                "primary" to "surfaceContainerHigh",
            ),
            3.0
        )
    }

    private fun assertPairs(pairs: List<Pair<String, String>>, minimum: Double) {
        for (scheme in listOf("light", "dark")) {
            for ((foreground, background) in pairs) {
                val ratio = contrast(color(scheme, foreground), color(scheme, background))
                assertTrue(
                    "$scheme $foreground on $background is ${"%.2f".format(ratio)}:1, below $minimum:1",
                    ratio >= minimum
                )
            }
        }
    }

    private fun color(scheme: String, role: String): String =
        colors["vagor_${scheme}_$role"] ?: throw AssertionError("Missing colour vagor_${scheme}_$role")

    private fun contrast(first: String, second: String): Double {
        val (lighter, darker) = listOf(luminance(first), luminance(second)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(hex: String): Double {
        val rgb = hex.removePrefix("#").takeLast(6)
        val (r, g, b) = (0..2).map { i ->
            val channel = rgb.substring(i * 2, i * 2 + 2).toInt(16) / 255.0
            if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
