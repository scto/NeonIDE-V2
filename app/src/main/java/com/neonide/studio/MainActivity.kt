package com.neonide.studio

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.neonide.studio.app.home.open.RecentProjectScreen
import com.neonide.studio.extensions.ExtensionsScreen
import com.neonide.studio.gitclone.GitCloneScreen
import com.neonide.studio.gitclone.GitViewModel
import com.neonide.studio.preference.AppSettingsScreen
import com.neonide.studio.preference.ApplyGradleSettings
import com.neonide.studio.preference.CodeRunnerScreen
import com.neonide.studio.preference.EditorSettingsScreen
import com.neonide.studio.preference.GradleSettingsScreen
import com.neonide.studio.preference.SettingsScreen
import com.neonide.studio.preference.SettingsState
import com.neonide.studio.projectwizard.CreateProjectScreen
import com.neonide.studio.projectwizard.FlutterProjectScreen
import com.neonide.studio.ui.components.AppButton
import com.neonide.studio.ui.components.AppCard
import com.neonide.studio.ui.components.AppIcon
import com.neonide.studio.ui.components.AppScaffold
import com.neonide.studio.ui.layout.AppBox
import com.neonide.studio.ui.layout.AppColumn
import com.neonide.studio.ui.layout.AppRow
import com.neonide.studio.ui.theme.AppTheme
import com.neonide.studio.ui.theme.ThemedContent
import com.neonide.studio.ui.theme.findActivity
import com.neonide.studio.ui.theme.rememberColorSchemeMode
import com.neonide.studio.ui.theme.rememberDynamicColorEnabled
import com.termux.app.TermuxActivity
import com.termux.app.TermuxInstaller
import com.termux.shared.termux.crash.TermuxCrashUtils
import kotlinx.serialization.Serializable

// route for navhost
@Serializable object PermissionRoute

@Serializable object MainScreenRoute

@Serializable object CreateProjectRoute

@Serializable object FlutterProjectRoute

@Serializable object IdeConfigRoute

@Serializable data class AppSettingsRoute(val title: String)

@Serializable data class EditorSettingsRoute(val title: String)

@Serializable data class GradleSettingsRoute(val title: String)

@Serializable data class CodeRunnerRoute(val title: String)

@Serializable object GitLayoutRoute

@Serializable object ExtensionsRoute

@Serializable object OpenProjectRoute

class MainActivity : ComponentActivity() {

    private var isFilesGranted by mutableStateOf(false)
    private var isInstallGranted by mutableStateOf(false)
    private var isNotificationsGranted by mutableStateOf(false)
    private var isSetupComplete by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initial check on startup
        updatePermissionStates()

        // skip if granted all
        isSetupComplete = isFilesGranted && isInstallGranted && isNotificationsGranted

        setContent {
            val colorSchemeMode = rememberColorSchemeMode()
            rememberDynamicColorEnabled()
            AppTheme(colorSchemeMode = colorSchemeMode) {
                ThemedContent(colorSchemeMode = colorSchemeMode) {
                    mainNavigation()
                }
            }
        }
    }

    @Composable
    private fun mainNavigation() {
        val backStack = remember {
            mutableStateListOf<Any>(if (isSetupComplete) MainScreenRoute else PermissionRoute)
        }
        LaunchedEffect(isSetupComplete) { if (isSetupComplete) backStack[0] = MainScreenRoute }
        val settings = remember { SettingsState(this@MainActivity) }

        LaunchedEffect(Unit) {
            ApplyGradleSettings()
            TermuxInstaller.setupBootstrapIfNeeded(this@MainActivity) {
                // p1
            }
        }

        AppScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                },
                popTransitionSpec = {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                },
                entryProvider = { key ->
                    when (key) {
                        is PermissionRoute -> NavEntry(key) {
                            permissionScreen()
                        }
                        is MainScreenRoute -> NavEntry(key) {
                            mainScreen(
                                onSetupDevKit = { DevKitSetup(this@MainActivity) },
                                onCreateProject = { backStack.add(CreateProjectRoute) },
                                onCreateFlutterProject = { backStack.add(FlutterProjectRoute) },
                                onOpenProject = { backStack.add(OpenProjectRoute) },
                                onCloneRepo = { backStack.add(GitLayoutRoute) },
                                onOpenTerminal = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            TermuxActivity::class.java
                                        )
                                    )
                                },
                                onOpenExtensions = { backStack.add(ExtensionsRoute) },
                                onOpenSettings = { backStack.add(IdeConfigRoute) },
                                onOpenAbout = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "NeonIDE Beta",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        is CreateProjectRoute -> NavEntry(key) {
                            CreateProjectScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        is FlutterProjectRoute -> NavEntry(key) {
                            FlutterProjectScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        is IdeConfigRoute -> NavEntry(key) {
                            SettingsScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onAppSettings = { title ->
                                    backStack.add(AppSettingsRoute(title))
                                },
                                onEditorSettings = { title ->
                                    backStack.add(EditorSettingsRoute(title))
                                },
                                onGradleSettings = { title ->
                                    backStack.add(GradleSettingsRoute(title))
                                },
                                onCodeRunner = { title ->
                                    backStack.add(CodeRunnerRoute(title))
                                },
                                settings = settings
                            )
                        }
                        is AppSettingsRoute -> NavEntry(key) {
                            AppSettingsScreen(
                                title = key.title,
                                onBack = { backStack.removeLastOrNull() },
                                settings = settings
                            )
                        }
                        is EditorSettingsRoute -> NavEntry(key) {
                            EditorSettingsScreen(
                                title = key.title,
                                onBack = { backStack.removeLastOrNull() },
                                settings = settings
                            )
                        }
                        is GradleSettingsRoute -> NavEntry(key) {
                            GradleSettingsScreen(
                                title = key.title,
                                onBack = { backStack.removeLastOrNull() },
                                settings = settings
                            )
                        }
                        is CodeRunnerRoute -> NavEntry(key) {
                            CodeRunnerScreen(
                                title = key.title,
                                onBack = { backStack.removeLastOrNull() },
                                settings = settings
                            )
                        }
                        is GitLayoutRoute -> NavEntry(key) {
                            val viewModel: GitViewModel = viewModel()
                            val state by viewModel.uiState.collectAsState()
                            GitCloneScreen(
                                onBack = { backStack.removeLastOrNull() },
                                state = state,
                                viewModel = viewModel,
                                onFinished = { backStack.removeLastOrNull() }
                            )
                        }
                        is ExtensionsRoute -> NavEntry(key) {
                            ExtensionsScreen(context = this@MainActivity, onBack = {
                                backStack.removeLastOrNull()
                            })
                        }
                        is OpenProjectRoute -> NavEntry(key) {
                            RecentProjectScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        else -> error("Unknown route: $key")
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, "MainActivity")
    }

    private fun updatePermissionStates() {
        isFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

        isInstallGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        isNotificationsGranted = NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    @Composable
    private fun permissionScreen() {
        AppBox(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                permissionContent()
            }
        }
    }

    @Composable
    private fun permissionContent() {
        val context = LocalContext.current
        val activity = context.findActivity() ?: this@MainActivity
        val allGranted = isFilesGranted && isInstallGranted && isNotificationsGranted

        AppColumn(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.permissions_required_to_continue),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            permissionItem(
                icon = painterResource(id = R.drawable.ic_files),
                title = stringResource(R.string.all_files_access),
                description = stringResource(R.string.all_files_access_desc),
                isGranted = isFilesGranted
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    ).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
            }

            permissionItem(
                icon = painterResource(id = R.drawable.ic_install),
                title = stringResource(R.string.install_unknown_apps),
                description = stringResource(R.string.install_unknown_apps_desc),
                isGranted = isInstallGranted
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
            }

            permissionItem(
                icon = painterResource(id = R.drawable.ic_notification),
                title = stringResource(R.string.notifications),
                description = stringResource(R.string.notifications_desc),
                isGranted = isNotificationsGranted
            ) {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                }
                activity.startActivity(intent)
            }

            AppButton(
                text = stringResource(R.string.continue_button),
                onClick = { isSetupComplete = true },
                enabled = allGranted,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    @Composable
    private fun permissionItem(
        icon: Painter,
        title: String,
        description: String,
        isGranted: Boolean,
        onClick: () -> Unit
    ) {
        AppRow(Modifier.fillMaxWidth()) {
            AppIcon(icon, size = 32.dp)

            Spacer(Modifier.width(16.dp))

            AppColumn(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, fontSize = 12.sp)
            }

            if (isGranted) {
                Text(
                    stringResource(R.string.granted),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            } else {
                AppButton(
                    text = stringResource(R.string.grant),
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
