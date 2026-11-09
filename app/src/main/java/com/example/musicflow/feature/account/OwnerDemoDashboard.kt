package com.example.musicflow.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.account.OwnerDemoSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Owner-only read-only overview for a school demo.
 * Supabase checks the role for EVERY query, not just on this screen.
 */
@Composable
internal fun OwnerDemoDashboard(rpc: suspend (String, JSONObject) -> String) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<OwnerDemoSnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun fetch() {
        busy = true
        error = null
        try {
            data = OwnerDemoSnapshot.fromResponse(rpc("musicflow_owner_demo_summary", JSONObject()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            data = null
            error = "Không tải được thống kê Owner. Kiểm tra kết nối và đăng nhập lại."
        } finally {
            busy = false
        }
    }
    LaunchedEffect(Unit) { fetch() }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("owner-demo-dashboard"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider()
        Text("Owner • Tổng quan bản demo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Dữ liệu tổng hợp từ hệ thống quản trị. Không hiển thị email, mật khẩu hoặc khóa bí mật.",
            style = MaterialTheme.typography.bodySmall
        )
        if (busy && data == null) {
            Text("Đang lấy thống kê...")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        data?.let { report ->
            Text(
                "Premium còn hạn: ${report.activePremiumUsers} · Tài khoản không có Premium còn hạn: ${report.nonPremiumUsers}",
                modifier = Modifier.testTag("owner-demo-tiers")
            )
            Text(
                "PlatformAdmin đủ ba quyền ủy nhiệm: ${report.fullyDelegatedAdmins}",
                modifier = Modifier.testTag("owner-demo-delegations")
            )
            Text(
                if (report.hasCoreDemoActors) {
                    "Đã có các vai trò Owner, PlatformAdmin đủ quyền, Moderator, Developer, Nghệ sĩ và Tác giả."
                } else {
                    "Chưa đủ vai trò demo: cần Owner, PlatformAdmin đủ ba quyền, Moderator, Developer, Nghệ sĩ và Tác giả."
                },
                modifier = Modifier.testTag("owner-demo-role-readiness")
            )
            Text(
                if (report.hasDemoSubscriptionExamples) {
                    "Đã có ví dụ tài khoản Premium và không Premium để so sánh."
                } else "Cần tài khoản Premium còn hạn và tài khoản không Premium để demo quyền gói."
            )
            Text(
                if (report.hasPendingCreatorExample) {
                    "Đã có yêu cầu Creator đang chờ để demo duyệt."
                } else "Không còn yêu cầu Creator đang chờ; cần tạo yêu cầu QA mới nếu muốn demo xét duyệt."
            )
            Text(
                "Chủ sở hữu: " + report.ownerCount +
                    if (report.hasExactlyOneOwner) " (hợp lệ)" else " (cần kiểm tra)",
                modifier = Modifier.testTag("owner-demo-owner-count")
            )
            Text(
                "Người dùng đăng ký: " + report.userCount,
                modifier = Modifier.testTag("owner-demo-users")
            )
            Text(
                "Cộng tác quản trị: " + report.platformAdmins +
                    " · Kiểm duyệt viên: " + report.moderators,
                modifier = Modifier.testTag("owner-demo-staff")
            )
            Text(
                "Nghệ sĩ: " + report.artists +
                    " · Tác giả: " + report.authors +
                    " · Developer: " + report.developers
            )
            Text(
                "Yêu cầu Creator chờ duyệt: " + report.pendingCreatorRequests +
                    " · Bản nháp chờ duyệt: " + report.pendingDrafts
            )
            Text(
                "Metadata đã duyệt: " + report.approvedMetadata +
                    " · Tệp riêng tư đã xác nhận: " + report.uploadedPrivateFiles
            )
            HorizontalDivider()
            Text("Bước tiếp theo để demo", style = MaterialTheme.typography.titleSmall)
            if (report.needsTestActors) {
                Text(
                    "1. Tạo các tài khoản Free thử nghiệm do bạn kiểm soát.\n" +
                        "2. Tại danh sách người dùng bên dưới, chọn tài khoản rồi cấp PlatformAdmin " +
                        "hoặc Moderator. Cấp Artist/Author cho tài khoản Creator.\n" +
                        "3. PlatformAdmin chỉ được xử lý phần Owner ủy quyền; " +
                        "Moderator chỉ xét duyệt thông tin tác phẩm.\n" +
                        "4. Đăng xuất/đăng nhập lại từng tài khoản để kiểm tra đúng vai trò."
                )
            } else {
                Text(
                    "Các vai trò chính đã có tài khoản. Bạn có thể thử: " +
                        "Creator tạo nháp → tải bản thu/chứng cứ riêng tư → gửi duyệt → " +
                        "Moderator duyệt thông tin tác phẩm; không xuất bản nhạc online."
                )
            }
            Text(
                "Chỉ dùng tài khoản và tác phẩm QA. " +
                    "Thống kê không chứng minh quy trình E2E đã chạy thành công.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedButton(
            enabled = !busy,
            onClick = { scope.launch { fetch() } },
            modifier = Modifier.fillMaxWidth().testTag("owner-demo-refresh")
        ) { Text("Làm mới tổng quan") }
    }
}

