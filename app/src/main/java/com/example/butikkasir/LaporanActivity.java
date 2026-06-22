package com.example.butikkasir;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.adapter.LaporanAdapter;
import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Transaksi;
import com.example.butikkasir.utils.CsvExporter;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.example.butikkasir.utils.PdfSaver;
import com.example.butikkasir.utils.PrintUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import android.graphics.drawable.GradientDrawable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LaporanActivity extends AppCompatActivity {

    private RecyclerView rvLaporan;
    private TextView tvTotalPendapatan, tvTotalTransaksi, tvJumlahTransaksi;
    private MaterialButton btnDariTanggal, btnSampaiTanggal, btnResetFilter;
    private MaterialButton btnBagikanLaporan, btnCetakLaporan;
    private MaterialButton btnHariIni, btnMingguIni, btnBulanIni;
    private Spinner spinnerKategori, spinnerKasir;
    private LinearLayout summaryMetodeContainer;

    private DatabaseHelper dbHelper;
    private final List<Transaksi> listTransaksi = new ArrayList<>();
    private LaporanAdapter adapter;

    private String filterFrom    = "";
    private String filterTo      = "";
    private String filterKategori = "Semua";
    private String filterKasir   = "Semua";
    private int    totalCount    = 0;
    private double totalPendapatan = 0;

    // Flag to prevent spinner callbacks during setup
    private boolean setupDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarLaporan);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvLaporan              = findViewById(R.id.rvLaporan);
        tvTotalPendapatan      = findViewById(R.id.tvTotalPendapatan);
        tvTotalTransaksi       = findViewById(R.id.tvTotalTransaksi);
        tvJumlahTransaksi      = findViewById(R.id.tvJumlahTransaksi);
        btnDariTanggal         = findViewById(R.id.btnDariTanggal);
        btnSampaiTanggal       = findViewById(R.id.btnSampaiTanggal);
        btnResetFilter         = findViewById(R.id.btnResetFilter);
        btnBagikanLaporan      = findViewById(R.id.btnBagikanLaporan);
        btnCetakLaporan        = findViewById(R.id.btnCetakLaporan);
        btnHariIni             = findViewById(R.id.btnHariIni);
        btnMingguIni           = findViewById(R.id.btnMingguIni);
        btnBulanIni            = findViewById(R.id.btnBulanIni);
        spinnerKategori        = findViewById(R.id.spinnerKategoriLaporan);
        spinnerKasir           = findViewById(R.id.spinnerKasirLaporan);
        summaryMetodeContainer = findViewById(R.id.summaryMetodeContainer);

        rvLaporan.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LaporanAdapter(listTransaksi);
        adapter.setOnItemClickListener(this::showDetail);
        rvLaporan.setAdapter(adapter);

        setupSpinners();

        btnDariTanggal.setOnClickListener(v    -> showFromDatePicker());
        btnSampaiTanggal.setOnClickListener(v  -> showToDatePicker());
        btnResetFilter.setOnClickListener(v    -> resetFilter());
        btnBagikanLaporan.setOnClickListener(v -> shareReport());
        btnCetakLaporan.setOnClickListener(v   -> showPrintMenu());
        btnHariIni.setOnClickListener(v        -> setShortcut("today"));
        btnMingguIni.setOnClickListener(v      -> setShortcut("week"));
        btnBulanIni.setOnClickListener(v       -> setShortcut("month"));
    }

    // ── Spinners ────────────────────────────────────────────────────

    private void setupSpinners() {
        List<String> kategoriList = dbHelper.getAllKategoriFromBarang();
        ArrayAdapter<String> katAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kategoriList);
        katAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKategori.setAdapter(katAdapter);
        spinnerKategori.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                filterKategori = kategoriList.get(pos);
                if (setupDone) loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        List<String> kasirList = dbHelper.getDistinctKasirFromTransaksi();
        ArrayAdapter<String> kasirAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kasirList);
        kasirAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKasir.setAdapter(kasirAdapter);
        spinnerKasir.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, android.view.View v, int pos, long id) {
                filterKasir = kasirList.get(pos);
                if (setupDone) loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setupDone = true;
        loadData();
    }

    // ── Shortcut ────────────────────────────────────────────────────

    private void setShortcut(String type) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat uiSdf = new SimpleDateFormat("dd MMM yy", new Locale("id", "ID"));

        switch (type) {
            case "today":
                filterFrom = filterTo = sdf.format(cal.getTime());
                btnDariTanggal.setText("Dari: " + uiSdf.format(cal.getTime()));
                btnSampaiTanggal.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
            case "week":
                Calendar weekStart = Calendar.getInstance();
                weekStart.add(Calendar.DAY_OF_YEAR, -6); // 7 hari ke belakang termasuk hari ini
                filterFrom = sdf.format(weekStart.getTime());
                filterTo   = sdf.format(cal.getTime());
                btnDariTanggal.setText("Dari: " + uiSdf.format(weekStart.getTime()));
                btnSampaiTanggal.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
            case "month":
                Calendar monthStart = Calendar.getInstance();
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                filterFrom = sdf.format(monthStart.getTime());
                filterTo   = sdf.format(cal.getTime());
                btnDariTanggal.setText("Dari: " + uiSdf.format(monthStart.getTime()));
                btnSampaiTanggal.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
        }
        loadData();
    }

    // ── Filter ──────────────────────────────────────────────────────

    private void showFromDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            filterFrom = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.getTime());
            btnDariTanggal.setText("Dari: " + formatShort(picked.getTime()));
            loadData();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showToDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            filterTo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.getTime());
            btnSampaiTanggal.setText("Sampai: " + formatShort(picked.getTime()));
            loadData();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void resetFilter() {
        filterFrom = "";
        filterTo   = "";
        filterKategori = "Semua";
        filterKasir    = "Semua";
        btnDariTanggal.setText("Dari: Semua");
        btnSampaiTanggal.setText("Sampai: Semua");
        spinnerKategori.setSelection(0);
        spinnerKasir.setSelection(0);
        loadData();
    }

    private String formatShort(Date date) {
        return new SimpleDateFormat("dd MMM yy", new Locale("id", "ID")).format(date);
    }

    private String formatDateUI(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd);
            return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(d);
        } catch (Exception e) { return yyyyMmDd; }
    }

    // ── Data load ────────────────────────────────────────────────────

    private void loadData() {
        listTransaksi.clear();
        totalPendapatan = 0;
        totalCount      = 0;

        Cursor cursor = dbHelper.getLaporanFiltered(filterFrom, filterTo, filterKasir);
        if (cursor != null && cursor.moveToFirst()) {
            int idxKasirCol = cursor.getColumnIndex("nama_kasir");
            do {
                int    id     = cursor.getInt(0);
                String tgl    = cursor.getString(1);
                double total  = cursor.getDouble(2);
                String metode = cursor.getString(3);
                String detail = cursor.getString(4);
                String kasir  = idxKasirCol >= 0 ? cursor.getString(idxKasirCol) : "Kasir";
                listTransaksi.add(new Transaksi(id, tgl, total, metode, detail, kasir));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // In-memory kategori filter
        if (!"Semua".equals(filterKategori)) {
            List<String> namaBarang = dbHelper.getBarangNamesByKategori(filterKategori);
            Iterator<Transaksi> it = listTransaksi.iterator();
            while (it.hasNext()) {
                Transaksi t = it.next();
                boolean found = false;
                String detail = t.getDetailBarang();
                if (detail != null) {
                    for (String nama : namaBarang) {
                        if (detail.contains(nama)) { found = true; break; }
                    }
                }
                if (!found) it.remove();
            }
        }

        for (Transaksi t : listTransaksi) {
            totalPendapatan += t.getTotalBelanja();
            totalCount++;
        }

        adapter.notifyDataSetChanged();
        tvTotalTransaksi.setText(String.valueOf(totalCount));
        tvTotalPendapatan.setText(CurrencyFormatter.formatRupiah(totalPendapatan));
        tvJumlahTransaksi.setText(totalCount + " data");

        updateMetodeSummary();
    }

    private void updateMetodeSummary() {
        summaryMetodeContainer.removeAllViews();
        Cursor c = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (c == null || !c.moveToFirst()) {
            addMetodeRow("Belum ada data", 0, 0);
            return;
        }
        do {
            addMetodeRow(c.getString(0), c.getInt(1), c.getDouble(2));
        } while (c.moveToNext());
        c.close();
    }

    private void addMetodeRow(String metode, int count, double total) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dpToPx(4);
        row.setLayoutParams(rp);

        TextView tvM = new TextView(this);
        tvM.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvM.setText("• " + metode);
        tvM.setTextColor(Color.parseColor("#880E4F"));
        tvM.setTextSize(12f);

        TextView tvC = new TextView(this);
        tvC.setText(count + " trx");
        tvC.setTextColor(Color.parseColor("#880E4F"));
        tvC.setTextSize(12f);
        tvC.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        TextView tvT = new TextView(this);
        tvT.setText(CurrencyFormatter.formatRupiah(total));
        tvT.setTextColor(Color.parseColor("#E91E63"));
        tvT.setTextSize(12f);
        tvT.setTypeface(null, Typeface.BOLD);

        row.addView(tvM); row.addView(tvC); row.addView(tvT);
        summaryMetodeContainer.addView(row);
    }

    // ── Share ────────────────────────────────────────────────────────

    private void shareReport() {
        if (listTransaksi.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk dibagikan", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, buildTextReport());
        startActivity(Intent.createChooser(intent, "Bagikan Laporan via"));
    }

    private String buildTextReport() {
        String now   = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        String period = buildPeriodLabel();

        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("          BUTIK KASIR\n");
        sb.append("        Laporan Penjualan\n");
        sb.append("=====================================\n");
        sb.append("Kasir    : ").append(kasir).append("\n");
        sb.append("Periode  : ").append(period).append("\n");
        if (!"Semua".equals(filterKategori)) sb.append("Kategori : ").append(filterKategori).append("\n");
        if (!"Semua".equals(filterKasir))    sb.append("Filter Kasir : ").append(filterKasir).append("\n");
        sb.append("Dicetak  : ").append(now).append("\n");
        sb.append("-------------------------------------\n");
        sb.append("Total Transaksi  : ").append(totalCount).append(" transaksi\n");
        sb.append("Total Pendapatan : ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n\n");

        Cursor c = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (c != null && c.moveToFirst()) {
            sb.append("Breakdown Pembayaran:\n");
            do {
                sb.append("  • ").append(c.getString(0)).append(" : ")
                  .append(c.getInt(1)).append(" trx  ")
                  .append(CurrencyFormatter.formatRupiah(c.getDouble(2))).append("\n");
            } while (c.moveToNext());
            c.close();
        }

        sb.append("-------------------------------------\n");
        sb.append("RIWAYAT TRANSAKSI\n-------------------------------------\n");
        int no = 1;
        for (Transaksi t : listTransaksi) {
            sb.append(String.format(Locale.getDefault(), "%2d. ", no++));
            sb.append(t.getTanggal()).append("\n");
            sb.append("    ").append(t.getMetodePembayaran()).append("\n");
            sb.append("    ").append(CurrencyFormatter.formatRupiah(t.getTotalBelanja())).append("\n\n");
        }
        sb.append("=====================================\n");
        sb.append("Total: ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n");
        sb.append("=====================================\n");
        sb.append("Laporan dibuat oleh BUTIK KASIR App");
        return sb.toString();
    }

    // ── Print / PDF / CSV menu ───────────────────────────────────────

    private void showPrintMenu() {
        if (listTransaksi.isEmpty()) {
            Toast.makeText(this, "Tidak ada data", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Simpan / Cetak Laporan")
            .setItems(new CharSequence[]{
                    "Simpan PDF ke Perangkat",
                    "Cetak ke Printer",
                    "Export CSV / Excel"
            }, (d, which) -> {
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                if (which == 0) {
                    PdfSaver.save(this, buildReportPdf(), "Laporan_Penjualan_" + ts + ".pdf",
                        new PdfSaver.Callback() {
                            @Override public void onSuccess(Uri uri, String name) {
                                PdfSaver.showSuccessDialog(LaporanActivity.this, uri, name, null);
                            }
                            @Override public void onError(String msg) {
                                Toast.makeText(LaporanActivity.this, "Gagal: " + msg, Toast.LENGTH_LONG).show();
                            }
                        });
                } else if (which == 1) {
                    PrintUtils.printPdf(this, buildReportPdf(), "Laporan_Penjualan_" + ts);
                } else {
                    CsvExporter.export(this, listTransaksi, "Laporan_Penjualan",
                        new CsvExporter.Callback() {
                            @Override public void onSuccess(Uri uri, String name) {
                                Toast.makeText(LaporanActivity.this,
                                    "CSV tersimpan: " + name, Toast.LENGTH_LONG).show();
                            }
                            @Override public void onError(String msg) {
                                Toast.makeText(LaporanActivity.this, "Gagal: " + msg, Toast.LENGTH_LONG).show();
                            }
                        });
                }
            })
            .show();
    }

    private String buildPeriodLabel() {
        if (filterFrom.isEmpty() && filterTo.isEmpty()) return "Semua Waktu";
        if (!filterFrom.isEmpty() && !filterTo.isEmpty()) return formatDateUI(filterFrom) + " s/d " + formatDateUI(filterTo);
        if (!filterFrom.isEmpty()) return "Dari " + formatDateUI(filterFrom);
        return "s/d " + formatDateUI(filterTo);
    }

    // ── PDF builder ──────────────────────────────────────────────────

    private android.graphics.pdf.PdfDocument buildReportPdf() {
        final int W = 595, H = 842, M = 40;

        Paint pTitle  = mkPaint(18f, Color.parseColor("#E91E63"), true,  Paint.Align.CENTER);
        Paint pSubC   = mkPaint(9f,  Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
        Paint pSec    = mkPaint(11f, Color.parseColor("#E91E63"), true,  Paint.Align.LEFT);
        Paint pBold   = mkPaint(9f,  Color.parseColor("#212121"), true,  Paint.Align.LEFT);
        Paint pBoldR  = mkPaint(9f,  Color.parseColor("#212121"), true,  Paint.Align.RIGHT);
        Paint pNorm   = mkPaint(9f,  Color.parseColor("#424242"), false, Paint.Align.LEFT);
        Paint pGray   = mkPaint(8f,  Color.parseColor("#9E9E9E"), false, Paint.Align.LEFT);
        Paint pRed    = mkPaint(8f,  Color.parseColor("#F44336"), true,  Paint.Align.LEFT);
        Paint pGreen  = mkPaint(8f,  Color.parseColor("#4CAF50"), true,  Paint.Align.LEFT);
        Paint pPinkR  = mkPaint(10f, Color.parseColor("#E91E63"), true,  Paint.Align.RIGHT);
        Paint pItemGr = mkPaint(7f,  Color.parseColor("#9E9E9E"), false, Paint.Align.LEFT);
        Paint pLine   = new Paint(); pLine.setColor(Color.parseColor("#EEEEEE")); pLine.setStrokeWidth(0.5f);
        Paint pDark   = new Paint(); pDark.setColor(Color.parseColor("#BDBDBD")); pDark.setStrokeWidth(0.5f);

        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();
        int[] pgNum = {0};
        android.graphics.pdf.PdfDocument.Page[] pg = {null};
        Canvas[] cv = {null};
        int[] y = {M};

        Runnable newPage = () -> {
            if (pg[0] != null) pdf.finishPage(pg[0]);
            pg[0] = pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(W, H, ++pgNum[0]).create());
            cv[0] = pg[0].getCanvas();
            y[0]  = M;
        };
        newPage.run();

        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko == null || namaToko.isEmpty()) namaToko = "BUTIK KASIR";
        String now    = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir  = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Admin");
        String period = buildPeriodLabel();

        // ── HEADER ───────────────────────────────────────────────────
        y[0] += 18;
        cv[0].drawText(namaToko.toUpperCase(), W / 2f, y[0], pTitle);
        y[0] += 13; cv[0].drawText("LAPORAN PENJUALAN", W / 2f, y[0], pSubC);
        y[0] += 11; cv[0].drawText("Periode: " + period, W / 2f, y[0], pSubC);
        y[0] += 10; cv[0].drawText("Admin: " + kasir + "   |   " + now, W / 2f, y[0], pSubC);
        y[0] += 10; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;

        // ── RINGKASAN ────────────────────────────────────────────────
        cv[0].drawText("RINGKASAN", M, y[0], pSec); y[0] += 13;
        double rata = totalCount > 0 ? totalPendapatan / totalCount : 0;
        int col2 = M + 260;
        cv[0].drawText("Total Pendapatan", M, y[0], pBold);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), col2, y[0], pBold);
        y[0] += 12;
        cv[0].drawText("Total Transaksi",  M, y[0], pNorm);
        cv[0].drawText(totalCount + " transaksi", col2, y[0], pNorm);
        y[0] += 12;
        cv[0].drawText("Rata-rata / Trx",  M, y[0], pNorm);
        cv[0].drawText(CurrencyFormatter.formatRupiah(rata), col2, y[0], pNorm);
        y[0] += 6; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 12;

        // ── METODE PEMBAYARAN ────────────────────────────────────────
        cv[0].drawText("METODE PEMBAYARAN", M, y[0], pSec); y[0] += 13;
        cv[0].drawText("Metode", M, y[0], pBold);
        cv[0].drawText("Trx",   M + 260, y[0], pBold);
        cv[0].drawText("Total", M + 310, y[0], pBold);
        cv[0].drawText("%",     W - M,   y[0], pBoldR);
        y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 10;
        Cursor mc = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (mc != null && mc.moveToFirst()) {
            do {
                String met = mc.getString(0);
                if (met != null && met.length() > 28) met = met.substring(0, 27) + "..";
                int pct = totalPendapatan > 0 ? (int) Math.round(mc.getDouble(2) / totalPendapatan * 100) : 0;
                cv[0].drawText(met != null ? met : "-", M, y[0], pNorm);
                cv[0].drawText(String.valueOf(mc.getInt(1)), M + 260, y[0], pNorm);
                cv[0].drawText(CurrencyFormatter.formatRupiah(mc.getDouble(2)), M + 310, y[0], pNorm);
                cv[0].drawText(pct + "%", W - M, y[0], pBoldR);
                y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 10;
            } while (mc.moveToNext());
            mc.close();
        }
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;

        // ── ITEM TERLARIS ────────────────────────────────────────────
        java.util.Map<String, int[]> itemMap = new java.util.LinkedHashMap<>();
        Pattern patItem = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)");
        for (Transaksi t : listTransaksi) {
            if (t.getDetailBarang() == null) continue;
            Matcher mIt = patItem.matcher(t.getDetailBarang());
            while (mIt.find()) {
                String nm = mIt.group(1).trim().replaceAll("^[,\\s]+", "");
                String uk = mIt.group(2).trim();
                int q = Integer.parseInt(mIt.group(3));
                String key = nm + " (" + uk + ")";
                if (!itemMap.containsKey(key)) itemMap.put(key, new int[]{0});
                itemMap.get(key)[0] += q;
            }
        }
        if (!itemMap.isEmpty()) {
            cv[0].drawText("ITEM TERLARIS", M, y[0], pSec); y[0] += 13;
            java.util.List<java.util.Map.Entry<String, int[]>> sorted = new java.util.ArrayList<>(itemMap.entrySet());
            sorted.sort((a, b) -> b.getValue()[0] - a.getValue()[0]);
            int rank = 1;
            for (java.util.Map.Entry<String, int[]> e : sorted) {
                if (rank > 5) break;
                String label = rank + ".  " + e.getKey();
                if (label.length() > 52) label = label.substring(0, 51) + "..";
                cv[0].drawText(label, M, y[0], pNorm);
                cv[0].drawText(e.getValue()[0] + " pcs", W - M, y[0], pBoldR);
                y[0] += 11; rank++;
            }
            y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;
        }

        // ── RIWAYAT TRANSAKSI ────────────────────────────────────────
        cv[0].drawText("RIWAYAT TRANSAKSI", M, y[0], pSec); y[0] += 13;

        // Kolom: No | Tanggal | Barang | Metode | Status | Total
        final int cNo  = M;
        final int cTgl = M + 20;
        final int cBrg = M + 85;
        final int cMet = M + 275;
        final int cStt = M + 382;
        final int cTot = W - M;

        Runnable drawHeader = () -> {
            cv[0].drawText("No",      cNo,  y[0], pBold);
            cv[0].drawText("Tanggal", cTgl, y[0], pBold);
            cv[0].drawText("Barang",  cBrg, y[0], pBold);
            cv[0].drawText("Metode",  cMet, y[0], pBold);
            cv[0].drawText("Status",  cStt, y[0], pBold);
            cv[0].drawText("Total",   cTot, y[0], pBoldR);
            y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 11;
        };
        drawHeader.run();

        final float brgMaxW = cMet - cBrg - 6;
        final float metMaxW = cStt - cMet - 6;
        final String namaTokoCopy = namaToko;
        int no = 1;
        for (Transaksi t : listTransaksi) {
            // Parse items
            java.util.List<String> items = new java.util.ArrayList<>();
            if (t.getDetailBarang() != null && !t.getDetailBarang().isEmpty()) {
                Matcher mIt = patItem.matcher(t.getDetailBarang());
                while (mIt.find()) {
                    String nm = mIt.group(1).trim().replaceAll("^[,\\s]+", "");
                    String uk = mIt.group(2).trim();
                    String q  = mIt.group(3).trim();
                    if (nm.isEmpty()) continue;
                    String line = nm + " (" + uk + ") ×" + q;
                    if (pItemGr.measureText(line) > brgMaxW) {
                        while (line.length() > 1 && pItemGr.measureText(line + "..") > brgMaxW)
                            line = line.substring(0, line.length() - 1).trim();
                        line += "..";
                    }
                    items.add(line);
                }
            }
            if (items.isEmpty()) items.add("-");

            int rowH = 12 + Math.max(0, items.size() - 1) * 9;
            if (y[0] + rowH + 4 > H - M - 20) {
                Paint pFoot2 = mkPaint(7f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
                cv[0].drawText("Halaman " + pgNum[0] + "  —  " + namaTokoCopy, W / 2f, H - M + 4, pFoot2);
                newPage.run();
                drawHeader.run();
            }

            String rawTgl = t.getTanggal();
            String tglFmt = rawTgl != null && rawTgl.length() >= 16
                    ? rawTgl.substring(5, 10).replace("-", "/") + " " + rawTgl.substring(11, 16) : "-";

            String met = t.getMetodePembayaran() != null ? t.getMetodePembayaran() : "-";
            if (met.length() > 14) met = met.substring(0, 13) + "..";
            if (pNorm.measureText(met) > metMaxW) met = met.substring(0, Math.min(met.length(), 13)) + "..";

            boolean hutang = "HUTANG".equals(t.getStatus());

            cv[0].drawText(String.valueOf(no++), cNo,  y[0], pGray);
            cv[0].drawText(tglFmt,               cTgl, y[0], pNorm);
            cv[0].drawText(items.get(0),         cBrg, y[0], pItemGr);
            cv[0].drawText(met,                  cMet, y[0], pNorm);
            cv[0].drawText(hutang ? "HUTANG" : "LUNAS", cStt, y[0], hutang ? pRed : pGreen);
            cv[0].drawText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()), cTot, y[0], pBoldR);
            y[0] += 9;
            for (int i = 1; i < items.size(); i++) {
                cv[0].drawText(items.get(i), cBrg, y[0], pItemGr);
                y[0] += 9;
            }
            y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 9;
        }

        // ── TOTAL ────────────────────────────────────────────────────
        if (y[0] + 30 > H - M) newPage.run();
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;
        cv[0].drawText("TOTAL PENDAPATAN", M, y[0], pSec);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), cTot, y[0], pPinkR);
        y[0] += 14; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;
        Paint pFoot = mkPaint(7.5f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
        cv[0].drawText("Laporan Penjualan  •  " + namaToko + "  •  " + now, W / 2f, y[0], pFoot);

        pdf.finishPage(pg[0]);
        return pdf;
    }

    private Paint mkPaint(float sz, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(sz); p.setColor(color);
        if (bold) p.setFakeBoldText(true);
        p.setTextAlign(align);
        return p;
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void showDetail(Transaksi t) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_detail_riwayat, null);
        sheet.setContentView(root);

        ((TextView) root.findViewById(R.id.detailRiwayatTvId))
                .setText("TRX #" + t.getIdTransaksi());
        ((TextView) root.findViewById(R.id.detailRiwayatTvTanggal))
                .setText(t.getTanggal() != null ? t.getTanggal() : "-");
        ((TextView) root.findViewById(R.id.detailRiwayatTvTotal))
                .setText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()));
        ((TextView) root.findViewById(R.id.detailRiwayatTvMetode))
                .setText(t.getMetodePembayaran() != null ? t.getMetodePembayaran() : "-");
        ((TextView) root.findViewById(R.id.detailRiwayatTvKasir))
                .setText(t.getNamaKasir() != null ? t.getNamaKasir() : "-");

        // Status badge
        TextView tvStatus = root.findViewById(R.id.detailRiwayatTvStatus);
        boolean hutang = "HUTANG".equals(t.getStatus());
        tvStatus.setText(hutang ? "HUTANG" : "LUNAS");
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dpToPx(6));
        badge.setColor(hutang ? Color.parseColor("#FF5722") : Color.parseColor("#4CAF50"));
        tvStatus.setBackground(badge);

        // Item list
        LinearLayout itemContainer = root.findViewById(R.id.detailRiwayatItemContainer);
        itemContainer.removeAllViews();
        String detail = t.getDetailBarang();
        if (detail != null && !detail.isEmpty()) {
            Pattern pat = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)");
            Matcher mat = pat.matcher(detail);
            boolean any = false;
            while (mat.find()) {
                any = true;
                String nama = mat.group(1).trim().replaceAll("^[,\\s]+", "");
                String ukuran = mat.group(2).trim();
                String qty = mat.group(3).trim();
                TextView tv = new TextView(this);
                tv.setText("• " + nama + " (" + ukuran + ")  ×" + qty);
                tv.setTextColor(Color.parseColor("#424242"));
                tv.setTextSize(13f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dpToPx(4);
                tv.setLayoutParams(lp);
                itemContainer.addView(tv);
            }
            if (!any) {
                TextView tv = new TextView(this);
                tv.setText(detail);
                tv.setTextColor(Color.parseColor("#757575"));
                tv.setTextSize(13f);
                itemContainer.addView(tv);
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("—");
            tv.setTextColor(Color.parseColor("#9E9E9E"));
            tv.setTextSize(13f);
            itemContainer.addView(tv);
        }

        root.findViewById(R.id.detailRiwayatBtnTutup).setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }
}
