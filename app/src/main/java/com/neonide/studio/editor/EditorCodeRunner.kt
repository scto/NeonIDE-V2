package com.neonide.studio.editor

import android.app.Activity
import com.neonide.studio.R
import com.neonide.studio.editor.bottomsheet.BottomSheetViewModel
import com.neonide.studio.editor.bottomsheet.buildoutput.BuildOutputBuffer
import com.neonide.studio.preference.RunOutputMode
import com.neonide.studio.utils.GradleBuildStatus
import com.neonide.studio.utils.GradleProjectActions
import com.neonide.studio.utils.GradleService
import com.termux.shared.termux.TermuxConstants
import java.io.File
import java.io.IOException

class EditorCodeRunner(
    private val activity: Activity,
    private val bottomSheetVm: BottomSheetViewModel
) {
    fun runCommandForFile(filePath: String): String? {
        val file = File(filePath)
        return when (file.extension.lowercase()) {
            "awk" -> "awk -f $file"
            "sh", "bash" -> "bash $file"
            "rs" -> "cargo run"
            "c" -> "clang $file && ./a.out && rm a.out"
            "cpp" -> "clang++ $file && ./a.out && rm a.out"
            "coffee" -> "coffee $file"
            "lm" -> " mkdir -p dir && colm -o dir/file $file && ./dir/file && rm -fr dir"
            "cr" -> "crystal run $file"
            "dash" -> "dash $file"
            "dart" -> "dart $file"
            "exs" -> "elixir $file"
            "elv" -> "elvish $file"
            "el" -> "emacs --batch -l $file"
            "erl" -> "escript $file"
            "fish" -> "fish $file"
            "go" -> "go run $file"
            "groovy" -> "groovy $file"
            "scm" -> "guile $file"
            "java" -> "java $file"
            "js" -> "node $file"
            "kt" ->
                "kotlinc $file -include-runtime -d $file.jar && java -jar $file.jar && rm $file.jar"
            "kts" -> "kotlin $file"
            "f90" -> "lfortran $file && rm $file.out"
            "lua" -> "luajit $file"
            "cs" -> "mcs $file && mono $file.exe && rm $file.exe"
            "mksh" -> "mksh $file"
            "nim" -> "nim r $file"
            "nu" -> "nu $file"
            "pl" -> "perl $file"
            "php" -> "termux-open http://localhost:8000/${file.name} && php -S localhost:8000"
            "py" -> "python $file"
            "rkt" -> "racket $file"
            "rl" ->
                "mkdir -p dir && ragel $file -o dir/file.c && clang dir/file.c -o dir/file" +
                    " && ./dir/file && rm -fr dir"
            "hs" -> "runghc $file"
            "rb" -> "ruby $file"
            "scss" -> "sassc $file"
            "pro" -> "swipl -g 'qsave_program(file, [goal(main)])' -t halt \"$file\" && rm file"
            "tcsh" -> "tcsh $file"
            "tcl" -> "tclsh $file"
            "wat" -> "wasmtime run $file"
            "zig" -> "zig run $file"
            "zsh" -> "zsh $file"
            else -> null
        }?.let { "$it" }
    }

    fun runOpenedFile(projectRoot: File, activeFilePath: String?, runOutputMode: String) {
        val command = activeFilePath?.let { runCommandForFile(it) } ?: return
        val workingDir = File(activeFilePath).parent ?: projectRoot.absolutePath
        if (runOutputMode == RunOutputMode.OUTPUT) {
            BuildOutputBuffer.clear()
            runCommandInOutput(command, File(workingDir))
        } else {
            bottomSheetVm.setPendingRunCommand(command, workingDir)
            bottomSheetVm.requestOpenTerminal()
        }
    }

    fun runCommandInOutput(command: String, workingDir: File) {
        val binDir = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH
        val bashrcPath = File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".bashrc").absolutePath
        val cmd = listOf(
            File(binDir, "bash").absolutePath,
            "-c",
            "source $bashrcPath && $command"
        )
        Thread {
            try {
                val pb = ProcessBuilder(cmd)
                pb.directory(workingDir)
                pb.redirectErrorStream(true)
                val env = pb.environment()
                env["PATH"] = "$binDir:${env["PATH"] ?: ""}"
                env.putIfAbsent("PYTHONUNBUFFERED", "1")
                env.putIfAbsent("TERM", "dumb")
                val process = pb.start()
                process.inputStream.bufferedReader().forEachLine { line ->
                    BuildOutputBuffer.appendLine(line)
                }
                process.waitFor()
            } catch (e: IOException) {
                BuildOutputBuffer.appendLine("[CodeRun] ${e.message}")
            }
        }.start()
    }
    fun onSyncProject(projectRoot: File) {
        if (GradleBuildStatus.isRunning) {
            GradleService.stopBuild(activity)
            return
        }
        val plan = GradleProjectActions.createSyncPlan()
        BuildOutputBuffer.clear()
        GradleService.startBuild(
            context = activity,
            projectDir = projectRoot,
            args = plan.args,
            actionLabel = activity.getString(R.string.sync_started),
            installOnSuccess = false
        )
    }

    fun onQuickRunOrCancel(projectRoot: File, variant: String = "debug") {
        if (GradleBuildStatus.isRunning) {
            GradleService.stopBuild(activity)
            return
        }

        val isFlutter = GradleProjectActions.isFlutterProject(projectRoot)
        val plan = if (isFlutter) {
            GradleProjectActions.createFlutterBuildPlan(projectRoot, variant)
        } else {
            GradleProjectActions.createQuickRunPlan(projectRoot, variant)
        }
        val actionLabel = activity.getString(R.string.build_started)
        val executable = if (isFlutter) "flutter" else null

        BuildOutputBuffer.clear()
        GradleService.startBuild(
            context = activity,
            projectDir = projectRoot,
            args = plan.args,
            actionLabel = actionLabel,
            installOnSuccess = true,
            variant = variant,
            executable = executable
        )
    }
}
