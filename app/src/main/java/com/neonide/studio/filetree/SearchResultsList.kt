package com.neonide.studio.filetree

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import java.io.File
import okio.Path
import okio.Path.Companion.toPath

@Composable
internal fun SearchResultsList(
    rootPath: Path,
    query: String,
    uiScale: Float,
    useRegex: Boolean,
    caseSensitive: Boolean,
    onFileClick: (String) -> Unit,
    onFolderLongClick: (Path, String) -> Unit,
    onFileLongClick: (Path, String) -> Unit
) {
    val results = remember(rootPath, query, useRegex, caseSensitive) {
        searchFiles(
            File(rootPath.toString()),
            query,
            useRegex = useRegex,
            caseSensitive = caseSensitive
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = results.size,
            key = { results[it].absolutePath }
        ) { index ->
            val file = results[index]
            val relativePath = file.absolutePath.removePrefix(rootPath.toString()).removePrefix("/")
            val iconRes = if (file.isDirectory) {
                iconForFolder(file.name, false)
            } else {
                iconForExtension(file.extension)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (file.isFile) onFileClick(file.absolutePath)
                        },
                        onLongClick = {
                            val path = file.absolutePath.toPath()
                            val name = file.name
                            if (file.isDirectory) {
                                onFolderLongClick(path, name)
                            } else {
                                onFileLongClick(path, name)
                            }
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp * uiScale)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = file.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp * uiScale
                    )
                    Text(
                        text = relativePath.substringBeforeLast("/", ""),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp * uiScale,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

internal fun searchFiles(
    root: File,
    query: String,
    maxResults: Int = 200,
    useRegex: Boolean = false,
    caseSensitive: Boolean = false
): List<File> {
    if (query.isEmpty()) return emptyList()
    val matcher: (String) -> Boolean = if (useRegex) {
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val regex = try {
            Regex(query, options)
        } catch (_: Exception) {
            return emptyList()
        }
        { name -> regex.containsMatchIn(name) }
    } else {
        { name -> name.contains(query, ignoreCase = !caseSensitive) }
    }
    val results = mutableListOf<File>()
    val queue = ArrayDeque<File>()
    queue.add(root)
    while (queue.isNotEmpty() && results.size < maxResults) {
        val dir = queue.removeFirst()
        val children = dir.listFiles() ?: continue
        for (child in children.sortedBy { it.name.lowercase() }) {
            if (results.size >= maxResults) break
            if (matcher(child.name)) {
                results.add(child)
            }
            if (child.isDirectory) {
                queue.add(child)
            }
        }
    }
    return results
}
