package com.neonide.studio.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neonide.studio.R
import com.neonide.studio.app.lsp.EditorLspController
import com.neonide.studio.preference.SettingsState
import com.neonide.studio.ui.components.AppIcon
import com.neonide.studio.ui.components.ToggleMenuItem
import com.neonide.studio.ui.layout.AppBox
import com.neonide.studio.utils.Divider.horizontalDivider
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier

@Composable
fun EditorTopBar(
    settings: SettingsState,
    editor: CodeEditor?,
    lspController: EditorLspController? = null,
    searchPanelVisible: Boolean,
    onSearchPanelToggle: () -> Unit,
    onNavigationClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSaveClick: () -> Unit,
    isGradleRunning: Boolean = false,
    buildVariant: String = "debug",
    onBuildVariantChange: (String) -> Unit = {},
    onBuildClick: () -> Unit,
    onSyncClick: () -> Unit,
    onTerminalClick: () -> Unit,
    onSwitchColors: () -> Unit,
    onSwitchTypeface: () -> Unit
) {
    var panelExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var buildMenuExpanded by remember { mutableStateOf(false) }
    var variantBuildMenuExpanded by remember { mutableStateOf(false) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var generalExpanded by remember { mutableStateOf(false) }
    var displayExpanded by remember { mutableStateOf(false) }
    var codeEditingExpanded by remember { mutableStateOf(false) }
    var inputMethodExpanded by remember { mutableStateOf(false) }
    DisposableEffect(editor) {
        canUndo = editor?.canUndo() == true
        canRedo = editor?.canRedo() == true
        val receipt = editor?.subscribeAlways(ContentChangeEvent::class.java) {
            canUndo = editor.canUndo()
            canRedo = editor.canRedo()
        }
        onDispose { receipt?.unsubscribe() }
    }
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20) {
                        panelExpanded = true
                    } else if (dragAmount < -20) {
                        panelExpanded = false
                        buildMenuExpanded = false
                        variantBuildMenuExpanded = false
                    }
                }
            }
    ) {
        // Build panel ABOVE toolbar
        AnimatedVisibility(
            visible = panelExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            BuildVariantPanel(
                buildVariant = buildVariant,
                onBuildVariantChange = onBuildVariantChange,
                buildMenuExpanded = buildMenuExpanded,
                onBuildMenuExpandedChange = { buildMenuExpanded = it },
                variantBuildMenuExpanded = variantBuildMenuExpanded,
                onVariantBuildMenuExpandedChange = { variantBuildMenuExpanded = it }
            )
        }

        horizontalDivider()

        // Toolbar
        TopAppBar(
            modifier = Modifier.height(40.dp),
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            navigationIcon = {
                IconButton(onClick = onNavigationClick) {
                    AppIcon(painterResource(R.drawable.ic_menu))
                }
            },
            actions = {
                IconButton(
                    enabled = canUndo,
                    onClick = {
                        onUndoClick()
                        canUndo = editor?.canUndo() == true
                        canRedo = editor?.canRedo() == true
                    }
                ) {
                    AppIcon(
                        painterResource(R.drawable.ic_undo),
                        tint = if (canUndo) {
                            Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.20f
                            )
                        }
                    )
                }
                IconButton(
                    enabled = canRedo,
                    onClick = {
                        onRedoClick()
                        canUndo = editor?.canUndo() == true
                        canRedo = editor?.canRedo() == true
                    }
                ) {
                    AppIcon(
                        painterResource(R.drawable.ic_redo),
                        tint = if (canRedo) {
                            Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.20f
                            )
                        }
                    )
                }
                IconButton(onClick = onSaveClick) { AppIcon(painterResource(R.drawable.ic_save)) }
                IconButton(onClick = onBuildClick) {
                    if (isGradleRunning) {
                        AppIcon(painterResource(R.drawable.ic_stop))
                    } else {
                        AppIcon(painterResource(R.drawable.ic_play))
                    }
                }
                IconButton(onClick = onSyncClick) {
                    AppIcon(painterResource(R.drawable.ic_refresh))
                }
                IconButton(onClick = onTerminalClick) {
                    AppIcon(painterResource(R.drawable.ic_terminal))
                }

                AppBox {
                    IconButton(onClick = { menuExpanded = true }) {
                        AppIcon(painterResource(R.drawable.ic_menu_kebab))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        MenuCategoryTitle(stringResource(R.string.search))
                        ToggleMenuItem(
                            text = stringResource(R.string.search_panel),
                            checked = searchPanelVisible,
                            onToggle = {
                                onSearchPanelToggle()
                                menuExpanded = false
                            }
                        )

                        horizontalDivider(color = Color.Gray)

                        MenuCategoryTitle(stringResource(R.string.editor_preference))

                        ExpandableMenuCategory(
                            title = "General",
                            expanded = generalExpanded,
                            onToggle = { generalExpanded = !generalExpanded }
                        )
                        if (generalExpanded) {
                            ToggleMenuItem(
                                text = stringResource(R.string.symbol_bar),
                                checked = settings.isSymbolBarVisible,
                                onToggle = { settings.isSymbolBarVisible = it }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.wordwrap),
                                checked = settings.isWordwrap,
                                onToggle = {
                                    settings.isWordwrap = it
                                    editor?.isWordwrap = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.magnifier),
                                checked = settings.isMagnifierEnabled,
                                onToggle = {
                                    settings.isMagnifierEnabled = it
                                    editor?.getComponent(Magnifier::class.java)?.isEnabled = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.inlay_hints),
                                checked = settings.isInlayHintsEnabled,
                                onToggle = {
                                    settings.isInlayHintsEnabled = it
                                    lspController?.currentEditor()?.isEnableInlayHint = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.signature_help),
                                checked = settings.isSignatureHelpEnabled,
                                onToggle = {
                                    settings.isSignatureHelpEnabled = it
                                    lspController?.currentEditor()?.isEnableSignatureHelp = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.hover_info),
                                checked = settings.isHoverInfoEnabled,
                                onToggle = {
                                    settings.isHoverInfoEnabled = it
                                    lspController?.currentEditor()?.isEnableHover = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.cursor_animation),
                                checked = settings.isCursorAnimationEnabled,
                                onToggle = {
                                    settings.isCursorAnimationEnabled = it
                                    editor?.isCursorAnimationEnabled = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.ligature),
                                checked = settings.isLigatureEnabled,
                                onToggle = {
                                    settings.isLigatureEnabled = it
                                    editor?.isLigatureEnabled = it
                                }
                            )
                        }

                        ExpandableMenuCategory(
                            title = "Display",
                            expanded = displayExpanded,
                            onToggle = { displayExpanded = !displayExpanded }
                        )
                        if (displayExpanded) {
                            ToggleMenuItem(
                                text = stringResource(R.string.completion_animation),
                                checked = settings.completionAnim,
                                onToggle = {
                                    settings.completionAnim = it
                                    editor?.getComponent(
                                        EditorAutoCompletion::class.java
                                    )?.setEnabledAnimation(it)
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.line_number),
                                checked = settings.isLineNumberVisible,
                                onToggle = {
                                    settings.isLineNumberVisible = it
                                    editor?.isLineNumberEnabled = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.pin_line_number),
                                checked = settings.isLineNumberPinned,
                                onToggle = {
                                    settings.isLineNumberPinned = it
                                    editor?.setPinLineNumber(it)
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.minimap),
                                checked = settings.isMinimapEnabled,
                                onToggle = {
                                    settings.isMinimapEnabled = it
                                    editor?.props?.showMinimap = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.highlight_current_line),
                                checked = settings.isHighlightCurrentLineEnabled,
                                onToggle = {
                                    settings.isHighlightCurrentLineEnabled = it
                                    editor?.isHighlightCurrentLine = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.highlight_current_block),
                                checked = settings.isHighlightCurrentBlockEnabled,
                                onToggle = {
                                    settings.isHighlightCurrentBlockEnabled = it
                                    editor?.isHighlightCurrentBlock = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.sticky_text_selection),
                                checked = settings.isStickyTextSelectionEnabled,
                                onToggle = {
                                    settings.isStickyTextSelectionEnabled = it
                                    editor?.isStickyTextSelection = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.scrollbar),
                                checked = settings.isScrollbarEnabled,
                                onToggle = {
                                    settings.isScrollbarEnabled = it
                                    editor?.setHorizontalScrollBarEnabled(it)
                                    editor?.setVerticalScrollBarEnabled(it)
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.first_line_number_always_visible),
                                checked = settings.isFirstLineNumberAlwaysVisibleEnabled,
                                onToggle = {
                                    settings.isFirstLineNumberAlwaysVisibleEnabled = it
                                    editor?.isFirstLineNumberAlwaysVisible = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.bidi_indicator),
                                checked = settings.isBidiIndicatorEnabled,
                                onToggle = {
                                    settings.isBidiIndicatorEnabled = it
                                    editor?.props?.showBidiDirectionIndicator = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.round_text_bg),
                                checked = settings.isRoundTextBackgroundEnabled,
                                onToggle = {
                                    settings.isRoundTextBackgroundEnabled = it
                                    editor?.props?.enableRoundTextBackground = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.side_block_line),
                                checked = settings.isSideBlockLineEnabled,
                                onToggle = {
                                    settings.isSideBlockLineEnabled = it
                                    editor?.let { editor ->
                                        editor.setBlockLineEnabled(it)
                                        editor.props.drawSideBlockLine = it
                                        editor.invalidate()
                                    }
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.sticky_scroll),
                                checked = settings.isStickyScrollEnabled,
                                onToggle = {
                                    settings.isStickyScrollEnabled = it
                                    editor?.props?.stickyScroll = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.scroll_fling),
                                checked = settings.isScrollFlingEnabled,
                                onToggle = {
                                    settings.isScrollFlingEnabled = it
                                    editor?.props?.scrollFling = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.overscroll),
                                checked = settings.isOverScrollEnabled,
                                onToggle = {
                                    settings.isOverScrollEnabled = it
                                    editor?.props?.overScrollEnabled = it
                                }
                            )
                        }

                        ExpandableMenuCategory(
                            title = "CodeEditing",
                            expanded = codeEditingExpanded,
                            onToggle = { codeEditingExpanded = !codeEditingExpanded }
                        )
                        if (codeEditingExpanded) {
                            ToggleMenuItem(
                                text = stringResource(R.string.auto_indent),
                                checked = settings.isAutoIndentEnabled,
                                onToggle = {
                                    settings.isAutoIndentEnabled = it
                                    editor?.props?.autoIndent = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.delete_multi_spaces),
                                checked = settings.isDeleteMultiSpacesEnabled,
                                onToggle = {
                                    settings.isDeleteMultiSpacesEnabled = it
                                    editor?.props?.deleteMultiSpaces = if (it) -1 else 1
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.bracket_highlight),
                                checked = settings.isBracketHighlightEnabled,
                                onToggle = {
                                    settings.isBracketHighlightEnabled = it
                                    editor?.props?.highlightMatchingDelimiters = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.bold_matching_brackets),
                                checked = settings.isBoldMatchingBrackets,
                                onToggle = {
                                    settings.isBoldMatchingBrackets = it
                                    editor?.props?.boldMatchingDelimiters = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.delete_empty_line_fast),
                                checked = settings.isDeleteEmptyLineFast,
                                onToggle = {
                                    settings.isDeleteEmptyLineFast = it
                                    editor?.props?.deleteEmptyLineFast = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.format_pasted_text),
                                checked = settings.isFormatPastedTextEnabled,
                                onToggle = {
                                    settings.isFormatPastedTextEnabled = it
                                    editor?.props?.formatPastedText = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.symbol_pair_completion),
                                checked = settings.isSymbolPairCompletionEnabled,
                                onToggle = {
                                    settings.isSymbolPairCompletionEnabled = it
                                    editor?.props?.symbolPairAutoCompletion = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.use_icu),
                                checked = settings.useIcu,
                                onToggle = {
                                    settings.useIcu = it
                                    editor?.props?.useICULibToSelectWords = it
                                }
                            )
                        }

                        ExpandableMenuCategory(
                            title = "Input Method",
                            expanded = inputMethodExpanded,
                            onToggle = { inputMethodExpanded = !inputMethodExpanded }
                        )
                        if (inputMethodExpanded) {
                            ToggleMenuItem(
                                text = stringResource(R.string.soft_keyboard),
                                checked = settings.softKbdEnabled,
                                onToggle = {
                                    settings.softKbdEnabled = it
                                    editor?.isSoftKeyboardEnabled = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.disable_soft_kbd_hard_kbd),
                                checked = settings.hardKbdDisabled,
                                onToggle = {
                                    settings.hardKbdDisabled = it
                                    editor?.isDisableSoftKbdIfHardKbdAvailable = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.enhanced_home_end),
                                checked = settings.isEnhancedHomeEndEnabled,
                                onToggle = {
                                    settings.isEnhancedHomeEndEnabled = it
                                    editor?.props?.enhancedHomeAndEnd = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.row_based_home_end),
                                checked = settings.isRowBasedHomeEndEnabled,
                                onToggle = {
                                    settings.isRowBasedHomeEndEnabled = it
                                    editor?.props?.rowBasedHomeEnd = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.auto_completion_on_composing),
                                checked = settings.isAutoCompletionOnComposingEnabled,
                                onToggle = {
                                    settings.isAutoCompletionOnComposingEnabled = it
                                    editor?.props?.autoCompletionOnComposing = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.allow_fullscreen),
                                checked = settings.isAllowFullscreenEnabled,
                                onToggle = {
                                    settings.isAllowFullscreenEnabled = it
                                    editor?.props?.allowFullscreen = it
                                }
                            )
                            ToggleMenuItem(
                                text = stringResource(R.string.mouse_context_menu),
                                checked = settings.isMouseContextMenuEnabled,
                                onToggle = {
                                    settings.isMouseContextMenuEnabled = it
                                    editor?.props?.mouseContextMenu = it
                                }
                            )
                        }

                        horizontalDivider(color = Color.Gray)

                        MenuCategoryTitle(stringResource(R.string.configuration))
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.switch_color_scheme)) },
                            onClick = {
                                onSwitchColors()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.switch_typeface)) },
                            onClick = {
                                onSwitchTypeface()
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        )

        horizontalDivider()
    }
}

@Composable
private fun MenuCategoryTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ExpandableMenuCategory(title: String, expanded: Boolean, onToggle: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIcon(
                    painterResource(
                        if (expanded) {
                            R.drawable.ic_chevron_down
                        } else {
                            R.drawable.ic_chevron_right
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        onClick = onToggle
    )
}
