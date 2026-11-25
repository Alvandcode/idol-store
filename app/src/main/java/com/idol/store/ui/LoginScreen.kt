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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.R
import com.idol.store.data.ClientIdMissingException

@Composable
fun LoginScreen(vm: AuthViewModel, onLoggedIn: () -> Unit) {
    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn) onLoggedIn()
    }

    var pat by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userToken by remember { mutableStateOf("") }
    // حالت ورود: 0 = کاربر عادی، 1 = گیت‌هاب (مدیر)
    var mode by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current

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
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.login_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // انتخاب حالت ورود: کاربر عادی یا مدیر گیت‌هاب
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == 0,
                    onClick = { mode = 0; vm.resetRegistration() },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.login_tab_user), fontSize = 13.sp) }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.login_tab_github), fontSize = 13.sp) }
            }

            Spacer(Modifier.height(20.dp))

            if (mode == 0) {
                UserAuthSection(
                    vm = vm,
                    email = userEmail,
                    onEmailChange = { userEmail = it },
                    token = userToken,
                    onTokenChange = { userToken = it },
                )
            } else if (vm.deviceCode == null) {
                // ورود با کد دستگاه
                Button(
                    onClick = { vm.startDeviceFlow() },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.login_device_btn)) }

                Text(
                    stringResource(R.string.login_or),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                // ورود با توکن شخصی
                OutlinedTextField(
                    value = pat,
                    onValueChange = { pat = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_pat_label)) },
                    placeholder = { Text("ghp_…") },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.loginWithToken(pat) },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.login_pat_btn)) }

                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.login_pat_note),
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
                            stringResource(R.string.login_code_prompt),
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
                            stringResource(R.string.login_code_page_hint),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { vm.openVerificationPage() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.login_open_page_btn)) }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (vm.polling) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.login_waiting), fontSize = 13.sp)
                            }
                        }
                        TextButton(onClick = { vm.startDeviceFlow() }) {
                            Text(stringResource(R.string.login_new_code))
                        }
                    }
                }
            }

            vm.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    // خطای Client ID را به‌صورت پیش‌فرضِ رشته‌ی محلی نشان می‌دهیم
                    if (it.contains("GITHUB_CLIENT_ID")) stringResource(R.string.login_err_client_id) else it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(20.dp))
            LanguageSwitcher()
        }
    }
}

/** بخش ورود/ثبت‌نام کاربر عادی: ایمیل + توکن دستگاه‌محور */
@Composable
private fun UserAuthSection(
    vm: AuthViewModel,
    email: String,
    onEmailChange: (String) -> Unit,
    token: String,
    onTokenChange: (String) -> Unit,
) {
    if (vm.regCode == null) {
        // ---- ورود با ایمیل و توکن ----
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.user_email_label)) },
            placeholder = { Text("me@gmail.com") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.user_token_label)) },
            placeholder = { Text("a1b2c3…") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { vm.loginUser(email, token) },
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.user_login_btn)) }

        if (vm.busy) {
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.user_signup_title),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.user_signup_hint),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { vm.prepareRegistration(email) },
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.user_signup_btn)) }
    } else {
        // ---- کد و توکن ساخته شد: نمایش + ارسال درخواست ----
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
                    stringResource(R.string.user_code_title),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                SelectionContainer {
                    Text(
                        vm.regCode!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.user_token_show),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        vm.regToken!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.user_token_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { vm.openRegistrationIssue() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.user_send_btn)) }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.user_send_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.resetRegistration() }) {
                    Text(stringResource(R.string.user_signup_again))
                }
            }
        }
    }
}

/** سوییچ زبان — با ذخیره خودکار (per-app locale) */
@Composable
fun LanguageSwitcher() {
    val context = LocalContext.current
    val current = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings_language), fontSize = 12.sp)
        OutlinedButton(onClick = {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags("fa")
            )
        }, enabled = current != "fa") { Text(stringResource(R.string.lang_fa), fontSize = 12.sp) }
        OutlinedButton(onClick = {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags("en")
            )
        }, enabled = current != "en") { Text(stringResource(R.string.lang_en), fontSize = 12.sp) }
    }
}
