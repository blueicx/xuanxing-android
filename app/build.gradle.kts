import java.security.MessageDigest
import java.time.Instant

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xuanji.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xuanji.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("regression") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".regression"
            versionNameSuffix = "-isolated-regression"
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testBuildType = if (providers.gradleProperty("xuanjiRegression").orNull == "true") "regression" else "debug"

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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("writeDebugApkManifest") {
    group = "build"
    description = "Writes local build metadata and SHA-256 beside the debug APK."
    dependsOn("assembleDebug")
    val apkDirectory = layout.buildDirectory.dir("outputs/apk/debug")
    outputs.file(apkDirectory.map { it.file("build-manifest.json") })
    // HEAD, dirty state and UTC time can change without changing the APK path.
    outputs.upToDateWhen { false }
    doLast {
        val apk = apkDirectory.get().file("app-debug.apk").asFile
        check(apk.isFile) { "Debug APK was not produced: ${apk.absolutePath}" }
        fun git(vararg arguments: String): String? = runCatching {
            val process = ProcessBuilder(listOf("git") + arguments).directory(rootDir).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor() == 0) output else null
        }.getOrNull()
        val digest = MessageDigest.getInstance("SHA-256").digest(apk.readBytes())
            .joinToString("") { "%02X".format(it) }
        val manifest = mapOf(
            "applicationId" to (android.defaultConfig.applicationId ?: "com.xuanji.app"),
            "variant" to "debug",
            "versionName" to (android.defaultConfig.versionName ?: "unknown"),
            "versionCode" to android.defaultConfig.versionCode,
            "sourceCommit" to (git("rev-parse", "HEAD") ?: "unavailable"),
            "workingTreeHasChanges" to (git("status", "--porcelain")?.isNotEmpty() ?: true),
            "builtAtUtc" to Instant.now().toString(),
            "apkFile" to apk.name,
            "apkBytes" to apk.length(),
            "apkSha256" to digest
        )
        val output = apkDirectory.get().file("build-manifest.json").asFile
        output.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(manifest)), Charsets.UTF_8)
    }
}

dependencies {
    // Compose BOM manages all Compose library versions
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Coroutines & JSON
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Instrumented UI tests. The test-only ComponentActivity is injected only into the
    // debug variant below and does not enter the main source manifest or release APK.
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")

    // Compose's test-only ComponentActivity must live in the target app process.
    // debugImplementation keeps it out of release while making createComposeRule launch correctly.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    add("regressionImplementation", "androidx.compose.ui:ui-test-manifest")
}
