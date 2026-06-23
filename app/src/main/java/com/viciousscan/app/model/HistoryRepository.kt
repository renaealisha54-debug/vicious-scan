package com.viciousscan.app.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HistoryRepository {
    private fun file(ctx: Context) = File(ctx.filesDir, "scan_history.json")

    fun load(ctx: Context): List<ScanHistoryEntry> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { arr.getJSONObject(it).toEntry() }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    fun save(ctx: Context, entry: ScanHistoryEntry) {
        val f = file(ctx)
        val list = load(ctx).filter { it.id != entry.id }.toMutableList()
        list.add(0, entry)
        val arr = JSONArray()
        list.take(50).forEach { arr.put(it.toJson()) }
        f.writeText(arr.toString())
    }

    fun delete(ctx: Context, id: String) {
        val f = file(ctx)
        val arr = JSONArray()
        load(ctx).filter { it.id != id }.forEach { arr.put(it.toJson()) }
        f.writeText(arr.toString())
    }

    fun clear(ctx: Context) = file(ctx).delete()

    private fun ScanHistoryEntry.toJson() = JSONObject().apply {
        put("id", id); put("timestamp", timestamp); put("targetPath", targetPath)
        put("projectType", projectType.name); put("totalFindings", totalFindings)
        put("requiredCount", requiredCount); put("recommendedCount", recommendedCount)
        put("optionalCount", optionalCount)
        val fa = JSONArray()
        findings.forEach { f ->
            fa.put(JSONObject().apply {
                put("name", f.name); put("type", f.type.name)
                put("reason", f.reason); put("severity", f.severity.name)
                put("autoFixSnippet", f.autoFixSnippet ?: "")
            })
        }
        put("findings", fa)
    }

    private fun JSONObject.toEntry(): ScanHistoryEntry {
        val fa = getJSONArray("findings")
        val findings = (0 until fa.length()).map {
            val f = fa.getJSONObject(it)
            ScanFinding(
                name = f.getString("name"),
                type = FindingType.valueOf(f.getString("type")),
                reason = f.getString("reason"),
                severity = Severity.valueOf(f.getString("severity")),
                autoFixSnippet = f.getString("autoFixSnippet").ifEmpty { null }
            )
        }
        return ScanHistoryEntry(
            id = getString("id"), timestamp = getLong("timestamp"),
            targetPath = getString("targetPath"),
            projectType = ProjectType.valueOf(getString("projectType")),
            totalFindings = getInt("totalFindings"), requiredCount = getInt("requiredCount"),
            recommendedCount = getInt("recommendedCount"), optionalCount = getInt("optionalCount"),
            findings = findings
        )
    }
}
