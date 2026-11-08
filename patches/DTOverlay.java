package org.rems.rsdkv5;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DTOverlay extends View {
    static final int UP = 0x26, DOWN = 0x28, LEFT = 0x25, RIGHT = 0x27;
    static final float[] OPAC = {0.45f, 0.75f, 1f};
    static final int GOLD = 0xFFFFC21A, DARK = 0xFF15181F, PRESSED = 0xFF4A3F1C;

    // Teclas oferecidas (codigos do Windows; a acao depende do mapa de teclas do Settings.ini)
    static final String[] KEY_NAMES = {"Cima", "Baixo", "Esquerda", "Direita",
            "A (pular / confirmar)", "B (voltar)", "C", "X", "Y (Super)", "Z",
            "Start (Enter)", "Select (Tab)", "Esc", "Espaço", "Shift", "Ctrl"};
    static final int[] KEY_CODES = {0x26, 0x28, 0x25, 0x27,
            0x41, 0x53, 0x44, 0x51, 0x57, 0x45,
            0x0D, 0x09, 0x1B, 0x20, 0x10, 0x11};

    static class Btn {
        int type; // 0 = direcional, 1 = redondo, 2 = pilula
        String label;
        int vk;
        String desc;
        float fx, fy, s;

        Btn(int type, String label, int vk, String desc, float fx, float fy, float s) {
            this.type = type;
            this.label = label;
            this.vk = vk;
            this.desc = desc;
            this.fx = fx;
            this.fy = fy;
            this.s = s;
        }
    }

    static class Skin {
        Bitmap dpad, dUp, dDown, dLeft, dRight, btn, btnOn, pill, pillOn;
        boolean labels = true;
    }

    private final Activity act;
    private final File base;
    private final SharedPreferences prefs;
    private final float d;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmpPaint = new Paint();
    private final Set<Integer> sent = new HashSet<>();
    private final RectF modsR = new RectF();
    private final RectF[] bar = new RectF[7];
    private ArrayList<Btn> btns;
    private Skin skin;
    private int vw = 0, vh = 0, opIdx = 1;
    private int modsPtr = -1;
    private boolean editing = false;
    private int selected = -1, dragPtr = -1;
    private float offX, offY;

    public static void attach(Activity a, File base) {
        a.addContentView(new DTOverlay(a, base), new ViewGroup.LayoutParams(-1, -1));
    }

    private DTOverlay(Activity a, File base) {
        super(a);
        this.act = a;
        this.base = base;
        this.prefs = a.getSharedPreferences("dt", Context.MODE_PRIVATE);
        this.d = a.getResources().getDisplayMetrics().density;
        fill.setStyle(Paint.Style.FILL);
        line.setStyle(Paint.Style.STROKE);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setFakeBoldText(true);
        for (int i = 0; i < bar.length; i++) bar[i] = new RectF();
        opIdx = prefs.getInt("op", 1);
        btns = loadLayout();
        skin = loadSkin();
    }

    // ---------- layout salvo ----------
    static ArrayList<Btn> defaults() {
        ArrayList<Btn> l = new ArrayList<>();
        l.add(new Btn(0, "", 0, "Direcional", 0.15f, 0.68f, 1f));
        l.add(new Btn(1, "A", 0x41, "Pular / confirmar", 0.91f, 0.78f, 1.1f));
        l.add(new Btn(1, "B", 0x53, "Voltar", 0.80f, 0.80f, 0.9f));
        l.add(new Btn(1, "S", 0x57, "Super", 0.86f, 0.55f, 0.8f));
        l.add(new Btn(2, "START", 0x0D, "Pausa", 0.93f, 0.10f, 1f));
        return l;
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private ArrayList<Btn> loadLayout() {
        String raw = prefs.getString("layout2", null);
        if (raw == null) return defaults();
        ArrayList<Btn> l = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length < 7) continue;
            try {
                l.add(new Btn(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]), p[3],
                        Float.parseFloat(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6])));
            } catch (Exception ignored) {
            }
        }
        return l.isEmpty() ? defaults() : l;
    }

    private void saveLayout() {
        StringBuilder sb = new StringBuilder();
        for (Btn b : btns)
            sb.append(b.type).append('\t').append(clean(b.label)).append('\t').append(b.vk).append('\t')
                    .append(clean(b.desc)).append('\t').append(b.fx).append('\t').append(b.fy).append('\t')
                    .append(b.s).append('\n');
        prefs.edit().putString("layout2", sb.toString()).putInt("op", opIdx).apply();
    }

    private boolean controls() {
        return prefs.getBoolean("controls", true);
    }

    // ---------- skins vindas de mods: <mod>/TouchControls/*.png ----------
    private static Map<String, Boolean> readCfg(File modsDir) {
        Map<String, Boolean> cfg = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(new File(modsDir, "modconfig.ini")))) {
            String ln;
            while ((ln = r.readLine()) != null) {
                int eq = ln.lastIndexOf('=');
                if (eq > 0)
                    cfg.put(ln.substring(0, eq).trim(), ln.substring(eq + 1).trim().toLowerCase().startsWith("y"));
            }
        } catch (Exception ignored) {
        }
        return cfg;
    }

    private static Bitmap pick(File dir, String name, Bitmap old) {
        File f = new File(dir, name);
        if (f.isFile()) {
            Bitmap b = BitmapFactory.decodeFile(f.getPath());
            if (b != null) return b;
        }
        return old;
    }

    private Skin loadSkin() {
        Skin k = new Skin();
        File mods = new File(base, "mods");
        Map<String, Boolean> cfg = readCfg(mods);
        File[] ds = mods.listFiles();
        if (ds == null) return k;
        Arrays.sort(ds);
        for (File m : ds) {
            Boolean on = cfg.get(m.getName());
            if (!m.isDirectory() || on == null || !on) continue;
            File t = new File(m, "TouchControls");
            if (!t.isDirectory()) continue;
            k.dpad = pick(t, "dpad.png", k.dpad);
            k.dUp = pick(t, "dpad_up.png", k.dUp);
            k.dDown = pick(t, "dpad_down.png", k.dDown);
            k.dLeft = pick(t, "dpad_left.png", k.dLeft);
            k.dRight = pick(t, "dpad_right.png", k.dRight);
            k.btn = pick(t, "button.png", k.btn);
            k.btnOn = pick(t, "button_pressed.png", k.btnOn);
            k.pill = pick(t, "pill.png", k.pill);
            k.pillOn = pick(t, "pill_pressed.png", k.pillOn);
            try (BufferedReader r = new BufferedReader(new FileReader(new File(t, "skin.ini")))) {
                String ln;
                while ((ln = r.readLine()) != null) {
                    String s = ln.trim().toLowerCase().replace(" ", "");
                    if (s.startsWith("showlabels=")) k.labels = s.substring(11).startsWith("y") || s.substring(11).startsWith("t") || s.substring(11).startsWith("1");
                }
            } catch (Exception ignored) {
            }
        }
        return k;
    }

    // ---------- geometria ----------
    private float px(Btn b) {
        return b.fx * vw;
    }

    private float py(Btn b) {
        return b.fy * vh;
    }

    private float rad(Btn b) {
        return (b.type == 0 ? 70f : 38f) * d * b.s;
    }

    private void pillRect(Btn b, RectF r, float grow) {
        int len = Math.max(3, b.label.length());
        float hw = (14f + 5.5f * len) * d * b.s + grow, hh = 15f * d * b.s + grow;
        r.set(px(b) - hw, py(b) - hh, px(b) + hw, py(b) + hh);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        vw = w;
        vh = h;
        modsR.set(w / 2f - 38 * d, 8 * d, w / 2f + 38 * d, 38 * d);
        float bw = 54 * d, gap = 5 * d, total = bar.length * bw + (bar.length - 1) * gap, x0 = (w - total) / 2f;
        for (int i = 0; i < bar.length; i++)
            bar[i].set(x0 + i * (bw + gap), 8 * d, x0 + i * (bw + gap) + bw, 40 * d);
    }

    // ---------- desenho ----------
    private int al(int a) {
        return (int) (a * OPAC[opIdx]);
    }

    private void drawBmp(Canvas c, Bitmap bm, RectF dst) {
        bmpPaint.setAlpha(al(255));
        c.drawBitmap(bm, null, dst, bmpPaint);
    }

    private void disc(Canvas c, float x, float y, float r, boolean on) {
        fill.setColor(on ? PRESSED : DARK);
        fill.setAlpha(al(on ? 240 : 205));
        c.drawCircle(x, y, r, fill);
        line.setColor(GOLD);
        line.setStrokeWidth(Math.max(2f, r * 0.09f));
        line.setAlpha(al(255));
        c.drawCircle(x, y, r * 0.92f, line);
    }

    private void label(Canvas c, String s, float x, float y, float size, boolean on) {
        txt.setColor(on ? GOLD : 0xFFFFFFFF);
        txt.setAlpha(al(255));
        txt.setTextSize(size);
        c.drawText(s, x, y + size * 0.35f, txt);
    }

    private void pillShape(Canvas c, RectF r, boolean on) {
        float rr = r.height() / 2f;
        fill.setColor(on ? PRESSED : DARK);
        fill.setAlpha(al(on ? 240 : 205));
        c.drawRoundRect(r, rr, rr, fill);
        line.setColor(GOLD);
        line.setStrokeWidth(Math.max(2f, r.height() * 0.07f));
        line.setAlpha(al(255));
        c.drawRoundRect(r, rr, rr, line);
    }

    private void pill(Canvas c, RectF r, String s, boolean on) {
        pillShape(c, r, on);
        label(c, s, r.centerX(), r.centerY(), r.height() * 0.42f, on);
    }

    private void tri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, boolean on, float r) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.close();
        fill.setColor(on ? GOLD : 0xFF2A2F3A);
        fill.setAlpha(al(on ? 255 : 230));
        c.drawPath(p, fill);
        line.setColor(GOLD);
        line.setStrokeWidth(Math.max(2f, r * 0.05f));
        line.setAlpha(al(255));
        c.drawPath(p, line);
    }

    private void drawDpad(Canvas c, Btn b) {
        float x = px(b), y = py(b), R = rad(b);
        if (skin.dpad != null) {
            RectF r = new RectF(x - R, y - R, x + R, y + R);
            drawBmp(c, skin.dpad, r);
            if (sent.contains(UP) && skin.dUp != null) drawBmp(c, skin.dUp, r);
            if (sent.contains(DOWN) && skin.dDown != null) drawBmp(c, skin.dDown, r);
            if (sent.contains(LEFT) && skin.dLeft != null) drawBmp(c, skin.dLeft, r);
            if (sent.contains(RIGHT) && skin.dRight != null) drawBmp(c, skin.dRight, r);
            return;
        }
        disc(c, x, y, R, false);
        float a = R * 0.80f, n = R * 0.40f, w = R * 0.26f;
        tri(c, x, y - a, x - w, y - n, x + w, y - n, sent.contains(UP), R);
        tri(c, x, y + a, x - w, y + n, x + w, y + n, sent.contains(DOWN), R);
        tri(c, x - a, y, x - n, y - w, x - n, y + w, sent.contains(LEFT), R);
        tri(c, x + a, y, x + n, y - w, x + n, y + w, sent.contains(RIGHT), R);
    }

    private void drawBtn(Canvas c, Btn b) {
        if (b.type == 0) {
            drawDpad(c, b);
            return;
        }
        boolean on = b.vk != 0 && sent.contains(b.vk);
        float x = px(b), y = py(b);
        if (b.type == 1) {
            float r = rad(b);
            Bitmap bm = (on && skin.btnOn != null) ? skin.btnOn : skin.btn;
            if (bm != null) drawBmp(c, bm, new RectF(x - r, y - r, x + r, y + r));
            else disc(c, x, y, r, on);
            if (skin.labels) {
                int len = Math.max(1, b.label.length());
                label(c, b.label, x, y, r * (len <= 1 ? 0.85f : Math.min(0.85f, 1.5f / len)), on);
            }
        } else {
            RectF r = new RectF();
            pillRect(b, r, 0);
            Bitmap bm = (on && skin.pillOn != null) ? skin.pillOn : skin.pill;
            if (bm != null) drawBmp(c, bm, r);
            else pillShape(c, r, on);
            if (skin.labels) label(c, b.label, r.centerX(), r.centerY(), r.height() * 0.42f, on);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (editing) {
            for (Btn b : btns) drawBtn(c, b);
            for (int i = 0; i < btns.size(); i++) {
                Btn b = btns.get(i);
                RectF r = new RectF();
                if (b.type == 2) pillRect(b, r, 4 * d);
                else r.set(px(b) - rad(b), py(b) - rad(b), px(b) + rad(b), py(b) + rad(b));
                if (i == selected) {
                    line.setColor(0xFFFFFFFF);
                    line.setStrokeWidth(2 * d);
                    line.setAlpha(255);
                    c.drawRoundRect(r, 8 * d, 8 * d, line);
                }
                if (b.desc != null && b.desc.length() > 0) {
                    txt.setColor(0xFFFFFFFF);
                    txt.setAlpha(230);
                    txt.setTextSize(10 * d);
                    c.drawText(b.desc, px(b), r.bottom + 12 * d, txt);
                }
            }
            String[] names = {"-", "+", "OPAC", "NOVO", "EDITAR", "RESET", "OK"};
            for (int i = 0; i < bar.length; i++) pill(c, bar[i], names[i], false);
            txt.setColor(0xFFFFFFFF);
            txt.setAlpha(255);
            txt.setTextSize(12 * d);
            c.drawText("Arraste para mover. Toque num botão e use - / + ou EDITAR.", vw / 2f, 56 * d, txt);
            return;
        }
        pill(c, modsR, "MODS", false);
        if (controls()) for (Btn b : btns) drawBtn(c, b);
    }

    // ---------- toque ----------
    private void keysAt(float x, float y, Set<Integer> out) {
        if (!controls()) return;
        for (Btn b : btns) {
            if (b.type == 0) {
                float R = rad(b), ddx = x - px(b), ddy = y - py(b);
                double dist = Math.hypot(ddx, ddy);
                if (dist <= R * 1.25 && dist > R * 0.18) {
                    float k = (float) (dist * 0.38);
                    if (ddx < -k) out.add(LEFT);
                    if (ddx > k) out.add(RIGHT);
                    if (ddy < -k) out.add(UP);
                    if (ddy > k) out.add(DOWN);
                }
            } else if (b.type == 1) {
                if (b.vk != 0 && Math.hypot(x - px(b), y - py(b)) <= rad(b) * 1.2) out.add(b.vk);
            } else {
                RectF r = new RectF();
                pillRect(b, r, 6 * d);
                if (b.vk != 0 && r.contains(x, y)) out.add(b.vk);
            }
        }
    }

    private void apply(Set<Integer> want) {
        for (Integer k : new HashSet<>(sent))
            if (!want.contains(k)) RSDK.nativeSetKey(k, false);
        for (Integer k : want)
            if (!sent.contains(k)) RSDK.nativeSetKey(k, true);
        sent.clear();
        sent.addAll(want);
    }

    private int pickBtn(float x, float y) {
        for (int i = btns.size() - 1; i >= 0; i--) {
            Btn b = btns.get(i);
            if (b.type == 2) {
                RectF r = new RectF();
                pillRect(b, r, 8 * d);
                if (r.contains(x, y)) return i;
            } else if (Math.hypot(x - px(b), y - py(b)) <= rad(b) * 1.1) return i;
        }
        return -1;
    }

    private void barTap(int i) {
        Btn b = (selected >= 0 && selected < btns.size()) ? btns.get(selected) : null;
        if (i == 0 && b != null) b.s = Math.max(0.4f, b.s * 0.9f);
        else if (i == 1 && b != null) b.s = Math.min(2.5f, b.s * 1.1f);
        else if (i == 2) opIdx = (opIdx + 1) % 3;
        else if (i == 3) {
            Btn nb = new Btn(1, "?", 0x41, "", 0.5f, 0.5f, 1f);
            btns.add(nb);
            selected = btns.size() - 1;
            openEditor(nb, true);
        } else if (i == 4 && b != null && b.type != 0) openEditor(b, false);
        else if (i == 5) {
            btns = defaults();
            selected = -1;
        } else if (i == 6) editing = false;
        saveLayout();
        invalidate();
    }

    private boolean editTouch(MotionEvent e) {
        int t = e.getActionMasked();
        int idx = e.getActionIndex();
        if (t == MotionEvent.ACTION_DOWN || t == MotionEvent.ACTION_POINTER_DOWN) {
            float x = e.getX(idx), y = e.getY(idx);
            for (int i = 0; i < bar.length; i++)
                if (bar[i].contains(x, y)) {
                    barTap(i);
                    return true;
                }
            int hit = pickBtn(x, y);
            if (hit >= 0 && dragPtr < 0) {
                selected = hit;
                dragPtr = e.getPointerId(idx);
                offX = btns.get(hit).fx * vw - x;
                offY = btns.get(hit).fy * vh - y;
            }
            invalidate();
        } else if (t == MotionEvent.ACTION_MOVE && dragPtr >= 0 && selected >= 0 && selected < btns.size()) {
            int pi = e.findPointerIndex(dragPtr);
            if (pi >= 0 && vw > 0 && vh > 0) {
                Btn b = btns.get(selected);
                b.fx = Math.max(0f, Math.min(1f, (e.getX(pi) + offX) / vw));
                b.fy = Math.max(0f, Math.min(1f, (e.getY(pi) + offY) / vh));
                invalidate();
            }
        } else if (t == MotionEvent.ACTION_UP || t == MotionEvent.ACTION_POINTER_UP || t == MotionEvent.ACTION_CANCEL) {
            if (t == MotionEvent.ACTION_CANCEL || e.getPointerId(idx) == dragPtr) {
                dragPtr = -1;
                saveLayout();
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (editing) return editTouch(e);
        int type = e.getActionMasked();
        int idx = e.getActionIndex();
        boolean up = type == MotionEvent.ACTION_UP || type == MotionEvent.ACTION_POINTER_UP;
        boolean cancel = type == MotionEvent.ACTION_CANCEL;

        if (type == MotionEvent.ACTION_DOWN || type == MotionEvent.ACTION_POINTER_DOWN) {
            float x = e.getX(idx), y = e.getY(idx);
            if (modsR.contains(x, y)) {
                modsPtr = e.getPointerId(idx);
                return true;
            }
            if (type == MotionEvent.ACTION_DOWN) {
                Set<Integer> t = new HashSet<>();
                keysAt(x, y, t);
                if (t.isEmpty()) return false;
            }
        }

        if ((up || cancel) && e.getPointerId(idx) == modsPtr) {
            boolean inside = modsR.contains(e.getX(idx), e.getY(idx));
            modsPtr = -1;
            if (up && inside) showMods();
        }

        Set<Integer> want = new HashSet<>();
        if (!cancel) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (up && i == idx) continue;
                if (e.getPointerId(i) == modsPtr) continue;
                keysAt(e.getX(i), e.getY(i), want);
            }
        }
        apply(want);
        invalidate();
        return true;
    }

    // ---------- editor de botao ----------
    static String keyName(int vk) {
        for (int i = 0; i < KEY_CODES.length; i++)
            if (KEY_CODES[i] == vk) return KEY_NAMES[i];
        if (vk == 0) return "nenhuma";
        if ((vk >= 0x30 && vk <= 0x39) || (vk >= 0x41 && vk <= 0x5A)) return String.valueOf((char) vk);
        return "0x" + Integer.toHexString(vk);
    }

    private void openEditor(final Btn b, final boolean isNew) {
        AlertDialog.Builder bd = new AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        final Context ctx = bd.getContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * d);
        box.setPadding(pad, pad / 2, pad, 0);
        final EditText label = new EditText(ctx);
        label.setHint("Texto do botão");
        label.setSingleLine(true);
        label.setText(b.label);
        final EditText desc = new EditText(ctx);
        desc.setHint("Função (opcional, só para organizar)");
        desc.setSingleLine(true);
        desc.setText(b.desc);
        final int[] vk = {b.vk};
        final Button keyBtn = new Button(ctx);
        keyBtn.setText("Tecla: " + keyName(vk[0]));
        keyBtn.setOnClickListener(v -> chooseKey(ctx, vk, keyBtn));
        box.addView(label);
        box.addView(desc);
        box.addView(keyBtn);
        bd.setTitle(isNew ? "Novo botão" : "Editar botão")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("OK", (dlg, which) -> {
                    b.label = clean(label.getText().toString());
                    b.desc = clean(desc.getText().toString());
                    b.vk = vk[0];
                    saveLayout();
                    invalidate();
                })
                .setNegativeButton("Cancelar", (dlg, which) -> {
                    if (isNew) {
                        btns.remove(b);
                        selected = -1;
                    }
                    saveLayout();
                    invalidate();
                })
                .setNeutralButton("Excluir", (dlg, which) -> {
                    btns.remove(b);
                    selected = -1;
                    saveLayout();
                    invalidate();
                })
                .show();
    }

    private void chooseKey(final Context ctx, final int[] vk, final Button keyBtn) {
        String[] items = new String[KEY_NAMES.length + 2];
        System.arraycopy(KEY_NAMES, 0, items, 0, KEY_NAMES.length);
        items[KEY_NAMES.length] = "Outra tecla (letra ou número)...";
        items[KEY_NAMES.length + 1] = "Nenhuma (botão sem função)";
        new AlertDialog.Builder(ctx)
                .setTitle("Tecla do botão")
                .setItems(items, (dlg, i) -> {
                    if (i < KEY_NAMES.length) {
                        vk[0] = KEY_CODES[i];
                        keyBtn.setText("Tecla: " + keyName(vk[0]));
                    } else if (i == KEY_NAMES.length) {
                        askChar(ctx, vk, keyBtn);
                    } else {
                        vk[0] = 0;
                        keyBtn.setText("Tecla: " + keyName(0));
                    }
                })
                .show();
    }

    private void askChar(final Context ctx, final int[] vk, final Button keyBtn) {
        final EditText in = new EditText(ctx);
        in.setHint("Uma letra ou número");
        in.setSingleLine(true);
        new AlertDialog.Builder(ctx)
                .setTitle("Outra tecla")
                .setView(in)
                .setPositiveButton("OK", (dlg, which) -> {
                    String s = in.getText().toString().trim();
                    if (s.length() > 0) {
                        char ch = Character.toUpperCase(s.charAt(0));
                        if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z')) {
                            vk[0] = ch;
                            keyBtn.setText("Tecla: " + keyName(vk[0]));
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- menu de mods ----------
    private void showMods() {
        final File dir = new File(base, "mods");
        File[] dirs = dir.listFiles();
        final ArrayList<String> names = new ArrayList<>();
        if (dirs != null)
            for (File f : dirs)
                if (f.isDirectory()) names.add(f.getName());
        java.util.Collections.sort(names);
        final Map<String, Boolean> cfg = readCfg(dir);

        final int n = names.size() + 1;
        final String[] items = new String[n];
        final boolean[] checked = new boolean[n];
        final boolean[] before = new boolean[n];
        items[0] = "Controles na tela";
        checked[0] = controls();
        for (int i = 0; i < names.size(); i++) {
            items[i + 1] = names.get(i);
            Boolean on = cfg.get(names.get(i));
            checked[i + 1] = on != null && on;
        }
        System.arraycopy(checked, 0, before, 0, n);

        new AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Mods")
                .setMultiChoiceItems(items, checked, (dlg, i, on) -> checked[i] = on)
                .setPositiveButton("Salvar", (dlg, which) -> {
                    prefs.edit().putBoolean("controls", checked[0]).apply();
                    boolean changed = false;
                    StringBuilder sb = new StringBuilder("[Mods]\n");
                    for (int i = 1; i < n; i++) {
                        sb.append(items[i]).append('=').append(checked[i] ? 'y' : 'n').append('\n');
                        if (checked[i] != before[i]) changed = true;
                    }
                    if (changed) {
                        try (FileOutputStream o = new FileOutputStream(new File(dir, "modconfig.ini"))) {
                            o.write(sb.toString().getBytes("UTF-8"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        restart();
                    } else {
                        invalidate();
                    }
                })
                .setNeutralButton("Editar botões", (dlg, which) -> {
                    prefs.edit().putBoolean("controls", true).apply();
                    apply(new HashSet<Integer>());
                    editing = true;
                    selected = -1;
                    invalidate();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void restart() {
        Intent i = act.getPackageManager().getLaunchIntentForPackage(act.getPackageName());
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        act.startActivity(i);
        act.finishAffinity();
        new Handler(Looper.getMainLooper()).postDelayed(() -> Runtime.getRuntime().exit(0), 300);
    }
}
