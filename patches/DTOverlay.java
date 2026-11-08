package org.rems.rsdkv5;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DTOverlay extends View {
    static final int UP = 0x26, DOWN = 0x28, LEFT = 0x25, RIGHT = 0x27, KA = 0x41, KB = 0x53, START = 0x0D;

    private final Activity act;
    private final File base;
    private final SharedPreferences prefs;
    private final float d;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Set<Integer> sent = new HashSet<>();
    private final RectF modsR = new RectF(), startR = new RectF();
    private int modsPtr = -1;
    private float dx0, dy0, dR, ax, ay, aR, bx, by, bR;

    public static void attach(Activity a, File base) {
        a.addContentView(new DTOverlay(a, base), new ViewGroup.LayoutParams(-1, -1));
    }

    private DTOverlay(Activity a, File base) {
        super(a);
        this.act = a;
        this.base = base;
        this.prefs = a.getSharedPreferences("dt", Context.MODE_PRIVATE);
        this.d = a.getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setFakeBoldText(true);
        txt.setColor(0xFFFFFFFF);
    }

    private boolean controls() {
        return prefs.getBoolean("controls", true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        dR = 70 * d;
        dx0 = dR + 28 * d;
        dy0 = h - dR - 28 * d;
        aR = 42 * d;
        ax = w - aR - 32 * d;
        ay = h - aR - 36 * d;
        bR = 34 * d;
        bx = ax - aR - bR - 12 * d;
        by = ay;
        startR.set(w - 92 * d, 12 * d, w - 16 * d, 42 * d);
        modsR.set(startR.left - 80 * d, 12 * d, startR.left - 10 * d, 42 * d);
    }

    private void pill(Canvas c, RectF r, String s, boolean on) {
        fill.setColor(0xFFFFFFFF);
        fill.setAlpha(on ? 170 : 70);
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill);
        txt.setTextSize(13 * d);
        c.drawText(s, r.centerX(), r.centerY() + 4.5f * d, txt);
    }

    private void circle(Canvas c, float x, float y, float r, String s, boolean on) {
        fill.setColor(0xFFFFFFFF);
        fill.setAlpha(on ? 170 : 70);
        c.drawCircle(x, y, r, fill);
        txt.setTextSize(r * 0.7f);
        c.drawText(s, x, y + r * 0.25f, txt);
    }

    private void arrow(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, boolean on) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.close();
        fill.setColor(0xFFFFFFFF);
        fill.setAlpha(on ? 200 : 110);
        c.drawPath(p, fill);
    }

    @Override
    protected void onDraw(Canvas c) {
        pill(c, modsR, "MODS", false);
        if (!controls()) return;
        pill(c, startR, "START", sent.contains(START));
        fill.setColor(0xFFFFFFFF);
        fill.setAlpha(45);
        c.drawCircle(dx0, dy0, dR, fill);
        float a = dR * 0.80f, b = dR * 0.42f, w = dR * 0.24f;
        arrow(c, dx0, dy0 - a, dx0 - w, dy0 - b, dx0 + w, dy0 - b, sent.contains(UP));
        arrow(c, dx0, dy0 + a, dx0 - w, dy0 + b, dx0 + w, dy0 + b, sent.contains(DOWN));
        arrow(c, dx0 - a, dy0, dx0 - b, dy0 - w, dx0 - b, dy0 + w, sent.contains(LEFT));
        arrow(c, dx0 + a, dy0, dx0 + b, dy0 - w, dx0 + b, dy0 + w, sent.contains(RIGHT));
        circle(c, ax, ay, aR, "A", sent.contains(KA));
        circle(c, bx, by, bR, "B", sent.contains(KB));
    }

    private void keysAt(float x, float y, Set<Integer> out) {
        if (!controls()) return;
        float ddx = x - dx0, ddy = y - dy0;
        double dist = Math.hypot(ddx, ddy);
        if (dist <= dR * 1.25 && dist > dR * 0.18) {
            float k = (float) (dist * 0.38);
            if (ddx < -k) out.add(LEFT);
            if (ddx > k) out.add(RIGHT);
            if (ddy < -k) out.add(UP);
            if (ddy > k) out.add(DOWN);
        }
        if (Math.hypot(x - ax, y - ay) <= aR * 1.2) out.add(KA);
        if (Math.hypot(x - bx, y - by) <= bR * 1.2) out.add(KB);
        if (startR.contains(x, y)) out.add(START);
    }

    private void apply(Set<Integer> want) {
        for (Integer k : new HashSet<>(sent))
            if (!want.contains(k)) RSDK.nativeSetKey(k, false);
        for (Integer k : want)
            if (!sent.contains(k)) RSDK.nativeSetKey(k, true);
        sent.clear();
        sent.addAll(want);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
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

    private void showMods() {
        final File dir = new File(base, "mods");
        File[] dirs = dir.listFiles();
        final java.util.ArrayList<String> names = new java.util.ArrayList<>();
        if (dirs != null)
            for (File f : dirs)
                if (f.isDirectory()) names.add(f.getName());
        java.util.Collections.sort(names);

        final Map<String, Boolean> cfg = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(new File(dir, "modconfig.ini")))) {
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.lastIndexOf('=');
                if (eq > 0) cfg.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim().toLowerCase().startsWith("y"));
            }
        } catch (Exception ignored) {
        }

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
                .setPositiveButton("Salvar", (dlg, w) -> {
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
