package com.idol.store.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.LayoutDirection
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

/**
 * گیج عقربه‌ای امتیاز اسکن: قوس قرمز (چپ) تا سبز (راست)، عقربه روی درصد پاک‌ها،
 * وسط «+P%»، چپ «منفی: (۱۰۰−P)٪» و راست «مثبت: P٪».
 */
@Composable
private fun ScanGauge(pct: Int) {
    val neg = 100 - pct
    val red = Color(0xFFEF4444)
    val green = Color(0xFF22C55E)
    val brightGreen = Color(0xFF4ADE80)
    val amber = Color(0xFFF59E0B)

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(96.dp),
        ) {
            val strokeW = 16.dp.toPx()
            // قوس همیشه در مرکز افقی کادر می‌نشیند
            val diameter = minOf(size.width - strokeW, size.height * 2f - strokeW)
            val topLeft = Offset((size.width - diameter) / 2f, strokeW / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)

            // مسیر پس‌زمینه
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            // قوس گرادیانی قرمز → کهربایی → سبز
            val brush = Brush.sweepGradient(
                0f to green,
                0.5f to red,
                0.68f to amber,
                0.84f to Color(0xFF84CC16),
                1f to green,
                center = center,
            )
            drawArc(
                brush = brush,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW),
            )
            // تیک‌های درجه‌بندی
            val rOuter = diameter / 2f
            val rIn1 = rOuter - strokeW * 1.35f
            val rIn2 = rOuter - strokeW * 1.8f
            for (i in 0..10) {
                val a = Math.toRadians((180.0 + 18.0 * i))
                val p1 = Offset(center.x + rIn1 * cos(a).toFloat(), center.y + rIn1 * sin(a).toFloat())
                val p2 = Offset(center.x + rIn2 * cos(a).toFloat(), center.y + rIn2 * sin(a).toFloat())
                drawLine(Color.White.copy(alpha = 0.30f), p1, p2, strokeWidth = 2.dp.toPx())
            }
            // عقربه — کاملاً مشکی و کمی ضخیم‌تر
            val na = Math.toRadians((180.0 + 1.8 * pct))
            val needleLen = rOuter - strokeW * 1.9f
            val tip = Offset(center.x + needleLen * cos(na).toFloat(), center.y + needleLen * sin(na).toFloat())
            drawLine(Color(0xFF0B0B0B), center, tip, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
            // توپی مرکزی
            drawCircle(Color(0xFF1E293B), radius = 10.dp.toPx(), center = center)
            drawCircle(Color(0xFF94A3B8), radius = 10.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ---- منفی (چپ) ----
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(red),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(
                        stringResource(R.string.sec_negative),
                        style = MaterialTheme.typography.labelSmall,
                        color = red,
                    )
                    Text(
                        "$neg%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = red,
                    )
                }
            }
            // ---- عدد بزرگ وسط ----
            Text(
                stringResource(R.string.sec_score_plus_fmt, pct),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = brightGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.2f),
            )
            // ---- مثبت (راست) ----
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.sec_positive),
                        style = MaterialTheme.typography.labelSmall,
                        color = green,
                    )
                    Text(
                        "+$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = green,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(green),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    }
}

/** جدول صریح نتیجه هر اسکن‌ر — برای شفافیت کامل به کاربر */
@Composable
fun EngineBreakdown(report: SecurityResult?) {
    val rows = report?.engineRows() ?: return
    if (rows.isEmpty()) return
    val score = scanScore(report)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // ---- گیج عقربه‌ای امتیاز اسکن ----
        if (score != null && score.total > 0) {
            ScanGauge(score.pct)
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
