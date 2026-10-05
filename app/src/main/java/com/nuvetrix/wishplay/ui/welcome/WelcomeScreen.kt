package com.nuvetrix.wishplay.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.R
import com.nuvetrix.wishplay.domain.model.Platform
import com.nuvetrix.wishplay.ui.components.ButtonStyle
import com.nuvetrix.wishplay.ui.components.CookieShape
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.ExpressiveButton
import com.nuvetrix.wishplay.ui.components.GroupPosition
import com.nuvetrix.wishplay.ui.components.GroupedItemContainer
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onNavigateToWishlist: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AnimatedContent(
        targetState = uiState.currentStep,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding_step"
    ) { step ->
        when (step) {
            0 -> WelcomeStepZero(
                onContinueWithGoogle = viewModel::onContinueWithGoogle,
                onContinueAsGuest = viewModel::onContinueAsGuest
            )
            1 -> SetupStepPlatforms(
                selectedPlatforms = uiState.selectedPlatforms,
                onTogglePlatform = viewModel::togglePlatform,
                onBack = viewModel::onStepBack,
                onSkip = { viewModel.onSkipOnboarding(onNavigateToWishlist) },
                onNext = viewModel::onStepNext
            )
            2 -> SetupStepLead(
                selectedLead = uiState.selectedLead,
                onSelectLead = viewModel::selectLead,
                onBack = viewModel::onStepBack,
                onSkip = { viewModel.onSkipOnboarding(onNavigateToWishlist) },
                onDone = { viewModel.onCompleteOnboarding(onNavigateToWishlist) }
            )
        }
    }
}

@Composable
private fun WelcomeStepZero(
    onContinueWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Brand Cookie Icon
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CookieShape(CookieShapeType.C9))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CookieShape(CookieShapeType.SQUIRCLE))
                        .background(customColors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WP",
                        color = customColors.onAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                }
            }

            Text(
                text = stringResource(R.string.onboarding_headline),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.onboarding_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpressiveButton(
                onClick = onContinueWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.FILLED,
                isLarge = true,
                text = stringResource(R.string.continue_with_google)
            )

            ExpressiveButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.TONAL,
                isLarge = true,
                text = stringResource(R.string.continue_as_guest)
            )

            Text(
                text = stringResource(R.string.guest_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SetupStepPlatforms(
    selectedPlatforms: Set<String>,
    onTogglePlatform: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val platforms = Platform.entries
    val customColors = WishPlayThemeColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            SetupTopBar(step = 1, totalSteps = 2, onBack = onBack, onSkip = onSkip)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.step_platforms_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.step_platforms_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2-column platform grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                platforms.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { platform ->
                            val isSelected = selectedPlatforms.contains(platform.shortName)
                            val shape = RoundedCornerShape(if (isSelected) 28.dp else 20.dp)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(shape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                    .border(
                                        BorderStroke(
                                            1.5.dp,
                                            if (isSelected) Color.Transparent else customColors.outlineVariant
                                        ),
                                        shape
                                    )
                                    .clickable { onTogglePlatform(platform.shortName) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) customColors.accent else customColors.surfaceContainerHigh
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = platform.shortName.take(2).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) customColors.onAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = platform.shortName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        ExpressiveButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.GOLD,
            isLarge = true,
            text = if (selectedPlatforms.isNotEmpty()) {
                "Continue with ${selectedPlatforms.size} platform${if (selectedPlatforms.size == 1) "" else "s"}"
            } else "Continue"
        )
    }
}

@Composable
private fun SetupStepLead(
    selectedLead: String,
    onSelectLead: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit
) {
    val leadOptions = listOf(
        Pair("1 day", "Just enough time to clear an evening"),
        Pair("3 days", "Time to pre-load or pre-order"),
        Pair("1 week", "Good for big releases you plan around")
    )
    val customColors = WishPlayThemeColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            SetupTopBar(step = 2, totalSteps = 2, onBack = onBack, onSkip = onSkip)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.step_lead_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.step_lead_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lead selection list
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                leadOptions.forEachIndexed { index, (lead, subtitle) ->
                    val isSelected = selectedLead == lead
                    val position = when (index) {
                        0 -> GroupPosition.FIRST
                        leadOptions.lastIndex -> GroupPosition.LAST
                        else -> GroupPosition.MIDDLE
                    }

                    GroupedItemContainer(
                        position = position,
                        onClick = { onSelectLead(lead) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$lead before",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = customColors.success
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notification preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, customColors.outlineVariant), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(customColors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = customColors.onAccent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Starfall Odyssey drops in $selectedLead",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nov 13 on PC, PS5 and Xbox",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        ExpressiveButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.GOLD,
            isLarge = true,
            text = stringResource(R.string.start_wishlist)
        )
    }
}

@Composable
private fun SetupTopBar(
    step: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val customColors = WishPlayThemeColors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Stepper progress indicator
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..totalSteps) {
                val isActive = i <= step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isActive) customColors.accent else customColors.track)
                )
            }
        }

        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
