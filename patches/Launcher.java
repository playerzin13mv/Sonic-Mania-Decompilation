package org.rems.rsdkv5;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Launcher extends AppCompatActivity {
    public static Launcher instance = null;
    private ActivityResultLauncher<Intent> gameLauncher;
    private boolean asked = false, useFallback = false, running = false;

    public static File defaultBase(Context c) {
        File f = c.getExternalFilesDir(null);
        return f != null ? f : c.getFilesDir();
    }

    // compatibilidade com o código antigo do RSDK.java
    public static Uri refreshStore() { return null; }
    public Uri createFile(String f) throws FileNotFoundException { throw new FileNotFoundException(f); }

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        instance = this;
        gameLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> { finishAffinity(); System.exit(0); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (running) return;
        if (hasAccess() || useFallback) { begin(); return; }
        if (asked) { useFallback = true; begin(); return; }
        asked = true;
        new AlertDialog.Builder(this)
                .setTitle("Permissão de arquivos")
                .setMessage("Para criar a pasta SonicManiaDT (onde ficam o Data.rsdk e os mods), o jogo precisa de \"Acesso a todos os arquivos\".")
                .setPositiveButton("Permitir", (d, i) -> requestAccess())
                .setNegativeButton("Usar pasta do app", (d, i) -> { useFallback = true; begin(); })
                .setCancelable(false).show();
    }

    private void requestAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void begin() {
        File base = (hasAccess() && !useFallback)
                ? new File(Environment.getExternalStorageDirectory(), "SonicManiaDT")
                : defaultBase(this);
        base.mkdirs();
        if (!base.isDirectory()) base = defaultBase(this);
        try { firstRun(base); } catch (Exception e) { e.printStackTrace(); }

        if (!new File(base, "Data.rsdk").exists()) {
            new AlertDialog.Builder(this)
                    .setTitle("Falta o Data.rsdk")
                    .setMessage("Copie o Data.rsdk do seu Sonic Mania Plus para:\n\n"
                            + base.getAbsolutePath() + "\n\nDepois toque em Tentar de novo.")
                    .setPositiveButton("Tentar de novo", (d, i) -> begin())
                    .setNegativeButton("Sair", (d, i) -> { finishAffinity(); System.exit(0); })
                    .setCancelable(false).show();
            return;
        }
        running = true;
        Intent intent = new Intent(this, RSDK.class);
        intent.putExtra("basePath", base.getAbsolutePath());
        gameLauncher.launch(intent);
    }

    private void firstRun(File base) throws IOException {
        File ini = new File(base, "Settings.ini");
        if (!ini.exists())
            writeText(ini, "; Retro Engine Config File\n\n[Game]\nlanguage=0\nusername=Playerzin13MV\ndevMenu=y\n");
        File marker = new File(base, "mods/.dt_installed");
        if (!marker.exists()) {
            copyAssets("mods", new File(base, "mods"));
            writeText(new File(base, "mods/modconfig.ini"),
                    "[Mods]\nNew Mania Touch Controls=y\nShaders=y\n");
            writeText(marker, "1");
        }
    }

    private void copyAssets(String src, File dst) throws IOException {
        AssetManager am = getAssets();
        String[] kids = am.list(src);
        if (kids != null && kids.length > 0) {
            dst.mkdirs();
            for (String k : kids) copyAssets(src + "/" + k, new File(dst, k));
        } else {
            dst.getParentFile().mkdirs();
            try (InputStream in = am.open(src); OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
        }
    }

    private static void writeText(File f, String t) throws IOException {
        f.getParentFile().mkdirs();
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(t.getBytes("UTF-8")); }
    }
}
