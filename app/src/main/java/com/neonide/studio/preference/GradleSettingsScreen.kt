package com.neonide.studio.preference

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neonide.studio.R
import com.neonide.studio.ui.components.AppIcon
import com.neonide.studio.ui.components.AppListItem
import com.neonide.studio.ui.components.AppSwitch
import com.neonide.studio.ui.components.AppTopBar
import com.neonide.studio.ui.layout.AppColumn
import com.neonide.studio.ui.layout.AppLazyColumn
import com.neonide.studio.utils.SmartNdkScanner
import com.termux.shared.termux.TermuxConstants
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val DEFAULT_GRADLE = """
    android.aapt2FromMavenOverride=${TermuxConstants.TERMUX_HOME_DIR_PATH}/android-sdk/build-tools/36.0.0/aapt2
""".trimIndent()
private const val PARALLEL_EXECUTION = "org.gradle.parallel=true"
private const val GRADLE_DAEMON = "org.gradle.daemon=true"

private val COMPATIBLE_NDK_VERSION = SmartNdkScanner.COMPATIBLE_NDK_VERSION

@Composable
fun GradleSettingsScreen(title: String, onBack: () -> Unit, settings: SettingsState) {
    val scope = rememberCoroutineScope()
    val gradleDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".gradle")

    fun toggleProperty(enabled: Boolean, fileName: String, line: String) {
        scope.launch(Dispatchers.IO) {
            if (!gradleDir.exists()) gradleDir.mkdirs()
            val file = File(gradleDir, fileName)
            if (enabled) file.addLine(line) else file.removeLine(line)
        }
    }

    AppColumn(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = title,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    AppIcon(painterResource(R.drawable.ic_chevron_left))
                }
            }
        )
        AppLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                AppListItem(
                    headlineText = stringResource(R.string.aapt2_ovveride),
                    supportingText = stringResource(R.string.aapt2_ovveride_desc),
                    trailingContent = {
                        AppSwitch(
                            checked = settings.isAapt2OverrideEnabled,
                            onCheckedChange = {
                                settings.isAapt2OverrideEnabled = it
                                toggleProperty(it, "gradle.properties", DEFAULT_GRADLE)
                            }
                        )
                    }
                )
            }
            item {
                AppListItem(
                    headlineText = stringResource(R.string.ndk_dir),
                    supportingText = stringResource(R.string.ndk_dir_desc),
                    trailingContent = {
                        AppSwitch(
                            checked = settings.isSmartNdkVersioningEnabled,
                            onCheckedChange = {
                                settings.isSmartNdkVersioningEnabled = it
                            }
                        )
                    }
                )
            }
            item {
                AppListItem(
                    headlineText = stringResource(R.string.parallel_execution),
                    supportingText = stringResource(R.string.parallel_execution_desc),
                    trailingContent = {
                        AppSwitch(
                            checked = settings.isParallelExecutionEnabled,
                            onCheckedChange = {
                                settings.isParallelExecutionEnabled = it
                                toggleProperty(it, "gradle.properties", PARALLEL_EXECUTION)
                            }
                        )
                    }
                )
            }
            item {
                AppListItem(
                    headlineText = stringResource(R.string.gradle_daemon),
                    supportingText = stringResource(R.string.gradle_daemon_desc),
                    trailingContent = {
                        AppSwitch(
                            checked = settings.isGradleDaemonEnabled,
                            onCheckedChange = {
                                settings.isGradleDaemonEnabled = it
                                toggleProperty(it, "gradle.properties", GRADLE_DAEMON)
                            }
                        )
                    }
                )
            }
        }
    }
}

fun ApplyGradleSettings() {
    val gradleDir = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".gradle")
    if (!gradleDir.exists()) gradleDir.mkdirs()
    val gradleFile = File(gradleDir, "gradle.properties")
    if (!gradleFile.exists()) {
        gradleFile.writeText("$DEFAULT_GRADLE\n$PARALLEL_EXECUTION\n$GRADLE_DAEMON\n")
    }
}

private fun File.addLine(line: String) {
    val existing = if (exists()) readText() else ""
    if (!existing.contains(line)) {
        val prefix = if (existing.endsWith("\n") || existing.isEmpty()) "" else "\n"
        writeText("$existing$prefix$line\n")
    }
}

private fun File.removeLine(line: String) {
    if (!exists()) return
    val lines = readText().lines().filter { it.trim() != line.trim() }
    writeText(lines.joinToString("\n").trimEnd() + "\n")
}
