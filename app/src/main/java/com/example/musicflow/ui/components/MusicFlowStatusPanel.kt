package com.example.musicflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Consistent, accessible loading / empty / error / permission presentation.
 *
 * Status only: the caller owns all retry/permission actions and ViewModel
 * state. No independent I/O or additional source of truth is introduced.
 */
enum class MusicFlowStatusKind { LOADING, EMPTY, ERROR, PERMISSION, INFO }

@Composable
fun MusicFlowStatusPanel(
    kind: MusicFlowStatusKind,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val error = kind == MusicFlowStatusKind.ERROR
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("musicflow-status-${kind.name.lowercase()}")
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(24.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (error) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)),
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (kind != MusicFlowStatusKind.LOADING) {
                OceanFlowWaveMark(
                    modifier = Modifier.fillMaxWidth(0.32f)
                        .height(30.dp)
                        .padding(bottom = 2.dp),
                    tint = if (error) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                )
            }
            if (kind == MusicFlowStatusKind.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onAction != null && !actionLabel.isNullOrBlank()) {
                if (error || kind == MusicFlowStatusKind.PERMISSION) {
                    Button(onClick = onAction) { Text(actionLabel) }
                } else {
                    OutlinedButton(onClick = onAction) { Text(actionLabel) }
                }
            }
        }
    }
}
