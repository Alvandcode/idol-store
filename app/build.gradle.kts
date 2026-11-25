plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.idol.store"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.idol.store"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // ⬇️ آدرس فهرست اپلیکیشن‌ها (کاتالوگ) را اینجا ست کنید.
        // یک ریپو جدا بسازید (مثلاً idol-store-catalog) و apps.json را داخلش بگذارید.
        // تا وقتی Alvandcode را عوض نکنید، اپ از کاتالوگ داخلی (assets) استفاده می‌کند.
        buildConfigField(
            "String",
            "CATALOG_URL",
            "\"https://raw.githubusercontent.com/Alvandcode/idol-store-catalog/main/apps.json\""
        )
        // ⬇️ برای ورود با کد دستگاه گیت‌هاب: یک OAuth App بساز (callback = http://localhost)
        // در github.com/settings/developers و Client ID آن را اینجا بگذار.
        // تا آن موقع، ورود با توکن شخصی (PAT) در دسترس است.
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"\"")
        // ⬇️ ریپوی کاتالوگ برای پنل مدیریت (کامیت apps.json از داخل اپ)
        buildConfigField("String", "CATALOG_REPO", "\"Alvandcode/idol-store-catalog\"")
        buildConfigField("String", "CATALOG_PATH", "\"apps.json\"")
        // ⬇️ رجیستری کاربران عادی (فایل users.json در ریپوی کاتالوگ؛ توسط GitHub Action پر می‌شود)
        buildConfigField(
            "String",
            "USER_REGISTRY_URL",
            "\"https://raw.githubusercontent.com/Alvandcode/idol-store-catalog/main/users.json\""
        )
        // ⬇️ ریپوی کاتالوگ برای ساخت Issue ثبت‌نام کاربران
        buildConfigField("String", "REGISTRY_ISSUE_REPO", "\"Alvandcode/idol-store-catalog\"")
        // ⬇️ راز تولید توکن (تغییرش همه توکن‌ها را نامعتبر می‌کند؛ تو را به خودت)
        buildConfigField("String", "REG_SECRET", "\"580a81cdb31d62ab055f2982d570c499fefb6927830df851\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
