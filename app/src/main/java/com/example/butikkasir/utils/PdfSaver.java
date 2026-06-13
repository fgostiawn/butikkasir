package com.example.butikkasir.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves a PdfDocument to device Downloads (API 29+) or app external docs (API 26-28),
 * then shows a success dialog with Open / Share / Print options.
 */
public class PdfSaver {

    public interface Callback {
        void onSuccess(Uri uri, String displayName);
        void onError(String message);
    }

    /**
     * Saves the PDF and calls the callback on the main thread.
     * Always closes the PdfDocument before returning.
     */
    public static void save(Context ctx,
                            android.graphics.pdf.PdfDocument pdf,
                            String fileName,
                            Callback callback) {
        try {
            Uri uri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — MediaStore Downloads, no permission needed
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                uri = ctx.getContentResolver()
                         .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new IOException("Gagal membuat file di Downloads");

                try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new IOException("Stream tidak dapat dibuka");
                    pdf.writeTo(os);
                }

            } else {
                // Android 8-9 — app-specific external docs, no permission needed
                File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = new File(ctx.getCacheDir(), "pdf");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();

                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    pdf.writeTo(out);
                }
                uri = FileProvider.getUriForFile(
                    ctx, "com.example.butikkasir.provider", file);
            }

            pdf.close();
            callback.onSuccess(uri, fileName);

        } catch (Exception e) {
            pdf.close();
            callback.onError(e.getMessage() != null ? e.getMessage() : "Terjadi kesalahan");
        }
    }

    /**
     * Shows a dialog after successful save with Open, Share, and dismiss actions.
     */
    public static void showSuccessDialog(AppCompatActivity activity,
                                         Uri fileUri,
                                         String fileName,
                                         Runnable onPrint) {
        String location = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? "Folder: Downloads"
                : "Folder: Dokumen Aplikasi (buka via File Manager)";

        new AlertDialog.Builder(activity)
            .setTitle("PDF Berhasil Disimpan")
            .setMessage(fileName + "\n\n" + location)
            .setPositiveButton("Buka PDF", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    android.widget.Toast.makeText(activity,
                        "Tidak ada aplikasi PDF reader", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Bagikan", (d, w) -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/pdf");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(share, "Kirim PDF via"));
            })
            .setNegativeButton("Tutup", null)
            .show();
    }
}
