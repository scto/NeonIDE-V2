package com.neonide.studio.dialog

import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neonide.studio.EditorActivity
import com.neonide.studio.R
import com.neonide.studio.projectwizard.ProjectValidators
import com.neonide.studio.ui.components.AppAlertDialog
import com.neonide.studio.ui.components.FormTextField
import com.neonide.studio.ui.components.ListAlertDialog
import com.neonide.studio.ui.theme.findActivity
import com.termux.shared.termux.TermuxConstants
import java.io.File

@Composable
fun NewProjectDialog(
    onCreateTemplate: () -> Unit,
    onCreateFlutterProject: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showEmptyFolderDialog by remember { mutableStateOf(false) }
    var emptyFolderName by remember { mutableStateOf("") }
    var emptyFolderNameError by remember { mutableStateOf<String?>(null) }

    val projectsDir = remember { File(TermuxConstants.TERMUX_HOME_DIR, "projects") }
    val flutterDir = remember { File(TermuxConstants.TERMUX_PREFIX_DIR, "opt/flutter") }

    fun resetDialogState() {
        showEmptyFolderDialog = false
        emptyFolderName = ""
        emptyFolderNameError = null
    }

    ListAlertDialog(
        title = stringResource(R.string.new_project),
        items = listOf(
            stringResource(R.string.empty_folder),
            stringResource(R.string.android_template),
            stringResource(R.string.flutter_project)
        ),
        onItemClick = { index ->
            when (index) {
                0 -> showEmptyFolderDialog = true
                1 -> {
                    onDismiss()
                    onCreateTemplate()
                }
                2 -> {
                    if (!flutterDir.exists()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.flutter_not_installed),
                            Toast.LENGTH_LONG
                        ).show()
                        return@ListAlertDialog
                    }
                    onDismiss()
                    onCreateFlutterProject()
                }
            }
        },
        onDismissRequest = onDismiss
    )
    if (showEmptyFolderDialog) {
        AppAlertDialog(
            onDismissRequest = {
                resetDialogState()
            },
            title = { Text(stringResource(R.string.empty_folder)) },
            text = {
                FormTextField(
                    value = emptyFolderName,
                    onValueChange = {
                        emptyFolderName = it
                        emptyFolderNameError = null
                    },
                    label = stringResource(R.string.project_name),
                    isError = emptyFolderNameError != null,
                    supportingText = emptyFolderNameError
                )
            },
            confirmText = stringResource(R.string.create),
            onConfirm = {
                val name = emptyFolderName.trim()
                if (!ProjectValidators.isValidProjectName(name)) {
                    emptyFolderNameError = context.getString(
                        R.string.create_project_error_invalid_name
                    )
                    return@AppAlertDialog
                }
                val folder = File(projectsDir, name)
                if (folder.exists()) {
                    emptyFolderNameError = context.getString(
                        R.string.create_project_error_dir_exists
                    )
                    return@AppAlertDialog
                }
                projectsDir.mkdirs()
                folder.mkdirs()
                resetDialogState()
                onDismiss()
                val activity = context.findActivity() ?: return@AppAlertDialog
                val intent = Intent(activity, EditorActivity::class.java).apply {
                    putExtra(EditorActivity.EXTRA_PROJECT_DIR, folder.absolutePath)
                }
                activity.startActivity(intent)
            },
            dismissText = stringResource(R.string.cancel),
            onDismiss = {
                resetDialogState()
            }
        )
    }
}
