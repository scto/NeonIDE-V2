package com.neonide.studio.app.lsp.server

import com.termux.shared.logger.Logger
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class ProcessStreamConnectionProvider(
    private val command: List<String>,
    private val workingDir: File? = null,
    private val env: Map<String, String> = emptyMap()
) : StreamConnectionProvider {

    private var process: Process? = null

    override fun start() {
        Logger.logInfo("Executing command: ${command.joinToString(" ")}")
        val pb = ProcessBuilder(command)
        workingDir?.let { pb.directory(it) }
        pb.environment().putAll(env)
        pb.redirectErrorStream(false)

        process = pb.start().also { p ->
            Logger.logInfo("LSP", "Process started, spawning stderr drainer...")
            // Drain stderr to prevent process hang when buffer is full
            thread(name = "LSP-Stderr", isDaemon = true) {
                runCatching {
                    p.errorStream.bufferedReader().forEachLine {
                        Logger.logDebug("LSP-stderr", it)
                    }
                }.onFailure { t ->
                    Logger.logWarn("LSP-stderr", "Drainer stopped: ${t.message}")
                }
            }
        }
    }

    override val inputStream: InputStream
        get() = process!!.inputStream

    override val outputStream: OutputStream
        get() = process!!.outputStream

    override val isClosed: Boolean
        get() = process == null

    override fun close() {
        process?.destroyForcibly()
        process = null
    }
}
