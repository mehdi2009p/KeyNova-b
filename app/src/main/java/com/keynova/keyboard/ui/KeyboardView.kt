package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keynova.keyboard.R
import com.keynova.keyboard.layout.KeyboardLayouts
import com.keynova.keyboard.logic.KeyboardController
import com.keynova.keyboard.logic.SuggestionEngine
import com.keynova.keyboard.model.Key
import com.keynova.keyboard.model.KeyAction
import com.keynova.keyboard.model.KeyboardLanguage
import com.keynova.keyboard.model.KeyboardMode
import com.keynova.keyboard.settings.KeyboardPrefs
import com.keynova.keyboard.theme.KeyboardTheme
import com.keynova.keyboard.theme.ThemeManager
import kotlin.math.abs

class KeyboardView(context: Context) : LinearLayout(context) {

    var controller: KeyboardController? = null
        set(value) {
            field = value
            render()
        }

    private val handler = Handler(Looper.getMainLooper())
    private var longPressFired = false
    private var popup: PopupWindow? = null
    private var popupItems: List<TextView> = emptyList()
    private var popupActionsCache: List<Pair<String, KeyAction>> = emptyList()
    private var popupAnchor: View? = null
    private val prefs = KeyboardPrefs(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val suggestions = SuggestionEngine(context)
    private var suggestionBar: LinearLayout? = null
    private var clipboardPanel: LinearLayout? = null
    private var clipboardView: View? = null
    private var clipboardActionBar: LinearLayout? = null
    private var keyboardContent: View? = null
    private var keyPreview: PopupWindow? = null
    private var keyPreviewAnchor: View? = null
    private var keyPreviewShowRunnable: Runnable? = null
    private var selectedClipText: String? = null
    @Volatile private var vibrationEnabled = prefs.vibrationEnabled
    @Volatile private var soundEnabled = prefs.soundEnabled
    @Volatile private var keyPreviewEnabled = prefs.keyPreviewEnabled
    @Volatile private var numberRowEnabled = prefs.numberRowEnabled
    @Volatile private var showHintsEnabled = prefs.showHintsEnabled
    @Volatile private var longPressSpaceDot = prefs.longPressSpaceDot
    @Volatile private var doubleSpacePeriod = prefs.doubleSpacePeriod
    @Volatile private var spaceSwipeMode = prefs.spaceSwipeMode
    @Volatile private var spaceLabelMode = prefs.spaceLabelMode
    @Volatile private var keyHeightDp = prefs.keyHeightDp
    @Volatile private var keyGapDp = prefs.keyGapDp
    @Volatile private var keyTextSizeSp = prefs.keyTextSizeSp
    @Volatile private var longPressDelayMs = prefs.longPressDelayMs
    @Volatile private var soundVolumePercent = prefs.soundVolumePercent
    @Volatile private var bottomPaddingDp = prefs.bottomPaddingDp
    @Volatile private var theme: KeyboardTheme = ThemeManager.byId(prefs.themeId)
    @Volatile private var suggestionsEnabled = prefs.suggestionsEnabled
    private var lastSpaceTapMs = 0L
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KeyboardPrefs.KEY_VIBRATION -> vibrationEnabled = prefs.vibrationEnabled
            KeyboardPrefs.KEY_SOUND -> soundEnabled = prefs.soundEnabled
            KeyboardPrefs.KEY_PREVIEW -> keyPreviewEnabled = prefs.keyPreviewEnabled
            KeyboardPrefs.KEY_HINTS -> { showHintsEnabled = prefs.showHintsEnabled; post { render() } }
            KeyboardPrefs.KEY_SPACE_DOT -> longPressSpaceDot = prefs.longPressSpaceDot
            KeyboardPrefs.KEY_DOUBLE_SPACE_PERIOD -> doubleSpacePeriod = prefs.doubleSpacePeriod
            KeyboardPrefs.KEY_SPACE_SWIPE_MODE, KeyboardPrefs.KEY_SWIPE_SPACE_LANG -> spaceSwipeMode = prefs.spaceSwipeMode
            KeyboardPrefs.KEY_SPACE_LABEL -> { spaceLabelMode = prefs.spaceLabelMode; post { render() } }
            KeyboardPrefs.KEY_LONG_PRESS_DELAY -> longPressDelayMs = prefs.longPressDelayMs
            KeyboardPrefs.KEY_SOUND_VOLUME -> soundVolumePercent = prefs.soundVolumePercent
            KeyboardPrefs.KEY_THEME -> { theme = ThemeManager.byId(prefs.themeId); post { applyTheme(); render() } }
            KeyboardPrefs.KEY_SUGGESTIONS -> { suggestionsEnabled = prefs.suggestionsEnabled; post { render() } }
            KeyboardPrefs.KEY_CLIPBOARD -> { post { refreshClipboard() } }
            KeyboardPrefs.KEY_BOTTOM_PADDING, KeyboardPrefs.KEY_HEIGHT, KeyboardPrefs.KEY_GAP, KeyboardPrefs.KEY_TEXT_SIZE, KeyboardPrefs.KEY_NUMBER_ROW -> {
                keyHeightDp = prefs.keyHeightDp; keyGapDp = prefs.keyGapDp; keyTextSizeSp = prefs.keyTextSizeSp
                numberRowEnabled = prefs.numberRowEnabled; bottomPaddingDp = prefs.bottomPaddingDp
                post { updatePadding(); render() }
            }
        }
    }

    init {
        orientation = VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        updatePadding()
        applyTheme()
        isClickable = true
        isFocusable = false
        isHapticFeedbackEnabled = true
        prefs.registerListener(prefListener)
    }

    private fun applyTheme() {
        setBackgroundColor(theme.background)
    }

    private fun updatePadding() {
        val pad = dp(4)
        setPadding(pad, dp(10), pad, dp(bottomPaddingDp))
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        dismissPopup()
        dismissKeyPreview()
        prefs.unregisterListener(prefListener)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility != VISIBLE) {
            handler.removeCallbacksAndMessages(null)
            longPressFired = false
            dismissPopup()
            dismissKeyPreview()
        }
        super.onWindowVisibilityChanged(visibility)
    }

    fun render() {
        dismissPopup()
        dismissKeyPreview()
        removeAllViews()
        val ctrl = controller ?: return

        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        }
        keyboardContent = content
        content.addView(createTopBar())
        val rows = KeyboardLayouts.rowsFor(ctrl.mode, ctrl.language, ctrl.shiftState).toMutableList()
        if (numberRowEnabled && ctrl.mode == KeyboardMode.LETTERS) rows.add(0, numberRow(ctrl.language))
        rows.forEach { rowKeys -> content.addView(createRow(rowKeys)) }

        val root = android.widget.FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            clipChildren = false
        }
        root.addView(content)
        val clip = createClipboardView()
        clipboardView = clip
        root.addView(clip)
        addView(root)
        updateSuggestions()
    }

    fun refreshClipboard() {
        val panel = clipboardPanel ?: return
        panel.removeAllViews()
        val items = prefs.clipboardItems
        val selected = selectedClipText?.takeIf { sel -> items.any { it.text == sel } }
        selectedClipText = selected
        updateClipboardActions(selected, items.find { it.text == selected }?.locked == true)

        if (items.isEmpty()) {
            panel.addView(makeClipboardRow("کلیپ‌بورد خالی است", locked = false, isEmpty = true, selected = false) {})
            return
        }
        items.take(20).forEach { item ->
            panel.addView(makeClipboardRow(item.text, item.locked, false, selected == item.text) {
                controller?.currentInputConnection()?.commitText(item.text, 1)
                hideClipboard()
                updateSuggestions()
            })
        }
    }

    private fun updateClipboardActions(selectedText: String?, locked: Boolean) {
        val bar = clipboardActionBar ?: return
        bar.removeAllViews()
        if (selectedText == null) { bar.visibility = View.GONE; return }
        bar.visibility = View.VISIBLE
        fun btn(label: String, onClick: () -> Unit) = TextView(context).apply {
            text = label
            setTextColor(if (label == "حذف" && !locked) 0xFFFF7A7A.toInt() else theme.accent)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { onClick() }
        }
        bar.addView(btn(if (locked) "باز کردن قفل" else "قفل کردن") {
            prefs.toggleClipboardLock(selectedText); refreshClipboard()
        })
        bar.addView(btn("حذف") {
            if (locked) android.widget.Toast.makeText(context, "ابتدا قفل را بردارید", android.widget.Toast.LENGTH_SHORT).show()
            else { prefs.removeClipboardItem(selectedText); selectedClipText = null; refreshClipboard() }
        })
        bar.addView(btn("لغو انتخاب") { selectedClipText = null; refreshClipboard() })
    }

    private fun createTopBar(): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(42))
        }
        val clipBtn = TextView(context).apply {
            text = "📋"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(theme.text)
            layoutParams = LayoutParams(dp(44), LayoutParams.MATCH_PARENT)
            setOnClickListener { showClipboard() }
        }
        val sugRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            weightSum = 3f
        }
        repeat(3) {
            val tv = TextView(context).apply {
                text = ""
                setTextColor(theme.text)
                textSize = 15f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            sugRow.addView(tv)
        }
        suggestionBar = sugRow
        row.addView(clipBtn)
        row.addView(sugRow)
        return row
    }

    private fun createClipboardView(): View {
        val panel = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setBackgroundColor(theme.background)
            layoutParams = android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
            setPadding(dp(12), 0, dp(12), 0)
        }
        val title = TextView(context).apply {
            text = "کلیپ‌بورد"
            setTextColor(theme.text)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        header.addView(title)

        val actionBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        clipboardActionBar = actionBar

        val scroll = android.widget.ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        val list = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        scroll.addView(list)
        clipboardPanel = list

        val bottomBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp(8), dp(6), dp(8), dp(8))
        }
        val back = TextView(context).apply {
            text = "بازگشت به کیبورد"
            setTextColor(theme.accent)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundResource(theme.keySpecialDrawable)
            setOnClickListener { hideClipboard() }
        }
        bottomBar.addView(back)

        panel.addView(header)
        panel.addView(actionBar)
        panel.addView(scroll)
        panel.addView(bottomBar)
        return panel
    }

    private fun makeClipboardRow(text: String, locked: Boolean, isEmpty: Boolean, selected: Boolean, onClick: () -> Unit): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (selected) theme.keyAccentDrawable else theme.keySpecialDrawable)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(4)) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val preview = text.replace("\n", " ").trim().let {
            val words = it.split(Regex("\\s+")).filter { w -> w.isNotBlank() }
            if (words.size <= 4) it else words.take(4).joinToString(" ") + " ..."
        }
        val tv = TextView(context).apply {
            this.text = if (locked) "🔒 $preview" else preview
            setTextColor(if (selected) 0xFF10131A.toInt() else theme.text)
            textSize = 14f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)
        if (!isEmpty) {
            row.setOnClickListener { onClick() }
            row.setOnLongClickListener {
                selectedClipText = text
                refreshClipboard()
                if (keyPreviewEnabled) {
                    try { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                }
                true
            }
        } else {
            row.alpha = 0.7f
        }
        return row
    }

    private fun showClipboard() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) prefs.addClipboardItem(text)
        } catch (_: Exception) {}
        selectedClipText = null
        refreshClipboard()
        keyboardContent?.visibility = View.GONE
        clipboardView?.visibility = View.VISIBLE
    }

    private fun hideClipboard() {
        selectedClipText = null
        clipboardView?.visibility = View.GONE
        keyboardContent?.visibility = View.VISIBLE
    }

    // clipboard now uses makeClipboardRow in a full-screen panel

    fun updateSuggestions() {
        val bar = suggestionBar ?: return
        val ctrl = controller ?: return
        val ic = ctrl.currentInputConnection()
        val before = ic?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val current = before.takeLastWhile { !it.isWhitespace() }
        val afterSpace = before.isEmpty() || before.last().isWhitespace()
        // learn previous word when space entered
        val tokens = Regex("[\\p{L}\\u200C']+").findAll(before).map { it.value }.toList()
        if (afterSpace && tokens.isNotEmpty()) suggestions.learn(tokens.takeLast(2).joinToString(" "))

        val list = if (suggestionsEnabled && ctrl.mode == KeyboardMode.LETTERS && current.isNotEmpty()) {
            suggestions.suggest(current, ctrl.language, afterSpace)
        } else emptyList()

        for (i in 0 until bar.childCount) {
            val tv = bar.getChildAt(i) as TextView
            val word = list.getOrNull(i)
            if (word == null) {
                tv.text = ""; tv.isClickable = false; tv.setOnClickListener(null); tv.alpha = 0f
            } else {
                tv.text = word; tv.alpha = 1f; tv.isClickable = true
                tv.setOnClickListener { applySuggestion(word, current) }
            }
        }
    }

    private fun applySuggestion(word: String, current: String) {
        val ic = controller?.currentInputConnection() ?: return
        if (current.isNotEmpty()) {
            ic.deleteSurroundingText(current.length, 0)
        }
        ic.commitText("$word ", 1)
        suggestions.learn(word)
        updateSuggestions()
    }

    private fun numberRow(language: KeyboardLanguage): List<Key> {
        val nums = if (language == KeyboardLanguage.FA) listOf("۱","۲","۳","۴","۵","۶","۷","۸","۹","۰") else listOf("1","2","3","4","5","6","7","8","9","0")
        return nums.map { Key(it, KeyAction.Character(it)) }
    }

    private fun createRow(keys: List<Key>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            gravity = Gravity.CENTER
        }
        keys.forEach { row.addView(createKeyView(it)) }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: Key): View {
        val keyH = keyHeightDp
        val container = android.widget.FrameLayout(context).apply {
            val lp = LayoutParams(0, dp(keyH), key.weight)
            val hGap = dp(keyGapDp)
            lp.setMargins(hGap, 0, hGap, 0)
            layoutParams = lp
            minimumHeight = dp(keyH)
            setBackgroundResource(
                when {
                    key.isAccent -> theme.keyAccentDrawable
                    key.isSpecial -> theme.keySpecialDrawable
                    else -> theme.keyDrawable
                }
            )
        }

        val tv = TextView(context).apply {
            text = when (key.action) {
                KeyAction.Space -> when (spaceLabelMode) {
                    0 -> if ((controller?.language ?: KeyboardLanguage.FA) == KeyboardLanguage.FA) "فارسی" else "English"
                    2 -> ""
                    else -> if ((controller?.language ?: KeyboardLanguage.FA) == KeyboardLanguage.FA) "فاصله" else "space"
                }
                else -> key.label
            }
            contentDescription = when (key.action) {
                KeyAction.Space -> "Space"
                KeyAction.Backspace -> "Backspace"
                KeyAction.Enter -> "Enter"
                else -> key.label
            }
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            val base = if (key.label.length > 3) (keyTextSizeSp - 6).coerceAtLeast(10) else keyTextSizeSp
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base.toFloat())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isAllCaps = false
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(3), dp(7), dp(3), dp(7))
        }
        container.addView(tv)

        if (showHintsEnabled && key.longPressLabel != null && key.popupActions.isEmpty()) {
            val hint = TextView(context).apply {
                text = key.longPressLabel
                setTextColor(theme.textSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                ).apply { setMargins(dp(4), dp(2), 0, 0) }
            }
            container.addView(hint)
        }

        when (key.action) {
            KeyAction.Backspace -> bindBackspace(container)
            KeyAction.Space -> bindSpaceTouch(container)
            else -> bindKeyTouch(container, key)
        }
        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindBackspace(v: View) {
        v.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) provideFeedback(view)
            val handled = controller?.onBackspaceTouch(event) ?: false
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                view.performClick()
            }
            handled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindSpaceTouch(v: View) {
        var downX = 0f
        var switched = false
        var longDotFired = false
        val dotRunnable = Runnable {
            if (!switched) {
                longDotFired = true
                controller?.handleAction(KeyAction.Character("."))
            }
        }
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    switched = false
                    longDotFired = false
                    provideFeedback(view)
                    if (longPressSpaceDot) handler.postDelayed(dotRunnable, longPressDelayMs.toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    if (!switched && abs(dx) > dp(44)) {
                        when (spaceSwipeMode) {
                            1 -> { switched = true; handler.removeCallbacks(dotRunnable); controller?.handleAction(KeyAction.SwitchLanguage) }
                            2 -> {
                                val steps = (dx / dp(28)).toInt()
                                if (steps != 0) {
                                    switched = true // prevent space on up; cursor moved
                                    handler.removeCallbacks(dotRunnable)
                                    controller?.moveCursor(steps)
                                    downX = event.x
                                    switched = false // allow continuous move, but mark moved
                                    longDotFired = true // reuse flag to suppress space
                                }
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(dotRunnable)
                    if (!switched && !longDotFired) {
                        val now = System.currentTimeMillis()
                        if (doubleSpacePeriod && now - lastSpaceTapMs < 320L) {
                            controller?.handleAction(KeyAction.Backspace)
                            controller?.handleAction(KeyAction.Character("."))
                            controller?.handleAction(KeyAction.Space)
                            lastSpaceTapMs = 0L
                        } else {
                            controller?.handleAction(KeyAction.Space)
                            lastSpaceTapMs = now
                        }
                    }
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(dotRunnable)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindKeyTouch(v: View, key: Key) {
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    handler.removeCallbacksAndMessages(null)
                    dismissPopup()
                    dismissKeyPreview()
                    longPressFired = false
                    provideFeedback(view)
                    showKeyPreview(view, key)
                    if (key.popupActions.isNotEmpty()) {
                        handler.postDelayed({
                            if (!view.isAttachedToWindow) return@postDelayed
                            longPressFired = true
                            showPopup(view, key.popupActions, dismissOnCommit = false)
                            refreshPopupHighlight(-1)
                        }, longPressDelayMs.toLong())
                    } else if (key.longPressAction != null) {
                        handler.postDelayed({
                            if (!view.isAttachedToWindow) return@postDelayed
                            longPressFired = true
                            val lpLabel = key.longPressLabel ?: ""
                            if (lpLabel.isNotBlank()) {
                                val fake = Key(lpLabel, key.longPressAction, longPressLabel = null, longPressAction = null)
                                showKeyPreview(view, fake)
                            }
                        }, longPressDelayMs.toLong())
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (longPressFired && popup != null) {
                        val idx = popupIndexAt(event.rawX, event.rawY)
                        refreshPopupHighlight(idx)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    dismissKeyPreview()
                    val wasLongPress = longPressFired
                    val popupIdx = if (wasLongPress && popup != null) popupIndexAt(event.rawX, event.rawY) else -1
                    val actionToFire = when {
                        !wasLongPress -> key.action
                        popup != null && popupIdx >= 0 && popupIdx < popupActionsCache.size -> popupActionsCache[popupIdx].second
                        popup == null && key.longPressAction != null -> key.longPressAction
                        else -> null
                    }
                    dismissPopup()
                    if (actionToFire != null) {
                        view.post { controller?.handleAction(actionToFire) }
                    }
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun showPopup(anchor: View, actions: List<Pair<String, KeyAction>>, dismissOnCommit: Boolean = true) {
        dismissPopup()
        if (actions.isEmpty()) return
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            val p = dp(4)
            setPadding(p, p, p, p)
        }
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(theme.keyBg)
                setStroke(dp(1), theme.accent)
            }
            addView(row)
        }
        val maxW = (resources.displayMetrics.widthPixels * 0.94f).toInt()
        val pw = PopupWindow(scroll, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false).apply {
            isOutsideTouchable = false
            isTouchable = false
            isFocusable = false
            isClippingEnabled = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            elevation = dp(8).toFloat()
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        popupActionsCache = actions
        popupAnchor = anchor
        val items = mutableListOf<TextView>()
        actions.forEachIndexed { index, (label, _) ->
            val item = TextView(context).apply {
                text = label
                tag = index
                setTextColor(theme.text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                gravity = Gravity.CENTER
                minWidth = dp(44)
                minHeight = dp(44)
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            items.add(item)
            row.addView(item)
        }
        popupItems = items
        popup = pw
        scroll.measure(View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val popupW = scroll.measuredWidth.coerceAtLeast(dp(48)).coerceAtMost(maxW)
        val popupH = scroll.measuredHeight.coerceAtLeast(dp(48))
        pw.width = popupW
        pw.height = popupH
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val xOff = ((anchor.width - popupW) / 2).coerceIn(-anchorLoc[0] + dp(4), resources.displayMetrics.widthPixels - anchorLoc[0] - popupW - dp(4))
        val yOff = -anchor.height - popupH - dp(8)
        try {
            pw.showAsDropDown(anchor, xOff, yOff)
        } catch (_: Exception) {
            try { pw.showAtLocation(anchor, Gravity.NO_GRAVITY, anchorLoc[0] + xOff, anchorLoc[1] + yOff) } catch (_: Exception) {}
        }
    }

    private fun refreshPopupHighlight(selected: Int) {
        popupItems.forEachIndexed { i, tv ->
            val on = i == selected
            tv.setBackgroundColor(if (on) theme.accent else android.graphics.Color.TRANSPARENT)
            tv.setTextColor(if (on) 0xFF10131A.toInt() else theme.text)
        }
    }

    private fun popupIndexAt(rawX: Float, rawY: Float): Int {
        val items = popupItems
        for (i in items.indices) {
            val loc = IntArray(2)
            items[i].getLocationOnScreen(loc)
            val l = loc[0]; val t = loc[1]
            val r = l + items[i].width; val b = t + items[i].height
            if (rawX >= l && rawX <= r && rawY >= t - dp(12) && rawY <= b + dp(12)) return i
        }
        return -1
    }

    private fun dismissPopup() {
        try { popup?.dismiss() } catch (_: Exception) {}
        popup = null
        popupItems = emptyList()
        popupActionsCache = emptyList()
        popupAnchor = null
    }

    private fun showKeyPreview(anchor: View, key: Key) {
        if (!keyPreviewEnabled) return
        val action = key.action
        if (action is KeyAction.Space || action is KeyAction.Enter || action is KeyAction.Backspace ||
            action is KeyAction.Shift || action is KeyAction.SwitchLanguage || action is KeyAction.ModeSymbols ||
            action is KeyAction.ModeSymbols2 || action is KeyAction.ModeLetters || action is KeyAction.ModeNumpad) return
        dismissKeyPreview()
        val tv = TextView(context).apply {
            text = key.label
            setTextColor(theme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, (keyTextSizeSp + 6).coerceAtMost(30).toFloat())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = dp(54)
            minHeight = dp(54)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(theme.keyRadiusDp).toFloat()
                setColor(theme.keyBgPressed)
                setStroke(dp(1), theme.accent)
            }
        }
        val pw = PopupWindow(tv, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false).apply {
            elevation = dp(8).toFloat()
            isClippingEnabled = false
            isTouchable = false
            isFocusable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        keyPreview = pw
        keyPreviewAnchor = anchor
        val showRunnable = Runnable {
            keyPreviewShowRunnable = null
            if (keyPreview != pw) return@Runnable
            if (!anchor.isAttachedToWindow || !isAttachedToWindow) {
                if (keyPreview == pw) dismissKeyPreview()
                return@Runnable
            }
            val w = tv.minWidth.coerceAtLeast(anchor.width)
            val xOff = (anchor.width - w) / 2
            try {
                pw.showAsDropDown(anchor, xOff, -anchor.height - dp(60))
            } catch (_: Exception) {
                if (keyPreview == pw) dismissKeyPreview()
            }
        }
        keyPreviewShowRunnable = showRunnable
        anchor.post(showRunnable)
    }

    private fun dismissKeyPreview() {
        keyPreviewShowRunnable?.let { keyPreviewAnchor?.removeCallbacks(it) }
        keyPreviewShowRunnable = null
        keyPreviewAnchor = null
        try { keyPreview?.dismiss() } catch (_: Exception) {}
        keyPreview = null
    }

    private fun longPressAlternates(label: String): List<Pair<String, KeyAction>> {
        val alts: List<String> = when (label) {
            "•" -> listOf("◦", "●", "▪", "▫")
            "'" -> listOf("`", "´", "‘", "’")
            "\"" -> listOf("“", "”", "«", "»")
            "✓" -> listOf("✔", "✕", "✗")
            "♡" -> listOf("♥", "❤", "❥")
            "☆" -> listOf("★", "✦", "✧")
            "=" -> listOf("≠", "≈", "≡")
            "+" -> listOf("±", "×", "÷")
            "\\" -> listOf("|", "/", "¦")
            "/" -> listOf("\\", "|", "÷")
            "(" -> listOf("[", "{", "<")
            ")" -> listOf("]", "}", ">")
            "-" -> listOf("–", "—", "_")
            "&" -> listOf("§", "¶")
            "_" -> listOf("-", "–", "—")
            "~" -> listOf("≈", "∼")
            "٪", "%" -> listOf("‰", "°")
            "#" -> listOf("№", "♯")
            "@" -> listOf("©", "®", "™")
            "؟", "?" -> listOf("¿", "‽")
            "!" -> listOf("¡", "‼")
            "؛", ";" -> listOf(":", "…")
            ":" -> listOf("؛", ";", "…")
            "«" -> listOf("<", "‹", "\"")
            "»" -> listOf(">", "›", "\"")
            "*" -> listOf("×", "★", "✦")
            "<" -> listOf("≤", "«", "‹")
            ">" -> listOf("≥", "»", "›")
            else -> emptyList()
        }
        return alts.map { it to KeyAction.Character(it) }
    }

    private fun provideFeedback(view: View) {
        if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundEnabled) audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, soundVolumePercent / 100f)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val LONG_PRESS_MS = 350L // fallback; actual delay from prefs
    }
}
