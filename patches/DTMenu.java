package org.rems.rsdkv5;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

final class DTMenu {
    private static final int THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private DTMenu() {
    }

    static void show(final DTOverlay ov, final Activity act, final File base) {
        String[] items = {"Mods", "Editar botões",
                "Controles na tela: " + (ov.controlsOn() ? "ligados" : "desligados"), "Nome de usuário"};
        new AlertDialog.Builder(act, THEME)
                .setTitle("Menu")
                .setItems(items, (dlg, i) -> {
                    if (i == 0) mods(ov, act, base);
                    else if (i == 1) ov.startEdit();
                    else if (i == 2) ov.setControls(!ov.controlsOn());
                    else name(ov, act, base);
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static String read(File f) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String ln;
            while ((ln = r.readLine()) != null) sb.append(ln).append('\n');
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static void write(File f, String s) {
        try (FileOutputStream o = new FileOutputStream(f)) {
            o.write(s.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isCode(String ini) {
        String l = ini.toLowerCase();
        return l.contains("dllfile=") || l.contains("logicfile=");
    }

    // Mods antigos sem TargetVersion costumam falhar: acrescenta TargetVersion=5 quando falta.
    static void fixMods(File base) {
        File[] ds = new File(base, "mods").listFiles();
        if (ds == null) return;
        for (File m : ds) {
            File ini = new File(m, "mod.ini");
            if (!m.isDirectory() || !ini.isFile()) continue;
            String s = read(ini);
            if (s.toLowerCase().contains("targetversion")) continue;
            write(ini, s + "TargetVersion=5\n");
        }
    }

    // Guarda o erro de Java em crash.txt (erros nativos ficam no log_anterior.txt)
    static void installCrashLog(final File base) {
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                write(new File(base, "crash.txt"), sw.toString());
            } catch (Throwable ignored) {
            }
            if (old != null) old.uncaughtException(t, e);
        });
    }

    private static void mods(final DTOverlay ov, final Activity act, final File base) {
        final File dir = new File(base, "mods");
        File[] ds = dir.listFiles();
        final ArrayList<String> names = new ArrayList<>();
        if (ds != null)
            for (File f : ds)
                if (f.isDirectory()) names.add(f.getName());
        Collections.sort(names);
        if (names.isEmpty()) {
            new AlertDialog.Builder(act, THEME).setTitle("Mods")
                    .setMessage("Nenhum mod encontrado em:\n" + dir.getAbsolutePath())
                    .setPositiveButton("OK", null).show();
            return;
        }
        final Map<String, Boolean> cfg = DTOverlay.readCfg(dir);
        final int n = names.size();
        final String[] items = new String[n];
        final boolean[] checked = new boolean[n];
        final boolean[] before = new boolean[n];
        final boolean[] code = new boolean[n];
        for (int i = 0; i < n; i++) {
            String nm = names.get(i);
            code[i] = isCode(read(new File(dir, nm + "/mod.ini")));
            items[i] = code[i] ? nm + " (código: não funciona aqui)" : nm;
            Boolean on = cfg.get(nm);
            checked[i] = !code[i] && on != null && on;
            before[i] = checked[i];
        }
        new AlertDialog.Builder(act, THEME)
                .setTitle("Mods")
                .setMultiChoiceItems(items, checked, (dlg, i, on) -> checked[i] = on && !code[i])
                .setPositiveButton("Salvar", (dlg, which) -> {
                    StringBuilder sb = new StringBuilder("[Mods]\n");
                    boolean changed = false;
                    for (int i = 0; i < n; i++) {
                        boolean on = checked[i] && !code[i];
                        sb.append(names.get(i)).append('=').append(on ? 'y' : 'n').append('\n');
                        if (on != before[i]) changed = true;
                    }
                    if (changed) {
                        write(new File(dir, "modconfig.ini"), sb.toString());
                        fixMods(base);
                        ov.restartGame();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void name(final DTOverlay ov, final Activity act, final File base) {
        final File ini = new File(base, "Settings.ini");
        String cur = "";
        for (String ln : read(ini).split("\n"))
            if (ln.trim().toLowerCase().startsWith("username=")) cur = ln.substring(ln.indexOf('=') + 1).trim();
        AlertDialog.Builder bd = new AlertDialog.Builder(act, THEME);
        final EditText in = new EditText(bd.getContext());
        in.setSingleLine(true);
        in.setText(cur);
        in.setHint("Seu nome (até 16 caracteres)");
        bd.setTitle("Nome de usuário")
                .setMessage("Aparece no menu principal. O jogo reinicia ao salvar.")
                .setView(in)
                .setPositiveButton("Salvar", (dlg, which) -> {
                    StringBuilder nm = new StringBuilder();
                    for (char c : in.getText().toString().toCharArray())
                        if ((Character.isLetterOrDigit(c) && c < 128) || c == ' ' || c == '_' || c == '-' || c == '.')
                            nm.append(c);
                    String chosen = nm.toString().trim();
                    if (chosen.length() > 16) chosen = chosen.substring(0, 16).trim();
                    if (chosen.isEmpty()) return;
                    setUsername(ini, chosen);
                    ov.restartGame();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void setUsername(File ini, String name) {
        String[] lines = read(ini).split("\n");
        StringBuilder out = new StringBuilder();
        boolean done = false, inGame = false;
        for (String ln : lines) {
            String t = ln.trim();
            if (t.startsWith("[")) inGame = t.equalsIgnoreCase("[Game]");
            if (inGame && t.toLowerCase().startsWith("username=")) continue;
            out.append(ln).append('\n');
            if (t.equalsIgnoreCase("[Game]") && !done) {
                out.append("username=").append(name).append('\n');
                done = true;
            }
        }
        if (!done) out.append("\n[Game]\nusername=").append(name).append('\n');
        write(ini, out.toString());
    }
}
