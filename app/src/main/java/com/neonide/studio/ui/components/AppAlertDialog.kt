package com.neonide.studio.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

data class DialogAction(val text: String, val onClick: () -> Unit)

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    actions: List<DialogAction> = emptyList(),
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        title = title,
        text = text,
        confirmButton = if (actions.isNotEmpty() || confirmText != null) {
            {
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) {
                        Text(action.text)
                    }
                }
                if (confirmText != null) {
                    TextButton(onClick = onConfirm ?: onDismissRequest) {
                        Text(confirmText)
                    }
                }
            }
        } else {
            {}
        },
        dismissButton = if (dismissText != null) {
            {
                TextButton(onClick = onDismiss ?: onDismissRequest) {
                    Text(dismissText)
                }
            }
        } else {
            {}
        }
    )
}

@Composable
fun AppAlertDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    actions: List<DialogAction> = emptyList(),
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium
) {
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        shape = shape,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium) },
        actions = actions,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        onDismiss = onDismiss
    )
}
