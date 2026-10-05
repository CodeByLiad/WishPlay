package com.nuvetrix.wishplay.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.components.GroupPosition
import com.nuvetrix.wishplay.ui.components.GroupedItemContainer
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AlertsViewModel,
    wishlistGames: List<Game>,
    onBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.of(2026, 10)) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = (currentYearMonth.atDay(1).dayOfWeek.value - 1) // 0 for Monday

    // Map releases by day in this month: dayOfMonth -> List<Pair<Game, List<String>>>
    val monthReleases = remember(currentYearMonth, wishlistGames) {
        val ymStr = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val map = mutableMapOf<Int, MutableList<Pair<Game, List<String>>>>()

        wishlistGames.forEach { game ->
            val matchingPlats = mutableMapOf<Int, MutableList<String>>()
            game.platforms.forEach { (platform, date) ->
                if (date != null && date.startsWith(ymStr)) {
                    val day = date.substring(8, 10).toIntOrNull()
                    if (day != null) {
                        matchingPlats.getOrPut(day) { mutableListOf() }.add(platform)
                    }
                }
            }

            matchingPlats.forEach { (day, plats) ->
                map.getOrPut(day) { mutableListOf() }.add(game to plats)
            }
        }
        map
    }

    val displayList: List<Triple<Int, Game, List<String>>> = remember(selectedDay, monthReleases) {
        val result = mutableListOf<Triple<Int, Game, List<String>>>()
        if (selectedDay != null) {
            monthReleases[selectedDay]?.forEach { (g, plats) ->
                result.add(Triple(selectedDay!!, g, plats))
            }
        } else {
            monthReleases.keys.sorted().forEach { day ->
                monthReleases[day]?.forEach { (g, plats) ->
                    result.add(Triple(day, g, plats))
                }
            }
        }
        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Release calendar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // Month Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        currentYearMonth = currentYearMonth.minusMonths(1)
                        selectedDay = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        currentYearMonth = currentYearMonth.plusMonths(1)
                        selectedDay = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Day of Week Header: M T W T F S S
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dow ->
                    Text(
                        text = dow,
                        modifier = Modifier.width(38.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Month Grid
            val totalSlots = firstDayOfWeek + daysInMonth
            val totalRows = (totalSlots + 6) / 7

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val slot = row * 7 + col
                            val dayNum = slot - firstDayOfWeek + 1

                            if (dayNum in 1..daysInMonth) {
                                val hasReleases = monthReleases.containsKey(dayNum)
                                val isSelected = selectedDay == dayNum

                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                hasReleases -> WishPlayThemeColors.surfaceContainerHigh
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = hasReleases) {
                                            selectedDay = if (isSelected) null else dayNum
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (hasReleases) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                hasReleases -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            }
                                        )

                                        if (hasReleases) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(width = 44.dp, height = 46.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Agenda Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerText = if (selectedDay != null) {
                    "${currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))} $selectedDay"
                } else {
                    "Everything this month"
                }

                Text(
                    text = headerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedDay != null) {
                    TextButton(onClick = { selectedDay = null }) {
                        Text(
                            text = "Show the whole month",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "${monthReleases.size} day${if (monthReleases.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Agenda List or Empty State
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(WishPlayThemeColors.surfaceContainer)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nothing lands in ${currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Swipe to another month, or add more games to your list.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExpressiveButton(
                            onClick = {
                                currentYearMonth = currentYearMonth.plusMonths(1)
                                selectedDay = null
                            },
                            text = "Next month",
                            style = ButtonStyle.TONAL
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(WishPlayThemeColors.surfaceContainer)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    displayList.forEachIndexed { index, (day, game, plats) ->
                        val position = when {
                            displayList.size == 1 -> GroupPosition.ONLY
                            index == 0 -> GroupPosition.FIRST
                            index == displayList.size - 1 -> GroupPosition.LAST
                            else -> GroupPosition.MIDDLE
                        }

                        GroupedItemContainer(
                            position = position,
                            onClick = { onNavigateToDetails(game.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val monAbbr = currentYearMonth.format(DateTimeFormatter.ofPattern("MMM", Locale.US)).uppercase()
                                Column(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(WishPlayThemeColors.surfaceContainerHigh),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = monAbbr,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val platStr = plats.joinToString(", ")
                                    val movedStr = if (game.movedFromDate != null) " · moved from ${game.movedFromDate}" else ""
                                    Text(
                                        text = "$platStr$movedStr",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
