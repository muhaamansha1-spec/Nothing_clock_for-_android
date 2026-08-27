package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherCondition

/**
 * Authentic Nothing-style dot matrix weather iconography.
 * Uses an 8x8 or 9x9 active dot grid to render iconic, high-contrast weather glyphs.
 */
object WeatherDotMatrix {

    const val GRID_SIZE = 9

    // 9x9 boolean grid patterns (81 points each)
    private val CLEAR_DAY_GRID = booleanArrayOf(
        false, false, false, false, true,  false, false, false, false,
        false, true,  false, false, false, false, false, true,  false,
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        true,  false, true,  true,  false, true,  true,  false, true,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, false, false, true,  true,  true,  false, false, false,
        false, true,  false, false, false, false, false, true,  false,
        false, false, false, false, true,  false, false, false, false
    )

    private val CLEAR_NIGHT_GRID = booleanArrayOf(
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, true,  true,  false, false, false, true,  true,  false,
        false, true,  true,  false, false, false, false, false, false,
        false, true,  true,  false, false, false, false, false, false,
        false, true,  true,  false, false, false, false, false, false,
        false, true,  true,  false, false, false, true,  true,  false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, false, false, true,  true,  true,  false, false, false
    )

    private val CLOUDY_GRID = booleanArrayOf(
        false, false, false, false, false, false, false, false, false,
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        true,  true,  true,  true,  true,  true,  true,  true,  true,
        true,  true,  true,  true,  true,  true,  true,  true,  true,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false
    )

    private val PARTLY_CLOUDY_DAY_GRID = booleanArrayOf(
        false, false, true,  false, false, false, false, false, false,
        false, true,  true,  true,  false, true,  true,  false, false,
        false, false, true,  true,  true,  true,  true,  true,  false,
        false, true,  false, true,  true,  true,  true,  true,  true,
        false, false, false, true,  true,  true,  true,  true,  true,
        false, false, true,  true,  true,  true,  true,  true,  true,
        false, false, false, true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false
    )

    private val PARTLY_CLOUDY_NIGHT_GRID = booleanArrayOf(
        false, false, true,  true,  true,  false, false, false, false,
        false, true,  true,  false, true,  true,  false, false, false,
        false, true,  false, false, false, true,  true,  false, false,
        false, false, false, true,  true,  true,  true,  true,  false,
        false, false, true,  true,  true,  true,  true,  true,  true,
        false, true,  true,  true,  true,  true,  true,  true,  true,
        false, false, true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false
    )

    private val RAINY_GRID = booleanArrayOf(
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        true,  true,  true,  true,  true,  true,  true,  true,  true,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        false, true,  false, false, true,  false, false, true,  false,
        false, false, true,  false, false, true,  false, false, true,
        false, false, false, false, false, false, false, false, false
    )

    private val THUNDERSTORM_GRID = booleanArrayOf(
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        true,  true,  true,  true,  true,  true,  true,  true,  true,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, true,  true,  false, false, false,
        false, false, false, true,  true,  false, false, false, false,
        false, false, false, false, true,  true,  false, false, false,
        false, false, false, false, true,  false, false, false, false
    )

    private val SNOWY_GRID = booleanArrayOf(
        false, false, false, false, true,  false, false, false, false,
        false, true,  false, false, true,  false, false, true,  false,
        false, false, true,  false, true,  false, true,  false, false,
        false, false, false, true,  true,  true,  false, false, false,
        true,  true,  true,  true,  false, true,  true,  true,  true,
        false, false, false, true,  true,  true,  false, false, false,
        false, false, true,  false, true,  false, true,  false, false,
        false, true,  false, false, true,  false, false, true,  false,
        false, false, false, false, true,  false, false, false, false
    )

    private val FOGGY_GRID = booleanArrayOf(
        false, false, false, false, false, false, false, false, false,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        true,  true,  true,  true,  true,  true,  true,  true,  true,
        false, false, false, false, false, false, false, false, false,
        false, true,  true,  true,  true,  true,  true,  true,  false,
        false, false, false, false, false, false, false, false, false,
        false, false, true,  true,  true,  true,  true,  false, false,
        false, false, false, false, false, false, false, false, false
    )

    private val WINDY_GRID = booleanArrayOf(
        false, false, false, false, false, true,  true,  true,  false,
        false, true,  true,  true,  true,  true,  false, false, true,
        false, false, false, false, false, false, false, false, true,
        false, false, false, false, false, false, false, false, false,
        true,  true,  true,  true,  true,  true,  false, false, false,
        false, false, false, false, false, false, true,  true,  false,
        false, false, false, false, false, false, false, false, true,
        false, true,  true,  true,  true,  false, false, false, false,
        false, false, false, false, false, false, false, false, false
    )

    fun getGrid(condition: WeatherCondition): BooleanArray {
        return when (condition) {
            WeatherCondition.CLEAR_DAY -> CLEAR_DAY_GRID
            WeatherCondition.CLEAR_NIGHT -> CLEAR_NIGHT_GRID
            WeatherCondition.PARTLY_CLOUDY_DAY -> PARTLY_CLOUDY_DAY_GRID
            WeatherCondition.PARTLY_CLOUDY_NIGHT -> PARTLY_CLOUDY_NIGHT_GRID
            WeatherCondition.CLOUDY -> CLOUDY_GRID
            WeatherCondition.FOGGY -> FOGGY_GRID
            WeatherCondition.RAINY -> RAINY_GRID
            WeatherCondition.THUNDERSTORM -> THUNDERSTORM_GRID
            WeatherCondition.SNOWY -> SNOWY_GRID
            WeatherCondition.WINDY -> WINDY_GRID
        }
    }

    /**
     * Generates a dot matrix Bitmap for RemoteViews widgets
     */
    fun createWeatherBitmap(
        condition: WeatherCondition,
        activeColor: Int = android.graphics.Color.WHITE,
        inactiveColor: Int = android.graphics.Color.argb(20, 255, 255, 255),
        dotSizePx: Float = 6f,
        dotSpacingPx: Float = 2.5f
    ): Bitmap {
        val grid = getGrid(condition)
        val size = (GRID_SIZE * dotSizePx + (GRID_SIZE - 1) * dotSpacingPx).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(android.graphics.Color.TRANSPARENT)

        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val isActive = grid[r * GRID_SIZE + c]
                paint.color = if (isActive) activeColor else inactiveColor

                if (paint.color != android.graphics.Color.TRANSPARENT) {
                    val cx = c * (dotSizePx + dotSpacingPx) + dotSizePx / 2f
                    val cy = r * (dotSizePx + dotSpacingPx) + dotSizePx / 2f
                    canvas.drawCircle(cx, cy, dotSizePx / 2f, paint)
                }
            }
        }
        return bitmap
    }
}

/**
 * Composable Dot Matrix Weather Icon for UI & Dock Mode screens.
 */
@Composable
fun WeatherDotMatrixIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color(0x14FFFFFF),
    dotSize: Dp = 3.dp,
    dotSpacing: Dp = 1.5.dp
) {
    val grid = WeatherDotMatrix.getGrid(condition)
    val cols = WeatherDotMatrix.GRID_SIZE
    val rows = WeatherDotMatrix.GRID_SIZE

    ComposeCanvas(
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

                if (color != Color.Transparent) {
                    val x = c * (dotSizePx + spacingPx) + dotSizePx / 2f
                    val y = r * (dotSizePx + spacingPx) + dotSizePx / 2f
                    drawCircle(
                        color = color,
                        radius = dotSizePx / 2f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
