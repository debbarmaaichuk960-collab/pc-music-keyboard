package com.pcmkeyboard.app;

import android.inputmethodservice.InputMethodService;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PCKeyboardService extends InputMethodService {

    private final int bg = Color.rgb(32, 33, 37);
    private final int frame = Color.rgb(43, 45, 50);
    private final int whiteKey = Color.rgb(239, 240, 244);
    private final int darkKey = Color.rgb(70, 72, 78);
    private final int redKey = Color.rgb(196, 25, 42);

    private boolean shift = false;
    private boolean caps = false;
    private boolean symbols = false;
    private boolean num = false;
    private boolean scr = false;
    private boolean ctrl = false;
    private boolean alt = false;
    private boolean altGr = false;
    private boolean win = false;

    private String pcPanel = "main";
    private String composingWord = "";

    private static final String[] WORDS = {
            "about", "after", "again", "also", "and",
            "because", "best", "can", "come", "do",
            "for", "from", "get", "good", "have",
            "hello", "help", "here", "how", "just",
            "know", "like", "love", "make", "more",
            "new", "now", "please", "the", "their",
            "there", "this", "time", "today", "want",
            "what", "when", "where", "with", "you", "your"
    };

    private static final int LIGHT = 0;
    private static final int DARK = 1;
    private static final int RED = 2;

    private static final String PREFS = "pc_music_keyboard";
    private static final String KEY_LAYOUT = "keyboard_layout";

    private static class KeySpec {
        String main;
        String top;
        String shifted;
        String symbol;
        String action;
        int type;
        float weight;

        KeySpec(String main, String top, String shifted,
                String symbol, int type, float weight, String action) {
            this.main = main;
            this.top = top;
            this.shifted = shifted;
            this.symbol = symbol;
            this.type = type;
            this.weight = weight;
            this.action = action;
        }
    }

    private KeySpec k(String main, String top, String shifted,
                      String symbol, int type, float weight, String action) {
        return new KeySpec(main, top, shifted, symbol, type, weight, action);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    @Override
    public View onCreateInputView() {
        String layout = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LAYOUT, "pc");

        if ("mobile".equals(layout)) {
            return createMobileKeyboard();
        }
        return createPcKeyboard();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            composingWord = "";
            shift = false;
            ctrl = false;
            alt = false;
            altGr = false;
            win = false;
        }
    }

    private void switchLayout(String layout) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAYOUT, layout)
                .apply();

        shift = false;
        symbols = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
        composingWord = "";
        refresh();
    }

    // =====================================
    // PC MECHANICAL KEYBOARD
    // =====================================

    private View createPcKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(5), dp(5), dp(5), dp(7));
        root.setBackgroundColor(bg);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        addChip(toolbar, "× Close", DARK, () -> requestHideSelf(0));
        addChip(toolbar, "⇄ MOBILE", DARK, () -> switchLayout("mobile"));
        addChip(toolbar, "● PC ACTIVE", LIGHT, null);
        addChip(toolbar, caps ? "● CAPS" : "CAPS", caps ? LIGHT : DARK, () -> {
            caps = !caps;
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) sendPcKey(ic, "CAPS_LOCK");
            clearOneShotModifiers();
            refresh();
        });
        addChip(toolbar, num ? "● NUM" : "NUM", num ? LIGHT : DARK, () -> {
            num = !num;
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) sendPcKey(ic, "NUM_LOCK");
            clearOneShotModifiers();
            refresh();
        });
        addChip(toolbar, scr ? "● SCR" : "SCR", scr ? LIGHT : DARK, () -> {
            scr = !scr;
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) sendPcKey(ic, "SCROLL_LOCK");
            clearOneShotModifiers();
            refresh();
        });
        addChip(toolbar, "HHKB 60%", DARK, null);
        addChip(toolbar, "Holy Pandas", DARK, null);
        addChip(toolbar, "Black", DARK, null);
        addChip(toolbar, "◖))", DARK, null);

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(40)));

        // Fixed PC keyboard panel buttons.
        LinearLayout panelTabs = new LinearLayout(this);
        panelTabs.setOrientation(LinearLayout.HORIZONTAL);
        panelTabs.setGravity(Gravity.CENTER);
        panelTabs.setPadding(dp(2), dp(2), dp(2), dp(2));

        addPanelTab(panelTabs, "MAIN", "main");
        addPanelTab(panelTabs, "F-KEYS", "function");
        addPanelTab(panelTabs, "NAV", "navigation");
        addPanelTab(panelTabs, "NUMPAD", "numpad");
        addPanelTab(panelTabs, "MEDIA", "media");

        LinearLayout.LayoutParams tabsParams =
                new LinearLayout.LayoutParams(-1, dp(38));
        tabsParams.bottomMargin = dp(4);
        root.addView(panelTabs, tabsParams);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(4), dp(3), dp(3));
        board.setBackground(shape(frame, 10, Color.rgb(78, 80, 86)));

        if ("main".equals(pcPanel)) {
            // Main number row, including the grave/tilde and Backspace keys.
            addRow(board, new KeySpec[] {
                    k("Esc", "", "", "", RED, 0.9f, "ESC"),
                    k("`", "~", "~", "~", LIGHT, 0.8f, null),
                    k("1", "!", "!", "!", LIGHT, 0.8f, null),
                    k("2", "@", "@", "@", LIGHT, 0.8f, null),
                    k("3", "#", "#", "#", LIGHT, 0.8f, null),
                    k("4", "$", "$", "$", LIGHT, 0.8f, null),
                    k("5", "%", "%", "%", LIGHT, 0.8f, null),
                    k("6", "^", "^", "^", LIGHT, 0.8f, null),
                    k("7", "&", "&", "&", LIGHT, 0.8f, null),
                    k("8", "*", "*", "*", LIGHT, 0.8f, null),
                    k("9", "(", "(", "(", LIGHT, 0.8f, null),
                    k("0", ")", ")", ")", LIGHT, 0.8f, null),
                    k("-", "_", "_", "_", LIGHT, 0.8f, null),
                    k("=", "+", "+", "+", LIGHT, 0.8f, null),
                    k("⌫", "", "", "", DARK, 1.25f, "BACKSPACE")
            }, 47);

            // QWERTY row, including the Backslash key.
            addRow(board, new KeySpec[] {
                    k("Tab", "", "", "", DARK, 1.25f, "TAB"),
                    k("q", "", "Q", "@", LIGHT, 1, null),
                    k("w", "", "W", "#", LIGHT, 1, null),
                    k("e", "", "E", "$", LIGHT, 1, null),
                    k("r", "", "R", "%", LIGHT, 1, null),
                    k("t", "", "T", "&", LIGHT, 1, null),
                    k("y", "", "Y", "-", LIGHT, 1, null),
                    k("u", "", "U", "+", LIGHT, 1, null),
                    k("i", "", "I", "(", LIGHT, 1, null),
                    k("o", "", "O", ")", LIGHT, 1, null),
                    k("p", "", "P", "*", LIGHT, 1, null),
                    k("[", "{", "{", "{", LIGHT, 1, null),
                    k("]", "}", "}", "}", LIGHT, 1, null),
                    k("\\", "|", "|", "|", LIGHT, 1, null)
            }, 48);

            // Home row with a real Caps Lock key.
            addRow(board, new KeySpec[] {
                    k("Caps", "", "", "", DARK, 1.45f, "CAPS"),
                    k("a", "", "A", "!", LIGHT, 1, null),
                    k("s", "", "S", "\"", LIGHT, 1, null),
                    k("d", "", "D", "'", LIGHT, 1, null),
                    k("f", "", "F", ":", LIGHT, 1, null),
                    k("g", "", "G", ";", LIGHT, 1, null),
                    k("h", "", "H", "/", LIGHT, 1, null),
                    k("j", "", "J", "?", LIGHT, 1, null),
                    k("k", "", "K", "=", LIGHT, 1, null),
                    k("l", "", "L", "_", LIGHT, 1, null),
                    k(";", ":", ":", ":", LIGHT, 1, null),
                    k("'", "\"", "\"", "\"", LIGHT, 1, null),
                    k("Enter", "", "", "", RED, 1.55f, "ENTER")
            }, 48);

            // Bottom letter row with left and right Shift.
            addRow(board, new KeySpec[] {
                    k("Shift", "", "", "", DARK, 1.55f, "SHIFT"),
                    k("z", "", "Z", "<", LIGHT, 1, null),
                    k("x", "", "X", ">", LIGHT, 1, null),
                    k("c", "", "C", "~", LIGHT, 1, null),
                    k("v", "", "V", "^", LIGHT, 1, null),
                    k("b", "", "B", "`", LIGHT, 1, null),
                    k("n", "", "N", "[", LIGHT, 1, null),
                    k("m", "", "M", "]", LIGHT, 1, null),
                    k(",", "<", "<", "<", LIGHT, 1, null),
                    k(".", ">", ">", ">", LIGHT, 1, null),
                    k("/", "?", "?", "?", LIGHT, 1, null),
                    k("Shift", "", "", "", DARK, 1.55f, "SHIFT")
            }, 48);

            // Full bottom modifier row: left/right Ctrl, Win, Alt, AltGr and Menu.
            addRow(board, new KeySpec[] {
                    k("Ctrl", "", "", "", DARK, 1.15f, "CTRL"),
                    k("Win", "", "", "", DARK, 1.0f, "WIN"),
                    k("Alt", "", "", "", DARK, 1.0f, "ALT"),
                    k("", "", "", "", LIGHT, 3.4f, "SPACE"),
                    k("AltGr", "", "", "", DARK, 1.0f, "ALTGR"),
                    k("Win", "", "", "", DARK, 1.0f, "WIN"),
                    k("Menu", "", "", "MENU", DARK, 1.0f, "PC_SEND"),
                    k("Ctrl", "", "", "", DARK, 1.15f, "CTRL")
            }, 49);

        } else if ("function".equals(pcPanel)) {
            addRow(board, new KeySpec[] {
                    k("Print", "", "", "SYSRQ", DARK, 1.5f, "PC_SEND"),
                    k("Scroll", "", "", "SCROLL_LOCK", DARK, 1.5f, "SCROLL_LOCK"),
                    k("Pause", "", "", "BREAK", DARK, 1.5f, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("F1", "", "", "F1", LIGHT, 1, "PC_SEND"),
                    k("F2", "", "", "F2", LIGHT, 1, "PC_SEND"),
                    k("F3", "", "", "F3", LIGHT, 1, "PC_SEND"),
                    k("F4", "", "", "F4", LIGHT, 1, "PC_SEND"),
                    k("F5", "", "", "F5", LIGHT, 1, "PC_SEND"),
                    k("F6", "", "", "F6", LIGHT, 1, "PC_SEND")
            }, 48);

            addRow(board, new KeySpec[] {
                    k("F7", "", "", "F7", LIGHT, 1, "PC_SEND"),
                    k("F8", "", "", "F8", LIGHT, 1, "PC_SEND"),
                    k("F9", "", "", "F9", LIGHT, 1, "PC_SEND"),
                    k("F10", "", "", "F10", LIGHT, 1, "PC_SEND"),
                    k("F11", "", "", "F11", LIGHT, 1, "PC_SEND"),
                    k("F12", "", "", "F12", LIGHT, 1, "PC_SEND")
            }, 48);

        } else if ("navigation".equals(pcPanel)) {
            addRow(board, new KeySpec[] {
                    k("Ins", "", "", "INSERT", DARK, 1, "PC_SEND"),
                    k("Home", "", "", "MOVE_HOME", LIGHT, 1, "PC_SEND"),
                    k("PgUp", "", "", "PAGE_UP", LIGHT, 1, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("Del", "", "", "FORWARD_DEL", RED, 1, "PC_SEND"),
                    k("End", "", "", "MOVE_END", LIGHT, 1, "PC_SEND"),
                    k("PgDn", "", "", "PAGE_DOWN", LIGHT, 1, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("←", "", "", "DPAD_LEFT", LIGHT, 1, "PC_SEND"),
                    k("↓", "", "", "DPAD_DOWN", LIGHT, 1, "PC_SEND"),
                    k("↑", "", "", "DPAD_UP", LIGHT, 1, "PC_SEND"),
                    k("→", "", "", "DPAD_RIGHT", LIGHT, 1, "PC_SEND")
            }, 48);

        } else if ("numpad".equals(pcPanel)) {
            addRow(board, new KeySpec[] {
                    k("Num Lock", "", "", "NUM_LOCK", DARK, 1.5f, "NUM_LOCK"),
                    k("/", "", "", "NUMPAD_DIVIDE", LIGHT, 1, "PC_SEND"),
                    k("*", "", "", "NUMPAD_MULTIPLY", LIGHT, 1, "PC_SEND"),
                    k("-", "", "", "NUMPAD_SUBTRACT", LIGHT, 1, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("7", "", "", "NUMPAD_7", LIGHT, 1, "PC_SEND"),
                    k("8", "", "", "NUMPAD_8", LIGHT, 1, "PC_SEND"),
                    k("9", "", "", "NUMPAD_9", LIGHT, 1, "PC_SEND"),
                    k("+", "", "", "NUMPAD_ADD", RED, 1, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("4", "", "", "NUMPAD_4", LIGHT, 1, "PC_SEND"),
                    k("5", "", "", "NUMPAD_5", LIGHT, 1, "PC_SEND"),
                    k("6", "", "", "NUMPAD_6", LIGHT, 1, "PC_SEND"),
                    k("Enter", "", "", "NUMPAD_ENTER", RED, 1, "PC_SEND")
            }, 46);

            addRow(board, new KeySpec[] {
                    k("1", "", "", "NUMPAD_1", LIGHT, 1, "PC_SEND"),
                    k("2", "", "", "NUMPAD_2", LIGHT, 1, "PC_SEND"),
                    k("3", "", "", "NUMPAD_3", LIGHT, 1, "PC_SEND"),
                    k("0", "", "", "NUMPAD_0", LIGHT, 1.5f, "PC_SEND"),
                    k(".", "", "", "NUMPAD_DOT", LIGHT, 1, "PC_SEND")
            }, 46);
        } else if ("media".equals(pcPanel)) {
            addRow(board, new KeySpec[] {
                    k("Vol −", "", "", "VOLUME_DOWN", DARK, 1, "PC_SEND"),
                    k("Mute", "", "", "VOLUME_MUTE", DARK, 1, "PC_SEND"),
                    k("Vol +", "", "", "VOLUME_UP", DARK, 1, "PC_SEND")
            }, 48);

            addRow(board, new KeySpec[] {
                    k("Previous", "", "", "MEDIA_PREVIOUS", LIGHT, 1, "PC_SEND"),
                    k("Play / Pause", "", "", "MEDIA_PLAY_PAUSE", RED, 1.4f, "PC_SEND"),
                    k("Stop", "", "", "MEDIA_STOP", LIGHT, 1, "PC_SEND"),
                    k("Next", "", "", "MEDIA_NEXT", LIGHT, 1, "PC_SEND")
            }, 48);

            addRow(board, new KeySpec[] {
                    k("Calculator", "", "", "CALCULATOR", LIGHT, 1.4f, "PC_SEND"),
                    k("Search", "", "", "SEARCH", LIGHT, 1, "PC_SEND"),
                    k("Mail", "", "", "ENVELOPE", LIGHT, 1, "PC_SEND"),
                    k("Browser", "", "", "EXPLORER", LIGHT, 1.2f, "PC_SEND")
            }, 48);
        }

        root.addView(board, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private void addPanelTab(LinearLayout parent, String label, String panel) {
        TextView tab = new TextView(this);
        boolean selected = panel.equals(pcPanel);
        tab.setText(label);
        tab.setTextSize(10);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setTextColor(selected ? Color.DKGRAY : Color.WHITE);
        tab.setBackground(shape(selected ? whiteKey : darkKey, 7,
                Color.rgb(24, 25, 29)));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(32), 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(tab, params);

        tab.setOnClickListener(v -> {
            pcPanel = panel;
            refresh();
        });
    }

    // =====================================
    // NORMAL MOBILE QWERTY KEYBOARD
    // =====================================

    private View createMobileKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(5), dp(5), dp(5), dp(7));
        root.setBackgroundColor(Color.rgb(29, 30, 34));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        addChip(toolbar, "× Close", DARK, () -> requestHideSelf(0));
        addChip(toolbar, "● MOBILE ACTIVE", LIGHT, null);
        addChip(toolbar, "⇄ PC", DARK, () -> switchLayout("pc"));
        addChip(toolbar, caps ? "● CAPS" : "CAPS", caps ? LIGHT : DARK, () -> {
            caps = !caps;
            refresh();
        });
        addChip(toolbar, symbols ? "ABC" : "?123", DARK, () -> {
            symbols = !symbols;
            refresh();
        });

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(40)));

        // Suggestions appear only on the normal letter keyboard.
        if (!symbols) {
            addMobileSuggestionBar(root);
        }

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(5), dp(3), dp(3));
        board.setBackground(shape(Color.rgb(38, 39, 44), 12,
                Color.rgb(65, 67, 73)));

        if (!symbols) {
            addMobileRow(board, new KeySpec[] {
                    mobileLetter("q", "1"), mobileLetter("w", "2"),
                    mobileLetter("e", "3"), mobileLetter("r", "4"),
                    mobileLetter("t", "5"), mobileLetter("y", "6"),
                    mobileLetter("u", "7"), mobileLetter("i", "8"),
                    mobileLetter("o", "9"), mobileLetter("p", "0")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    mobileLetter("a", "@"), mobileLetter("s", "#"),
                    mobileLetter("d", "$"), mobileLetter("f", "%"),
                    mobileLetter("g", "&"), mobileLetter("h", "-"),
                    mobileLetter("j", "+"), mobileLetter("k", "("),
                    mobileLetter("l", ")")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    k("⇧", "", "", "", DARK, 1.5f, "SHIFT"),
                    mobileLetter("z", "*"), mobileLetter("x", "!"),
                    mobileLetter("c", "\""), mobileLetter("v", "'"),
                    mobileLetter("b", ":"), mobileLetter("n", ";"),
                    mobileLetter("m", "?"),
                    k("⌫", "", "", "", DARK, 1.5f, "BACKSPACE")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    k("?123", "", "", "", DARK, 1.45f, "SYMBOLS"),
                    k(",", "", "", ",", LIGHT, 0.8f, null),
                    k("English", "", "", "", LIGHT, 4.0f, "SPACE"),
                    k(".", "", "", ".", LIGHT, 0.8f, null),
                    k("↵", "", "", "", RED, 1.35f, "ENTER")
            }, 47);

        } else {
            addMobileRow(board, new KeySpec[] {
                    simpleKey("1"), simpleKey("2"), simpleKey("3"),
                    simpleKey("4"), simpleKey("5"), simpleKey("6"),
                    simpleKey("7"), simpleKey("8"), simpleKey("9"),
                    simpleKey("0")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    simpleKey("@"), simpleKey("#"), simpleKey("$"),
                    simpleKey("%"), simpleKey("&"), simpleKey("-"),
                    simpleKey("+"), simpleKey("("), simpleKey(")"),
                    simpleKey("/")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    simpleKey("="), simpleKey("*"), simpleKey("\""),
                    simpleKey("'"), simpleKey(":"), simpleKey(";"),
                    simpleKey("!"), simpleKey("?"),
                    k("⌫", "", "", "", DARK, 1.5f, "BACKSPACE")
            }, 44);

            addMobileRow(board, new KeySpec[] {
                    k("ABC", "", "", "", DARK, 1.4f, "SYMBOLS"),
                    simpleKey(","),
                    k("English", "", "", "", LIGHT, 4.0f, "SPACE"),
                    simpleKey("."),
                    k("↵", "", "", "", RED, 1.35f, "ENTER")
            }, 47);
        }

        root.addView(board, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private KeySpec mobileLetter(String letter, String alternate) {
        return k(letter, "", letter.toUpperCase(), alternate, LIGHT, 1, null);
    }

    private KeySpec simpleKey(String value) {
        return k(value, "", value, value, LIGHT, 1, null);
    }

    private void addMobileSuggestionBar(LinearLayout parent) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(4), dp(2), dp(4), dp(2));
        bar.setBackground(shape(Color.rgb(24, 25, 29), 8,
                Color.rgb(55, 57, 63)));

        int count = 0;
        String prefix = composingWord.toLowerCase();

        for (String word : WORDS) {
            if (count >= 3) {
                break;
            }

            if (!prefix.isEmpty() && word.startsWith(prefix)
                    && !word.equals(prefix)) {
                final String suggestion = word;

                TextView item = new TextView(this);
                item.setText(suggestion);
                item.setTextColor(Color.WHITE);
                item.setTextSize(13);
                item.setTypeface(Typeface.DEFAULT_BOLD);
                item.setGravity(Gravity.CENTER);

                bar.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
                item.setOnClickListener(v -> applyWordSuggestion(suggestion));
                count++;
            }
        }

        if (count == 0) {
            TextView hint = new TextView(this);
            hint.setText(prefix.isEmpty() ? "Word suggestions" : "No suggestions");
            hint.setTextColor(Color.LTGRAY);
            hint.setTextSize(11);
            hint.setGravity(Gravity.CENTER);
            bar.addView(hint, new LinearLayout.LayoutParams(-1, -1));
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(34));
        params.bottomMargin = dp(4);
        parent.addView(bar, params);
    }

    private void applyWordSuggestion(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || composingWord.isEmpty()) {
            return;
        }

        ic.deleteSurroundingText(composingWord.length(), 0);
        ic.commitText(word + " ", 1);
        composingWord = "";
        refresh();
    }

    private void addMobileRow(LinearLayout parent, KeySpec[] keys, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(2), 0, dp(2), 0);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, dp(height));
        rowParams.bottomMargin = dp(6);
        parent.addView(row, rowParams);

        for (KeySpec spec : keys) {
            FrameLayout shell = new FrameLayout(this);

            int base = spec.type == LIGHT ? Color.rgb(105, 107, 113)
                    : spec.type == RED ? Color.rgb(105, 12, 22)
                    : Color.rgb(25, 26, 30);

            int face = spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;

            int foreground = spec.type == LIGHT
                    ? Color.rgb(35, 36, 41) : Color.WHITE;

            shell.setBackground(shape(base, 7, Color.rgb(18, 19, 22)));

            TextView key = new TextView(this);
            key.setText(currentLabel(spec));
            key.setTextColor(foreground);
            key.setGravity(Gravity.CENTER);
            key.setTypeface(Typeface.DEFAULT_BOLD);
            key.setIncludeFontPadding(false);
            key.setTextSize(spec.main.length() > 4 ? 11 : 17);
            key.setBackground(shape(face, 6, Color.rgb(150, 152, 158)));

            FrameLayout.LayoutParams faceParams =
                    new FrameLayout.LayoutParams(-1, -1);
            faceParams.setMargins(dp(1), dp(1), dp(1), dp(4));
            shell.addView(key, faceParams);

            LinearLayout.LayoutParams keyParams =
                    new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(2), 0, dp(2), 0);
            row.addView(shell, keyParams);

            shell.setElevation(dp(1));
            shell.setOnClickListener(v -> handleKey(spec));
        }
    }

    // =====================================
    // SHARED KEYBOARD FUNCTIONS
    // =====================================

    private void addChip(LinearLayout parent, String label,
                         int type, Runnable click) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(10);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(type == LIGHT ? Color.DKGRAY : Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(9), dp(7), dp(9), dp(7));

        int color = type == LIGHT ? whiteKey : darkKey;
        chip.setBackground(shape(color, 7, Color.rgb(24, 25, 29)));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-2, dp(34));
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, params);

        if (click != null) {
            chip.setOnClickListener(v -> click.run());
        }
    }

    private void addRow(LinearLayout parent, KeySpec[] keys, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, dp(height));
        rowParams.bottomMargin = dp(3);
        parent.addView(row, rowParams);

        for (KeySpec spec : keys) {
            FrameLayout shell = new FrameLayout(this);

            int base = spec.type == LIGHT ? Color.rgb(112, 114, 120)
                    : spec.type == RED ? Color.rgb(105, 12, 22)
                    : Color.rgb(27, 28, 32);

            boolean active = isModifierActive(spec.action);
            int face = active ? Color.rgb(255, 193, 7)
                    : spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;

            int foreground = active || spec.type == LIGHT
                    ? Color.rgb(35, 36, 41) : Color.WHITE;

            shell.setBackground(shape(base, 6, Color.rgb(19, 20, 23)));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            content.setPadding(dp(2), dp(1), dp(2), dp(1));
            content.setBackground(shape(face, 4,
                    spec.type == LIGHT
                            ? Color.rgb(190, 192, 198)
                            : Color.rgb(35, 36, 40)));

            FrameLayout.LayoutParams faceParams =
                    new FrameLayout.LayoutParams(-1, -1);
            faceParams.setMargins(dp(2), dp(1), dp(2), dp(5));
            shell.addView(content, faceParams);

            String label = currentLabel(spec);
            boolean hasTop = !symbols && !spec.top.isEmpty();

            if (hasTop) {
                TextView top = new TextView(this);
                top.setText(spec.top);
                top.setTextSize(8);
                top.setTextColor(foreground);
                top.setGravity(Gravity.TOP | Gravity.START);
                top.setPadding(dp(3), 0, 0, 0);
                content.addView(top, new LinearLayout.LayoutParams(
                        -1, 0, 0.55f));
            }

            TextView main = new TextView(this);
            main.setText(label.isEmpty() ? " " : label);
            main.setTextColor(foreground);
            main.setGravity(Gravity.CENTER);
            main.setTypeface(Typeface.DEFAULT_BOLD);
            main.setIncludeFontPadding(false);
            main.setTextSize(label.length() > 3 ? 8 : 15);
            content.addView(main, new LinearLayout.LayoutParams(
                    -1, 0, hasTop ? 1.3f : 1f));

            LinearLayout.LayoutParams keyParams =
                    new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(1), 0, dp(1), 0);
            row.addView(shell, keyParams);

            shell.setElevation(dp(2));
            shell.setOnClickListener(v -> handleKey(spec));
        }
    }

    private String currentLabel(KeySpec spec) {
        if (spec.action != null) {
            return spec.main;
        }
        if (symbols && !spec.symbol.isEmpty()) {
            return spec.symbol;
        }
        boolean letterKey = spec.main.length() == 1
                && Character.isLetter(spec.main.charAt(0));
        // Caps Lock affects letters only; Shift also affects punctuation and numbers.
        boolean useShift = letterKey ? (shift ^ caps) : shift;
        if (useShift && !spec.shifted.isEmpty()) {
            return spec.shifted;
        }
        return spec.main;
    }

    private boolean hasOneShotModifier() {
        return shift || ctrl || alt || altGr || win;
    }

    private boolean isModifierActive(String action) {
        if (action == null) return false;
        switch (action) {
            case "SHIFT": return shift;
            case "CTRL": return ctrl;
            case "ALT": return alt;
            case "ALTGR": return altGr;
            case "WIN": return win;
            case "CAPS": return caps;
            case "NUM_LOCK": return num;
            case "SCROLL_LOCK": return scr;
            default: return false;
        }
    }

    private int getModifierMetaState() {
        int metaState = 0;
        if (shift) metaState |= KeyEvent.META_SHIFT_ON;
        if (ctrl) metaState |= KeyEvent.META_CTRL_ON;
        if (alt) metaState |= KeyEvent.META_ALT_ON;
        if (altGr) {
            metaState |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_RIGHT_ON
                    | KeyEvent.META_CTRL_ON;
        }
        if (win) metaState |= KeyEvent.META_META_ON;
        if (caps) metaState |= KeyEvent.META_CAPS_LOCK_ON;
        if (num) metaState |= KeyEvent.META_NUM_LOCK_ON;
        if (scr) metaState |= KeyEvent.META_SCROLL_LOCK_ON;
        return metaState;
    }

    private void clearOneShotModifiers() {
        shift = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
    }

    private void sendKeyCode(InputConnection ic, int keyCode) {
        if (ic == null || keyCode == KeyEvent.KEYCODE_UNKNOWN) return;

        long downTime = SystemClock.uptimeMillis();
        int metaState = getModifierMetaState();
        ic.sendKeyEvent(new KeyEvent(downTime, downTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        ic.sendKeyEvent(new KeyEvent(downTime, SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, keyCode, 0, metaState));
    }

    private int keyCodeForText(String value) {
        if (value == null || value.length() != 1) {
            return KeyEvent.KEYCODE_UNKNOWN;
        }

        char ch = Character.toUpperCase(value.charAt(0));
        if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
            return KeyEvent.keyCodeFromString("KEYCODE_" + ch);
        }

        switch (ch) {
            case '`': case '~': return KeyEvent.KEYCODE_GRAVE;
            case '-': case '_': return KeyEvent.KEYCODE_MINUS;
            case '=': case '+': return KeyEvent.KEYCODE_EQUALS;
            case '[': case '{': return KeyEvent.KEYCODE_LEFT_BRACKET;
            case ']': case '}': return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case '\\': case '|': return KeyEvent.KEYCODE_BACKSLASH;
            case ';': case ':': return KeyEvent.KEYCODE_SEMICOLON;
            case '\'': case '"': return KeyEvent.KEYCODE_APOSTROPHE;
            case ',': case '<': return KeyEvent.KEYCODE_COMMA;
            case '.': case '>': return KeyEvent.KEYCODE_PERIOD;
            case '/': case '?': return KeyEvent.KEYCODE_SLASH;
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    private void sendPcKey(InputConnection ic, String keyName) {
        if (ic == null || keyName == null || keyName.isEmpty()) return;

        String normalized = keyName;
        if ("PRINT_SCREEN".equals(normalized)) normalized = "SYSRQ";
        if ("PAUSE".equals(normalized)) normalized = "BREAK";
        int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + normalized);
        sendKeyCode(ic, keyCode);
    }

    private void handleKey(KeySpec spec) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        String action = spec.action;

        if (action == null) {
            String value = currentLabel(spec);
            if (!value.isEmpty()) {
                if (ctrl || alt || altGr || win) {
                    // For shortcuts, send the physical key with modifier metadata.
                    int keyCode = keyCodeForText(spec.main);
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        sendKeyCode(ic, keyCode);
                    } else {
                        ic.commitText(value, 1);
                    }
                    composingWord = "";
                } else {
                    ic.commitText(value, 1);
                    if (value.matches("[a-zA-Z']+")) {
                        composingWord += value;
                    } else {
                        composingWord = "";
                    }
                }
            }
            clearOneShotModifiers();
            refresh();
            return;
        }

        switch (action) {
            case "BACKSPACE":
                if (hasOneShotModifier()) {
                    sendKeyCode(ic, KeyEvent.KEYCODE_DEL);
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
                if (!composingWord.isEmpty()) {
                    composingWord = composingWord.substring(0,
                            composingWord.length() - 1);
                }
                clearOneShotModifiers();
                refresh();
                break;

            case "SPACE":
                if (hasOneShotModifier()) {
                    sendKeyCode(ic, KeyEvent.KEYCODE_SPACE);
                } else {
                    ic.commitText(" ", 1);
                }
                composingWord = "";
                clearOneShotModifiers();
                refresh();
                break;

            case "TAB":
                sendKeyCode(ic, KeyEvent.KEYCODE_TAB);
                clearOneShotModifiers();
                refresh();
                break;

            case "ENTER": {
                if (hasOneShotModifier()) {
                    sendKeyCode(ic, KeyEvent.KEYCODE_ENTER);
                } else {
                    EditorInfo info = getCurrentInputEditorInfo();
                    int actionId = info == null ? EditorInfo.IME_ACTION_NONE
                            : info.imeOptions & EditorInfo.IME_MASK_ACTION;
                    if (actionId != EditorInfo.IME_ACTION_NONE
                            && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                        ic.performEditorAction(actionId);
                    } else {
                        ic.commitText("\n", 1);
                    }
                }
                composingWord = "";
                clearOneShotModifiers();
                refresh();
                break;
            }

            case "SHIFT":
                shift = !shift;
                refresh();
                break;

            case "FN":
            case "SYMBOLS":
                symbols = !symbols;
                refresh();
                break;

            case "CAPS":
                caps = !caps;
                sendPcKey(ic, "CAPS_LOCK");
                clearOneShotModifiers();
                refresh();
                break;

            case "CTRL":
                ctrl = !ctrl;
                refresh();
                break;

            case "ALT":
                alt = !alt;
                refresh();
                break;

            case "ALTGR":
                altGr = !altGr;
                refresh();
                break;

            case "WIN":
                win = !win;
                refresh();
                break;

            case "PC_SEND":
                sendPcKey(ic, spec.symbol);
                clearOneShotModifiers();
                refresh();
                break;

            case "NUM_LOCK":
                num = !num;
                sendPcKey(ic, "NUM_LOCK");
                clearOneShotModifiers();
                refresh();
                break;

            case "SCROLL_LOCK":
                scr = !scr;
                sendPcKey(ic, "SCROLL_LOCK");
                clearOneShotModifiers();
                refresh();
                break;

            case "ESC":
                sendPcKey(ic, "ESCAPE");
                clearOneShotModifiers();
                refresh();
                break;

            default:
                break;
        }
    }

    private void refresh() {
        setInputView(onCreateInputView());
    }
}
