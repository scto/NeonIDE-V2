package com.neonide.studio.preference

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.neonide.studio.ui.theme.ColorSchemeMode
import com.neonide.studio.utils.PersistedBoolean
import com.neonide.studio.utils.PersistedString
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences

object RunOutputMode {
    const val OUTPUT = "output"
    const val TERMINAL = "terminal"
}

class SettingsState(context: Context) {

    private val prefs = context.getSharedPreferences("neonide_settings", Context.MODE_PRIVATE)

    var isSymbolBarVisible by PersistedBoolean(prefs, "symbol_bar", true)
    var isWordwrap by PersistedBoolean(prefs, "wordwrap", false)
    var isLineNumberVisible by PersistedBoolean(prefs, "line_number", true)
    var isLineNumberPinned by PersistedBoolean(prefs, "pin_line_number", false)
    var isMagnifierEnabled by PersistedBoolean(prefs, "magnifier", true)
    var useIcu by PersistedBoolean(prefs, "use_icu", true)
    var completionAnim by PersistedBoolean(prefs, "completion_anim", true)
    var softKbdEnabled by PersistedBoolean(prefs, "soft_kbd", true)
    var hardKbdDisabled by PersistedBoolean(prefs, "hard_kbd_disabled", true)

    var isMinimapEnabled by PersistedBoolean(prefs, "minimap", false)
    var isAutoIndentEnabled by PersistedBoolean(prefs, "auto_indent", true)
    var isBracketHighlightEnabled by PersistedBoolean(prefs, "bracket_highlight", true)
    var isBoldMatchingBrackets by PersistedBoolean(prefs, "bold_bracket", false)
    var isSymbolPairCompletionEnabled by PersistedBoolean(prefs, "symbol_pair", true)
    var isFormatPastedTextEnabled by PersistedBoolean(prefs, "format_paste", false)
    var isStickyScrollEnabled by PersistedBoolean(prefs, "sticky_scroll", true)
    var isEnhancedHomeEndEnabled by PersistedBoolean(prefs, "enhanced_home_end", true)
    var isScrollFlingEnabled by PersistedBoolean(prefs, "scroll_fling", true)
    var isOverScrollEnabled by PersistedBoolean(prefs, "overscroll", true)
    var isDeleteEmptyLineFast by PersistedBoolean(prefs, "delete_empty_fast", true)
    var isSideBlockLineEnabled by PersistedBoolean(prefs, "side_block", true)
    var isRoundTextBackgroundEnabled by PersistedBoolean(prefs, "round_text_bg", true)

    var isInlayHintsEnabled by PersistedBoolean(prefs, "inlay_hints", false)
    var isSignatureHelpEnabled by PersistedBoolean(prefs, "signature_help", false)
    var isHoverInfoEnabled by PersistedBoolean(prefs, "hover_info", false)

    var isHighlightCurrentLineEnabled by PersistedBoolean(prefs, "highlight_current_line", true)
    var isHighlightCurrentBlockEnabled by PersistedBoolean(prefs, "highlight_current_block", true)
    var isStickyTextSelectionEnabled by PersistedBoolean(prefs, "sticky_text_selection", false)
    var isCursorAnimationEnabled by PersistedBoolean(prefs, "cursor_animation", true)
    var isScrollbarEnabled by PersistedBoolean(prefs, "scrollbar", true)
    var isLigatureEnabled by PersistedBoolean(prefs, "ligature", true)
    var isFirstLineNumberAlwaysVisibleEnabled by PersistedBoolean(
        prefs,
        "first_line_number_always_visible",
        true
    )
    var isAllowFullscreenEnabled by PersistedBoolean(prefs, "allow_fullscreen", false)
    var isRowBasedHomeEndEnabled by PersistedBoolean(prefs, "row_based_home_end", true)
    var isBidiIndicatorEnabled by PersistedBoolean(prefs, "bidi_indicator", true)
    var isAutoCompletionOnComposingEnabled by PersistedBoolean(
        prefs,
        "auto_completion_on_composing",
        true
    )
    var isDeleteMultiSpacesEnabled by PersistedBoolean(prefs, "delete_multi_spaces", false)
    var isMouseContextMenuEnabled by PersistedBoolean(prefs, "mouse_context_menu", true)

    var isDynamicColorEnabled by PersistedBoolean(prefs, "dynamic_color", false)

    var colorSchemeModeKey by PersistedString(
        prefs,
        "color_scheme_mode",
        ColorSchemeMode.SYSTEM.key
    )

    var isAapt2OverrideEnabled by PersistedBoolean(prefs, "gradle_enabled", true)
    var isSmartNdkVersioningEnabled by PersistedBoolean(prefs, "local_enabled", true)
    var isParallelExecutionEnabled by PersistedBoolean(prefs, "parallel_enabled", true)
    var isGradleDaemonEnabled by PersistedBoolean(prefs, "gradle_daemon_enabled", true)

    var isCodeRunEnabled by PersistedBoolean(prefs, "code_run", false)
    var runOutputMode by PersistedString(prefs, "run_output_mode", RunOutputMode.TERMINAL)

    private val termuxPrefs = TermuxAppSharedPreferences.build(context, false)
    private val _isIdeFileLoggingEnabled = mutableStateOf(
        termuxPrefs?.isIdeFileLoggingEnabled ?: false
    )
    var isIdeFileLoggingEnabled: Boolean
        get() = _isIdeFileLoggingEnabled.value
        set(value) {
            _isIdeFileLoggingEnabled.value = value
            termuxPrefs?.setIdeFileLoggingEnabled(value)
        }
}
