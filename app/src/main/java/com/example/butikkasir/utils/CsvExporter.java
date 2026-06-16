package com.example.butikkasir.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.butikkasir.model.Transaksi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExporter {
    public interface Callback {
        void onSuccess(Uri uri, String fileName);
        void onError(String message);
    }

    public static void export(Context context, List<Transaksi> list, String fileNamePrefix, Callback cb) {
        String fileName = fileNamePrefix + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
        try {
            Uri uri;
            OutputStream os;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) { cb.onError("Gagal membuat file"); return; }
                os = context.getContentResolver().openOutputStream(uri);
            } else {
                java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(dir, fileName);
                uri = Uri.fromFile(file);
                os = new java.io.FileOutputStream(file);
            }
            if (os == null) { cb.onError("Gagal membuka stream"); return; }

            OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
            w.write('﻿'); // BOM for Excel
            w.write("No,ID Transaksi,Tanggal,Kasir,Metode Pembayaran,Total (Rp),Detail Barang\n");
            int no = 1;
            for (Transaksi t : list) {
                w.write(no++ + ",");
                w.write(t.getIdTransaksi() + ",");
                w.write(csv(t.getTanggal()) + ",");
                w.write(csv(t.getNamaKasir()) + ",");
                w.write(csv(t.getMetodePembayaran()) + ",");
                w.write(String.format("%.0f", t.getTotalBelanja()) + ",");
                w.write(csv(t.getDetailBarang()) + "\n");
            }
            w.flush();
            w.close();
            os.close();
            cb.onSuccess(uri, fileName);
        } catch (IOException e) {
            cb.onError(e.getMessage() != null ? e.getMessage() : "Error tidak diketahui");
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
