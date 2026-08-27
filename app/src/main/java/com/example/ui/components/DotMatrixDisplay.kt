package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Dot Matrix display that renders digits and characters using an authentic retro-tech dot grid,
 * matching the signature Nothing(R) hardware and glyph style.
 */
object DotMatrix {
    // 5x7 bitmap definitions
    private val DIGITS = mapOf(
        '0' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true
        ),
        '1' to booleanArrayOf(
            false, false, true,  false, false,
            false, true,  true,  false, false,
            false, false, true,  false, false,
            false, false, true,  false, false,
            false, false, true,  false, false,
            false, false, true,  false, false,
            false, true,  true,  true,  false
        ),
        '2' to booleanArrayOf(
            true,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            true,  true,  true,  true,  true,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  true,  true,  true,  true
        ),
        '3' to booleanArrayOf(
            true,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            false,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            true,  true,  true,  true,  true
        ),
        '4' to booleanArrayOf(
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            false, false, false, false, true
        ),
        '5' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  true,  true,  true,  false,
            false, false, false, false, true,
            false, false, false, false, true,
            true,  true,  true,  true,  false
        ),
        '6' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  true,  true,  true,  true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true
        ),
        '7' to booleanArrayOf(
            true,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            false, false, false, true,  false,
            false, false, true,  false, false,
            false, true,  false, false, false,
            true,  false, false, false, false
        ),
        '8' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true
        ),
        '9' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, true,
            true,  false, false, false, true,
            true,  true,  true,  true,  true,
            false, false, false, false, true,
            false, false, false, false, true,
            true,  true,  true,  true,  true
        ),
        ':' to booleanArrayOf(
            false, false, false, false, false,
            false, false, true,  false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, true,  false, false,
            false, false, false, false, false
        ),
        '.' to booleanArrayOf(
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, true,  false, false
        ),
        '°' to booleanArrayOf(
            true,  true,  false, false, false,
            true,  true,  false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false
        ),
        'C' to booleanArrayOf(
            false, true,  true,  true,  true,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  false, false, false, false,
            false, true,  true,  true,  true
        ),
        'F' to booleanArrayOf(
            true,  true,  true,  true,  true,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  true,  true,  true,  false,
            true,  false, false, false, false,
            true,  false, false, false, false,
            true,  false, false, false, false
        ),
        '-' to booleanArrayOf(
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            true,  true,  true,  true,  true,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false
        ),
        '+' to booleanArrayOf(
            false, false, false, false, false,
            false, false, true,  false, false,
            false, false, true,  false, false,
            true,  true,  true,  true,  true,
            false, false, true,  false, false,
            false, false, true,  false, false,
            false, false, false, false, false
        ),
        ' ' to booleanArrayOf(
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false
        )
    )

    fun getGrid(char: Char): BooleanArray {
        return DIGITS[char.uppercaseChar()] ?: DIGITS[' ']!!
    }
}

@Composable
fun DotMatrixChar(
    char: Char,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 4.dp,
    dotSpacing: Dp = 2.dp
) {
    val grid = DotMatrix.getGrid(char)
    val cols = 5
    val rows = 7

    Canvas(
        modifier = modifier
            .width(dotSize * cols + dotSpacing * (cols - 1))
            .height(dotSize * rows + dotSpacing * (rows - 1))
    ) {
        val dotSizePx = dotSize.toPx()
        val spacingPx = dotSpacing.toPx()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isActive = grid[r * cols + c]
                val color = if (isActive) activeColor else inactiveColor
                
                // Pure black Amoled support, save energy: do not draw inactive dots if color is fully transparent
                if (color != Color.Transparent) {
                    val x = c * (dotSizePx + spacingPx) + dotSizePx / 2
                    val y = r * (dotSizePx + spacingPx) + dotSizePx / 2
                    drawCircle(
                        color = color,
                        radius = dotSizePx / 2,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun DotMatrixString(
    text: String,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color(0x0AFFFFFF), // Low-brightness dots for outline effect, or transparent
    charSpacing: Dp = 8.dp,
    dotSize: Dp = 4.dp,
    dotSpacing: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->
            DotMatrixChar(
                char = char,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                dotSize = dotSize,
                dotSpacing = dotSpacing,
                modifier = Modifier.padding(
                    end = if (index < text.length - 1) charSpacing else 0.dp
                )
            )
        }
    }
}
