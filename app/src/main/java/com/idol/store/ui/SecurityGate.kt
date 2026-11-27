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
import androidx.compose.material3.LinearProgressIndicator
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
import com.idol.store.data.engineRows
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
                        if (status == SecurityGateStatus.INCOMPLETE || status == SecurityGateStatus.WARNING) {
                            // توضیح = فقط شمارش مثبت/منفی (درصد و ردیف‌ها در ادامه)
                            val sc = scanScore(report)
                            if (sc != null && sc.total > 0) {
                                Text(
                                    stringResource(R.string.sec_counts_fmt, sc.clean, sc.threat + sc.error),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    stringResource(gateSubtitle(status)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            EngineBreakdown(report)
                        } else {
                            Text(
                                stringResource(gateSubtitle(status)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp,
                            )
                        }
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

/** نوار وضعیت اسکن روی صفحه اپ — نتیجه بدون نیاز به زدن دانلود دیده می‌شود */
@Composable
fun SecurityStatusCard(vm: StoreViewModel, appId: String, tag: String) {
    val securityMap by vm.securityStates.collectAsState()
    val loading by vm.securityLoading.collectAsState()
    val report = securityMap[appId]?.takeIf { it.scannedTag == null || it.scannedTag == tag }
    val status = when {
        loading && report == null -> SecurityGateStatus.LOADING
        report == null -> SecurityGateStatus.NOT_SCANNED
        else -> report.gateStatusFor(tag)
    }
    val style = gateStyle(status)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(style.color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            SecurityGateStatus.LOADING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = style.color,
            )
            else -> Icon(
                imageVector = when (status) {
                    SecurityGateStatus.SAFE -> Icons.Filled.GppGood
                    SecurityGateStatus.DANGEROUS -> Icons.Filled.GppBad
                    SecurityGateStatus.WARNING, SecurityGateStatus.INCOMPLETE -> Icons.Filled.GppMaybe
                    else -> Icons.Filled.HelpOutline
                },
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(gateTitle(status)),
                style = MaterialTheme.typography.titleSmall,
                color = style.color,
            )
            Text(
                stringResource(R.string.security_gate_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status == SecurityGateStatus.INCOMPLETE || status == SecurityGateStatus.WARNING) {
                Spacer(Modifier.height(6.dp))
                EngineBreakdown(report)
            }
        }
    }
}

/** امتیاز اسکن: سهم بررسی‌هایی که پاک بودند از کل، به درصد */
private data class ScanScore(val clean: Int, val threat: Int, val error: Int) {
    val total: Int get() = clean + threat + error
    val pct: Int get() = if (total == 0) 0 else clean * 100 / total
}

private fun engineIsClean(key: String, status: String): Boolean = when (key) {
    "clamav" -> status in setOf("no_detection", "inspected")
    "yara" -> status in setOf("no_detection", "no_findings")
    "dependency" -> status == "no_known_vulnerabilities"
    "semgrep" -> status == "no_findings"
    "gitleaks" -> status == "no_findings"
    "assets" -> status == "no_detection"
    else -> false
}

private fun scanScore(report: SecurityResult?): ScanScore? {
    val rows = report?.engineRows() ?: return null
    if (rows.isEmpty()) return null
    var clean = 0
    var threat = 0
    var error = 0
    rows.forEach { (key, status) ->
        when {
            engineIsClean(key, status) -> clean++
            status == "detection" || status == "dangerous" || status == "secrets_found" -> threat++
            else -> error++
        }
    }
    return ScanScore(clean, threat, error)
}

/** جدول صریح نتیجه هر اسکن‌ر — برای شفافیت کامل به کاربر */
@Composable
fun EngineBreakdown(report: SecurityResult?) {
    val rows = report?.engineRows() ?: return
    if (rows.isEmpty()) return
    val score = scanScore(report)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // ---- امتیاز درصدی + شمارش مثبت/منفی ----
        if (score != null && score.total > 0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.sec_score_fmt, score.pct),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        score.pct == 100 -> Color(0xFF16A34A)
                        score.pct >= 50 -> Color(0xFFD97706)
                        else -> Color(0xFFEA580C)
                    },
                )
                Text(
                    stringResource(R.string.sec_counts_fmt, score.clean, score.threat + score.error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { score.pct / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    score.pct == 100 -> Color(0xFF16A34A)
                    score.pct >= 50 -> Color(0xFFD97706)
                    else -> Color(0xFFEA580C)
                },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            )
            Spacer(Modifier.height(4.dp))
        }

        val labels = mapOf(
            "clamav" to stringResource(R.string.eng_clamav),
            "yara" to stringResource(R.string.eng_yara),
            "dependency" to stringResource(R.string.eng_dependency),
            "semgrep" to stringResource(R.string.eng_semgrep),
            "gitleaks" to stringResource(R.string.eng_gitleaks),
            "assets" to stringResource(R.string.eng_assets),
        )
        val resultOk = stringResource(R.string.eng_ok)
        val resultErr = stringResource(R.string.eng_error)
        val resultFound = stringResource(R.string.eng_found)
        val resultThreat = stringResource(R.string.eng_threat)
        rows.forEach { (key, status) ->
            val label = labels[key] ?: return@forEach
            val (result, color) = when {
                engineIsClean(key, status) -> resultOk to Color(0xFF16A34A)
                status == "scan_error" || status == "download_or_scan_error" ->
                    resultErr to Color(0xFFEA580C)
                status == "secrets_found" -> resultFound to Color(0xFFD97706)
                status == "detection" || status == "dangerous" ->
                    resultThreat to Color(0xFFDC2626)
                else -> status to Color(0xFF64748B)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text(result, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}
