package com.viciousscan.app.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Reads source files from a SAF (Storage Access Framework) tree URI.
 * Returns a map of relative path → file content for all supported extensions.
 */
object FileReader {

    private val SUPPORTED_EXTENSIONS = setOf(
        "kt", "java", "xml", "gradle", "kts", "json",
        "py", "js", "ts", "tsx", "jsx", "cpp", "c", "h",
        "swift", "dart", "rb", "go", "rs", "toml", "yaml", "yml"
    )

    /**
     * Walk [treeUri] recursively and read all supported source files.
     * Returns an empty map if the URI is not a directory tree.
     */
    fun readTree(context: Context, treeUri: Uri): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        walkDocument(context, root, "", result)
        return result
    }

    /**
     * Read a single file URI.
     */
    fun readSingleFile(context: Context, uri: Uri, displayName: String): Map<String, String> {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (ext !in SUPPORTED_EXTENSIONS) return emptyMap()
        return try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readText() ?: return emptyMap()
            mapOf(displayName to content)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun walkDocument(
        context: Context,
        dir: DocumentFile,
        path: String,
        result: MutableMap<String, String>
    ) {
        for (child in dir.listFiles()) {
            val childPath = if (path.isEmpty()) child.name ?: continue
                           else "$path/${child.name ?: continue}"
            when {
                child.isDirectory -> walkDocument(context, child, childPath, result)
                child.isFile -> {
                    val ext = childPath.substringAfterLast('.', "").lowercase()
                    if (ext in SUPPORTED_EXTENSIONS) {
                        try {
                            val content = context.contentResolver
                                .openInputStream(child.uri)
                                ?.bufferedReader()
                                ?.readText() ?: continue
                            result[childPath] = content
                        } catch (_: Exception) { /* skip unreadable files */ }
                    }
                }
            }
        }
    }
}
