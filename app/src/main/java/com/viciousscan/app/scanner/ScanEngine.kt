package com.viciousscan.app.scanner

import com.viciousscan.app.model.*

/**
 * Pure Kotlin scanner engine — no Android framework dependencies so it can
 * be unit-tested on the JVM without Robolectric.
 *
 * Add new rules by appending to ANDROID_RULES or GENERIC_RULES.
 */
object ScanEngine {

    // -------------------------------------------------------------------------
    // Rule tables
    // -------------------------------------------------------------------------

    /** Rules that fire when the source file contains [trigger] text. */
    data class Rule(
        val trigger: String,               // substring match against file content
        val finding: ScanFinding
    )

    private val ANDROID_RULES: List<Rule> = listOf(
        // --- Permissions ---
        Rule(
            "LocationManager",
            ScanFinding(
                name = "android.permission.ACCESS_FINE_LOCATION",
                type = FindingType.PERMISSION,
                reason = "LocationManager usage detected — fine location permission required.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />"
            )
        ),
        Rule(
            "getLastKnownLocation",
            ScanFinding(
                name = "android.permission.ACCESS_COARSE_LOCATION",
                type = FindingType.PERMISSION,
                reason = "getLastKnownLocation requires at minimum coarse location.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />"
            )
        ),
        Rule(
            "Camera",
            ScanFinding(
                name = "android.permission.CAMERA",
                type = FindingType.PERMISSION,
                reason = "Camera API usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.CAMERA\" />"
            )
        ),
        Rule(
            "BluetoothAdapter",
            ScanFinding(
                name = "android.permission.BLUETOOTH_CONNECT",
                type = FindingType.PERMISSION,
                reason = "BluetoothAdapter requires BLUETOOTH_CONNECT on API 31+.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.BLUETOOTH_CONNECT\" />"
            )
        ),
        Rule(
            "WifiManager",
            ScanFinding(
                name = "android.permission.ACCESS_WIFI_STATE",
                type = FindingType.PERMISSION,
                reason = "WifiManager usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />"
            )
        ),
        Rule(
            "ConnectivityManager",
            ScanFinding(
                name = "android.permission.ACCESS_NETWORK_STATE",
                type = FindingType.PERMISSION,
                reason = "ConnectivityManager requires network state permission.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />"
            )
        ),
        Rule(
            "Vibrator",
            ScanFinding(
                name = "android.permission.VIBRATE",
                type = FindingType.PERMISSION,
                reason = "Vibrator usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.VIBRATE\" />"
            )
        ),
        Rule(
            "AudioRecord",
            ScanFinding(
                name = "android.permission.RECORD_AUDIO",
                type = FindingType.PERMISSION,
                reason = "AudioRecord requires microphone permission.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.RECORD_AUDIO\" />"
            )
        ),
        Rule(
            "MediaStore.Images",
            ScanFinding(
                name = "android.permission.READ_MEDIA_IMAGES",
                type = FindingType.PERMISSION,
                reason = "Reading images from MediaStore requires READ_MEDIA_IMAGES on API 33+.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.READ_MEDIA_IMAGES\" />"
            )
        ),
        Rule(
            "ContactsContract",
            ScanFinding(
                name = "android.permission.READ_CONTACTS",
                type = FindingType.PERMISSION,
                reason = "ContactsContract usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.READ_CONTACTS\" />"
            )
        ),
        Rule(
            "NotificationManagerCompat",
            ScanFinding(
                name = "android.permission.POST_NOTIFICATIONS",
                type = FindingType.PERMISSION,
                reason = "Posting notifications requires POST_NOTIFICATIONS on API 33+.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />"
            )
        ),
        Rule(
            "HttpURLConnection",
            ScanFinding(
                name = "android.permission.INTERNET",
                type = FindingType.PERMISSION,
                reason = "HTTP networking detected — INTERNET permission required.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.INTERNET\" />"
            )
        ),
        Rule(
            "OkHttpClient",
            ScanFinding(
                name = "android.permission.INTERNET",
                type = FindingType.PERMISSION,
                reason = "OkHttp networking detected — INTERNET permission required.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.INTERNET\" />"
            )
        ),
        Rule(
            "Retrofit",
            ScanFinding(
                name = "android.permission.INTERNET",
                type = FindingType.PERMISSION,
                reason = "Retrofit networking detected — INTERNET permission required.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "<uses-permission android:name=\"android.permission.INTERNET\" />"
            )
        ),
        // --- Gradle dependencies ---
        Rule(
            "NavHost",
            ScanFinding(
                name = "androidx.navigation:navigation-compose",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "NavHost/NavController usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"androidx.navigation:navigation-compose:2.7.7\")"
            )
        ),
        Rule(
            "ViewModel()",
            ScanFinding(
                name = "androidx.lifecycle:lifecycle-viewmodel-compose",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "viewModel() composable requires lifecycle-viewmodel-compose.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4\")"
            )
        ),
        Rule(
            "Room",
            ScanFinding(
                name = "androidx.room:room-runtime + room-ktx + room-compiler",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Room database usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = """implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")"""
            )
        ),
        Rule(
            "Hilt",
            ScanFinding(
                name = "com.google.dagger:hilt-android",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Hilt DI usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = """implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")"""
            )
        ),
        Rule(
            "Coil",
            ScanFinding(
                name = "io.coil-kt:coil-compose",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Coil image loading detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"io.coil-kt:coil-compose:2.6.0\")"
            )
        ),
        Rule(
            "Gson",
            ScanFinding(
                name = "com.google.code.gson:gson",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Gson JSON library detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"com.google.code.gson:gson:2.10.1\")"
            )
        ),
        Rule(
            "Moshi",
            ScanFinding(
                name = "com.squareup.moshi:moshi-kotlin",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Moshi JSON library detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"com.squareup.moshi:moshi-kotlin:1.15.1\")"
            )
        ),
        Rule(
            "WorkManager",
            ScanFinding(
                name = "androidx.work:work-runtime-ktx",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "WorkManager usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"androidx.work:work-runtime-ktx:2.9.0\")"
            )
        ),
        Rule(
            "DataStore",
            ScanFinding(
                name = "androidx.datastore:datastore-preferences",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "DataStore usage detected.",
                severity = Severity.REQUIRED,
                autoFixSnippet = "implementation(\"androidx.datastore:datastore-preferences:1.1.1\")"
            )
        ),
        Rule(
            "coroutineScope",
            ScanFinding(
                name = "org.jetbrains.kotlinx:kotlinx-coroutines-android",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Coroutines usage detected.",
                severity = Severity.RECOMMENDED,
                autoFixSnippet = "implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0\")"
            )
        ),
        Rule(
            "Flow",
            ScanFinding(
                name = "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                type = FindingType.GRADLE_DEPENDENCY,
                reason = "Kotlin Flow usage detected.",
                severity = Severity.RECOMMENDED,
                autoFixSnippet = "implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0\")"
            )
        ),
    )

    private val GENERIC_RULES: List<Rule> = listOf(
        Rule(
            "import java.net.HttpURLConnection",
            ScanFinding(
                name = "Network I/O",
                type = FindingType.GENERIC_IMPORT,
                reason = "HTTP networking found — ensure INTERNET permission is declared.",
                severity = Severity.RECOMMENDED
            )
        ),
        Rule(
            "import java.io.File",
            ScanFinding(
                name = "File I/O",
                type = FindingType.GENERIC_IMPORT,
                reason = "Direct File I/O detected — consider Scoped Storage / SAF on Android.",
                severity = Severity.OPTIONAL
            )
        ),
        Rule(
            "Log.d",
            ScanFinding(
                name = "Debug Logging",
                type = FindingType.GENERIC_IMPORT,
                reason = "Debug log calls found — strip before release build.",
                severity = Severity.OPTIONAL
            )
        ),
        Rule(
            "TODO()",
            ScanFinding(
                name = "Unfinished TODO",
                type = FindingType.GENERIC_IMPORT,
                reason = "TODO() calls throw NotImplementedError at runtime — resolve before shipping.",
                severity = Severity.REQUIRED
            )
        ),
        Rule(
            "System.out.println",
            ScanFinding(
                name = "System.out.println",
                type = FindingType.GENERIC_IMPORT,
                reason = "println found — replace with proper logging in production.",
                severity = Severity.OPTIONAL
            )
        ),
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Scan a map of [fileName -> fileContent] strings.
     * Returns a [ScanReport] with all de-duplicated findings.
     */
    fun scan(files: Map<String, String>): ScanReport {
        val start = System.currentTimeMillis()
        val projectType = detectProjectType(files)

        val rules = when (projectType) {
            ProjectType.ANDROID -> ANDROID_RULES + GENERIC_RULES
            else -> GENERIC_RULES
        }

        // De-duplicate by finding name so the same permission isn't listed 10×
        val seen = mutableSetOf<String>()
        val findings = mutableListOf<ScanFinding>()

        for ((fileName, content) in files) {
            val lines = content.lines()
            for (rule in rules) {
                if (rule.trigger in content && seen.add(rule.finding.name)) {
                    // Find the line number and snippet
                    val lineIndex = lines.indexOfFirst { rule.trigger in it }
                    val snippet = if (lineIndex >= 0) {
                        val start = maxOf(0, lineIndex - 1)
                        val end = minOf(lines.size - 1, lineIndex + 1)
                        lines.subList(start, end + 1).joinToString("\n")
                    } else null
                    findings.add(rule.finding.copy(
                        sourceFile = fileName,
                        sourceLine = if (lineIndex >= 0) lineIndex + 1 else null,
                        codeSnippet = snippet
                    ))
                }
            }
        }

        // Sort: REQUIRED first, then RECOMMENDED, then OPTIONAL
        findings.sortBy { it.severity.ordinal }

        return ScanReport(
            scannedFiles = files.keys.toList(),
            findings = findings,
            scanDurationMs = System.currentTimeMillis() - start,
            projectType = projectType
        )
    }

    private fun detectProjectType(files: Map<String, String>): ProjectType {
        val names = files.keys.map { it.lowercase() }
        return when {
            names.any { it.endsWith("androidmanifest.xml") } ||
            names.any { it.endsWith(".gradle") || it.endsWith(".gradle.kts") } ||
            files.values.any { "android.app.Activity" in it || "androidx." in it } ->
                ProjectType.ANDROID
            names.any { it.endsWith(".kt") || it.endsWith(".java") } ->
                ProjectType.GENERIC
            else -> ProjectType.UNKNOWN
        }
    }
}
