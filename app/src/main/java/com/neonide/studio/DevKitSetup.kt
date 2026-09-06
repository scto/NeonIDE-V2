package com.neonide.studio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.system.Os
import com.termux.app.TermuxActivity
import com.termux.app.TermuxService
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE
import java.io.File

fun DevKitSetup(activity: Activity) {
    val script = File(TermuxConstants.TERMUX_HOME_DIR_PATH, "setup.sh")
    setupFile(activity, script, "setup.sh")
    Os.chmod(script.absolutePath, 455)
    runSetup(activity, script)
    activity.startActivity(Intent(activity, TermuxActivity::class.java))
}

fun setupFile(activity: Activity, destinationFile: File, setupPath: String) {
    destinationFile.parentFile?.mkdirs()
    activity.assets.open(setupPath).use {
        it.copyTo(destinationFile.outputStream())
    }
}

fun runSetup(activity: Activity, scriptPath: File) = activity.startService(
    Intent(
        TERMUX_SERVICE.ACTION_SERVICE_EXECUTE,
        Uri.parse("file://${TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH}/bash")
    )
        .setClass(activity, TermuxService::class.java)
        .putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, arrayOf(scriptPath.absolutePath))
        .putExtra(TERMUX_SERVICE.EXTRA_SHELL_NAME, activity.getString(R.string.setup_devkit))
)
