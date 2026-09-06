package com.neonide.studio.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neonide.studio.R
import com.neonide.studio.ui.components.AppSurface
import com.neonide.studio.ui.layout.AppColumn
import com.neonide.studio.ui.layout.AppRow
import com.neonide.studio.utils.SmartNdkScanner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ANIM_DURATION_MS = 500

@Composable
fun SmartNdkSlidePanel(
    projectPath: File,
    detectedVersion: String?,
    visible: Boolean,
    onDismissed: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val onApply: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) {
                SmartNdkScanner.applyCompatibleNdk(projectPath)
            }
            onDismissed()
        }
    }

    val versionLabel = "${detectedVersion ?: "?"} \u2192 ${SmartNdkScanner.COMPATIBLE_NDK_VERSION}"

    val onCancel: () -> Unit = {
        scope.launch {
            delay(ANIM_DURATION_MS.toLong())
            onDismissed()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(durationMillis = ANIM_DURATION_MS),
            initialOffsetX = { -it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = ANIM_DURATION_MS),
            targetOffsetX = { -it }
        )
    ) {
        AppSurface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            AppColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                AppRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSurface(
                        modifier = Modifier
                            .width(3.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                    Text(
                        text = "${stringResource(R.string.smart_ndk_detected)} $versionLabel",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).basicMarquee()
                    )
                    TextButton(onClick = onCancel) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    TextButton(onClick = onApply) {
                        Text(
                            text = stringResource(R.string.apply),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
