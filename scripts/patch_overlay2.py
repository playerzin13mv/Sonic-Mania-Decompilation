import sys
j = 'dependencies/RSDKv5/android/app/src/main/java/org/rems/rsdkv5/'

def edit(fn, pairs):
    s = open(j + fn, encoding='utf-8').read()
    for a, b in pairs:
        if a not in s:
            sys.exit('patch_overlay2: nao achei em %s: %s' % (fn, a[:50]))
        s = s.replace(a, b, 1)
    open(j + fn, 'w', encoding='utf-8').write(s)

edit('DTOverlay.java', [
    ('if (up && inside) showMods();', 'if (up && inside) DTMenu.show(this, act, base);'),
    ('    private static Map<String, Boolean> readCfg(', '    static Map<String, Boolean> readCfg('),
    ('    private void restart() {', '''    void startEdit() {
        prefs.edit().putBoolean("controls", true).apply();
        apply(new HashSet<Integer>());
        editing = true;
        selected = -1;
        invalidate();
    }

    boolean controlsOn() {
        return controls();
    }

    void setControls(boolean on) {
        prefs.edit().putBoolean("controls", on).apply();
        invalidate();
    }

    void restartGame() {
        restart();
    }

    private void restart() {'''),
])
edit('Launcher.java', [
    ('        Intent intent = new Intent(this, RSDK.class);',
     '        DTMenu.fixMods(base);\n        DTMenu.installCrashLog(base);\n        Intent intent = new Intent(this, RSDK.class);'),
])
edit('RSDK.java', [
    ('log = new java.io.FileOutputStream(new java.io.File(baseDir, "log.txt"), false);',
     '''{
                java.io.File lf = new java.io.File(baseDir, "log.txt");
                if (lf.exists() && lf.length() > 0) {
                    java.io.File pf = new java.io.File(baseDir, "log_anterior.txt");
                    pf.delete();
                    lf.renameTo(pf);
                }
                log = new java.io.FileOutputStream(lf, false);
            }'''),
])
print('overlay2 ok')
