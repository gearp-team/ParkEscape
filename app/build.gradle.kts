import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 릴리스 서명 정보(git 제외). 없으면 debug 키로 서명해 assembleRelease가 항상 통과하게 한다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKeystore = !keystoreProperties.isEmpty

android {
    namespace = "com.team.pleosapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.team.pleosapp"
        // 차량 사이버보안 요건(서명 키 로테이션·lineage)으로 28 이상 필수
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "boolean",
            "USE_FAKE_SDK",
            providers.gradleProperty("pleos.useFakeSdk").getOrElse("false"),
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // 키 로테이션·lineage는 APK 서명 v3가 있어야 가능하다. minSdk 28이라 AGP는 v3만 넣는다(v2 생략).
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (hasReleaseKeystore) "release" else "debug")
        }
    }

    // CarUxRestrictionsManager(주행 규제) 등 AAOS Car API
    useLibrary("android.car")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // 버전은 Vehicle SDK Sample App 검증 조합(compileSdk 35)에 맞춰 고정했다. 올릴 때는 에뮬레이터에서 다시 검증한다.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
    }
}

dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// 공모전 제출용: release APK를 빌드해 apk/<팀명>_<앱이름>_release.apk로 복사한다.
val submissionApkName = providers.gradleProperty("pleos.teamName")
    .zip(providers.gradleProperty("pleos.appName")) { team, app -> "${team}_${app}_release.apk" }

tasks.register<Copy>("copyReleaseApk") {
    group = "distribution"
    description = "Builds the release APK and copies it to apk/ with the contest file name."
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*.apk") }
    into(rootProject.layout.projectDirectory.dir("apk"))
    rename { submissionApkName.get() }
}
