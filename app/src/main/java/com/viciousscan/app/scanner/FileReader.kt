package com.viciousscan.app.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileReader {

    private val SUPPORTED_EXTENSIONS = setOf(
        "kt", "java", "xml", "gradle", "kts", "json",
        "py", "js", "ts", "tsx", "jsx", "cpp", "c", "h",
        "swift", "dart", "rb", "go", "rs", "toml", "yaml", "yml"
    )

    private const val MAX_FILES = 500
    private const val MAX_DEPTH = 10

    fun readTree(context: Context, treeUri: Uri): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        walkDocument(context, root, "", result, 0)
        return result
    }

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
        result: MutableMap<String, String>,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) return
        if (result.size >= MAX_FILES) return

        for (child in dir.listFiles()) {
            if (result.size >= MAX_FILES) return
            val childPath = if (path.isEmpty()) child.name ?: continue
                            else "$path/${child.name ?: continue}"
            when {
                child.isDirectory -> walkDocument(context, child, childPath, result, depth + 1)
                child.isFile -> {
                    val ext = childPath.substringAfterLast('.', "").lowercase()
                    if (ext in SUPPORTED_EXTENSIONS) {
                        try {
                            val content = context.contentResolver
                                .openInputStream(child.uri)
                                ?.bufferedReader()
                                ?.readText() ?: continue
                            result[childPath] = content
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }
}
