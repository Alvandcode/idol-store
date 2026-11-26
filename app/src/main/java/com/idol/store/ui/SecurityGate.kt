package com.idol.store.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.idol.store.R
import com.idol.store.data.SecurityGateStatus
import com.idol.store.data.SecurityResult
import com.idol.store.data.gateStatusFor
import com.idol.store.data.scannedTag

/** کادر رنگی نتیجه اسکن — استایل‌های هر وضعیت */
private data class GateStyle(val color: Color)

private fun gateStyle(status: SecurityGateStatus): GateStyle = when (status) {
    SecurityGateStatus.SAFE -> GateStyle(Color(0xFF16A34A))
    SecurityGateStatus.WARNING -> GateStyle(Color(0xFFD97706))
    SecurityGateStatus.INCOMPLETE -> GateStyle(Color(0xFFEA580C))
    SecurityGateStatus.NOT_SCANNED -> GateStyle(Color(0xFF64748B))
    SecurityGateStatus.DANGEROUS -> GateStyle(Color(0xFFDC2626))
    SecurityGateStatus.LOADING -> GateStyle(Color(0xFF64748B))
}

private fun gateTitle(status: SecurityGateStatus): Int = when (status) {
    SecurityGateStatus.SAFE -> R.string.sec_status_safe
    SecurityGateStatus.WARNING -> R.string.sec_status_warning
    SecurityGateStatus.INCOMPLETE -> R.string.sec_status_incomplete
    SecurityGateStatus.NOT_SCANNED -> R.string.sec_status_not_scanned
    SecurityGateStatus.DANGEROUS -> R.string.sec_status_dangerous
    SecurityGateStatus.LOADING -> R.string.security_scanning
}

private fun gateSubtitle(status: SecurityGateStatus): Int = when (status) {
    SecurityGateStatus.SAFE -> R.string.sec_status_safe_sub
    SecurityGateStatus.WARNING -> R.string.sec_status_warning_sub
    SecurityGateStatus.INCOMPLETE -> R.string.sec_status_incomplete_sub
    SecurityGateStatus.NOT_SCANNED -> R.string.sec_status_not_scanned_sub
    SecurityGateStatus.DANGEROUS -> R.string.sec_status_dangerous_sub
    SecurityGateStatus.LOADING -> R.string.security_scanning_sub
}

/** دیالوگ گیت امنیتی: نمایش نتیجه اسکن + درخواست تایید کاربر برای دانلود */
@Composable
fun SecurityGateSheet(vm: StoreViewModel) {
    val gate by vm.securityGate.collectAsState()
    val securityMap by vm.securityStates.collectAsState()
    val loading by vm.securityLoading.collectAsState()
    val request = gate ?: return

    val report: SecurityResult? = securityMap[request.app.id]?.takeIf {
        it.scannedTag == null || it.scannedTag == request.tag
    }
    val status = when {
        loading && report == null -> SecurityGateStatus.LOADING
        report == null -> SecurityGateStatus.NOT_SCANNED
        else -> report.gateStatusFor(request.tag)
    }
    val style = gateStyle(status)

    Dialog(onDismissRequest = { vm.cancelGate() }) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.GppGood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.security_gate_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.security_gate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))

                // ---- کادر جداگانه نتیجه اسکن ----
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(style.color.copy(alpha = 0.12f))
                        .padding(14.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(style.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                when (status) {
                                    SecurityGateStatus.LOADING -> CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = style.color,
                                    )
                                    else -> Icon(
                                        imageVector = when (status) {
                                            SecurityGateStatus.SAFE -> Icons.Filled.GppGood
                                            SecurityGateStatus.DANGEROUS -> Icons.Filled.GppBad
                                            SecurityGateStatus.WARNING -> Icons.Filled.GppMaybe
                                            SecurityGateStatus.INCOMPLETE -> Icons.Filled.GppMaybe
                                            else -> Icons.Filled.HelpOutline
                                        },
                                        contentDescription = null,
                                        tint = style.color,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(gateTitle(status)),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = style.color,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(gateSubtitle(status)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp,
                        )
                        report?.scannedTag?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.sec_scanned_version_fmt, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (status) {
                    SecurityGateStatus.DANGEROUS -> {
                        // تهدید قطعی → دانلود مسدود، فقط بستن
                        Button(
                            onClick = { vm.cancelGate() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.sec_close_btn)) }
                    }
                    SecurityGateStatus.LOADING -> {
                        OutlinedButton(
                            onClick = { vm.cancelGate() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.sec_cancel_btn)) }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { vm.confirmGateDownload() },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.sec_confirm_btn)) }
                            OutlinedButton(
                                onClick = { vm.cancelGate() },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.sec_cancel_btn)) }
                        }
                    }
                }
            }
        }
    }
}
