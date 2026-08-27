package com.example.ui.screens

import com.example.ui.theme.LocalCustomFont


import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DotMatrixString

@Composable
fun StopwatchScreen(
    timeMs: Long,
    isRunning: Boolean,
    laps: List<Long>,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    // Format minutes, seconds, centiseconds
    val minutes = (timeMs / 60000) % 60
    val seconds = (timeMs / 1000) % 60
    val centiseconds = (timeMs / 10) % 100

    val timeStr = String.format("%02d:%02d", minutes, seconds)
    val centiStr = String.format(".%02d", centiseconds)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Stopwatch Headmount and Playback Pills
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "STOPWATCH",
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DotMatrixString(
                        text = timeStr,
                        activeColor = Color.White,
                        inactiveColor = Color(0x05FFFFFF),
                        charSpacing = 6.dp,
                        dotSize = 6.dp,
                        dotSpacing = 1.dp
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = centiStr,
                        color = MaterialTheme.colorScheme.tertiary, // Nothing Red for quick centiseconds
                        fontSize = 26.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timeMs > 0) {
                        Box(
                            modifier = Modifier
                                .testTag("lap_reset_stopwatch_btn")
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color(0x2B18181B))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp))
                                .clickable {
                                    if (isRunning) onLap() else onReset()
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isRunning) "LAP" else "RESET",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("toggle_stopwatch_btn")
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isRunning) MaterialTheme.colorScheme.tertiary else Color.White)
                            .clickable {
                                if (isRunning) onPause() else onStart()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "PAUSE" else "START MATCH",
                            color = if (isRunning) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Right Column: Lap records scrolling list
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1A18181B))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
            ) {
                if (laps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO LAPS RECORDED",
                            color = Color(0xFF333333),
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(laps, key = { index, _ -> "lap_${laps.size - index}" }) { index, lapTime ->
                            val lapNumber = laps.size - index
                            
                            val lMin = (lapTime / 60000) % 60
                            val lSec = (lapTime / 1000) % 60
                            val lCs = (lapTime / 10) % 100
                            val lapFormatted = String.format("%02d:%02d.%02d", lMin, lSec, lCs)

                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("LAP %02d", lapNumber),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = lapFormatted,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 12.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // Large Premium Dot Matrix and Mono Stopwatch Layout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STOPWATCH",
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hybrid representation: Matrix minutes:seconds and glowing red centiseconds
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DotMatrixString(
                        text = timeStr,
                        activeColor = Color.White,
                        inactiveColor = Color(0x05FFFFFF),
                        charSpacing = 8.dp,
                        dotSize = 8.dp,
                        dotSpacing = 2.dp
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = centiStr,
                        color = MaterialTheme.colorScheme.tertiary, // Nothing Red for quick centiseconds
                        fontSize = 32.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Action Pill Button Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary button: Lap / Reset
                if (timeMs > 0) {
                    Box(
                        modifier = Modifier
                            .testTag("lap_reset_stopwatch_btn")
                            .clip(RoundedCornerShape(40.dp))
                            .background(Color(0x2B18181B))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp))
                            .clickable {
                                if (isRunning) onLap() else onReset()
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "LAP" else "RESET",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // Dim placeholder spacer to keep visual structure centered
                    Box(modifier = Modifier.width(90.dp))
                }

                // Primary pill button: Play / Pause
                Box(
                    modifier = Modifier
                        .testTag("toggle_stopwatch_btn")
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isRunning) MaterialTheme.colorScheme.tertiary else Color.White)
                        .clickable {
                            if (isRunning) onPause() else onStart()
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRunning) "HOLD / PAUSE" else "START MATCH",
                        color = if (isRunning) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                
                // Layout spacer for alignment
                if (timeMs == 0L) {
                    Box(modifier = Modifier.width(90.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lap Times Scrolling Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(Color(0x1A18181B))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            ) {
                if (laps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO LAPS RECORDED",
                            color = Color(0xFF333333),
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(laps, key = { index, _ -> "lap_${laps.size - index}" }) { index, lapTime ->
                            val lapNumber = laps.size - index
                            
                            // Format lap duration
                            val lMin = (lapTime / 60000) % 60
                            val lSec = (lapTime / 1000) % 60
                            val lCs = (lapTime / 10) % 100
                            val lapFormatted = String.format("%02d:%02d.%02d", lMin, lSec, lCs)

                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("LAP %02d", lapNumber),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = lapFormatted,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 13.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}