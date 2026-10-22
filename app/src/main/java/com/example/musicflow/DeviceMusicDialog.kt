
package com.example.musicflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun DeviceMusicDialog(
    onDismiss: () -> Unit,
    onSelect: (List<DeviceSong>, Int) -> Unit
) {

    val context = LocalContext.current

    var songs by remember {
        mutableStateOf<List<DeviceSong>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var scanVersion by remember {
        mutableIntStateOf(0)
    }

    val permission = if (
        Build.VERSION.SDK_INT >= 33
    ) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                scanVersion++
            } else {
                message =
                    "Chưa có quyền đọc nhạc. " +
                            "Bạn vẫn có thể dùng nút chọn file MP3."
            }
        }

    fun requestScan() {

        val granted =
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            scanVersion++
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Thử quét lần đầu khi cửa sổ xuất hiện.
    LaunchedEffect(Unit) {
        requestScan()
    }

    // Quét trên luồng I/O thông qua scanner.
    LaunchedEffect(scanVersion) {

        if (scanVersion == 0) {
            return@LaunchedEffect
        }

        isLoading = true
        message = null

        try {

            songs = DeviceMusicScanner.scan(context)

            if (songs.isEmpty()) {
                message =
                    "Không tìm thấy bài hát được " +
                            "Android phân loại là nhạc."
            }

        } catch (_: SecurityException) {

            message = "Ứng dụng chưa có quyền đọc thư viện."

        } catch (e: Exception) {

            message =
                "Quét nhạc thất bại: " +
                        e.javaClass.simpleName

        } finally {

            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Nhạc thiết bị (${songs.size})")
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text =
                        "Chỉ hiển thị bài hát Android " +
                                "phân loại là nhạc.",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                message?.let {
                    Text(
                        text = it,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(
                        max = 360.dp
                    )
                ) {

                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->

                        TextButton(
                            onClick = {
                                onSelect(songs, index)
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(song.title)

                                Text(
                                    text = song.artist,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = { requestScan() }
            ) {
                Text("Quét lại")
            }
        },

        dismissButton = {

            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
