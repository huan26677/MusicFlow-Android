package com.example.musicflow.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Shared Ocean Flow visual language. All visuals are local, static and offline. */
@Composable
fun OceanFlowWaveMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        repeat(3) { line ->
            val path = Path()
            val offset = line * 0.19f
            for (step in 0..64) {
                val x = step / 64f
                val wave = sin((x * 7.0 + offset * 5.0).toDouble()).toFloat() * 0.15f
                val y = size.height * (0.40f + line * 0.11f + wave)
                if (step == 0) path.moveTo(0f, y) else path.lineTo(size.width * x, y)
            }
            drawPath(
                path = path,
                color = tint.copy(alpha = 0.34f - line * 0.06f),
                style = Stroke(width = (3.8f - line * 0.5f).dp.toPx())
            )
        }
    }
}

@Composable
fun OceanFlowHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.surfaceContainerHigh,
                        scheme.surfaceContainerLow)
                )
            ).testTag("ocean-flow-header")
    ) {
        OceanFlowWaveMark(
            modifier = Modifier.align(Alignment.TopEnd)
                .width(148.dp).height(115.dp),
            tint = scheme.primary
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                modifier = Modifier.testTag("ocean-flow-eyebrow")
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OceanFlowSectionTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 4.dp, height = 23.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun OceanFlowCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, scheme.outlineVariant.copy(alpha = 0.68f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

/** Atmospheric album-art stage. No network lookup or color extraction from user files. */
@Composable
fun OceanFlowArtworkStage(
    artworkUri: Uri?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth().height(328.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer, scheme.surfaceContainerHigh,
                        scheme.background)
                )
            ).testTag("ocean-flow-artwork-stage"),
        contentAlignment = Alignment.Center
    ) {
        OceanFlowWaveMark(
            modifier = Modifier.fillMaxWidth().height(168.dp)
                .align(Alignment.BottomCenter),
            tint = scheme.primary
        )
        SongArtwork(
            uri = artworkUri,
            modifier = Modifier.fillMaxWidth(0.78f).widthIn(max = 274.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(2.dp, scheme.primary.copy(alpha = 0.23f),
                    RoundedCornerShape(26.dp))
                .aspectRatio(1f)
        )
    }
}
