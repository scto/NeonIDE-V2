package com.neonide.studio.editor

import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.neonide.studio.preference.SettingsState
import com.neonide.studio.utils.HexColorScanner
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier

class NeonCodeEditor(context: Context) : CodeEditor(context) {
    override fun setHighlightTexts(highlights: HighlightTextContainer?) {
        val container = highlights ?: HighlightTextContainer()
        HexColorScanner.appendHighlights(text, container)
        super.setHighlightTexts(container)
    }
}

@Composable
fun SoraEditor(
    settings: SettingsState,
    modifier: Modifier = Modifier,
    onEditorCreated: (CodeEditor) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            NeonCodeEditor(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                props.cancelCompletionNs = 150 * 1_000_000L // 150ms
                isCursorAnimationEnabled = settings.isCursorAnimationEnabled
                nonPrintablePaintingFlags =
                    CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
                    CodeEditor.FLAG_DRAW_LINE_SEPARATOR or
                    CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION or
                    CodeEditor.FLAG_DRAW_SOFT_WRAP

                isWordwrap = settings.isWordwrap
                isLineNumberEnabled = settings.isLineNumberVisible
                setPinLineNumber(settings.isLineNumberPinned)
                isSoftKeyboardEnabled = settings.softKbdEnabled
                isDisableSoftKbdIfHardKbdAvailable = settings.hardKbdDisabled

                isHighlightCurrentLine = settings.isHighlightCurrentLineEnabled
                isHighlightCurrentBlock = settings.isHighlightCurrentBlockEnabled
                isStickyTextSelection = settings.isStickyTextSelectionEnabled
                isUndoEnabled = true
                isLigatureEnabled = settings.isLigatureEnabled
                isFirstLineNumberAlwaysVisible = settings.isFirstLineNumberAlwaysVisibleEnabled
                setHorizontalScrollBarEnabled(settings.isScrollbarEnabled)
                setVerticalScrollBarEnabled(settings.isScrollbarEnabled)

                props.showMinimap = settings.isMinimapEnabled
                props.useICULibToSelectWords = settings.useIcu
                props.autoIndent = settings.isAutoIndentEnabled
                props.highlightMatchingDelimiters = settings.isBracketHighlightEnabled
                props.boldMatchingDelimiters = settings.isBoldMatchingBrackets
                props.symbolPairAutoCompletion = settings.isSymbolPairCompletionEnabled
                props.formatPastedText = settings.isFormatPastedTextEnabled
                props.stickyScroll = settings.isStickyScrollEnabled
                props.enhancedHomeAndEnd = settings.isEnhancedHomeEndEnabled
                props.scrollFling = settings.isScrollFlingEnabled
                props.overScrollEnabled = settings.isOverScrollEnabled
                props.deleteEmptyLineFast = settings.isDeleteEmptyLineFast
                setBlockLineEnabled(settings.isSideBlockLineEnabled)
                props.drawSideBlockLine = settings.isSideBlockLineEnabled
                props.enableRoundTextBackground = settings.isRoundTextBackgroundEnabled

                props.allowFullscreen = settings.isAllowFullscreenEnabled
                props.rowBasedHomeEnd = settings.isRowBasedHomeEndEnabled
                props.showBidiDirectionIndicator = settings.isBidiIndicatorEnabled
                props.autoCompletionOnComposing = settings.isAutoCompletionOnComposingEnabled
                props.deleteMultiSpaces = if (settings.isDeleteMultiSpacesEnabled) -1 else 1
                props.mouseContextMenu = settings.isMouseContextMenuEnabled

                getComponent(Magnifier::class.java).isEnabled = settings.isMagnifierEnabled
                getComponent(EditorAutoCompletion::class.java)
                    .setEnabledAnimation(settings.completionAnim)

                onEditorCreated(this)
            }
        },
        onRelease = { it.release() }
    )
}
