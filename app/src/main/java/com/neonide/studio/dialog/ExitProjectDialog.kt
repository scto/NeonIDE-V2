package com.neonide.studio.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neonide.studio.R
import com.neonide.studio.ui.components.AppAlertDialog

@Composable
fun ExitProjectDialog(onClose: () -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
        title = stringResource(R.string.exit_project),
        text = stringResource(R.string.exit_project_message),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.close),
        onConfirm = onClose,
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismiss
    )
}
