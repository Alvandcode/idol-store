package com.idol.store.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(vm: AuthViewModel, onLoggedIn: () -> Unit) {
    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn) onLoggedIn()
    }

    var pat by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🛍️", fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text("Idol Store", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "برای استفاده از استور، با حساب گیت‌هاب وارد شوید",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            if (vm.deviceCode == null) {
                // ورود با کد دستگاه
                Button(
                    onClick = { vm.startDeviceFlow() },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ورود با کد دستگاه گیت‌هاب") }

                Text(
                    "— یا —",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                // ورود با توکن شخصی
                OutlinedTextField(
                    value = pat,
                    onValueChange = { pat = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("توکن دسترسی شخصی (PAT)") },
                    placeholder = { Text("ghp_…") },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.loginWithToken(pat) },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ورود با توکن") }

                Spacer(Modifier.height(10.dp))
                Text(
                    "توکن Classic از github.com/settings/tokens با دسترسی public_repo بسازید. " +
                        "برای ورود با کد دستگاه، Client ID اپ OAuth را در app/build.gradle.kts تنظیم کنید.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                // کارت کد دستگاه
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "این کد را در گیت‌هاب وارد کنید:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            vm.deviceCode!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "در صفحه github.com/login/device",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { vm.openVerificationPage() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("باز کردن صفحه ورود گیت‌هاب") }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (vm.polling) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("در انتظار تأیید شما…", fontSize = 13.sp)
                            }
                        }
                        TextButton(onClick = { vm.startDeviceFlow() }) { Text("ساخت کد جدید") }
                    }
                }
            }

            vm.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
