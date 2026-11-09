import sys
j = 'dependencies/RSDKv5/android/app/src/main/java/org/rems/rsdkv5/'

def edit(fn, pairs):
    s = open(j + fn, encoding='utf-8').read()
    for a, b in pairs:
        if a not in s:
            sys.exit('patch_overlay3: nao achei em %s: %s' % (fn, a[:50]))
        s = s.replace(a, b, 1)
    open(j + fn, 'w', encoding='utf-8').write(s)

NEW = '''    private Typeface tf = null;

    void loadFont() {
        String f = prefs.getString("font", "default");
        Typeface t = null;
        try {
            if (f.equals("mod") && skin.font != null) t = Typeface.createFromFile(skin.font);
            else if (f.equals("sans")) t = Typeface.create("sans-serif", Typeface.BOLD);
            else if (f.equals("serif")) t = Typeface.create("serif", Typeface.BOLD);
            else if (f.equals("mono")) t = Typeface.create("monospace", Typeface.BOLD);
            else if (f.equals("condensed")) t = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            else if (f.equals("black")) t = Typeface.create("sans-serif-black", Typeface.NORMAL);
            else if (f.equals("casual")) t = Typeface.create("casual", Typeface.NORMAL);
            else if (f.equals("cursive")) t = Typeface.create("cursive", Typeface.NORMAL);
        } catch (Exception ignored) {
            t = null;
        }
        tf = t;
        txt.setTypeface(t != null ? t : Typeface.DEFAULT_BOLD);
        invalidate();
    }

    boolean hasModFont() {
        return skin.font != null;
    }

    String fontId() {
        return prefs.getString("font", "default");
    }

    void setFont(String id) {
        prefs.edit().putString("font", id).apply();
        loadFont();
    }

    boolean hapticOn() {
        return prefs.getBoolean("haptic", true);
    }

    void setHaptic(boolean on) {
        prefs.edit().putBoolean("haptic", on).apply();
    }

    String layoutText() {
        StringBuilder sb = new StringBuilder();
        for (Btn b : btns)
            sb.append(b.type).append('\\t').append(clean(b.label)).append('\\t').append(b.vk).append('\\t')
                    .append(clean(b.desc)).append('\\t').append(b.fx).append('\\t').append(b.fy).append('\\t')
                    .append(b.s).append('\\n');
        return sb.toString();
    }

    void applyLayoutText(String raw) {
        prefs.edit().putString("layout2", raw).apply();
        btns = loadLayout();
        selected = -1;
        saveLayout();
        invalidate();
    }

    void startEdit() {'''

edit('DTOverlay.java', [
    ('import android.graphics.RectF;\n', 'import android.graphics.RectF;\nimport android.graphics.Typeface;\n'),
    ('        boolean labels = true;\n    }', '        boolean labels = true;\n        File font;\n    }'),
    ('            k.pillOn = pick(t, "pill_pressed.png", k.pillOn);\n',
     '            k.pillOn = pick(t, "pill_pressed.png", k.pillOn);\n'
     '            if (new File(t, "font.ttf").isFile()) k.font = new File(t, "font.ttf");\n'
     '            else if (new File(t, "font.otf").isFile()) k.font = new File(t, "font.otf");\n'),
    ('        skin = loadSkin();\n    }', '        skin = loadSkin();\n        loadFont();\n    }'),
    ('        for (Integer k : want)\n            if (!sent.contains(k)) RSDK.nativeSetKey(k, true);\n',
     '        for (Integer k : want)\n            if (!sent.contains(k)) {\n                RSDK.nativeSetKey(k, true);\n'
     '                if (hapticOn()) performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);\n            }\n'),
    ('    void startEdit() {', NEW),
])
edit('Launcher.java', [
    ('        DTMenu.installCrashLog(base);\n', '        DTMenu.installCrashLog(base);\n        DTMenu.installExample(this, base);\n'),
])
print('overlay3 ok')
