package com.neonide.studio.projectwizard

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neonide.studio.EditorActivity
import com.neonide.studio.R
import com.neonide.studio.ui.components.AppButton
import com.neonide.studio.ui.components.AppIcon
import com.neonide.studio.ui.components.AppTopBar
import com.neonide.studio.ui.components.DropdownField
import com.neonide.studio.ui.components.FormTextField
import com.neonide.studio.ui.layout.AppColumn
import com.neonide.studio.ui.layout.AppRow
import com.neonide.studio.ui.theme.findActivity
import com.neonide.studio.utils.GradleProjectActions
import com.neonide.studio.utils.GradleRunner
import com.neonide.studio.utils.rememberDirectoryLauncher
import com.termux.shared.logger.Logger
import com.termux.shared.termux.TermuxConstants
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FlutterProjectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectName by remember { mutableStateOf("Untitled") }
    var description by remember { mutableStateOf("A new Flutter project.") }
    var packageName by remember { mutableStateOf("com.example") }
    var saveLocation by remember {
        mutableStateOf(File(TermuxConstants.TERMUX_HOME_DIR, "projects").absolutePath)
    }
    var language by remember { mutableStateOf("Kotlin") }
    val platforms = remember {
        mutableStateOf(
            mapOf(
                "Android" to true,
                "iOS" to false,
                "Linux" to false,
                "macOS" to false,
                "Web" to false,
                "Windows" to false
            )
        )
    }
    var projectNameError by remember { mutableStateOf<String?>(null) }
    var packageNameError by remember { mutableStateOf<String?>(null) }
    var saveLocationError by remember { mutableStateOf<String?>(null) }

    val folderPickerLauncher = rememberDirectoryLauncher { file ->
        saveLocation = file.absolutePath
        saveLocationError = null
    }

    AppColumn(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(id = R.string.flutter_project),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    AppIcon(painter = painterResource(id = R.drawable.ic_chevron_left))
                }
            }
        )

        AppColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FormTextField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    projectNameError = null
                },
                label = stringResource(id = R.string.project_name),
                leadingIcon = painterResource(id = R.drawable.ic_add),
                isError = projectNameError != null,
                supportingText = projectNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            FormTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(id = R.string.description),
                leadingIcon = painterResource(id = R.drawable.ic_add),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            FormTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    packageNameError = null
                },
                label = stringResource(id = R.string.package_name),
                leadingIcon = painterResource(id = R.drawable.ic_language_android),
                isError = packageNameError != null,
                supportingText = packageNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            FormTextField(
                value = saveLocation,
                onValueChange = { saveLocation = it },
                label = stringResource(id = R.string.project_location),
                leadingIcon = painterResource(id = R.drawable.ic_folder),
                trailingIcon = {
                    IconButton(onClick = {
                        folderPickerLauncher.launch(null)
                    }) {
                        AppIcon(painter = painterResource(id = R.drawable.ic_folder))
                    }
                },
                isError = saveLocationError != null,
                supportingText = saveLocationError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            DropdownField(
                label = stringResource(id = R.string.language),
                options = listOf("Kotlin", "Java"),
                selectedOption = language,
                onOptionSelected = { language = it },
                leadingIcon = if (language == "Java") {
                    painterResource(id = R.drawable.ic_filetype_java)
                } else {
                    painterResource(id = R.drawable.ic_filetype_kotlin)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            Text(
                text = stringResource(id = R.string.platforms),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            platforms.value.toList().chunked(3).forEach { row ->
                AppRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { (name, checked) ->
                        AppRow(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    platforms.value = platforms.value + (name to it)
                                }
                            )
                            Text(name)
                        }
                    }
                }
            }

            AppRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(id = R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(onClick = {
                    scope.launch {
                        if (!ProjectValidators.isValidProjectName(projectName)) {
                            projectNameError =
                                context.getString(R.string.create_project_error_invalid_name)
                            return@launch
                        }
                        if (!ProjectValidators.isValidPackageName(packageName)) {
                            packageNameError =
                                context.getString(R.string.create_project_error_invalid_package)
                            return@launch
                        }
                        val base = File(saveLocation)
                        val projectDir = File(base, projectName.lowercase())
                        if (projectDir.exists()) {
                            saveLocationError =
                                context.getString(R.string.create_project_error_dir_exists)
                            return@launch
                        }
                        base.mkdirs()
                        val args = listOf(
                            "create",
                            projectName.lowercase(),
                            "--org",
                            packageName,
                            "--android-language",
                            language.lowercase(),
                            "--description",
                            "'$description'",
                            "--platforms=${platforms.value.filterValues {
                                it
                            }.keys.joinToString(",").lowercase()}"
                        )
                        Logger.logDebug(
                            "FlutterWizard",
                            "flutter create: args=$args dir=$saveLocation"
                        )
                        val handle = withContext(Dispatchers.IO) {
                            GradleRunner.start(
                                projectDir = base,
                                args = args,
                                envOverrides = GradleProjectActions.getGradleEnvironment(context),
                                onOutputLine = { Logger.logDebug("FlutterWizard", "flutter: $it") },
                                executable = "flutter"
                            )
                        }
                        val result = withContext(Dispatchers.IO) { handle.waitFor() }
                        Logger.logDebug(
                            "FlutterWizard",
                            "flutter create exit=${result.exitCode} cancelled=${result.wasCancelled}"
                        )
                        if (result.isSuccessful) {
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.create_project_success,
                                    projectDir.absolutePath
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                            val activity = context.findActivity() ?: return@launch
                            val intent = Intent(activity, EditorActivity::class.java).apply {
                                putExtra(
                                    EditorActivity.EXTRA_PROJECT_DIR,
                                    projectDir.absolutePath
                                )
                            }
                            activity.startActivity(intent)
                            onBack()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }) {
                    Text(stringResource(id = R.string.create))
                }
            }
        }
    }
}
