package com.viciousscan.app.model

import android.content.Context
import org.json.JSONObject
import java.io.File

data class ProjectInfo(
    val developerName: String = "",
    val email: String = "",
    val githubUsername: String = "",
    val repoUrl: String = "",
    val packageName: String = "",
    val appName: String = ""
)

object ProjectInfoRepository {
    private fun file(ctx: Context) = File(ctx.filesDir, "project_info.json")

    fun load(ctx: Context): ProjectInfo {
        val f = file(ctx)
        if (!f.exists()) return ProjectInfo()
        return try {
            val j = JSONObject(f.readText())
            ProjectInfo(
                developerName = j.optString("developerName"),
                email = j.optString("email"),
                githubUsername = j.optString("githubUsername"),
                repoUrl = j.optString("repoUrl"),
                packageName = j.optString("packageName"),
                appName = j.optString("appName")
            )
        } catch (e: Exception) { ProjectInfo() }
    }

    fun save(ctx: Context, info: ProjectInfo) {
        val j = JSONObject().apply {
            put("developerName", info.developerName)
            put("email", info.email)
            put("githubUsername", info.githubUsername)
            put("repoUrl", info.repoUrl)
            put("packageName", info.packageName)
            put("appName", info.appName)
        }
        file(ctx).writeText(j.toString())
    }
}
