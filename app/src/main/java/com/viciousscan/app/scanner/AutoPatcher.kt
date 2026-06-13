package com.viciousscan.app.scanner

import android.content.Context
import android.net.Uri
import com.viciousscan.app.model.FindingType
import com.viciousscan.app.model.ScanFinding

/**
 * Attempts to auto-patch AndroidManifest.xml and build.gradle(.kts) files
 * by inserting the [ScanFinding.autoFixSnippet] at the correct location.
 *
 * IMPORTANT: This always works on a copy of the content — it never blindly
 * overwrites until the caller confirms and writes back via [writeBack].
 */
object AutoPatcher {

    data class PatchPreview(
        val fileName: String,
        val uri: Uri,
        val originalContent: String,
        val patchedContent: String,
        val appliedFindings: List<ScanFinding>
    )

    /**
     * Produce patch previews for all patchable findings.
     * The caller can review each preview before calling [writeBack].
     */
    fun buildPreviews(
        manifestUri: Uri?,
        manifestContent: String?,
        gradleUri: Uri?,
        gradleContent: String?,
        findings: List<ScanFinding>
    ): List<PatchPreview> {
        val previews = mutableListOf<PatchPreview>()

        val permissionFindings = findings.filter {
            it.type == FindingType.PERMISSION && it.autoFixSnippet != null
        }
        val dependencyFindings = findings.filter {
            it.type == FindingType.GRADLE_DEPENDENCY && it.autoFixSnippet != null
        }

        if (manifestUri != null && manifestContent != null && permissionFindings.isNotEmpty()) {
            val patched = patchManifest(manifestContent, permissionFindings)
            if (patched != manifestContent) {
                previews.add(
                    PatchPreview(
                        fileName = "AndroidManifest.xml",
                        uri = manifestUri,
                        originalContent = manifestContent,
                        patchedContent = patched,
                        appliedFindings = permissionFindings
                    )
                )
            }
        }

        if (gradleUri != null && gradleContent != null && dependencyFindings.isNotEmpty()) {
            val patched = patchGradle(gradleContent, dependencyFindings)
            if (patched != gradleContent) {
                previews.add(
                    PatchPreview(
                        fileName = "build.gradle.kts",
                        uri = gradleUri,
                        originalContent = gradleContent,
                        patchedContent = patched,
                        appliedFindings = dependencyFindings
                    )
                )
            }
        }

        return previews
    }

    /** Write patched content back to the SAF URI. */
    fun writeBack(context: Context, preview: PatchPreview): Boolean {
        return try {
            context.contentResolver.openOutputStream(preview.uri, "wt")
                ?.bufferedWriter()
                ?.use { it.write(preview.patchedContent) }
            true
        } catch (e: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun patchManifest(content: String, findings: List<ScanFinding>): String {
        var result = content
        val insertMarker = "<application"
        for (finding in findings) {
            val snippet = finding.autoFixSnippet ?: continue
            // Skip if the permission name is already present
            if (finding.name in result) continue
            val idx = result.indexOf(insertMarker)
            if (idx == -1) continue
            result = result.substring(0, idx) +
                    "    $snippet\n\n    " +
                    result.substring(idx)
        }
        return result
    }

    private fun patchGradle(content: String, findings: List<ScanFinding>): String {
        var result = content
        val insertMarker = "dependencies {"
        for (finding in findings) {
            val snippet = finding.autoFixSnippet ?: continue
            // Skip if already present
            if (finding.name.substringBefore(":") in result) continue
            val idx = result.indexOf(insertMarker)
            if (idx == -1) continue
            val insertAt = result.indexOf('\n', idx) + 1
            result = result.substring(0, insertAt) +
                    "    $snippet\n" +
                    result.substring(insertAt)
        }
        return result
    }
}
