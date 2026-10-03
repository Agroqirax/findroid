package dev.jdtech.jellyfin.presentation.film.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/**
 * Top bar floating above an item screen.
 *
 * When a [title] is given the bar collapses the [ItemHeader] below it: it turns opaque right before
 * the header's title would scroll behind the buttons and shows [title] itself once the header's
 * title is gone.
 *
 * @param scrollOffset How far the [ItemHeader] has been scrolled, in pixels.
 * @param headerTitleHeight Height in pixels of the title at the bottom of the [ItemHeader].
 */
@Composable
fun ItemTopBar(
    hasBackButton: Boolean,
    hasHomeButton: Boolean,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    title: String? = null,
    scrollOffset: () -> Int = { 0 },
    headerTitleHeight: () -> Int = { 0 },
    content: @Composable (RowScope.() -> Unit) = {},
) {
    val safePadding = rememberSafePadding()
    val backgroundColor = MaterialTheme.colorScheme.background

    val headerHeight = with(LocalDensity.current) { ItemHeaderHeight.toPx() }
    val fadeDistance = with(LocalDensity.current) { 32.dp.toPx() }

    val currentScrollOffset by rememberUpdatedState(scrollOffset)
    val currentHeaderTitleHeight by rememberUpdatedState(headerTitleHeight)
    var barHeight by remember { mutableIntStateOf(0) }

    val collapsible = title != null
    val backgroundAlpha by
        remember(collapsible, headerHeight, fadeDistance) {
            derivedStateOf {
                if (!collapsible || barHeight == 0) return@derivedStateOf 0f
                val titleTop = headerHeight - currentScrollOffset() - currentHeaderTitleHeight()
                (1f - (titleTop - barHeight) / fadeDistance).coerceIn(0f, 1f)
            }
        }
    val titleAlpha by
        remember(collapsible, headerHeight, fadeDistance) {
            derivedStateOf {
                if (!collapsible || barHeight == 0) return@derivedStateOf 0f
                val titleBottom = headerHeight - currentScrollOffset()
                (1f - (titleBottom - barHeight) / fadeDistance).coerceIn(0f, 1f)
            }
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .onSizeChanged { barHeight = it.height }
                .drawBehind { drawRect(backgroundColor, alpha = backgroundAlpha) }
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.small,
                    top = safePadding.top + MaterialTheme.spacings.small,
                    end = safePadding.end + MaterialTheme.spacings.small,
                    bottom = MaterialTheme.spacings.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasBackButton) {
            ItemTopBarButton(
                icon = CoreR.drawable.ic_arrow_left,
                onClick = onBackClick,
                collapsedFraction = backgroundAlpha,
            )
        }
        if (hasHomeButton) {
            ItemTopBarButton(
                icon = CoreR.drawable.ic_home,
                onClick = onHomeClick,
                collapsedFraction = backgroundAlpha,
            )
        }
        content()
        if (title != null) {
            Text(
                text = title,
                modifier =
                    Modifier.weight(1f)
                        .padding(horizontal = MaterialTheme.spacings.small)
                        .graphicsLayer { alpha = titleAlpha },
                color = MaterialTheme.colorScheme.onBackground,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** Sits in a translucent black circle above the backdrop and blends into the bar once collapsed. */
@Composable
private fun ItemTopBarButton(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    collapsedFraction: Float,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.alpha(0.7f + 0.3f * collapsedFraction),
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 1f - collapsedFraction),
                contentColor =
                    lerp(Color.White, MaterialTheme.colorScheme.onBackground, collapsedFraction),
            ),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null)
    }
}

@Composable
@Preview(showBackground = true)
private fun ItemTopBarPreview() {
    FindroidTheme { ItemTopBar(hasBackButton = true, hasHomeButton = true) }
}
