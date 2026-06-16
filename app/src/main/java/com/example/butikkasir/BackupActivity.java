package com.example.butikkasir;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupActivity extends AppCompatActivity {

    private static final int REQUEST_RESTORE = 1001;
    private static final String DB_NAME = "ButikDB";

    private TextView tvInfoBackupTerakhir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        MaterialToolbar toolbar = findViewById(R.id.toolbarBackup);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvInfoBackupTerakhir = findViewById(R.id.tvInfoBackupTerakhir);

        MaterialButton btnBackup  = findViewById(R.id.btnMulaiBackup);
        MaterialButton btnRestore = findViewById(R.id.btnPilihFileRestore);

        btnBackup.setOnClickListener(v -> mulaiBackup());
        btnRestore.setOnClickListener(v -> konfirmasiRestore());
    }

    // ── Backup ────────────────────────────────────────────────────────

    private void mulaiBackup() {
        File dbFile = getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            Toast.makeText(this, "File database tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName  = "ButikDea_Backup_" + timestamp + ".db";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new IOException("Gagal membuat file di Downloads");

                try (InputStream is = new FileInputStream(dbFile);
                     OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new IOException("Stream tidak bisa dibuka");
                    salinStream(is, os);
                }
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = new File(getCacheDir(), "backup");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File outFile = new File(dir, fileName);
                try (InputStream is = new FileInputStream(dbFile);
                     OutputStream os = new FileOutputStream(outFile)) {
                    salinStream(is, os);
                }
            }

            String lokasi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? "Folder: Downloads" : "Folder: Dokumen Aplikasi";

            tvInfoBackupTerakhir.setText("Backup terakhir: " + fileName);
            tvInfoBackupTerakhir.setVisibility(android.view.View.VISIBLE);

            new AlertDialog.Builder(this)
                    .setTitle("Backup Berhasil")
                    .setMessage("File berhasil disimpan:\n\n" + fileName + "\n\n" + lokasi)
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Backup Gagal")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // ── Restore ───────────────────────────────────────────────────────

    private void konfirmasiRestore() {
        new AlertDialog.Builder(this)
                .setTitle("Pulihkan Data")
                .setMessage("PERHATIAN!\n\nSemua data saat ini akan diganti dengan data dari file backup yang dipilih.\n\nPastikan file yang dipilih adalah file backup yang valid (.db).\n\nLanjutkan?")
                .setPositiveButton("Pilih File", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_RESTORE);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESTORE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            prosesRestore(data.getData());
        }
    }

    private void prosesRestore(Uri uri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);

            try (InputStream is = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(dbFile)) {
                if (is == null) throw new IOException("Tidak bisa membaca file yang dipilih");
                salinStream(is, os);
            }

            new AlertDialog.Builder(this)
                    .setTitle("Restore Berhasil")
                    .setMessage("Data berhasil dipulihkan. Aplikasi akan ditutup dan perlu dibuka kembali secara manual.")
                    .setCancelable(false)
                    .setPositiveButton("Tutup Aplikasi", (d, w) -> {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    })
                    .show();

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Restore Gagal")
                    .setMessage("Pastikan file yang dipilih adalah file backup yang valid.\n\nDetail: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────

    private void salinStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();
    }
}
