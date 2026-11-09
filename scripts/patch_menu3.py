import sys
p = 'dependencies/RSDKv5/android/app/src/main/java/org/rems/rsdkv5/DTMenu.java'
s = open(p, encoding='utf-8').read()
a = s.find('    static void show(final DTOverlay ov')
b = s.find('    private static String read(File f) {')
if a < 0 or b < 0 or b < a:
    sys.exit('patch_menu3: nao achei o metodo show')

NEW = '''    static void show(final DTOverlay ov, final Activity act, final File base) {
        String[] items = {"Mods", "Editar botões",
                "Controles na tela: " + (ov.controlsOn() ? "ligados" : "desligados"),
                "Fonte dos botões", "Vibração: " + (ov.hapticOn() ? "ligada" : "desligada"),
                "Nome de usuário", "Salvar / carregar layout dos botões", "Sobre"};
        new AlertDialog.Builder(act, THEME)
                .setTitle("Menu")
                .setItems(items, (dlg, i) -> {
                    if (i == 0) mods(ov, act, base);
                    else if (i == 1) ov.startEdit();
                    else if (i == 2) ov.setControls(!ov.controlsOn());
                    else if (i == 3) fonts(ov, act);
                    else if (i == 4) ov.setHaptic(!ov.hapticOn());
                    else if (i == 5) name(ov, act, base);
                    else if (i == 6) layoutFile(ov, act, base);
                    else about(act);
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static final String[] FONT_IDS = {"default", "sans", "serif", "mono", "condensed", "black", "casual", "cursive", "mod"};
    private static final String[] FONT_NAMES = {"Padrão", "Sans", "Serifa", "Monoespaçada", "Condensada", "Black", "Casual", "Cursiva", "Fonte do mod (font.ttf)"};

    private static void fonts(final DTOverlay ov, final Activity act) {
        int sel = 0;
        for (int i = 0; i < FONT_IDS.length; i++)
            if (FONT_IDS[i].equals(ov.fontId())) sel = i;
        final String[] names = FONT_NAMES.clone();
        if (!ov.hasModFont()) names[names.length - 1] = "Fonte do mod (nenhum mod ativo com font.ttf)";
        new AlertDialog.Builder(act, THEME)
                .setTitle("Fonte dos botões")
                .setSingleChoiceItems(names, sel, (dlg, i) -> {
                    if (i == FONT_IDS.length - 1 && !ov.hasModFont()) return;
                    ov.setFont(FONT_IDS[i]);
                    dlg.dismiss();
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static void layoutFile(final DTOverlay ov, final Activity act, final File base) {
        final File f = new File(base, "layout_botoes.txt");
        final String head = "DT-LAYOUT 1\\n";
        new AlertDialog.Builder(act, THEME)
                .setTitle("Layout dos botões")
                .setMessage("Arquivo: " + f.getAbsolutePath())
                .setPositiveButton("Exportar", (dlg, which) -> write(f, head + ov.layoutText()))
                .setNeutralButton("Importar", (dlg, which) -> {
                    String s = read(f);
                    if (s.startsWith(head)) ov.applyLayoutText(s.substring(head.length()));
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static void about(Activity act) {
        new AlertDialog.Builder(act, THEME)
                .setTitle("Sonic Mania: D.T (beta)")
                .setMessage("Projeto de fã, gratuito e sem fins lucrativos. Não é um produto oficial e não tem ligação com a SEGA.\\n\\n"
                        + "Não inclui o jogo: é preciso ter o seu próprio Data.rsdk do Sonic Mania Plus.\\n\\n"
                        + "Baseado na RSDKv5 Decompilation e na decompilação do Sonic Mania (Rubberduckycooly e RSDKModding).\\n"
                        + "Port e controles: Playerzin13MV.")
                .setPositiveButton("OK", null)
                .show();
    }

    // Instala a skin de exemplo na pasta mods (uma vez)
    static void installExample(android.content.Context c, File base) {
        try {
            File marker = new File(base, "mods/.example_installed");
            if (marker.exists()) return;
            copyAsset(c.getAssets(), "mods/ExampleSkin", new File(base, "mods/ExampleSkin"));
            marker.getParentFile().mkdirs();
            write(marker, "1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyAsset(android.content.res.AssetManager am, String src, File dst) throws java.io.IOException {
        String[] kids = am.list(src);
        if (kids != null && kids.length > 0) {
            dst.mkdirs();
            for (String k : kids) copyAsset(am, src + "/" + k, new File(dst, k));
        } else {
            dst.getParentFile().mkdirs();
            try (java.io.InputStream in = am.open(src); FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
        }
    }

'''
s = s[:a] + NEW + s[b:]
open(p, 'w', encoding='utf-8').write(s)
print('menu3 ok')
