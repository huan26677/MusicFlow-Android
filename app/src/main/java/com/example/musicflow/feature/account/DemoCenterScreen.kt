package com.example.musicflow.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.account.DemoCenterPlan
import com.example.musicflow.core.account.DemoCenterStore
import com.example.musicflow.core.account.DemoPersona
import com.example.musicflow.ui.components.OceanFlowHeader

@Composable
fun DemoCenterRoute(onClose: () -> Unit, onOpenBilling: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { DemoCenterStore(context.applicationContext) }
    var actor by remember { mutableStateOf(store.selected()) }
    var progress by remember { mutableStateOf(store.progress(actor)) }
    var error by remember { mutableStateOf<String?>(null) }

    DemoCenterScreen(
        actor = actor,
        progress = progress,
        error = error,
        onSelect = { selected ->
            runCatching {
                store.select(selected)
                actor = selected
                progress = store.progress(selected)
                error = null
            }.onFailure { error = "Không lưu được vai trò mô phỏng." }
        },
        onAdvance = {
            runCatching {
                val next = DemoCenterPlan.advance(actor, progress)
                store.progressTo(actor, next)
                progress = next
                error = null
            }.onFailure { error = "Không lưu được bước mô phỏng." }
        },
        onReset = {
            runCatching {
                store.reset(actor)
                progress = 0
                error = null
            }.onFailure { error = "Không đặt lại được kịch bản." }
        },
        onClose = onClose,
        onOpenBilling = onOpenBilling
    )
}

/** Pure UI: all callbacks are local mock state, never a real Owner/Creator RPC. */
@Composable
internal fun DemoCenterScreen(
    actor: DemoPersona,
    progress: Int,
    error: String?,
    onSelect: (DemoPersona) -> Unit,
    onAdvance: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    onOpenBilling: () -> Unit
) {
    var choicesOpen by remember { mutableStateOf(false) }
    val steps = DemoCenterPlan.steps(actor)
    val done = DemoCenterPlan.boundedProgress(actor, progress)

    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 690.dp)
                .verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("demo-center-scroll"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OceanFlowHeader(
                eyebrow = "Demo Center · 9 vai trò",
                title = "Trung tâm trình diễn MusicFlow",
                subtitle = "Thử từng hành trình mẫu — không thay đổi dữ liệu tài khoản thật."
            )
            Text(
                "MÔ PHỎNG — Dữ liệu, vai trò và kết quả bên dưới hoàn toàn là ví dụ. " +
                    "Không đăng nhập trực tuyến, không cấp quyền thật, không gửi dữ liệu lên đám mây.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("demo-center-warning")
            )
            Text("Chọn một trong 9 loại tài khoản để trình diễn quy trình. " +
                "Có thể đổi vai trò ngay và tiếp tục kịch bản đã lưu trên thiết bị.",
                style = MaterialTheme.typography.bodySmall)

            OutlinedButton(
                onClick = { choicesOpen = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("demo-center-actor-picker")
            ) { Text("Vai trò mẫu: ${actor.title} · Đổi vai trò") }
            DropdownMenu(expanded = choicesOpen, onDismissRequest = { choicesOpen = false }) {
                DemoPersona.entries.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.title) },
                        onClick = {
                            choicesOpen = false
                            onSelect(choice)
                        },
                        modifier = Modifier.testTag("demo-center-actor-${choice.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider()
            Text("Đang xem: ${actor.title} (giả lập)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("demo-center-current-actor"))
            Text(actor.summary)
            Text("Tiến độ kịch bản: $done / ${steps.size}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("demo-center-progress"))
            steps.forEachIndexed { index, step ->
                val completed = index < done
                val isCurrent = index == done
                Text("${index + 1}. ${step.title}",
                    style = MaterialTheme.typography.titleSmall)
                if (completed) {
                    Text(step.outcome,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("demo-center-outcome-$index"))
                } else if (isCurrent) {
                    Text(step.explanation, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("demo-center-advance")
                    ) { Text(step.action) }
                } else {
                    Text("Sẽ mở sau bước ${index}.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            if (done == steps.size) {
                Text("Đã hoàn tất kịch bản ${actor.title} (100% mô phỏng).",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("demo-center-finished"))
            }
            if (actor == DemoPersona.PREMIUM) {
                OutlinedButton(
                    onClick = onOpenBilling,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("demo-center-open-billing")
                ) { Text("Thử mua Premium mô phỏng") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .testTag("demo-center-reset")
                ) { Text("Làm lại") }
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .testTag("demo-center-close")
                ) { Text("Đóng") }
            }
        }
    }
}
