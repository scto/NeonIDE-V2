package com.neonide.studio.utils

import java.io.File

object SmartNdkScanner {

    const val COMPATIBLE_NDK_VERSION = "29.0.14206865"

    data class NdkMismatch(val filePath: String, val detectedVersion: String)

    fun validateProject(projectDir: File): Boolean {
        val hasGradle = File(projectDir, "build.gradle.kts").exists() ||
            File(projectDir, "settings.gradle.kts").exists() ||
            File(projectDir, "build.gradle").exists()
        val isFlutter = File(projectDir, "pubspec.yaml").exists() &&
            File(projectDir, "lib/main.dart").exists()
        return hasGradle || isFlutter
    }

    private val TARGET_FILES = setOf(
        "build.gradle",
        "build.gradle.kts",
        "gradle.properties",
        "local.properties"
    )
    private val EXCLUDE_DIRS = setOf(
        ".git",
        ".gradle",
        "build",
        "node_modules",
        ".idea",
        "gradle/wrapper"
    )

    private val NDK_VERSION_REGEX = Regex(
        """(?:ndkVersion\s*[=:]?\s*["']|ndkVersion\s*=\s*|ndk\s*[.=:]\s*["']?)(\d+\.\d+\.\d+)"""
    )

    private fun findFile(projectDir: File): Sequence<File> = projectDir.walk()
        .onEnter { it.name !in EXCLUDE_DIRS }
        .filter { it.isFile && it.name in TARGET_FILES }

    private fun findNdkVersionInContent(content: String): Set<String> {
        val versions = mutableSetOf<String>()
        NDK_VERSION_REGEX.findAll(content).forEach { match ->
            versions.add(match.groupValues[1])
        }
        return versions
    }

    private fun processNdkFiles(
        projectDir: File,
        onFile: (file: File, content: String, mismatchedVersions: List<String>) -> Unit
    ) {
        for (file in findFile(projectDir)) {
            val content = file.readText()
            val mismatches = findNdkVersionInContent(content)
                .filter { it != COMPATIBLE_NDK_VERSION }
            if (mismatches.isNotEmpty()) {
                onFile(file, content, mismatches)
            }
        }
    }

    fun findNdkMismatches(projectDir: File): List<NdkMismatch> {
        val mismatches = mutableListOf<NdkMismatch>()
        processNdkFiles(projectDir) { file, _, versions ->
            for (version in versions) {
                mismatches.add(NdkMismatch(file.path, version))
            }
        }
        return mismatches
    }

    fun applyCompatibleNdk(projectDir: File): Int {
        var changed = 0
        processNdkFiles(projectDir) { file, content, versions ->
            var updated = content
            for (version in versions) {
                updated = updated.replace(version, COMPATIBLE_NDK_VERSION)
            }
            file.writeText(updated)
            changed++
        }
        return changed
    }
}
