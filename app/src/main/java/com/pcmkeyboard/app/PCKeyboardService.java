package com.pcmkeyboard.app;

import android.Manifest;
import android.content.Intent;
import android.media.AudioManager;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.PopupWindow;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PCKeyboardService extends InputMethodService {

    // ---------- theme (loaded from prefs, changed via ThemeActivity) ----------
    private int bg, frame, whiteKey, darkKey, redKey, accent, mutedText, darkText;
    private Bitmap bgBitmap;
    private long bgStamp = -1;
    private int dimAlpha = 110;
    private boolean hapticOn = true;
    private long lastStamp = 0;
    private String voiceLang = "en-IN";
    private int gradEnd = 0;
    private int pcStyle = 0;      // 0 classic 3D, 1 flat rounded, 2 outline, 3 minimal
    private int mobileStyle = 3;
    private String bgName = "";
    private boolean hapticForce = true;
    private boolean soundOn = false;
    // voice typing bookkeeping
    private boolean voiceComposing = false;   // true while WE have unfinished partial text in the field
    private int voiceSession = 0;             // invalidates callbacks of old recognizers
    private long lastWordsReset = 0;
    private PopupWindow previewPopup;

    // bg, frame, lightKey, darkKey, redKey, accent, muted, darkKeyText, gradientEnd (0 = solid)
    static final String[] THEME_NAMES = {
            "Midnight", "Default", "Default dark", "Purple", "Green", "Blue", "Cyan", "Red",
            "Pink", "Orange", "Peach", "Sky", "Mint", "Dusk", "Ocean", "Ember"};
    static final int[][] THEMES = {
            {Color.rgb(8, 15, 28), Color.rgb(18, 30, 48), Color.rgb(225, 235, 246),
                    Color.rgb(39, 57, 80), Color.rgb(255, 103, 82), Color.rgb(48, 215, 194),
                    Color.rgb(154, 174, 197), Color.WHITE, 0},
            {Color.rgb(232, 237, 244), Color.rgb(205, 214, 226), Color.WHITE,
                    Color.rgb(170, 184, 203), Color.rgb(255, 103, 82), Color.rgb(0, 150, 136),
                    Color.rgb(90, 105, 125), Color.rgb(20, 30, 45), 0},
            {Color.BLACK, Color.rgb(12, 12, 14), Color.rgb(215, 215, 220),
                    Color.rgb(40, 40, 46), Color.rgb(230, 70, 70), Color.rgb(0, 200, 120),
                    Color.rgb(130, 130, 140), Color.WHITE, 0},
            {Color.rgb(20, 10, 36), Color.rgb(38, 22, 66), Color.rgb(235, 226, 250),
                    Color.rgb(70, 48, 112), Color.rgb(255, 95, 140), Color.rgb(180, 130, 255),
                    Color.rgb(176, 160, 208), Color.WHITE, 0},
            // colours
            {Color.rgb(0, 96, 80), Color.rgb(0, 120, 100), Color.rgb(224, 247, 242),
                    Color.rgb(0, 150, 125), Color.rgb(255, 112, 67), Color.rgb(255, 214, 64),
                    Color.rgb(178, 223, 214), Color.WHITE, 0},
            {Color.rgb(21, 101, 192), Color.rgb(30, 120, 220), Color.rgb(227, 238, 252),
                    Color.rgb(25, 80, 160), Color.rgb(255, 112, 67), Color.rgb(255, 235, 59),
                    Color.rgb(187, 214, 245), Color.WHITE, 0},
            {Color.rgb(128, 222, 234), Color.rgb(77, 208, 225), Color.WHITE,
                    Color.rgb(0, 151, 167), Color.rgb(255, 112, 67), Color.rgb(0, 96, 100),
                    Color.rgb(0, 96, 110), Color.WHITE, 0},
            {Color.rgb(183, 28, 28), Color.rgb(211, 47, 47), Color.rgb(255, 235, 238),
                    Color.rgb(140, 20, 20), Color.rgb(255, 160, 0), Color.rgb(255, 214, 64),
                    Color.rgb(255, 205, 210), Color.WHITE, 0},
            {Color.rgb(194, 24, 91), Color.rgb(216, 67, 121), Color.rgb(252, 228, 236),
                    Color.rgb(150, 20, 70), Color.rgb(255, 171, 64), Color.rgb(255, 235, 59),
                    Color.rgb(248, 187, 208), Color.WHITE, 0},
            {Color.rgb(230, 81, 0), Color.rgb(245, 124, 0), Color.rgb(255, 243, 224),
                    Color.rgb(180, 60, 0), Color.rgb(0, 121, 107), Color.rgb(255, 241, 118),
                    Color.rgb(255, 224, 178), Color.WHITE, 0},
            // light gradients
            {Color.rgb(255, 236, 210), Color.rgb(255, 226, 208), Color.WHITE,
                    Color.rgb(240, 185, 165), Color.rgb(255, 103, 82), Color.rgb(214, 69, 65),
                    Color.rgb(130, 80, 70), Color.rgb(60, 30, 30), Color.rgb(252, 182, 159)},
            {Color.rgb(224, 247, 250), Color.rgb(200, 232, 248), Color.WHITE,
                    Color.rgb(129, 190, 230), Color.rgb(255, 112, 67), Color.rgb(2, 119, 189),
                    Color.rgb(40, 90, 130), Color.rgb(10, 40, 70), Color.rgb(129, 212, 250)},
            {Color.rgb(232, 245, 233), Color.rgb(205, 232, 208), Color.WHITE,
                    Color.rgb(150, 205, 155), Color.rgb(255, 112, 67), Color.rgb(46, 125, 50),
                    Color.rgb(50, 100, 55), Color.rgb(20, 50, 25), Color.rgb(165, 214, 167)},
            // dark gradients
            {Color.rgb(44, 62, 80), Color.rgb(40, 36, 72), Color.rgb(235, 230, 250),
                    Color.rgb(75, 62, 120), Color.rgb(255, 95, 140), Color.rgb(255, 200, 100),
                    Color.rgb(190, 180, 220), Color.WHITE, Color.rgb(142, 68, 173)},
            {Color.rgb(15, 32, 39), Color.rgb(20, 45, 58), Color.rgb(225, 240, 245),
                    Color.rgb(40, 75, 95), Color.rgb(255, 112, 67), Color.rgb(100, 230, 220),
                    Color.rgb(150, 190, 205), Color.WHITE, Color.rgb(44, 83, 100)},
            {Color.rgb(30, 10, 10), Color.rgb(50, 18, 16), Color.rgb(255, 235, 225),
                    Color.rgb(100, 40, 30), Color.rgb(255, 138, 0), Color.rgb(255, 200, 80),
                    Color.rgb(210, 160, 140), Color.WHITE, Color.rgb(120, 30, 20)}
    };

    // ---------- state ----------
    private boolean shift, caps, symbols, num, scr, ctrl, alt, altGr, win;
    private boolean mobileLayout = false;
    private boolean emojiOpen = false;
    private boolean menuOpen = false;
    private boolean needRefresh = false;
    private int symPage = 0;
    private int emojiCat = 0;
    private long lastShiftTap = 0;

    private String pcPanel = "main";
    private String composingWord = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> userWords = new ArrayList<>();

    // voice
    private SpeechRecognizer recognizer;
    private boolean listening = false;
    private TextView micView;
    private LinearLayout sugMiddle;

    private static final String[] WORDS = {
            "about", "after", "again", "also", "and", "always", "any", "are", "ask",
            "because", "before", "best", "better", "big", "but", "call", "can", "come",
            "could", "day", "do", "does", "done", "down", "for", "from", "get", "give",
            "go", "good", "great", "have", "hello", "help", "here", "home", "how", "just",
            "know", "like", "little", "look", "love", "make", "many", "meet", "more",
            "need", "new", "night", "now", "okay", "only", "please", "right", "see",
            "send", "should", "sorry", "tell", "thanks", "thank", "that", "the", "their",
            "there", "think", "this", "time", "today", "tomorrow", "want", "what", "when",
            "where", "which", "with", "work", "would", "yes", "you", "your",
            // Hinglish
            "aap", "accha", "achha", "agar", "ab", "aur", "bahut", "bhai", "bhi", "bilkul",
            "chalo", "dekho", "haan", "hai", "hain", "hum", "kaha", "kahan", "kaise",
            "kab", "karo", "karna", "kaun", "kya", "kyu", "kyun", "kuch", "mera", "meri",
            "main", "mujhe", "nahi", "nahin", "padhai", "phir", "raha", "rahi", "sab",
            "tum", "tumhara", "theek", "thik", "toh", "wala", "yaar", "zaroor"
    };

    private static final String[] EMOJI_TABS = {"😀", "❤️", "🐱", "🍔", "⚽", "🎭"};
    private static final String[] EMOJI_DATA = {
            "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😍 🥰 😘 😗 😋 😛 😜 🤪 😎 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 😶 😐 😑 😬 🙄 😯 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕",
            "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 👍 👎 👌 ✌️ 🤞 🤟 🤘 👋 🤚 ✋ 🙏 👏 🙌 🤝 💪 👀 🔥 ✨ ⭐ 🌟 💯 ✅ ❌ 🎉 🎊",
            "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🦆 🦅 🦉 🐺 🐴 🦄 🐝 🦋 🐢 🐍 🐙 🐬 🐳 🐟",
            "🍎 🍌 🍇 🍓 🍉 🍑 🍍 🥭 🍅 🥕 🌽 🍕 🍔 🍟 🌭 🌮 🍗 🍜 🍝 🍣 🍩 🍪 🎂 🍫 🍿 ☕ 🍵 🥤 🍺",
            "⚽ 🏀 🏏 🏈 🎾 🏐 🎱 🏓 🎮 🎯 🎲 🎧 🎤 🎸 🎹 🥁 🎬 📷 🚗 ✈️ 🚀 🏠 📱 💻 ⌚ 💡 🔔 🎁 💰",
            "(╯°□°)╯︵ ┻━┻|¯\\_(ツ)_/¯|( ͡° ͜ʖ ͡°)|ಠ_ಠ|(づ｡◕‿‿◕｡)づ|ʕ•ᴥ•ʔ|(≧◡≦)|(｡♥‿♥｡)|♪┏(・o･)┛♪|(¬_¬)|"
                    + "Good Morning ☀️|Good Night 🌙|Thank you 🙏|Happy Birthday 🎂🎉|I love you ❤️|Miss you 💔|"
                    + "Congratulations 🎊|Bhai 🔥🔥🔥|Mast hai 😎👍|Kya baat hai! 👏|Sorry 🥺🙏|OK Done ✅"
    };

    private static final int LIGHT = 0;
    private static final int DARK = 1;
    private static final int RED = 2;
    private static final int SPACER = 3;

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
        return new KeySpec(main, top, shifted, symbol, type, weight, action);
    }

    private KeySpec gap(float w) {
        return k("", "", "", "", SPACER, w, null);
    }

    // PC helpers
    private KeySpec pcKey(String main, String top) {
        return k(main, top, top, "", LIGHT, 1f, null);
    }

    private KeySpec pcLetter(String l) {
        return k(l, "", l.toUpperCase(Locale.ROOT), "", LIGHT, 1f, null);
    }

    private KeySpec send(String label, String code, int type, float w) {
        return k(label, "", "", code, type, w, "PC_SEND");
    }

    private KeySpec txt(String label, String text, int type, float w) {
        return k(label, "", "", text, type, w, "TEXT");
    }

    private KeySpec combo(String label, String caption, String combo) {
        return k(label, caption, "", combo, LIGHT, 1f, "COMBO");
    }

    private KeySpec mobileLetter(String letter, String alternate) {
        return k(letter, "", letter.toUpperCase(Locale.ROOT), alternate, LIGHT, 1, null);
    }

    private KeySpec simpleKey(String value) {
        return k(value, "", value, value, LIGHT, 1, null);
    }

    // ---------- util ----------
    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
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

    /** Make colours slightly see-through when a photo background is used. */
    private int a(int c) {
        return bgBitmap != null ? ((c & 0x00FFFFFF) | 0xCC000000) : c;
    }

    private int darken(int c) {
        return Color.rgb((int) (Color.red(c) * 0.6f), (int) (Color.green(c) * 0.6f),
                (int) (Color.blue(c) * 0.6f));
    }

    private int fgFor(KeySpec s, boolean active) {
        if (active) return Color.rgb(7, 28, 39);
        if (s.type == LIGHT) return Color.rgb(35, 36, 41);
        if (s.type == RED) return Color.WHITE;
        return darkText;
    }

    private int labelSp(String s) {
        int n = s.length();
        return n <= 1 ? 18 : n == 2 ? 14 : n == 3 ? 10 : n <= 5 ? 10 : n <= 8 ? 9 : 8;
    }

    /**
     * Key feedback. Uses the system's own keyboard haptic (KEYBOARD_TAP), so it follows the
     * phone's "touch feedback" setting and strength.
     *  - Android 12 and below: FLAG_IGNORE_GLOBAL_SETTING can still be used ("haptic_force").
     *  - Android 13, 14, 15, 16: that flag has no effect any more, so the system setting always
     *    decides. We do NOT try to bypass it.
     * Only if the system refuses to perform the haptic at all (returns false) a short "tick" is
     * played through the Vibrator, and only when the app already holds the VIBRATE permission.
     */
    @SuppressWarnings("deprecation")
    private void haptic(View v) {
        if (hapticOn) {
            boolean done = false;
            if (v != null && v.isAttachedToWindow()) {
                int flags = 0;
                if (hapticForce && Build.VERSION.SDK_INT < 33) {
                    flags = HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
                }
                try {
                    done = v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags);
                } catch (RuntimeException ignored) {
                }
            }
            if (!done) vibrateFallback();
        }
        if (soundOn) {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1);
        }
    }

    private boolean systemHapticEnabled() {
        try {
            return android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Last-resort tick. Needs android.permission.VIBRATE in the manifest, otherwise it does nothing. */
    private void vibrateFallback() {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            if (checkSelfPermission(Manifest.permission.VIBRATE)
                    != PackageManager.PERMISSION_GRANTED) return;
            Vibrator vib;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vib = vm == null ? null : vm.getDefaultVibrator();
            } else {
                vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (vib == null || !vib.hasVibrator()) return;
            VibrationEffect effect = Build.VERSION.SDK_INT >= 29
                    ? VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    : VibrationEffect.createOneShot(10, 60);
            if (Build.VERSION.SDK_INT >= 33) {
                // touch usage: the system applies the user's touch-feedback setting itself
                vib.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
            } else {
                if (!hapticForce && !systemHapticEnabled()) return;
                vib.vibrate(effect);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void showPreview(View key, String text) {
        hidePreview();
        try {
            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(30);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.rgb(35, 36, 41));
            tv.setBackground(shape(whiteKey, 12, Color.rgb(160, 184, 211)));
            int w = Math.max(key.getWidth() + dp(14), dp(48));
            int h = dp(58);
            PopupWindow pw = new PopupWindow(tv, w, h);
            pw.setClippingEnabled(false);
            pw.setTouchable(false);
            pw.setFocusable(false);
            int[] loc = new int[2];
            key.getLocationOnScreen(loc);
            pw.showAtLocation(key, Gravity.TOP | Gravity.START,
                    loc[0] + key.getWidth() / 2 - w / 2, loc[1] - h - dp(2));
            previewPopup = pw;
        } catch (RuntimeException ignored) {
            previewPopup = null;
        }
    }

    private void hidePreview() {
        if (previewPopup != null) {
            try {
                previewPopup.dismiss();
            } catch (RuntimeException ignored) {
            }
            previewPopup = null;
        }
    }

    private Bitmap decodeScaled(String path) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int target = Math.max(720, getResources().getDisplayMetrics().widthPixels);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= target) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, o);
    }

    // ---------- field safety ----------
    private boolean isPasswordField() {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei == null) return false;
        int cls = ei.inputType & InputType.TYPE_MASK_CLASS;
        int var = ei.inputType & InputType.TYPE_MASK_VARIATION;
        if (cls == InputType.TYPE_CLASS_TEXT) {
            return var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        }
        return cls == InputType.TYPE_CLASS_NUMBER
                && var == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }

    /** No suggestions / learning in password, number, phone and "no suggestions" fields. */
    private boolean noSuggestField() {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei == null) return true;
        if (isPasswordField()) return true;
        int cls = ei.inputType & InputType.TYPE_MASK_CLASS;
        if (cls != InputType.TYPE_CLASS_TEXT && cls != 0) return true;
        if ((ei.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return true;
        return (ei.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
    }

    // ---------- system keys (volume, media, apps) ----------
    private boolean launchCategory(String category) {
        try {
            Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Is phone me app nahi mili", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private boolean mediaKey(AudioManager am, int code) {
        if (am == null) return true;
        long t = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0));
        return true;
    }

    private boolean handleSystemKey(String name) {
        if (name == null) return false;
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (name) {
            case "VOLUME_UP":
                if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case "VOLUME_DOWN":
                if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case "VOLUME_MUTE":
                if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                return true;
            case "MEDIA_PLAY_PAUSE": return mediaKey(am, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            case "MEDIA_NEXT": return mediaKey(am, KeyEvent.KEYCODE_MEDIA_NEXT);
            case "MEDIA_PREVIOUS": return mediaKey(am, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            case "MEDIA_STOP": return mediaKey(am, KeyEvent.KEYCODE_MEDIA_STOP);
            case "CALCULATOR": return launchCategory(Intent.CATEGORY_APP_CALCULATOR);
            case "ENVELOPE": return launchCategory(Intent.CATEGORY_APP_EMAIL);
            case "EXPLORER": return launchCategory(Intent.CATEGORY_APP_BROWSER);
            case "SEARCH":
                try {
                    Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "Search app nahi mili", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return false;
        }
    }

    private String describe(String label) {
        switch (label) {
            case "⌫": return "Backspace";
            case "⇧": case "⬆": case "⇪": return "Shift";
            case "↵": case "➜": case "➤": case "✓": return "Enter";
            case "🔍": return "Search";
            case "😊": return "Emoji";
            case "↑": return "Up arrow";
            case "↓": return "Down arrow";
            case "←": return "Left arrow";
            case "→": return "Right arrow";
            default: return label;
        }
    }

    private int sysColor(String name, int fallback) {
        try {
            int id = getResources().getIdentifier(name, "color", "android");
            if (id != 0) return getColor(id);
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private int[] dynamicTheme() {
        if (Build.VERSION.SDK_INT >= 31) {
            int[] f = THEMES[1];
            return new int[]{
                    sysColor("system_accent2_100", f[0]), sysColor("system_accent2_200", f[1]),
                    sysColor("system_accent2_10", f[2]), sysColor("system_accent1_200", f[3]),
                    sysColor("system_accent3_300", f[4]), sysColor("system_accent1_600", f[5]),
                    sysColor("system_accent2_600", f[6]), sysColor("system_accent1_900", f[7]), 0};
        }
        return THEMES[1];
    }

    private void loadTheme(SharedPreferences p) {
        int t = p.getInt("theme", 0);
        int[] c;
        if (t == 100) {   // system auto
            boolean night = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            c = THEMES[night ? 2 : 1];
        } else if (t == 101) {   // dynamic colour
            c = dynamicTheme();
        } else {
            if (t < 0 || t >= THEMES.length) t = 0;
            c = THEMES[t];
        }
        bg = c[0];
        frame = c[1];
        whiteKey = c[2];
        darkKey = c[3];
        redKey = c[4];
        accent = c[5];
        mutedText = c[6];
        darkText = c[7];
        gradEnd = c[8];
        hapticOn = p.getBoolean("haptic", true);
        hapticForce = p.getBoolean("haptic_force", true);
        soundOn = p.getBoolean("sound", false);
        dimAlpha = p.getInt("bg_dim", 110);
        lastStamp = p.getLong("theme_stamp", 0);
        voiceLang = p.getString("voice_lang", "en-IN");
        pcStyle = p.getInt("pc_style", 0);
        mobileStyle = p.getInt("mobile_style", 3);

        String name = p.getString("bg_file", "kb_bg.jpg");
        File f = new File(getFilesDir(), name);
        if (p.getBoolean("bg_image", false) && f.exists()) {
            long stamp = f.lastModified();
            if (bgBitmap == null || stamp != bgStamp || !name.equals(bgName)) {
                try {
                    bgBitmap = decodeScaled(f.getAbsolutePath());
                    bgStamp = stamp;
                    bgName = name;
                } catch (Throwable e) {
                    bgBitmap = null;
                }
            }
        } else {
            bgBitmap = null;
        }
    }

    /** Frame that forces an exact height (the IME window ignores LayoutParams height). */
    private static class FixedHeightFrame extends FrameLayout {
        private final int heightPx;
        private int bottomInset = 0;

        FixedHeightFrame(android.content.Context c, int heightPx) {
            super(c);
            this.heightPx = heightPx;
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            // Android 15+ draws the keyboard behind the navigation bar
            if (Build.VERSION.SDK_INT >= 35) {
                int b = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                if (b != bottomInset) {
                    bottomInset = b;
                    setPadding(0, 0, 0, b);
                    requestLayout();
                }
            }
            return super.onApplyWindowInsets(insets);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec,
                    MeasureSpec.makeMeasureSpec(heightPx + bottomInset, MeasureSpec.EXACTLY));
        }
    }

    /** Wraps the keyboard in a fixed-height frame with optional photo background.
     *  The photo lives only inside this frame, nowhere else. */
    private View wrap(View content, int heightDp) {
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.62f);
        FrameLayout fl = new FixedHeightFrame(this, Math.min(dp(heightDp), maxH));
        if (gradEnd != 0 && bgBitmap == null) {
            fl.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{bg, gradEnd}));
        } else {
            fl.setBackgroundColor(bg);
        }
        if (bgBitmap != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bgBitmap);
            fl.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            View dim = new View(this);
            dim.setBackgroundColor(Color.argb(dimAlpha, 0, 0, 0));
            fl.addView(dim, new FrameLayout.LayoutParams(-1, -1));
        }
        fl.addView(content, new FrameLayout.LayoutParams(-1, -1));
        fl.setClipChildren(true);
        return fl;
    }

    // ---------- lifecycle ----------
    @Override
    public void onCreate() {
        super.onCreate();
        String saved = prefs().getString("user_words", "");
        if (!saved.isEmpty()) {
            for (String w : saved.split(",")) if (!w.isEmpty()) userWords.add(w);
        }
    }

    @Override
    public void onDestroy() {
        destroyRecognizer();
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        SharedPreferences p = prefs();
        loadTheme(p);
        mobileLayout = "mobile".equals(p.getString(KEY_LAYOUT, "pc"));
        micView = null;
        sugMiddle = null;
        return mobileLayout ? createMobileKeyboard() : createPcKeyboard();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            composingWord = "";
            if (shift || ctrl || alt || altGr || win) needRefresh = true;
            shift = false;
            ctrl = false;
            alt = false;
            altGr = false;
            win = false;
            if (emojiOpen || menuOpen) {
                emojiOpen = false;
                menuOpen = false;
                needRefresh = true;
            }
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        long stamp = prefs().getLong("theme_stamp", 0);
        long wr = prefs().getLong("words_reset", 0);
        if (wr != lastWordsReset) {
            lastWordsReset = wr;
            userWords.clear();
        }
        boolean rebuild = needRefresh || stamp != lastStamp;
        needRefresh = false;
        if (mobileLayout && autoCaps()) rebuild = true;
        if (rebuild) refresh();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;   // never take over the whole screen in landscape
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
                                  int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        // a selection appeared while dictating -> stop voice typing right away
        if (listening && newSelStart != newSelEnd) abortVoiceForSelection();
        if (composingWord.isEmpty()) return;
        // cursor moved / text selected -> forget the half-typed word
        if (newSelStart != newSelEnd || Math.abs(newSelStart - oldSelStart) > 3) {
            composingWord = "";
            if (mobileLayout) updateSuggestions();
        }
    }

    @Override
    public void onFinishInput() {
        // runs while the old field is still connected, so a half-dictated phrase is cleared there
        stopVoiceNow();
        super.onFinishInput();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        hidePreview();
        stopVoiceNow();
        super.onFinishInputView(finishingInput);
    }

    private void refresh() {
        handler.removeCallbacksAndMessages(null);
        hidePreview();
        setInputView(onCreateInputView());
    }

    private void showSystemKeyboardPicker() {
        try {
            if (switchToNextInputMethod(false)) return;
        } catch (RuntimeException ignored) {
        }
        InputMethodManager manager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.showInputMethodPicker();
    }

    /** Starts an activity from the keyboard; never crashes the keyboard if it cannot open. */
    private boolean startActivitySafely(Intent i) {
        try {
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            Toast.makeText(this, "Screen khul nahi saki", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void openThemeScreen() {
        Intent i = new Intent(this, ThemeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // start first, hide after: the keyboard window is what lets Android 14+ allow the launch
        if (startActivitySafely(i)) requestHideSelf(0);
    }

    private void switchLayout(String layout) {
        prefs().edit().putString(KEY_LAYOUT, layout).apply();
        shift = false;
        symbols = false;
        emojiOpen = false;
        menuOpen = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
        composingWord = "";
        refresh();
    }

    // =====================================
    // PC KEYBOARD
    // =====================================

    private View createPcKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(3), dp(4), dp(3), dp(3));

        if (menuOpen) {
            buildMenuPanel(root);
            return wrap(root, 340);
        }

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, dp(2), 0, dp(4));
        addPanelTab(tabs, "MAIN", "main");
        addPanelTab(tabs, "F1-12", "function");
        addPanelTab(tabs, "NAV", "navigation");
        addPanelTab(tabs, "NUM", "numpad");
        addPanelTab(tabs, "MEDIA", "media");
        addPanelTab(tabs, "CTRL+", "shortcuts");
        addPanelTab(tabs, "MORE", "extra");

        // menu button (4 squares) after MORE
        TextView gridBtn = new TextView(this);
        gridBtn.setText("▦");
        gridBtn.setTextSize(17);
        gridBtn.setGravity(Gravity.CENTER);
        gridBtn.setTextColor(darkText);
        gridBtn.setBackground(shape(a(darkKey), 8, Color.rgb(57, 80, 108)));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(38), dp(32));
        gp.setMargins(dp(3), 0, dp(1.5f), 0);
        tabs.addView(gridBtn, gp);
        gridBtn.setOnClickListener(v -> {
            haptic(v);
            menuOpen = true;
            refresh();
        });
        root.addView(tabs, new LinearLayout.LayoutParams(-1, dp(36)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(4), dp(3), dp(3));
        if (pcStyle <= 1) board.setBackground(shape(a(frame), 12, Color.rgb(42, 67, 91)));
        buildPcPanel(board);

        scroll.addView(board, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        refreshMicUi();
        return wrap(root, 340);
    }

    private int darkTextOnBg() {
        if (bgBitmap != null) return Color.WHITE;
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg)
                + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? Color.rgb(20, 30, 45) : Color.WHITE;
    }

    private void buildPcPanel(LinearLayout board) {
        if ("main".equals(pcPanel)) {
            // every row sums to 15.5 so all keys are the same size
            addRow(board, new KeySpec[]{
                    k("Esc", "", "", "", RED, 1f, "ESC"),
                    pcKey("`", "~"), pcKey("1", "!"), pcKey("2", "@"), pcKey("3", "#"),
                    pcKey("4", "$"), pcKey("5", "%"), pcKey("6", "^"), pcKey("7", "&"),
                    pcKey("8", "*"), pcKey("9", "("), pcKey("0", ")"), pcKey("-", "_"),
                    pcKey("=", "+"),
                    k("⌫", "", "", "", DARK, 1f, "BACKSPACE")
            }, 48);
            addRow(board, new KeySpec[]{
                    k("Tab", "", "", "", DARK, 1.5f, "TAB"),
                    pcLetter("q"), pcLetter("w"), pcLetter("e"), pcLetter("r"), pcLetter("t"),
                    pcLetter("y"), pcLetter("u"), pcLetter("i"), pcLetter("o"), pcLetter("p"),
                    pcKey("[", "{"), pcKey("]", "}"),
                    k("\\", "|", "|", "", LIGHT, 1.5f, null)
            }, 48);
            addRow(board, new KeySpec[]{
                    k("Caps", "", "", "", DARK, 1.75f, "CAPS"),
                    pcLetter("a"), pcLetter("s"), pcLetter("d"), pcLetter("f"), pcLetter("g"),
                    pcLetter("h"), pcLetter("j"), pcLetter("k"), pcLetter("l"),
                    pcKey(";", ":"), pcKey("'", "\""),
                    k("Enter", "", "", "", RED, 2.25f, "ENTER")
            }, 48);
            addRow(board, new KeySpec[]{
                    k("Shift", "", "", "", DARK, 2.5f, "SHIFT"),
                    pcLetter("z"), pcLetter("x"), pcLetter("c"), pcLetter("v"), pcLetter("b"),
                    pcLetter("n"), pcLetter("m"),
                    pcKey(",", "<"), pcKey(".", ">"), pcKey("/", "?"),
                    send("↑", "DPAD_UP", DARK, 1f),
                    send("Del", "FORWARD_DEL", DARK, 1.5f)
            }, 48);
            addRow(board, new KeySpec[]{
                    k("Ctrl", "", "", "", DARK, 1.5f, "CTRL"),
                    k("Win", "", "", "", DARK, 1.25f, "WIN"),
                    k("Alt", "", "", "", DARK, 1.25f, "ALT"),
                    k("Space", "", "", "", LIGHT, 8f, "SPACE"),
                    send("←", "DPAD_LEFT", DARK, 1f),
                    send("↓", "DPAD_DOWN", DARK, 1f),
                    send("→", "DPAD_RIGHT", DARK, 1f)
            }, 48);

        } else if ("function".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("F1", "F1", LIGHT, 1), send("F2", "F2", LIGHT, 1),
                    send("F3", "F3", LIGHT, 1), send("F4", "F4", LIGHT, 1),
                    send("F5", "F5", LIGHT, 1), send("F6", "F6", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    send("F7", "F7", LIGHT, 1), send("F8", "F8", LIGHT, 1),
                    send("F9", "F9", LIGHT, 1), send("F10", "F10", LIGHT, 1),
                    send("F11", "F11", LIGHT, 1), send("F12", "F12", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    k("Esc", "", "", "", RED, 1, "ESC"),
                    k("PrtSc", "remote", "", "SYSRQ", DARK, 1, "PC_SEND"),
                    k("ScrLk", "remote", "", "SCROLL_LOCK", DARK, 1, "SCROLL_LOCK"),
                    k("Pause", "remote", "", "BREAK", DARK, 1, "PC_SEND")
            }, 46);

        } else if ("navigation".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("Insert", "INSERT", DARK, 1), send("Home", "MOVE_HOME", LIGHT, 1),
                    send("PgUp", "PAGE_UP", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    send("Delete", "FORWARD_DEL", RED, 1), send("End", "MOVE_END", LIGHT, 1),
                    send("PgDn", "PAGE_DOWN", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{gap(1), send("↑", "DPAD_UP", LIGHT, 1), gap(1)}, 46);
            addRow(board, new KeySpec[]{
                    send("←", "DPAD_LEFT", LIGHT, 1), send("↓", "DPAD_DOWN", LIGHT, 1),
                    send("→", "DPAD_RIGHT", LIGHT, 1)
            }, 46);

        } else if ("numpad".equals(pcPanel)) {
            // numpad keys type real characters (Android turns NUMPAD_7 into "Home" when NumLock is off)
            addRow(board, new KeySpec[]{
                    txt("%", "%", DARK, 1), txt("/", "/", LIGHT, 1), txt("*", "*", LIGHT, 1),
                    txt("−", "-", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    txt("7", "7", LIGHT, 1), txt("8", "8", LIGHT, 1), txt("9", "9", LIGHT, 1),
                    txt("+", "+", RED, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    txt("4", "4", LIGHT, 1), txt("5", "5", LIGHT, 1), txt("6", "6", LIGHT, 1),
                    k("⌫", "", "", "", DARK, 1, "BACKSPACE")
            }, 46);
            addRow(board, new KeySpec[]{
                    txt("1", "1", LIGHT, 1), txt("2", "2", LIGHT, 1), txt("3", "3", LIGHT, 1),
                    k("Enter", "", "", "", RED, 1, "ENTER")
            }, 46);
            addRow(board, new KeySpec[]{
                    txt("0", "0", LIGHT, 2), txt(".", ".", LIGHT, 1), txt("=", "=", LIGHT, 1)
            }, 46);

        } else if ("media".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("Vol −", "VOLUME_DOWN", DARK, 1), send("Mute", "VOLUME_MUTE", DARK, 1),
                    send("Vol +", "VOLUME_UP", DARK, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    send("Prev", "MEDIA_PREVIOUS", LIGHT, 1),
                    send("Play/Pause", "MEDIA_PLAY_PAUSE", RED, 1.5f),
                    send("Stop", "MEDIA_STOP", LIGHT, 1), send("Next", "MEDIA_NEXT", LIGHT, 1)
            }, 46);
            addRow(board, new KeySpec[]{
                    send("Calc", "CALCULATOR", LIGHT, 1), send("Search", "SEARCH", LIGHT, 1),
                    send("Mail", "ENVELOPE", LIGHT, 1), send("Browser", "EXPLORER", LIGHT, 1)
            }, 46);

        } else if ("shortcuts".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    combo("Copy", "Ctrl+C", "CTRL+C"), combo("Paste", "Ctrl+V", "CTRL+V"),
                    combo("Cut", "Ctrl+X", "CTRL+X"), combo("Sel All", "Ctrl+A", "CTRL+A")
            }, 48);
            addRow(board, new KeySpec[]{
                    combo("Undo", "Ctrl+Z", "CTRL+Z"), combo("Redo", "Ctrl+Y", "CTRL+Y"),
                    combo("Save", "Ctrl+S", "CTRL+S"), combo("Find", "Ctrl+F", "CTRL+F")
            }, 48);
            addRow(board, new KeySpec[]{
                    combo("Print", "Ctrl+P", "CTRL+P"), combo("New", "Ctrl+N", "CTRL+N"),
                    combo("Open", "Ctrl+O", "CTRL+O"), combo("Bold", "Ctrl+B", "CTRL+B")
            }, 48);
            addRow(board, new KeySpec[]{
                    combo("New Tab", "Ctrl+T", "CTRL+T"), combo("Close", "Ctrl+W", "CTRL+W"),
                    combo("Reload", "Ctrl+R", "CTRL+R"), combo("Switch", "Alt+Tab", "ALT+TAB")
            }, 48);
            addRow(board, new KeySpec[]{
                    combo("Desktop", "Win+D", "WIN+D"), combo("Files", "Win+E", "WIN+E"),
                    combo("Quit", "Alt+F4", "ALT+F4"), combo("Reopen", "Ctrl+Sh+T", "CTRL+SHIFT+T")
            }, 48);

        } else if ("extra".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    k("AltGr", "", "", "", DARK, 1, "ALTGR"),
                    k("Menu", "remote", "", "MENU", DARK, 1, "PC_SEND"),
                    k("Esc", "", "", "", RED, 1, "ESC")
            }, 46);
            addRow(board, new KeySpec[]{
                    k("Tab", "", "", "", DARK, 1, "TAB"),
                    k("⌫", "", "", "", DARK, 1, "BACKSPACE"),
                    k("Enter", "", "", "", RED, 1, "ENTER")
            }, 46);
        }
    }

    private void addPanelTab(LinearLayout parent, String label, String panel) {
        TextView tab = new TextView(this);
        boolean selected = panel.equals(pcPanel);
        tab.setText(label);
        tab.setTextSize(8.5f);
        tab.setSingleLine(true);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setTextColor(selected ? Color.rgb(7, 28, 39) : darkText);
        tab.setBackground(shape(selected ? accent : a(darkKey), 8,
                selected ? accent : Color.rgb(57, 80, 108)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(30), 1f);
        params.setMargins(dp(1.5f), 0, dp(1.5f), 0);
        parent.addView(tab, params);
        tab.setOnClickListener(v -> {
            haptic(v);
            pcPanel = panel;
            refresh();
        });
    }

    private void addRow(LinearLayout parent, KeySpec[] keys, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(height));
        rowParams.bottomMargin = dp(3);
        parent.addView(row, rowParams);

        for (KeySpec spec : keys) {
            if (spec.type == SPACER) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, spec.weight));
                continue;
            }
            FrameLayout shell = new FrameLayout(this);
            boolean active = isModifierActive(spec.action);
            int face = active ? accent
                    : spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;
            int fg;
            boolean boxed = pcStyle <= 1;

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            content.setPadding(dp(1), dp(1), dp(1), dp(1));
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(-1, -1);

            switch (pcStyle) {
                case 1: // flat rounded
                    fg = fgFor(spec, active);
                    content.setBackground(shape(a(face), 9, a(face)));
                    faceParams.setMargins(dp(1.5f), dp(1.5f), dp(1.5f), dp(1.5f));
                    break;
                case 2: // outline
                    fg = active ? accent : spec.type == RED ? redKey : darkTextOnBg();
                    content.setBackground(shape(Color.TRANSPARENT, 9,
                            active ? accent : spec.type == RED ? redKey : mutedText));
                    faceParams.setMargins(dp(1.5f), dp(1.5f), dp(1.5f), dp(1.5f));
                    break;
                case 3: // minimal (no boxes)
                    if (active) {
                        fg = Color.rgb(7, 28, 39);
                        content.setBackground(shape(accent, 9, accent));
                    } else {
                        fg = spec.type == RED ? redKey : darkTextOnBg();
                    }
                    faceParams.setMargins(dp(1.5f), dp(1.5f), dp(1.5f), dp(1.5f));
                    break;
                default: // classic 3D
                    fg = fgFor(spec, active);
                    shell.setBackground(shape(a(darken(face)), 6, Color.rgb(7, 13, 24)));
                    content.setBackground(shape(a(face), 4,
                            spec.type == LIGHT ? Color.rgb(160, 184, 211) : Color.rgb(55, 77, 103)));
                    faceParams.setMargins(dp(1), dp(1), dp(1), dp(3));
                    break;
            }
            shell.addView(content, faceParams);

            String label = currentLabel(spec);
            boolean hasTop = !spec.top.isEmpty();
            if (hasTop) {
                TextView top = new TextView(this);
                top.setText(spec.top);
                top.setTextSize(7.5f);
                top.setSingleLine(true);
                top.setTextColor(boxed && !active && spec.type == LIGHT
                        ? Color.rgb(90, 100, 118) : (boxed ? fg : mutedText));
                top.setGravity(Gravity.CENTER);
                content.addView(top, new LinearLayout.LayoutParams(-1, 0, 0.5f));
            }

            TextView main = new TextView(this);
            main.setText(label.isEmpty() ? " " : label);
            main.setTextColor(fg);
            main.setGravity(Gravity.CENTER);
            main.setTypeface(pcStyle == 3 ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
            main.setIncludeFontPadding(false);
            main.setSingleLine(true);
            main.setTextSize(labelSp(label));
            content.addView(main, new LinearLayout.LayoutParams(-1, 0, hasTop ? 1.2f : 1f));

            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(1), 0, dp(1), 0);
            row.addView(shell, keyParams);
            if (pcStyle == 0) shell.setElevation(dp(2));
            shell.setOnTouchListener(new KeyTouch(spec, false));
            shell.setContentDescription(describe(label));
            shell.setOnClickListener(v -> handleKey(spec));   // TalkBack double-tap
        }
    }

    // =====================================
    // MOBILE KEYBOARD
    // =====================================

    private View createMobileKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(2), dp(2), dp(2), dp(4));

        if (menuOpen) {
            buildMenuPanel(root);
            return wrap(root, 300);
        }
        if (emojiOpen) {
            buildEmojiPanel(root);
            return wrap(root, 300);
        }

        addMobileSuggestionBar(root);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(0, dp(2), 0, 0);

        String enter = enterLabel();
        if (!symbols) {
            addMobileRow(board, new KeySpec[]{
                    mobileLetter("q", "1"), mobileLetter("w", "2"), mobileLetter("e", "3"),
                    mobileLetter("r", "4"), mobileLetter("t", "5"), mobileLetter("y", "6"),
                    mobileLetter("u", "7"), mobileLetter("i", "8"), mobileLetter("o", "9"),
                    mobileLetter("p", "0")
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    gap(0.5f),
                    mobileLetter("a", "@"), mobileLetter("s", "#"), mobileLetter("d", "$"),
                    mobileLetter("f", "%"), mobileLetter("g", "&"), mobileLetter("h", "-"),
                    mobileLetter("j", "+"), mobileLetter("k", "("), mobileLetter("l", ")"),
                    gap(0.5f)
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    k(caps ? "⇪" : (shift ? "⬆" : "⇧"), "", "", "", DARK, 1.4f, "SHIFT"),
                    mobileLetter("z", "*"), mobileLetter("x", "\""), mobileLetter("c", "'"),
                    mobileLetter("v", ":"), mobileLetter("b", ";"), mobileLetter("n", "!"),
                    mobileLetter("m", "?"),
                    k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    k("?123", "", "", "", DARK, 1.4f, "SYMBOLS"),
                    k("😊", "", "", "", DARK, 1f, "EMOJI"),
                    k(",", "", "", "", LIGHT, 0.8f, null),
                    k("English", "", "", "", LIGHT, 4f, "SPACE"),
                    k(".", "", "", "", LIGHT, 0.8f, null),
                    k(enter, "", "", "", RED, 1.4f, "ENTER")
            }, 0);
        } else {
            if (symPage == 0) {
                addMobileRow(board, new KeySpec[]{
                        simpleKey("1"), simpleKey("2"), simpleKey("3"), simpleKey("4"),
                        simpleKey("5"), simpleKey("6"), simpleKey("7"), simpleKey("8"),
                        simpleKey("9"), simpleKey("0")}, 0);
                addMobileRow(board, new KeySpec[]{
                        simpleKey("@"), simpleKey("#"), simpleKey("$"), simpleKey("%"),
                        simpleKey("&"), simpleKey("-"), simpleKey("+"), simpleKey("("),
                        simpleKey(")"), simpleKey("/")}, 0);
                addMobileRow(board, new KeySpec[]{
                        k("=\\<", "", "", "", DARK, 1.4f, "SYMPAGE"),
                        simpleKey("*"), simpleKey("\""), simpleKey("'"), simpleKey(":"),
                        simpleKey(";"), simpleKey("!"), simpleKey("?"),
                        k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")}, 0);
            } else {
                addMobileRow(board, new KeySpec[]{
                        simpleKey("~"), simpleKey("`"), simpleKey("|"), simpleKey("•"),
                        simpleKey("√"), simpleKey("π"), simpleKey("÷"), simpleKey("×"),
                        simpleKey("¶"), simpleKey("∆")}, 0);
                addMobileRow(board, new KeySpec[]{
                        simpleKey("£"), simpleKey("¢"), simpleKey("€"), simpleKey("¥"),
                        simpleKey("^"), simpleKey("°"), simpleKey("="), simpleKey("{"),
                        simpleKey("}"), simpleKey("\\")}, 0);
                addMobileRow(board, new KeySpec[]{
                        k("?123", "", "", "", DARK, 1.4f, "SYMPAGE"),
                        simpleKey("%"), simpleKey("©"), simpleKey("®"), simpleKey("™"),
                        simpleKey("✓"), simpleKey("["), simpleKey("]"),
                        k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")}, 0);
            }
            addMobileRow(board, new KeySpec[]{
                    k("ABC", "", "", "", DARK, 1.4f, "SYMBOLS"),
                    k("😊", "", "", "", DARK, 1f, "EMOJI"),
                    simpleKey(","),
                    k("English", "", "", "", LIGHT, 4f, "SPACE"),
                    simpleKey("."),
                    k(enter, "", "", "", RED, 1.4f, "ENTER")
            }, 0);
        }

        root.addView(board, new LinearLayout.LayoutParams(-1, 0, 1f));
        refreshMicUi();
        return wrap(root, 300);
    }

    private String enterLabel() {
        switch (editorAction()) {
            case EditorInfo.IME_ACTION_SEARCH: return "🔍";
            case EditorInfo.IME_ACTION_SEND: return "➤";
            case EditorInfo.IME_ACTION_GO: return "➜";
            case EditorInfo.IME_ACTION_DONE: return "✓";
            case EditorInfo.IME_ACTION_NEXT: return "→";
            default: return "↵";
        }
    }

    private int editorAction() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return EditorInfo.IME_ACTION_NONE;
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return EditorInfo.IME_ACTION_NONE;
        }
        int act = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        return act == EditorInfo.IME_ACTION_UNSPECIFIED ? EditorInfo.IME_ACTION_NONE : act;
    }

    private int pillFill() {
        return bgBitmap != null ? Color.argb(95, 255, 255, 255) : darkKey;
    }

    private int pillFg() {
        return bgBitmap != null ? Color.WHITE : darkText;
    }

    /** Mobile keys. Style 3 = Gboard-like minimal (default); 0 classic 3D; 1 flat rounded;
     *  2 outline. */
    private void addMobileRow(LinearLayout parent, KeySpec[] keys, int heightDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = heightDp > 0
                ? new LinearLayout.LayoutParams(-1, dp(heightDp))
                : new LinearLayout.LayoutParams(-1, 0, 1f);
        rp.bottomMargin = dp(2);
        parent.addView(row, rp);

        for (KeySpec spec : keys) {
            if (spec.type == SPACER) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, spec.weight));
                continue;
            }
            boolean active = isModifierActive(spec.action);
            String label = currentLabel(spec);
            boolean enter = "ENTER".equals(spec.action);
            boolean pill = "SYMBOLS".equals(spec.action) || "ABC".equals(spec.action)
                    || "SPACE".equals(spec.action);
            boolean letter = spec.action == null && spec.main.length() == 1
                    && Character.isLetter(spec.main.charAt(0));
            int face = active ? accent
                    : spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;

            FrameLayout shell = new FrameLayout(this);
            TextView key = new TextView(this);
            key.setText(label);
            key.setGravity(Gravity.CENTER);
            key.setIncludeFontPadding(false);
            key.setSingleLine(true);
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(-1, -1);
            int blue = Color.rgb(77, 142, 247);

            if (enter) {
                key.setTextColor(Color.WHITE);
                key.setTextSize(22);
                key.setTypeface(Typeface.DEFAULT_BOLD);
                key.setBackground(shape(blue, mobileStyle == 3 ? 30 : 10, blue));
                faceParams.setMargins(dp(3), dp(4), dp(3), dp(4));
            } else if (mobileStyle == 3) {
                if (pill) {
                    key.setTextColor(pillFg());
                    key.setTextSize(label.length() > 4 ? 14 : 15);
                    key.setBackground(shape(pillFill(), 30, pillFill()));
                    faceParams.setMargins(dp(3), dp(4), dp(3), dp(4));
                } else {
                    key.setTextColor(active ? accent : darkTextOnBg());
                    key.setTextSize(letter ? 25 : label.length() <= 2 ? 23 : 15);
                }
            } else {
                int fg;
                if (mobileStyle == 0) {
                    fg = fgFor(spec, active);
                    shell.setBackground(shape(a(darken(face)), 8, Color.rgb(7, 13, 24)));
                    key.setBackground(shape(a(face), 7, Color.rgb(75, 99, 128)));
                    key.setTypeface(Typeface.DEFAULT_BOLD);
                    faceParams.setMargins(dp(1), dp(1), dp(1), dp(4));
                } else if (mobileStyle == 1) {
                    fg = fgFor(spec, active);
                    key.setBackground(shape(a(face), 11, a(face)));
                    faceParams.setMargins(dp(2), dp(3), dp(2), dp(3));
                } else {
                    fg = active ? accent : darkTextOnBg();
                    key.setBackground(shape(Color.TRANSPARENT, 11, active ? accent : mutedText));
                    faceParams.setMargins(dp(2), dp(3), dp(2), dp(3));
                }
                key.setTextColor(fg);
                key.setTextSize(letter ? 22 : label.length() <= 2 ? 20 : label.length() > 4 ? 13 : 15);
            }
            shell.addView(key, faceParams);

            boolean hintKey = spec.action == null && !symbols && letter && !spec.symbol.isEmpty();
            if (hintKey) {
                TextView hint = new TextView(this);
                hint.setText(spec.symbol);
                hint.setTextSize(9);
                hint.setTextColor(bgBitmap != null ? Color.argb(210, 255, 255, 255) : mutedText);
                hint.setIncludeFontPadding(false);
                FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2,
                        Gravity.TOP | Gravity.END);
                hp.setMargins(0, dp(mobileStyle == 3 ? 2 : 5), dp(mobileStyle == 3 ? 4 : 8), 0);
                shell.addView(hint, hp);
            }

            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, -1, spec.weight);
            row.addView(shell, keyParams);
            shell.setOnTouchListener(new KeyTouch(spec, hintKey));
            shell.setContentDescription(describe(label));
            shell.setOnClickListener(v -> handleKey(spec));   // TalkBack double-tap
        }
    }

    // ---------- suggestion bar + voice button ----------
    private void addMobileSuggestionBar(LinearLayout parent) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), 0, dp(6), 0);

        TextView menu = iconButton("▦");
        menu.setTextColor(darkTextOnBg());
        menu.setOnClickListener(v -> {
            haptic(v);
            menuOpen = true;
            refresh();
        });
        bar.addView(menu, new LinearLayout.LayoutParams(dp(44), -1));

        sugMiddle = new LinearLayout(this);
        sugMiddle.setOrientation(LinearLayout.HORIZONTAL);
        sugMiddle.setGravity(Gravity.CENTER);
        bar.addView(sugMiddle, new LinearLayout.LayoutParams(0, -1, 1f));

        micView = iconButton("🎤");
        micView.setOnClickListener(v -> {
            haptic(v);
            toggleVoice();
        });
        micView.setOnLongClickListener(v -> {
            voiceLang = "en-IN".equals(voiceLang) ? "hi-IN" : "en-IN";
            prefs().edit().putString("voice_lang", voiceLang).apply();
            Toast.makeText(this, "Voice: " + ("hi-IN".equals(voiceLang) ? "Hindi" : "English (India)"),
                    Toast.LENGTH_SHORT).show();
            return true;
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(40), dp(40));
        bar.addView(micView, mp);

        parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
        updateSuggestions();
    }

    private TextView iconButton(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(19);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void refreshMicUi() {
        if (micView == null) return;
        micView.setText(listening ? "⏹" : "🎤");
        micView.setTextColor(listening ? Color.WHITE : darkText);
        if (listening) {
            micView.setBackground(shape(redKey, 10, redKey));
        } else if (mobileLayout) {
            micView.setBackground(shape(pillFill(), 20, pillFill()));
        } else {
            micView.setBackground(shape(darkKey, 9, Color.rgb(60, 82, 109)));
        }
    }

    private List<String> getSuggestions() {
        List<String> out = new ArrayList<>();
        String prefix = composingWord.toLowerCase(Locale.ROOT);
        if (prefix.isEmpty() || noSuggestField()) return out;
        boolean cap = Character.isUpperCase(composingWord.charAt(0));
        for (String w : userWords) {
            if (out.size() >= 3) break;
            if (w.startsWith(prefix) && !w.equals(prefix) && !out.contains(w)) out.add(w);
        }
        for (String w : WORDS) {
            if (out.size() >= 3) break;
            if (w.startsWith(prefix) && !w.equals(prefix) && !out.contains(w)) out.add(w);
        }
        if (cap) {
            for (int i = 0; i < out.size(); i++) {
                String w = out.get(i);
                out.set(i, Character.toUpperCase(w.charAt(0)) + w.substring(1));
            }
        }
        return out;
    }

    private void updateSuggestions() {
        if (sugMiddle == null) return;
        sugMiddle.removeAllViews();
        if (listening) {
            TextView t = new TextView(this);
            t.setText("🎙 Bolo… (" + ("hi-IN".equals(voiceLang) ? "Hindi" : "English") + ")");
            t.setTextColor(accent);
            t.setTextSize(14);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setGravity(Gravity.CENTER);
            sugMiddle.addView(t, new LinearLayout.LayoutParams(-1, -1));
            return;
        }
        if (composingWord.isEmpty()) {
            // idle toolbar: sticker/emoji + language, like Gboard
            TextView sticker = iconButton("😊");
            sticker.setOnClickListener(v -> {
                haptic(v);
                emojiOpen = true;
                refresh();
            });
            sugMiddle.addView(sticker, new LinearLayout.LayoutParams(0, -1, 1f));
            TextView lang = iconButton("🌐");
            lang.setOnClickListener(v -> {
                haptic(v);
                showSystemKeyboardPicker();
            });
            sugMiddle.addView(lang, new LinearLayout.LayoutParams(0, -1, 1f));
            return;
        }
        List<String> list = getSuggestions();
        for (int i = 0; i < list.size(); i++) {
            final String word = list.get(i);
            TextView item = new TextView(this);
            item.setText(word);
            item.setTextColor(darkTextOnBg());
            item.setTextSize(16);
            item.setSingleLine(true);
            item.setGravity(Gravity.CENTER);
            sugMiddle.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            item.setOnClickListener(v -> {
                haptic(v);
                applyWordSuggestion(word);
            });
            if (i < list.size() - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.argb(70, 150, 160, 175));
                sugMiddle.addView(div, new LinearLayout.LayoutParams(dp(1), dp(20)));
            }
        }
    }

    private void applyWordSuggestion(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (!composingWord.isEmpty()) {
            CharSequence before = ic.getTextBeforeCursor(composingWord.length(), 0);
            if (before == null || !before.toString().equals(composingWord)) {
                // cursor is no longer after the word we think we typed
                composingWord = "";
                updateSuggestions();
                return;
            }
            ic.deleteSurroundingText(composingWord.length(), 0);
        }
        ic.commitText(word + " ", 1);
        learnWord(word);
        composingWord = "";
        if (autoCaps()) refresh(); else updateSuggestions();
    }

    private void learnWord(String w) {
        if (w == null || noSuggestField()) return;
        String word = w.toLowerCase(Locale.ROOT);
        if (word.length() < 3 || !word.matches("[a-z']+")) return;
        userWords.remove(word);
        userWords.add(0, word);
        while (userWords.size() > 400) userWords.remove(userWords.size() - 1);
        prefs().edit().putString("user_words", android.text.TextUtils.join(",", userWords)).apply();
    }

    // ---------- grid menu (Settings / Share / Languages / Switch to Mobile) ----------
    private void buildMenuPanel(LinearLayout root) {
        String[][] items = {
                {"↩", "Undo", "UNDO"},
                {"⚙", "Settings  ›", "SETTINGS"},
                {"🔗", "Share text", "SHARE"},
                {"🌐", "Languages", "LANG"},
                {"📱", mobileLayout ? "Switch to PC" : "Switch to Mobile", "MOBILE"},
                {"🎤", "Voice typing", "VOICE"},
                {"⌨", "Hide keyboard", "HIDE"}
        };
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(6), dp(6), dp(6), dp(4));

        for (int i = 0; i < items.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 2; j++) {
                if (i + j < items.length) {
                    row.addView(menuPill(items[i + j][0], items[i + j][1], items[i + j][2], false),
                            pillParams(1f));
                } else {
                    row.addView(new View(this), pillParams(1f));
                }
            }
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        LinearLayout back = new LinearLayout(this);
        back.addView(menuPill("⌨", "Back to keyboard", "BACK", true), pillParams(1f));
        list.addView(back, new LinearLayout.LayoutParams(-1, -2));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    private LinearLayout.LayoutParams pillParams(float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), w);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    private View menuPill(String icon, String text, String id, boolean highlight) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(16), 0, dp(12), 0);
        int fill = highlight ? accent : (bgBitmap != null ? Color.argb(110, 200, 200, 200)
                : darkKey);
        pill.setBackground(shape(fill, 27, highlight ? accent : Color.rgb(60, 82, 109)));
        int fg = highlight ? Color.rgb(7, 28, 39) : darkText;

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(22);
        ic.setTextColor(fg);
        ic.setGravity(Gravity.CENTER);
        pill.addView(ic, new LinearLayout.LayoutParams(dp(34), -2));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setSingleLine(true);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(fg);
        tv.setPadding(dp(8), 0, 0, 0);
        pill.addView(tv, new LinearLayout.LayoutParams(0, -2, 1f));

        pill.setOnClickListener(v -> {
            haptic(v);
            runMenuAction(id);
        });
        return pill;
    }

    private void runMenuAction(String id) {
        menuOpen = false;
        switch (id) {
            case "UNDO":
                sendCombo(getCurrentInputConnection(), "CTRL+Z");
                refresh();
                break;
            case "SETTINGS":
                refresh();
                openThemeScreen();
                break;
            case "SHARE":
                refresh();
                shareText();
                break;
            case "LANG":
                refresh();
                showSystemKeyboardPicker();
                break;
            case "MOBILE":
                switchLayout(mobileLayout ? "pc" : "mobile");
                break;
            case "VOICE":
                refresh();
                toggleVoice();
                break;
            case "HIDE":
                refresh();
                requestHideSelf(0);
                break;
            default:
                refresh();
                break;
        }
    }

    /** Shares selected text, or the whole text of the current field. */
    private void shareText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (isPasswordField()) {
            Toast.makeText(this, "Password field ka text share nahi hota", Toast.LENGTH_SHORT).show();
            return;
        }
        String text;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) {
            text = sel.toString();
        } else {
            CharSequence before = ic.getTextBeforeCursor(5000, 0);
            CharSequence after = ic.getTextAfterCursor(5000, 0);
            text = (before == null ? "" : before.toString()) + (after == null ? "" : after.toString());
        }
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Share karne ke liye pehle kuch likho ya select karo",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        Intent chooser = Intent.createChooser(send, "Share via");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivitySafely(chooser);
    }

    // ---------- emoji / stickers ----------
    private void buildEmojiPanel(LinearLayout root) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        for (int i = 0; i < EMOJI_TABS.length; i++) {
            final int idx = i;
            TextView t = iconButton(EMOJI_TABS[i]);
            t.setTextSize(18);
            t.setBackground(shape(idx == emojiCat ? accent : a(darkKey), 9,
                    idx == emojiCat ? accent : Color.rgb(60, 82, 109)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(t, lp);
            t.setOnClickListener(v -> {
                haptic(v);
                emojiCat = idx;
                refresh();
            });
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(36));
        tp.bottomMargin = dp(4);
        root.addView(tabs, tp);

        boolean stickers = emojiCat == EMOJI_DATA.length - 1;
        String[] items = stickers ? EMOJI_DATA[emojiCat].split("\\|")
                : EMOJI_DATA[emojiCat].split(" ");
        int cols = stickers ? 2 : 8;

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(shape(a(frame), 12, Color.rgb(42, 67, 91)));
        list.setPadding(dp(3), dp(4), dp(3), dp(4));

        for (int i = 0; i < items.length; i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < cols; j++) {
                if (i + j < items.length) {
                    final String item = items[i + j];
                    TextView cell = new TextView(this);
                    cell.setText(item);
                    cell.setGravity(Gravity.CENTER);
                    cell.setTextSize(stickers ? 13 : 24);
                    cell.setTextColor(darkTextOnBg());
                    cell.setSingleLine(stickers);
                    if (stickers) {
                        cell.setBackground(shape(a(darkKey), 8, Color.rgb(60, 82, 109)));
                        cell.setTextColor(darkText);
                    }
                    cell.setOnClickListener(v -> {
                        haptic(v);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(stickers ? item + " " : item, 1);
                            composingWord = "";
                        }
                    });
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(stickers ? 44 : 42), 1f);
                    if (stickers) cp.setMargins(dp(3), dp(3), dp(3), dp(3));
                    row.addView(cell, cp);
                } else {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(40), 1f));
                }
            }
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        sv.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        addMobileRow(bottom, new KeySpec[]{
                k("ABC", "", "", "", DARK, 1.5f, "ABC"),
                k("English", "", "", "", LIGHT, 4f, "SPACE"),
                k("⌫", "", "", "", DARK, 1.5f, "BACKSPACE")
        }, 44);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(4);
        root.addView(bottom, bp);
    }

    // =====================================
    // VOICE TYPING
    // =====================================

    private final Runnable voiceStopTimeout = new Runnable() {
        @Override
        public void run() {
            // stopListening() never delivered a result -> give up cleanly
            if (listening) cancelVoice();
        }
    };

    private void toggleVoice() {
        if (listening) {
            if (recognizer != null) {
                try {
                    recognizer.stopListening();   // final result (or error) arrives in the listener
                    handler.postDelayed(voiceStopTimeout, 6000);
                } catch (RuntimeException e) {
                    cancelVoice();
                }
            }
            return;
        }
        if (isPasswordField()) {
            Toast.makeText(this, "Password field me voice typing band hai", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Pehle mic permission allow karo", Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, ThemeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("ask_mic", true);
            if (startActivitySafely(i)) requestHideSelf(0);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Is phone me voice service nahi mili (Google app check karo)",
                    Toast.LENGTH_LONG).show();
            return;
        }
        startVoice();
    }

    /**
     * True when the user has some text selected in the field right now.
     * Fails safe: if the editor cannot tell us (getSelectedText throws), the answer is "yes,
     * there may be a selection", so the callers abort the voice session instead of risking
     * an overwrite of the user's text.
     */
    private boolean hasUserSelection(InputConnection ic) {
        if (ic == null) return false;
        try {
            CharSequence selected = ic.getSelectedText(0);
            return selected != null && selected.length() > 0;
        } catch (RuntimeException e) {
            return true;   // unknown -> treat as selected -> caller aborts voice typing
        }
    }

    /**
     * The user selected text while dictation was active. Voice typing must never replace that
     * selection, so the session is stopped and invalidated (late callbacks are ignored).
     * Text that was already committed is left untouched; unfinished partial text is only
     * finished as normal text (never deleted), because removing it could hit the selection.
     */
    private void abortVoiceForSelection() {
        cancelVoice();   // invalidates the session; clearVoiceComposing() has the selection guard
        Toast.makeText(this, "Text select hai, isliye voice typing band ki (text safe hai)",
                Toast.LENGTH_SHORT).show();
    }

    /** Removes only the unfinished partial text we put in the field. Committed text is never touched. */
    private void clearVoiceComposing() {
        if (!voiceComposing) return;
        voiceComposing = false;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        try {
            CharSequence selected = ic.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                // the user has selected other text meanwhile: keep it, just end composing
                ic.finishComposingText();
                return;
            }
            ic.commitText("", 1);   // replaces only the composing region with nothing
        } catch (RuntimeException ignored) {
        }
    }

    private String voiceErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Kuch sunai nahi diya, phir try karo";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Internet check karo, phir try karo";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Mic use nahi ho pa raha";
            case SpeechRecognizer.ERROR_SERVER:
                return "Voice server error, thodi der baad try karo";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Voice service busy hai, phir try karo";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Mic permission nahi hai";
            case SpeechRecognizer.ERROR_CLIENT:
                return null;   // normal result of cancel(), nothing to tell the user
            case 10:   // ERROR_TOO_MANY_REQUESTS (API 31)
            case 11:   // ERROR_SERVER_DISCONNECTED (API 31)
                return "Voice service abhi available nahi hai";
            case 12:   // ERROR_LANGUAGE_NOT_SUPPORTED (API 33)
            case 13:   // ERROR_LANGUAGE_UNAVAILABLE (API 33)
                return "Ye language voice ke liye available nahi, English/Hindi badal ke dekho";
            default:
                return "Voice typing fail ho gayi";
        }
    }

    private void startVoice() {
        destroyRecognizer();
        final int session = ++voiceSession;
        voiceComposing = false;
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onEvent(int eventType, Bundle params) { }

            @Override
            public void onPartialResults(Bundle partial) {
                if (session != voiceSession || !listening) return;
                ArrayList<String> p = partial == null ? null
                        : partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (p == null || p.isEmpty() || p.get(0).trim().isEmpty()) return;
                InputConnection ic = getCurrentInputConnection();
                if (ic == null || isPasswordField()) return;
                if (hasUserSelection(ic)) {
                    abortVoiceForSelection();
                    return;
                }
                ic.setComposingText(p.get(0), 1);
                voiceComposing = true;
            }

            @Override
            public void onResults(Bundle results) {
                if (session != voiceSession) return;
                ArrayList<String> r = results == null ? null
                        : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (r == null || r.isEmpty() || r.get(0) == null) ? "" : r.get(0).trim();
                InputConnection ic = getCurrentInputConnection();
                if (hasUserSelection(ic)) {
                    abortVoiceForSelection();        // never overwrite the user's selection
                    return;
                }
                if (!text.isEmpty() && ic != null && !isPasswordField()) {
                    voiceComposing = false;          // final text replaces the partial text
                    ic.commitText(text + " ", 1);
                    composingWord = "";
                } else {
                    clearVoiceComposing();
                    if (text.isEmpty()) {
                        Toast.makeText(PCKeyboardService.this, "Kuch sunai nahi diya, phir try karo",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                endVoiceUi();
            }

            @Override
            public void onError(int error) {
                if (session != voiceSession) return;
                clearVoiceComposing();   // drop the unfinished partial text only
                String msg = voiceErrorMessage(error);
                if (msg != null) {
                    Toast.makeText(PCKeyboardService.this, msg, Toast.LENGTH_SHORT).show();
                }
                endVoiceUi();
            }
        });
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLang);
        listening = true;
        if (!mobileLayout) Toast.makeText(this, "🎙 Bolo…", Toast.LENGTH_SHORT).show();
        refreshMicUi();
        updateSuggestions();
        try {
            recognizer.startListening(it);
        } catch (RuntimeException e) {
            clearVoiceComposing();
            Toast.makeText(this, "Voice typing shuru nahi ho saki", Toast.LENGTH_SHORT).show();
            endVoiceUi();
        }
    }

    /** Normal end of a dictation (result or error already handled). */
    private void endVoiceUi() {
        handler.removeCallbacks(voiceStopTimeout);
        listening = false;
        destroyRecognizer();
        refreshMicUi();
        updateSuggestions();
    }

    /** Abort: clears unfinished partial text, keeps everything already committed. */
    private void cancelVoice() {
        handler.removeCallbacks(voiceStopTimeout);
        clearVoiceComposing();
        listening = false;
        destroyRecognizer();
        refreshMicUi();
        updateSuggestions();
    }

    private void stopVoiceNow() {
        if (listening || recognizer != null || voiceComposing) cancelVoice();
    }

    private void destroyRecognizer() {
        voiceSession++;   // callbacks of the old recognizer are ignored from now on
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) {
            }
            recognizer = null;
        }
    }

    // =====================================
    // SHARED
    // =====================================

    private TextView addChip(LinearLayout parent, String label, int type, Runnable click) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(label.length() > 2 ? 10 : 14);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(type == LIGHT ? Color.rgb(6, 30, 39) : darkText);
        chip.setGravity(Gravity.CENTER);
        chip.setMinWidth(dp(34));
        chip.setPadding(dp(6), dp(4), dp(6), dp(4));
        chip.setBackground(shape(type == LIGHT ? accent : a(darkKey), 9,
                type == LIGHT ? accent : Color.rgb(60, 82, 109)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(30));
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, params);
        if (click != null) {
            chip.setOnClickListener(v -> {
                haptic(v);
                click.run();
            });
        }
        return chip;
    }

    /** Touch handling: haptic on press, key fires on release, hold-to-repeat for backspace,
     *  long-press for the number/symbol hint on mobile letters. */
    private class KeyTouch implements View.OnTouchListener {
        private final KeySpec spec;
        private final boolean allowLongPress;
        private boolean consumed = false;
        private boolean repeating = false;
        private View target;

        private final Runnable repeat = new Runnable() {
            @Override public void run() {
                handleKey(spec);
                handler.postDelayed(this, 50);
            }
        };
        private final Runnable longPress = new Runnable() {
            @Override public void run() {
                consumed = true;
                haptic(target);
                commitAlternate(spec);
            }
        };

        KeyTouch(KeySpec spec, boolean allowLongPress) {
            this.spec = spec;
            this.allowLongPress = allowLongPress;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    target = v;
                    consumed = false;
                    repeating = false;
                    v.setAlpha(0.65f);
                    v.setScaleX(0.95f);
                    v.setScaleY(0.95f);
                    haptic(v);
                    if (allowLongPress) showPreview(v, currentLabel(spec));
                    if ("BACKSPACE".equals(spec.action)) {
                        repeating = true;
                        handleKey(spec);
                        handler.postDelayed(repeat, 400);
                    } else if (allowLongPress) {
                        handler.postDelayed(longPress, 380);
                    }
                    return true;
                case MotionEvent.ACTION_UP: {
                    boolean wasRepeating = repeating;
                    boolean inside = e.getX() >= 0 && e.getX() <= v.getWidth()
                            && e.getY() >= 0 && e.getY() <= v.getHeight();
                    release(v);
                    if (!wasRepeating && !consumed && inside) handleKey(spec);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    release(v);
                    return true;
                default:
                    return true;
            }
        }

        private void release(View v) {
            handler.removeCallbacks(repeat);
            handler.removeCallbacks(longPress);
            hidePreview();
            repeating = false;
            v.setAlpha(1f);
            v.setScaleX(1f);
            v.setScaleY(1f);
        }
    }

    private void commitAlternate(KeySpec spec) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || spec.symbol.isEmpty()) return;
        ic.commitText(spec.symbol, 1);
        composingWord = "";
        if (shift) {
            shift = false;
            refresh();
        } else {
            updateSuggestions();
        }
    }

    private String currentLabel(KeySpec spec) {
        if (spec.action != null) return spec.main;
        if (symbols && !spec.symbol.isEmpty() && mobileLayout) return spec.symbol;
        boolean letterKey = spec.main.length() == 1 && Character.isLetter(spec.main.charAt(0));
        boolean useShift = letterKey ? (shift ^ caps) : shift;
        if (useShift && !spec.shifted.isEmpty()) return spec.shifted;
        return spec.main;
    }

    private boolean hasOneShotModifier() {
        return shift || ctrl || alt || altGr || win;
    }

    private boolean isModifierActive(String action) {
        if (action == null) return false;
        switch (action) {
            case "SHIFT": return shift || (mobileLayout && caps);
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

    private void clearOneShotModifiers() {
        shift = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
    }

    private int lockMeta() {
        int m = 0;
        if (caps) m |= KeyEvent.META_CAPS_LOCK_ON;
        if (num) m |= KeyEvent.META_NUM_LOCK_ON;
        if (scr) m |= KeyEvent.META_SCROLL_LOCK_ON;
        return m;
    }

    /** Sends real modifier key-down events, then the key, then releases the modifiers. */
    private void sendKeyEventsWithMods(InputConnection ic, int keyCode, boolean c, boolean s,
                                       boolean al, boolean ag, boolean w) {
        if (ic == null || keyCode == KeyEvent.KEYCODE_UNKNOWN) return;
        int[] codes = new int[5];
        int[] metas = new int[5];
        int n = 0;
        int meta = lockMeta();
        if (c) {
            meta |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_CTRL_LEFT; metas[n++] = meta;
        }
        if (s) {
            meta |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_SHIFT_LEFT; metas[n++] = meta;
        }
        if (al) {
            meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_ALT_LEFT; metas[n++] = meta;
        }
        if (ag) {
            meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_RIGHT_ON;
            codes[n] = KeyEvent.KEYCODE_ALT_RIGHT; metas[n++] = meta;
        }
        if (w) {
            meta |= KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_META_LEFT; metas[n++] = meta;
        }
        long t = SystemClock.uptimeMillis();
        for (int i = 0; i < n; i++) {
            ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, codes[i], 0, metas[i]));
        }
        ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0, meta));
        ic.sendKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                keyCode, 0, meta));
        for (int i = n - 1; i >= 0; i--) {
            int m = i > 0 ? metas[i - 1] : lockMeta();
            ic.sendKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                    codes[i], 0, m));
        }
    }

    private void sendKeyCode(InputConnection ic, int keyCode) {
        sendKeyEventsWithMods(ic, keyCode, ctrl, shift, alt, altGr, win);
    }

    private int keyCodeForText(String value) {
        if (value == null || value.length() != 1) return KeyEvent.KEYCODE_UNKNOWN;
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
        sendKeyCode(ic, KeyEvent.keyCodeFromString("KEYCODE_" + keyName));
    }

    /** Ctrl+C / Ctrl+V / Ctrl+X / Ctrl+A use the editor's own actions (works in every
     *  text field); everything else is sent as real key events with real modifiers. */
    private boolean clipboardAction(InputConnection ic, boolean c, boolean s, boolean al,
                                    boolean w, String key) {
        if (!c || s || al || w) return false;
        switch (key.toUpperCase(Locale.ROOT)) {
            case "C": return ic.performContextMenuAction(android.R.id.copy);
            case "V": return ic.performContextMenuAction(android.R.id.paste);
            case "X": return ic.performContextMenuAction(android.R.id.cut);
            case "A": return ic.performContextMenuAction(android.R.id.selectAll);
            default: return false;
        }
    }

    private void sendCombo(InputConnection ic, String combo) {
        String[] parts = combo.split("\\+");
        boolean c = false, s = false, al = false, w = false;
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i]) {
                case "CTRL": c = true; break;
                case "SHIFT": s = true; break;
                case "ALT": al = true; break;
                case "WIN": w = true; break;
                default: break;
            }
        }
        String key = parts[parts.length - 1];
        if (clipboardAction(ic, c, s, al, w, key)) return;
        int code = key.length() == 1 ? keyCodeForText(key)
                : KeyEvent.keyCodeFromString("KEYCODE_" + key);
        sendKeyEventsWithMods(ic, code, c, s, al, false, w);
    }

    private boolean autoCaps() {
        if (!mobileLayout || caps || shift) return false;
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei == null || ic == null || ei.inputType == 0) return false;
        if (ic.getCursorCapsMode(ei.inputType) != 0) {
            shift = true;
            return true;
        }
        return false;
    }

    private void endWord() {
        learnWord(composingWord);
        composingWord = "";
    }

    private void finishKey(boolean rebuild) {
        if (mobileLayout && !rebuild && autoCaps()) rebuild = true;
        if (rebuild) refresh();
        else if (mobileLayout) updateSuggestions();
    }

    private void handleKey(KeySpec spec) {
        String action = spec.action;

        if ("SYSTEM_SWITCH".equals(action)) {
            showSystemKeyboardPicker();
            return;
        }
        if ("EMOJI".equals(action)) {
            emojiOpen = true;
            refresh();
            return;
        }
        if ("ABC".equals(action)) {
            emojiOpen = false;
            symbols = false;
            refresh();
            return;
        }

        if (listening) cancelVoice();   // a key press ends dictation; unfinished partial text is removed
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        boolean hadMods = hasOneShotModifier();

        if (action == null) {
            String value = currentLabel(spec);
            if (!value.isEmpty()) {
                if (ctrl || alt || altGr || win) {
                    int keyCode = keyCodeForText(spec.main);
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        if (!clipboardAction(ic, ctrl, shift, alt, win, spec.main)) {
                            sendKeyCode(ic, keyCode);
                        }
                    } else {
                        ic.commitText(value, 1);
                    }
                    composingWord = "";
                } else {
                    ic.commitText(value, 1);
                    if (value.matches("[a-zA-Z']+")) {
                        if (!noSuggestField()) composingWord += value;
                    } else {
                        endWord();
                    }
                }
            }
            clearOneShotModifiers();
            finishKey(hadMods);
            return;
        }

        switch (action) {
            case "BACKSPACE":
                sendKeyCode(ic, KeyEvent.KEYCODE_DEL);
                if (!composingWord.isEmpty()) {
                    composingWord = composingWord.substring(0, composingWord.length() - 1);
                }
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "SPACE":
                if (hasOneShotModifier()) sendKeyCode(ic, KeyEvent.KEYCODE_SPACE);
                else ic.commitText(" ", 1);
                endWord();
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "TAB":
                sendKeyCode(ic, KeyEvent.KEYCODE_TAB);
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "ENTER": {
                if (hasOneShotModifier()) {
                    sendKeyCode(ic, KeyEvent.KEYCODE_ENTER);
                } else {
                    int act = editorAction();
                    if (act != EditorInfo.IME_ACTION_NONE) ic.performEditorAction(act);
                    else sendKeyCode(ic, KeyEvent.KEYCODE_ENTER);
                }
                endWord();
                clearOneShotModifiers();
                finishKey(hadMods);
                break;
            }

            case "SHIFT": {
                long now = SystemClock.uptimeMillis();
                if (mobileLayout) {
                    if (caps) {
                        caps = false;
                        shift = false;
                    } else if (now - lastShiftTap < 400) {
                        caps = true;
                        shift = false;
                    } else {
                        shift = !shift;
                    }
                    lastShiftTap = now;
                } else {
                    shift = !shift;
                }
                refresh();
                break;
            }

            case "SYMBOLS":
                symbols = !symbols;
                symPage = 0;
                refresh();
                break;

            case "SYMPAGE":
                symPage = 1 - symPage;
                refresh();
                break;

            case "CAPS":
                caps = !caps;
                refresh();
                break;

            case "CTRL": ctrl = !ctrl; refresh(); break;
            case "ALT":
                if (alt) {   // second tap = press the Alt key itself
                    alt = false;
                    sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_ALT_LEFT, false, false, false, false, false);
                } else {
                    alt = true;
                }
                refresh();
                break;
            case "ALTGR": altGr = !altGr; refresh(); break;
            case "WIN":
                if (win) {   // second tap = press the Win / Meta key itself
                    win = false;
                    sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_META_LEFT, false, false, false, false, false);
                } else {
                    win = true;
                }
                refresh();
                break;

            case "TEXT":
                ic.commitText(spec.symbol, 1);
                composingWord = "";
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "PC_SEND":
                if (!handleSystemKey(spec.symbol)) sendPcKey(ic, spec.symbol);
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            case "COMBO":
                sendCombo(ic, spec.symbol);
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            case "NUM_LOCK":
                num = !num;
                sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_NUM_LOCK, false, false, false, false, false);
                refresh();
                break;

            case "SCROLL_LOCK":
                scr = !scr;
                sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_SCROLL_LOCK, false, false, false, false, false);
                refresh();
                break;

            case "ESC":
                sendPcKey(ic, "ESCAPE");
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            default:
                break;
        }
    }
}
