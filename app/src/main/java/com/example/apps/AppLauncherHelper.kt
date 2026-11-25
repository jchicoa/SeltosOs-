package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.model.InstalledAppInfo

/**
 * Discovers installed Android apps and web browsers, launching any selected app or
 * external browser in Android Multi-Window Adjacent Split-Screen or full screen.
 */
object AppLauncherHelper {

    fun queryLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                InstalledAppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun queryInstalledBrowsers(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                InstalledAppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun openUrlInBrowser(
        context: Context,
        url: String,
        browserPackageName: String? = null,
        splitScreen: Boolean = true
    ): Boolean {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) {
            url.trim()
        } else {
            "https://${url.trim()}"
        }
        val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (!browserPackageName.isNullOrBlank()) {
                setPackage(browserPackageName)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (splitScreen) {
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
        }
        return try {
            if (browserPackageName.isNullOrBlank()) {
                val chooser = Intent.createChooser(baseIntent, "Abrir en navegador").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } else {
                context.startActivity(baseIntent)
            }
            true
        } catch (_: Exception) {
            runCatching {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }

    fun launchInSplitScreen(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            try {
                val fallback = pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: return false
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun launchNavigationSplit(context: Context): Boolean {
        return launchInSplitScreen(context, "com.waze") ||
            launchInSplitScreen(context, "com.google.android.apps.maps") ||
            runCatching {
                val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                }
                context.startActivity(geo)
                true
            }.getOrDefault(false)
    }
}
