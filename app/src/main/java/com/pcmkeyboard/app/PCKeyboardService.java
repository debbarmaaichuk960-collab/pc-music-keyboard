
package com.pcmkeyboard.app;

import android.inputmethodservice.InputMethodService;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
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
        String main, top, shifted, symbol, action;
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
        return new KeySpec(main, top, shifted, symbol,
                type, weight, action);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
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

    private void switchLayout(String layout) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAYOUT, layout)
                .apply();

        shift = false;
        symbols = false;
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
        addChip(toolbar, "MAIN",
        pcPanel.equals("main") ? LIGHT : DARK,
        () -> { pcPanel = "main"; refresh(); });

addChip(toolbar, "F-KEYS",
        pcPanel.equals("function") ? LIGHT : DARK,
        () -> { pcPanel = "function"; refresh(); });

addChip(toolbar, "NAV",
        pcPanel.equals("navigation") ? LIGHT : DARK,
        () -> { pcPanel = "navigation"; refresh(); });

addChip(toolbar, "NUMPAD",
        pcPanel.equals("numpad") ? LIGHT : DARK,
        () -> { pcPanel = "numpad"; refresh(); });
        addChip(toolbar, caps ? "● CAPS" : "CAPS",
                caps ? LIGHT : DARK, () -> {
                    caps = !caps;
                    refresh();
                });
        addChip(toolbar, num ? "● NUM" : "NUM",
                num ? LIGHT : DARK, () -> {
                    num = !num;
                    refresh();
                });
        addChip(toolbar, scr ? "● SCR" : "SCR",
                scr ? LIGHT : DARK, () -> {
                    scr = !scr;
                    refresh();
                });
        addChip(toolbar, "HHKB 60%", DARK, null);
        addChip(toolbar, "Holy Pandas", DARK, null);
        addChip(toolbar, "Black", DARK, null);
        addChip(toolbar, "◖))", DARK, null);

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(40)));


        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(4), dp(3), dp(3));
        board.setBackground(shape(frame, 10, Color.rgb(78, 80, 86)));

        if ("main".equals(pcPanel)) {

        // Row 1
        addRow(board, new KeySpec[] {
            k("Esc","","","",RED,1.25f,"ESC"),
            k("1","!","!","!",LIGHT,1,null),
            k("2","@","@","@",LIGHT,1,null),
            k("3","#","#","#",LIGHT,1,null),
            k("4","$","$","$",LIGHT,1,null),
            k("5","%","%","%",LIGHT,1,null),
            k("6","^","^","^",LIGHT,1,null),
            k("7","&","&","&",LIGHT,1,null),
            k("8","*","*","*",LIGHT,1,null),
            k("9","(","(","(",LIGHT,1,null),
            k("0",")",")",")",LIGHT,1,null),
            k("-","_","_","_",LIGHT,1,null),
            k("=","+","+","+",LIGHT,1,null),
            k("\\","|","|","|",LIGHT,1,null)
        }, 47);

        // Row 2
        addRow(board, new KeySpec[] {
            k("Tab","","","",DARK,1.35f,"TAB"),
            k("q","","Q","@",LIGHT,1,null),
            k("w","","W","#",LIGHT,1,null),
            k("e","","E","$",LIGHT,1,null),
            k("r","","R","%",LIGHT,1,null),
            k("t","","T","&",LIGHT,1,null),
            k("y","","Y","-",LIGHT,1,null),
            k("u","","U","+",LIGHT,1,null),
            k("i","","I","(",LIGHT,1,null),
            k("o","","O",")",LIGHT,1,null),
            k("p","","P","*",LIGHT,1,null),
            k("[","{","{","{",LIGHT,1,null),
            k("]", "}", "}", "}", LIGHT,1,null),
            k("⌫","","","",DARK,1.55f,"BACKSPACE")
        }, 48);

        // Row 3
        addRow(board, new KeySpec[] {
            k("Ctrl","","","",DARK,1.7f,"CTRL"),
            k("a","","A","!",LIGHT,1,null),
            k("s","","S","\"",LIGHT,1,null),
            k("d","","D","'",LIGHT,1,null),
            k("f","","F",":",LIGHT,1,null),
            k("g","","G",";",LIGHT,1,null),
            k("h","","H","/",LIGHT,1,null),
            k("j","","J","?",LIGHT,1,null),
            k("k","","K","=",LIGHT,1,null),
            k("l","","L","_",LIGHT,1,null),
            k(";",":",":",":",LIGHT,1,null),
            k("'","\"","\"","\"",LIGHT,1,null),
            k("Enter","","","",RED,1.8f,"ENTER")
        }, 48);

        // Row 4
        addRow(board, new KeySpec[] {
            k("Shift","","","",DARK,1.9f,"SHIFT"),
            k("z","","Z","<",LIGHT,1,null),
            k("x","","X",">",LIGHT,1,null),
            k("c","","C","~",LIGHT,1,null),
            k("v","","V","^",LIGHT,1,null),
            k("b","","B","`",LIGHT,1,null),
            k("n","","N","[",LIGHT,1,null),
            k("m","","M","]",LIGHT,1,null),
            k(",", "<", "<", "<", LIGHT,1,null),
            k(".",">",">",">",LIGHT,1,null),
            k("/","?","?","?",LIGHT,1,null),
            k("Shift","","","",DARK,1.9f,"SHIFT"),
            k("Fn","","","",DARK,0.85f,"FN")
        }, 48);

        // Row 5
        addRow(board, new KeySpec[] {
            k("Alt","","","",DARK,1.15f,"ALT"),
            k("Win","","","",DARK,1.15f,"WIN"),
            k("","", "", "",LIGHT,5.2f,"SPACE"),
            k("Win","","","",DARK,1.15f,"WIN"),
            k("Alt","","","",DARK,1.15f,"ALT")
        }, 49);


} else if ("function".equals(pcPanel)) {

    // System keys
    addRow(board, new KeySpec[] {
        k("Print", "", "", "SYSRQ", DARK, 1.5f, "PC_SEND"),
        k("Scroll", "", "", "SCROLL_LOCK", DARK, 1.5f, "PC_SEND"),
        k("Pause", "", "", "BREAK", DARK, 1.5f, "PC_SEND")
    }, 46);

    // F1 to F6
    addRow(board, new KeySpec[] {
        k("F1","","","F1",LIGHT,1,"PC_SEND"),
        k("F2","","","F2",LIGHT,1,"PC_SEND"),
        k("F3","","","F3",LIGHT,1,"PC_SEND"),
        k("F4","","","F4",LIGHT,1,"PC_SEND"),
        k("F5","","","F5",LIGHT,1,"PC_SEND"),
        k("F6","","","F6",LIGHT,1,"PC_SEND")
    }, 48);

    // F7 to F12
    addRow(board, new KeySpec[] {
        k("F7","","","F7",LIGHT,1,"PC_SEND"),
        k("F8","","","F8",LIGHT,1,"PC_SEND"),
        k("F9","","","F9",LIGHT,1,"PC_SEND"),
        k("F10","","","F10",LIGHT,1,"PC_SEND"),
        k("F11","","","F11",LIGHT,1,"PC_SEND"),
        k("F12","","","F12",LIGHT,1,"PC_SEND")
    }, 48);

} else if ("navigation".equals(pcPanel)) {

    addRow(board, new KeySpec[] {
        k("Ins","","","INSERT",DARK,1,"PC_SEND"),
        k("Home","","","MOVE_HOME",LIGHT,1,"PC_SEND"),
        k("PgUp","","","PAGE_UP",LIGHT,1,"PC_SEND")
    }, 46);

    addRow(board, new KeySpec[] {
        k("Del","","","FORWARD_DEL",RED,1,"PC_SEND"),
        k("End","","","MOVE_END",LIGHT,1,"PC_SEND"),
        k("PgDn","","","PAGE_DOWN",LIGHT,1,"PC_SEND")
    }, 46);

    addRow(board, new KeySpec[] {
        k("←","","","DPAD_LEFT",LIGHT,1,"PC_SEND"),
        k("↓","","","DPAD_DOWN",LIGHT,1,"PC_SEND"),
        k("↑","","","DPAD_UP",LIGHT,1,"PC_SEND"),
        k("→","","","DPAD_RIGHT",LIGHT,1,"PC_SEND")
    }, 48);

} else if ("numpad".equals(pcPanel)) {

    addRow(board, new KeySpec[] {
        k("Num Lock","","","NUM_LOCK",DARK,1.5f,"NUM_LOCK"),
        k("/","","","NUMPAD_DIVIDE",LIGHT,1,"PC_SEND"),
        k("*","","","NUMPAD_MULTIPLY",LIGHT,1,"PC_SEND"),
        k("-","","","NUMPAD_SUBTRACT",LIGHT,1,"PC_SEND")
    }, 46);

    addRow(board, new KeySpec[] {
        k("7","","","NUMPAD_7",LIGHT,1,"PC_SEND"),
        k("8","","","NUMPAD_8",LIGHT,1,"PC_SEND"),
        k("9","","","NUMPAD_9",LIGHT,1,"PC_SEND"),
        k("+","","","NUMPAD_ADD",RED,1,"PC_SEND")
    }, 46);

    addRow(board, new KeySpec[] {
        k("4","","","NUMPAD_4",LIGHT,1,"PC_SEND"),
        k("5","","","NUMPAD_5",LIGHT,1,"PC_SEND"),
        k("6","","","NUMPAD_6",LIGHT,1,"PC_SEND"),
        k("Enter","","","NUMPAD_ENTER",RED,1,"PC_SEND")
    }, 46);

    addRow(board, new KeySpec[] {
        k("1","","","NUMPAD_1",LIGHT,1,"PC_SEND"),
        k("2","","","NUMPAD_2",LIGHT,1,"PC_SEND"),
        k("3","","","NUMPAD_3",LIGHT,1,"PC_SEND"),
        k("0","","","NUMPAD_0",LIGHT,1.5f,"PC_SEND"),
        k(".","","","NUMPAD_DOT",LIGHT,1,"PC_SEND")
    }, 46);
}


        root.addView(board, new LinearLayout.LayoutParams(-1, -2));
        return root;
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
        addChip(toolbar, caps ? "● CAPS" : "CAPS",
                caps ? LIGHT : DARK, () -> {
                    caps = !caps;
                    refresh();
                });
        addChip(toolbar, symbols ? "ABC" : "?123",
                DARK, () -> {
                    symbols = !symbols;
                    refresh();
                });

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(40)));

        addMobileSuggestionBar(root);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(5), dp(3), dp(3));
        board.setBackground(shape(Color.rgb(38, 39, 44), 12,
                Color.rgb(65, 67, 73)));

        if (!symbols) {
            // QWERTY row
            addMobileRow(board, new KeySpec[] {
                mobileLetter("q","1"), mobileLetter("w","2"),
                mobileLetter("e","3"), mobileLetter("r","4"),
                mobileLetter("t","5"), mobileLetter("y","6"),
                mobileLetter("u","7"), mobileLetter("i","8"),
                mobileLetter("o","9"), mobileLetter("p","0")
            }, 44);

            // Home row
            addMobileRow(board, new KeySpec[] {
                mobileLetter("a","@"), mobileLetter("s","#"),
                mobileLetter("d","$"), mobileLetter("f","%"),
                mobileLetter("g","&"), mobileLetter("h","-"),
                mobileLetter("j","+"), mobileLetter("k","("),
                mobileLetter("l",")")
            }, 44);

            // Shift + bottom row + backspace
            addMobileRow(board, new KeySpec[] {
                k("⇧","","","",DARK,1.5f,"SHIFT"),
                mobileLetter("z","*"), mobileLetter("x","!"),
                mobileLetter("c","\""), mobileLetter("v","'"),
                mobileLetter("b",":"), mobileLetter("n",";"),
                mobileLetter("m","?"),
                k("⌫","","","",DARK,1.5f,"BACKSPACE")
            }, 44);

            // Bottom control row
            addMobileRow(board, new KeySpec[] {
                k("?123","","","",DARK,1.45f,"SYMBOLS"),
                k(",","","",",",LIGHT,0.8f,null),
                k("English","","","",LIGHT,4.0f,"SPACE"),
                k(".","","",".",LIGHT,0.8f,null),
                k("↵","","","",RED,1.35f,"ENTER")
            }, 47);

        } else {
            // Number row
            addMobileRow(board, new KeySpec[] {
                simpleKey("1"), simpleKey("2"), simpleKey("3"),
                simpleKey("4"), simpleKey("5"), simpleKey("6"),
                simpleKey("7"), simpleKey("8"), simpleKey("9"),
                simpleKey("0")
            }, 44);

            // Symbols row
            addMobileRow(board, new KeySpec[] {
                simpleKey("@"), simpleKey("#"), simpleKey("$"),
                simpleKey("%"), simpleKey("&"), simpleKey("-"),
                simpleKey("+"), simpleKey("("), simpleKey(")"),
                simpleKey("/")
            }, 44);

            // More symbols
            addMobileRow(board, new KeySpec[] {
                simpleKey("="), simpleKey("*"), simpleKey("\""),
                simpleKey("'"), simpleKey(":"), simpleKey(";"),
                simpleKey("!"), simpleKey("?"),
                k("⌫","","","",DARK,1.5f,"BACKSPACE")
            }, 44);

            // Return to letters
            addMobileRow(board, new KeySpec[] {
                k("ABC","","","",DARK,1.4f,"SYMBOLS"),
                simpleKey(","),
                k("English","","","",LIGHT,4.0f,"SPACE"),
                simpleKey("."),
                k("↵","","","",RED,1.35f,"ENTER")
            }, 47);
        }

        root.addView(board, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private KeySpec mobileLetter(String letter, String alternate) {
        return k(letter, "", letter.toUpperCase(), alternate,
                LIGHT, 1, null);
    }

    private KeySpec simpleKey(String value) {
        return k(value, "", value, value, LIGHT, 1, null);
    }
    private void addMobileSuggestionBar(LinearLayout parent) {
    LinearLayout bar = new LinearLayout(this);
    bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.setGravity(Gravity.CENTER);
    bar.setPadding(dp(4), dp(2), dp(4), dp(2));
    bar.setBackground(shape(
            Color.rgb(24, 25, 29), 8, Color.rgb(55, 57, 63)));

    int count = 0;
    String prefix = composingWord.toLowerCase();

    for (String word : WORDS) {
        if (count >= 3) break;

        if (!prefix.isEmpty()
                && word.startsWith(prefix)
                && !word.equals(prefix)) {

            final String suggestion = word;

            TextView item = new TextView(this);
            item.setText(suggestion);
            item.setTextColor(Color.WHITE);
            item.setTextSize(13);
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setGravity(Gravity.CENTER);

            bar.addView(item,
                    new LinearLayout.LayoutParams(0, -1, 1));

            item.setOnClickListener(v -> applyWordSuggestion(suggestion));
            count++;
        }
    }

    if (count == 0) {
        TextView hint = new TextView(this);
        hint.setText(prefix.isEmpty()
                ? "Word suggestions"
                : "No suggestions");
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

    if (ic == null || composingWord.isEmpty()) return;

    ic.deleteSurroundingText(composingWord.length(), 0);

    ic.commitText(word + " ", 1);

    composingWord = "";
    refresh();
}
    private void addMobileRow(
            LinearLayout parent, KeySpec[] keys, int height) {

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

            int base = spec.type == LIGHT
                    ? Color.rgb(105, 107, 113)
                    : spec.type == RED
                    ? Color.rgb(105, 12, 22)
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
            key.setBackground(shape(face, 6,
                    Color.rgb(150, 152, 158)));

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

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-2, dp(34));
        p.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, p);

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

            int base = spec.type == LIGHT
                    ? Color.rgb(112, 114, 120)
                    : spec.type == RED
                    ? Color.rgb(105, 12, 22)
                    : Color.rgb(27, 28, 32);

            int face = spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;

            int foreground = spec.type == LIGHT
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
        if (spec.action != null) return spec.main;

        if (symbols && !spec.symbol.isEmpty()) {
            return spec.symbol;
        }

        if ((shift || caps) && !spec.shifted.isEmpty()) {
            return spec.shifted;
        }

        return spec.main;
    }

    private void handleKey(KeySpec spec) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        String action = spec.action;

if (action == null) {
    String value = currentLabel(spec);

    if (!value.isEmpty()) {
        ic.commitText(value, 1);

        if (value.matches("[a-zA-Z']+")) {
            composingWord += value;
        } else {
            composingWord = "";
        }
    }

    if (shift) {
        shift = false;
    }

    refresh();
    return;
}
        switch (action) {
            case "BACKSPACE":
    ic.deleteSurroundingText(1, 0);

    if (!composingWord.isEmpty()) {
        composingWord = composingWord.substring(
                0, composingWord.length() - 1);
    }

    refresh();
    break;


            case "SPACE":
    ic.commitText(" ", 1);
    composingWord = "";
    refresh();
    break;

            case "TAB":
                ic.commitText("\t", 1);
                break;

            case "ENTER": {
                EditorInfo info = getCurrentInputEditorInfo();
                int actionId = info == null
                        ? EditorInfo.IME_ACTION_NONE
                        : info.imeOptions & EditorInfo.IME_MASK_ACTION;

                if (actionId != EditorInfo.IME_ACTION_NONE
                        && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId);
                } else {
                    ic.commitText("\n", 1);
                }
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
                refresh();
                break;

                case "PC_SEND":
    sendPcKey(ic, spec.symbol);
    break;

case "NUM_LOCK":
    num = !num;
    sendPcKey(ic, "NUM_LOCK");
    refresh();
    break;

            case "ESC":
                ic.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
                ic.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE));
                break;

            default:
                break;
        }
    }

    private void sendPcKey(InputConnection ic, String keyName) {
    if (ic == null || keyName == null || keyName.isEmpty()) {
        return;
    }

    int keyCode = KeyEvent.keyCodeFromString(
            "KEYCODE_" + keyName);

    if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
        return;
    }

    ic.sendKeyEvent(new KeyEvent(
            KeyEvent.ACTION_DOWN, keyCode));

    ic.sendKeyEvent(new KeyEvent(
            KeyEvent.ACTION_UP, keyCode));
}

    private void refresh() {
        setInputView(onCreateInputView());
    }
}
