package com.example.musicflow.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.account.DemoBillingMethod
import com.example.musicflow.core.account.DemoBillingPlan
import com.example.musicflow.core.account.DemoBillingReceipt
import com.example.musicflow.ui.components.OceanFlowHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

private fun demoPrice(amount: Int): String =
    NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(amount) + " đ"

private fun demoDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

/** A visibly simulated checkout: neither credentials nor actual entitlement are involved. */
@Composable
fun DemoBillingScreen(
    receipt: DemoBillingReceipt?,
    feedback: String?,
    onPurchase: (DemoBillingPlan, DemoBillingMethod, Boolean) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    nowMs: Long = System.currentTimeMillis()
) {
    var plan by rememberSaveable { mutableStateOf(DemoBillingPlan.MONTH) }
    var method by rememberSaveable { mutableStateOf(DemoBillingMethod.WALLET) }
    var simulateDecline by rememberSaveable { mutableStateOf(false) }
    var checkoutOpen by rememberSaveable { mutableStateOf(false) }
    var cancelOpen by rememberSaveable { mutableStateOf(false) }

    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 690.dp)
                .verticalScroll(rememberScrollState()).padding(18.dp)
                .testTag("demo-billing-scroll"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OceanFlowHeader(
                eyebrow = "Premium · Trình diễn",
                title = "Thanh toán Premium — MÔ PHỎNG",
                subtitle = "Trải nghiệm hành trình mua gói, không thực hiện thanh toán thật."
            )
            Text("Chỉ dùng trong dự án báo cáo. KHÔNG thu tiền, KHÔNG kết nối ngân hàng, " +
                "KHÔNG cấp Premium thật trên máy chủ hoặc quyền quản trị.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("demo-billing-disclaimer"))

            if (receipt != null) {
                val active = receipt.active(nowMs)
                Text(if (active) "Premium mô phỏng: Đang có hiệu lực"
                    else if (receipt.cancelled) "Premium mô phỏng: Đã hủy"
                    else "Premium mô phỏng: Đã hết hạn",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("demo-billing-status"))
                Text("Mã biên nhận giả lập: ${receipt.id}",
                    modifier = Modifier.testTag("demo-billing-receipt"))
                Text("Gói: ${receipt.plan.label} · Phương thức: ${receipt.method.label}")
                Text("Ngày mô phỏng: ${demoDate(receipt.purchasedAtMs)}")
                Text("Có hiệu lực tới (demo): ${demoDate(receipt.expiresAtMs)}")
                if (active) {
                    OutlinedButton(
                        onClick = { cancelOpen = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("demo-billing-cancel")
                    ) { Text("Hủy gói mô phỏng") }
                } else {
                    TextButton(onClick = onReset,
                        modifier = Modifier.testTag("demo-billing-reset")) {
                        Text("Xóa biên nhận giả lập")
                    }
                }
            } else {
                Text("Chưa có giao dịch mô phỏng trên thiết bị này.",
                    modifier = Modifier.testTag("demo-billing-empty"))
            }

            HorizontalDivider()
            Text("1. Chọn gói (giá minh họa)", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DemoBillingPlan.entries.forEach { candidate ->
                    FilterChip(
                        selected = plan == candidate,
                        onClick = { plan = candidate },
                        label = { Text("${candidate.label} — ${demoPrice(candidate.priceVnd)}") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("demo-billing-plan-${candidate.name.lowercase()}")
                    )
                }
            }
            Text("2. Chọn phương thức giả lập", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DemoBillingMethod.entries.forEach { candidate ->
                    FilterChip(
                        selected = method == candidate,
                        onClick = { method = candidate },
                        label = { Text(candidate.label) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("demo-billing-method-${candidate.name.lowercase()}")
                    )
                }
            }
            Text("3. Kết quả để kiểm thử", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !simulateDecline, onClick = { simulateDecline = false },
                    label = { Text("Thành công") },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .testTag("demo-billing-result-success")
                )
                FilterChip(
                    selected = simulateDecline, onClick = { simulateDecline = true },
                    label = { Text("Thất bại") },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .testTag("demo-billing-result-declined")
                )
            }
            Button(
                onClick = { checkoutOpen = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("demo-billing-checkout")
            ) { Text(if (receipt?.active(nowMs) == true) "Mô phỏng gia hạn"
                else "Mô phỏng thanh toán") }
            feedback?.let { Text(it, modifier = Modifier.testTag("demo-billing-feedback")) }
            Text("Hóa đơn và thời hạn chỉ lưu cục bộ cho phạm vi tài khoản đang xem. " +
                "Không chuyển thành quyền Premium máy chủ, không mở khóa đồng bộ hoặc phát nhạc trực tuyến " +
                "hay quyền Owner/Admin.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onClose,
                modifier = Modifier.fillMaxWidth().testTag("demo-billing-close")) {
                Text("Đóng màn mô phỏng")
            }
        }
    }

    if (checkoutOpen) {
        AlertDialog(
            onDismissRequest = { checkoutOpen = false },
            title = { Text("Xác nhận thanh toán MÔ PHỎNG") },
            text = { Text("Gói ${plan.label} · ${demoPrice(plan.priceVnd)} · " +
                "${method.label}. Kết quả thử nghiệm: " +
                (if (simulateDecline) "Thất bại" else "Thành công") +
                ". Không có giao dịch tiền thật hoặc kết nối máy chủ thanh toán.") },
            confirmButton = {
                TextButton(onClick = {
                    checkoutOpen = false
                    onPurchase(plan, method, simulateDecline)
                }, modifier = Modifier.testTag("demo-billing-confirm")) {
                    Text("Xác nhận mô phỏng")
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutOpen = false },
                    modifier = Modifier.testTag("demo-billing-abort")) { Text("Không thực hiện") }
            }
        )
    }
    if (cancelOpen) {
        AlertDialog(
            onDismissRequest = { cancelOpen = false },
            title = { Text("Hủy gói Premium mô phỏng?") },
            text = { Text("Chỉ xóa hiệu lực gói demo trên thiết bị. " +
                "Không hủy bất kỳ gói trả phí hoặc entitlement thật nào.") },
            confirmButton = {
                TextButton(onClick = {
                    cancelOpen = false
                    onCancel()
                }, modifier = Modifier.testTag("demo-billing-confirm-cancel")) {
                    Text("Hủy gói demo")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelOpen = false }) { Text("Giữ gói demo") }
            }
        )
    }
}
