package com.neonide.studio.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neonide.studio.R
import com.neonide.studio.ui.components.AppIcon
import com.neonide.studio.ui.components.AppListItem
import com.neonide.studio.ui.components.AppSwitch
import com.neonide.studio.ui.components.AppTopBar
import com.neonide.studio.ui.components.RadioListDialog
import com.neonide.studio.ui.layout.AppColumn
import com.neonide.studio.ui.layout.AppLazyColumn

private val RUN_OUTPUT_OPTIONS = listOf("Output", "Terminal")
private val RUN_OUTPUT_VALUES = listOf(RunOutputMode.OUTPUT, RunOutputMode.TERMINAL)

@Composable
fun CodeRunnerScreen(title: String, onBack: () -> Unit, settings: SettingsState) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedIndex = RUN_OUTPUT_VALUES.indexOf(settings.runOutputMode).coerceAtLeast(0)

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
                    headlineText = stringResource(R.string.enable_coderun),
                    supportingText = stringResource(R.string.enable_coderun_desc),
                    trailingContent = {
                        AppSwitch(
                            checked = settings.isCodeRunEnabled,
                            onCheckedChange = { settings.isCodeRunEnabled = it }
                        )
                    }
                )
            }
            item {
                AppListItem(
                    modifier = Modifier.clickable { showDialog = true },
                    headlineText = stringResource(R.string.run_output),
                    supportingText = stringResource(R.string.run_output_desc),
                    trailingText = RUN_OUTPUT_OPTIONS[selectedIndex]
                )
            }
        }
    }

    if (showDialog) {
        RadioListDialog(
            title = stringResource(R.string.run_output),
            items = RUN_OUTPUT_OPTIONS,
            selectedIndex = selectedIndex,
            onItemClick = { index ->
                settings.runOutputMode = RUN_OUTPUT_VALUES[index]
                showDialog = false
            },
            onDismissRequest = { showDialog = false }
        )
    }
}
