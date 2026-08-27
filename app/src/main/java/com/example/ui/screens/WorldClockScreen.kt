package com.example.ui.screens

import com.example.ui.theme.LocalCustomFont


import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WorldClock
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldClockScreen(
    currentTimestamp: Long,
    clocks: List<WorldClock>,
    is24Hour: Boolean = true,
    onAddClock: (cityName: String, timezoneId: String, country: String) -> Unit,
    onDeleteClock: (WorldClock) -> Unit,
    onOpenDockMode: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Hardcoded high-quality pre-configured locations to add from
    val availableCities = listOf(
        CityData("London", "Europe/London", "UK"),
        CityData("New York", "America/New_York", "USA"),
        CityData("Auckland", "Pacific/Auckland", "New Zealand"),
        CityData("Bangkok", "Asia/Bangkok", "Thailand"),
        CityData("Beijing", "Asia/Shanghai", "China"),
        CityData("Berlin", "Europe/Berlin", "Germany"),
        CityData("Cairo", "Africa/Cairo", "Egypt"),
        CityData("Cape Town", "Africa/Johannesburg", "South Africa"),
        CityData("Chicago", "America/Chicago", "USA"),
        CityData("Dubai", "Asia/Dubai", "UAE"),
        CityData("Hong Kong", "Asia/Hong_Kong", "China"),
        CityData("Istanbul", "Europe/Istanbul", "Turkey"),
        CityData("Los Angeles", "America/Los_Angeles", "USA"),
        CityData("Mumbai", "Asia/Kolkata", "India"),
        CityData("Paris", "Europe/Paris", "France"),
        CityData("Rio de Janeiro", "America/Sao_Paulo", "Brazil"),
        CityData("Seoul", "Asia/Seoul", "South Korea"),
        CityData("Singapore", "Asia/Singapore", "Singapore"),
        CityData("Sydney", "Australia/Sydney", "Australia"),
        CityData("Tokyo", "Asia/Tokyo", "Japan"),
        CityData("Vancouver", "America/Vancouver", "Canada")
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cal = Calendar.getInstance()
    val tzName = cal.timeZone.id.substringAfter("/").replace("_", " ")
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val mainTime = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(currentTimestamp))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Local Time & World Map
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL TIME · $tzName".uppercase(),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (onOpenDockMode != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                    .background(Color(0xFF141414))
                                    .clickable { onOpenDockMode() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                    Text(
                                        text = "DOCK MODE ↗",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = mainTime,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NothingWorldMap(
                        clocks = clocks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0xFF161616))
                )

                // Right Column: World Clock List or Empty State
                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                ) {
                    if (clocks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "World Map",
                                    tint = Color(0x33FFFFFF),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "NO WORLD CLOCKS",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp,
                                    fontFamily = LocalCustomFont.current,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(clocks, key = { it.id }) { clock ->
                                WorldClockItemRow(
                                    clock = clock,
                                    currentTimestamp = currentTimestamp,
                                    is24Hour = is24Hour,
                                    onDelete = { onDeleteClock(clock) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }

                    // Floating Add City Pill in Landscape
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .testTag("add_world_clock_button")
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable { showAddDialog = true }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add World Clock",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADD CITY",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = LocalCustomFont.current,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Layout
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Home / Primary Clock (dot matrix styling) represented dramatically
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (onOpenDockMode != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                    .background(Color(0xFF141414))
                                    .clickable { onOpenDockMode() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                    Text(
                                        text = "DOCK MODE ↗",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Text(
                        text = "LOCAL TIME · $tzName".uppercase(),
                        color = MaterialTheme.colorScheme.tertiary, // Nothing red accent
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Extra big elegant digital readout
                    Text(
                        text = mainTime,
                        color = Color.White,
                        fontSize = 54.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                }

                // Divider styled with subtle dot pattern or subtle border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF161616))
                )

                NothingWorldMap(
                    clocks = clocks,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                if (clocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "World Map",
                                tint = Color(0x33FFFFFF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO WORLD CLOCKS",
                                color = Color(0xFF666666),
                                fontSize = 14.sp,
                                fontFamily = LocalCustomFont.current,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(clocks, key = { it.id }) { clock ->
                            WorldClockItemRow(
                                clock = clock,
                                currentTimestamp = currentTimestamp,
                                is24Hour = is24Hour,
                                onDelete = { onDeleteClock(clock) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }

            // Search/Add floating pill button for portrait
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .testTag("add_world_clock_button")
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add World Clock",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD CITY",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = LocalCustomFont.current,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (showAddDialog) {
            AddCityDialog(
                availableCities = availableCities,
                currentClocks = clocks,
                onDismiss = { showAddDialog = false },
                onAddCity = { city ->
                    onAddClock(city.cityName, city.timezoneId, city.country)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorldClockItemRow(
    clock: WorldClock,
    currentTimestamp: Long,
    is24Hour: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone(clock.timezoneId)
    }
    
    val timeString = formatter.format(Date(currentTimestamp))

    // Calculate timezone difference compared to local timezone
    val localTz = Calendar.getInstance().timeZone
    val targetTz = TimeZone.getTimeZone(clock.timezoneId)
    val diffMs = targetTz.getOffset(currentTimestamp) - localTz.getOffset(currentTimestamp)
    val diffHours = diffMs / (1000 * 60 * 60)
    
    val diffText = when {
        diffHours > 0 -> "+${diffHours}H AHEAD"
        diffHours < 0 -> "${diffHours}H BEHIND"
        else -> "SAME TIME"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x2B18181B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = clock.cityName.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${clock.country.uppercase()} · $diffText",
                    color = Color(0xFF888888),
                    fontSize = 11.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .testTag("delete_world_clock_row_btn")
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove City",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

data class CityData(
    val cityName: String,
    val timezoneId: String,
    val country: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityDialog(
    availableCities: List<CityData>,
    currentClocks: List<WorldClock>,
    onDismiss: () -> Unit,
    onAddCity: (CityData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = availableCities.filter {
        it.cityName.lowercase().contains(searchQuery.lowercase()) ||
        it.country.lowercase().contains(searchQuery.lowercase())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "ADD WORLD CLOCK",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Box styled inside a neat pill
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("SEARCH CITY...", color = Color(0xFF444444), fontSize = 11.sp, fontFamily = LocalCustomFont.current) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF2C2C2C),
                        focusedContainerColor = Color(0xFF0F0F0F),
                        unfocusedContainerColor = Color(0xFF0F0F0F)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth().testTag("city_search_bar")
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val alreadyAddedNames = currentClocks.map { it.cityName.lowercase() }
                    val itemsToDisplay = filteredCities.filter { !alreadyAddedNames.contains(it.cityName.lowercase()) }

                    if (itemsToDisplay.isEmpty()) {
                        item {
                            Text(
                                text = "NO CITIES FOUND",
                                color = Color(0xFF444444),
                                fontSize = 12.sp,
                                fontFamily = LocalCustomFont.current,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp)
                            )
                        }
                    } else {
                        items(itemsToDisplay) { city ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onAddCity(city) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF131313))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = city.cityName.uppercase(),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = city.country.uppercase(),
                                            color = Color(0xFF888888),
                                            fontSize = 10.sp,
                                            fontFamily = LocalCustomFont.current,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "ADD",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontFamily = LocalCustomFont.current,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151515))
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color(0xFF888888),
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NothingWorldMap(
    clocks: List<WorldClock>,
    modifier: Modifier = Modifier
) {
    val cityCoordinates = remember {
        mapOf(
            "london" to Pair(0.48f, 0.28f),
            "new york" to Pair(0.25f, 0.36f),
            "auckland" to Pair(0.92f, 0.82f),
            "bangkok" to Pair(0.76f, 0.52f),
            "beijing" to Pair(0.79f, 0.36f),
            "berlin" to Pair(0.53f, 0.28f),
            "cairo" to Pair(0.57f, 0.42f),
            "cape town" to Pair(0.54f, 0.74f),
            "chicago" to Pair(0.22f, 0.35f),
            "dubai" to Pair(0.64f, 0.45f),
            "hong kong" to Pair(0.80f, 0.47f),
            "istanbul" to Pair(0.56f, 0.34f),
            "los angeles" to Pair(0.12f, 0.38f),
            "mumbai" to Pair(0.69f, 0.48f),
            "paris" to Pair(0.50f, 0.30f),
            "rio de janeiro" to Pair(0.34f, 0.70f),
            "seoul" to Pair(0.83f, 0.36f),
            "singapore" to Pair(0.77f, 0.58f),
            "sydney" to Pair(0.89f, 0.76f),
            "tokyo" to Pair(0.86f, 0.38f),
            "vancouver" to Pair(0.14f, 0.28f)
        )
    }

    val top5Clocks = remember(clocks) { clocks.take(5) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 2.5f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
    ) {
        val width = maxWidth
        val height = maxHeight

        // Inverted Halftone Dot Matrix World Map
        Image(
            painter = painterResource(id = com.example.R.drawable.img_world_map_dot_matrix),
            contentDescription = "Halftone Dot Matrix World Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.88f)
        )

        // Overlay Coordinate Grid & Active Pulsing City Pins
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle technical crosshair lines
            val accentPaintColor = Color(0x1AFFFFFF)
            drawLine(
                color = accentPaintColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = accentPaintColor,
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )

            // Draw pulsing and solid pins
            top5Clocks.forEach { clock ->
                val key = clock.cityName.lowercase()
                val coords = cityCoordinates[key] ?: run {
                    val offsetHrs = TimeZone.getTimeZone(clock.timezoneId).rawOffset / (1000 * 60 * 60f)
                    val fx = (0.48f + (offsetHrs / 12f) * 0.45f).coerceIn(0.05f, 0.95f)
                    Pair(fx, 0.4f)
                }

                val px = coords.first * size.width
                val py = coords.second * size.height

                // Pulse ring
                drawCircle(
                    color = tertiaryColor,
                    radius = pulseRadius.dp.toPx(),
                    alpha = pulseAlpha,
                    center = androidx.compose.ui.geometry.Offset(px, py)
                )

                // Solid center
                drawCircle(
                    color = tertiaryColor,
                    radius = 3.5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(px, py)
                )
            }
        }

        // Technical telemetry header
        Text(
            text = "GLYPH WORLD PROJECTION",
            color = Color(0x66FFFFFF),
            fontSize = 8.sp,
            fontFamily = LocalCustomFont.current,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        )

        Text(
            text = "PINNED CITIES: ${top5Clocks.size}/5",
            color = Color(0x66FFFFFF),
            fontSize = 8.sp,
            fontFamily = LocalCustomFont.current,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )

        // Floating city badges for pinned locations
        top5Clocks.forEach { clock ->
            val key = clock.cityName.lowercase()
            val coords = cityCoordinates[key] ?: run {
                val offsetHrs = TimeZone.getTimeZone(clock.timezoneId).rawOffset / (1000 * 60 * 60f)
                val fx = (0.48f + (offsetHrs / 12f) * 0.45f).coerceIn(0.05f, 0.95f)
                Pair(fx, 0.4f)
            }

            val xDp = width * coords.first
            val yDp = height * coords.second

            Box(
                modifier = Modifier
                    .offset(x = (xDp - 16.dp).coerceAtLeast(4.dp), y = (yDp - 16.dp).coerceAtLeast(4.dp))
                    .background(Color(0xE60A0A0C), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0x40FFFFFF), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = clock.cityName.uppercase().take(3),
                    color = Color.White,
                    fontSize = 7.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}