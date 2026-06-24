package com.viciousscan.app.model

/**
 * Represents a single permission or dependency finding from a scan.
 */
data class ScanFinding(
    val name: String,
    val type: FindingType,
    val reason: String,
    val severity: Severity = Severity.RECOMMENDED,
    val autoFixSnippet: String? = null,   // null = manual-only
    val codeSnippet: String? = null,      // actual line(s) from source
    val sourceFile: String? = null,       // file where trigger was found
    val sourceLine: Int? = null           // line number of trigger
)

enum class FindingType {
    PERMISSION,
    GRADLE_DEPENDENCY,
    MANIFEST_ATTRIBUTE,
    GENERIC_IMPORT
}

enum class Severity {
    REQUIRED,       // App will crash / not compile without this
    RECOMMENDED,    // Best practice or missing for used API
    OPTIONAL        // Nice-to-have enhancement
}

/**
 * Aggregate result returned after scanning a project or file set.
 */
data class ScanReport(
    val scannedFiles: List<String>,
    val findings: List<ScanFinding>,
    val scanDurationMs: Long,
    val projectType: ProjectType
)

enum class ProjectType {
    ANDROID,
    GENERIC,
    UNKNOWN
}

data class ScanHistoryEntry(
    val id: String,
    val timestamp: Long,
    val targetPath: String,
    val projectType: ProjectType,
    val totalFindings: Int,
    val requiredCount: Int,
    val recommendedCount: Int,
    val optionalCount: Int,
    val findings: List<ScanFinding>
)
