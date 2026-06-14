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
import com.google.android.material.button.MaterialButton;

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
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
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
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            filterFrom = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            btnDariTanggal.setText("Dari: " + formatShort(cal.getTime()));
            loadData();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showToDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            filterTo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            btnSampaiTanggal.setText("Sampai: " + formatShort(cal.getTime()));
            loadData();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
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
        final int PAGE_W = 595, PAGE_H = 842, MARGIN = 40;
        final int CONTENT_W = PAGE_W - 2 * MARGIN;

        Paint pTitle = mkPaint(20f, Color.parseColor("#E91E63"), true, Paint.Align.CENTER);
        Paint pSub   = mkPaint(11f, Color.parseColor("#757575"), false, Paint.Align.CENTER);
        Paint pHdr   = mkPaint(13f, Color.parseColor("#E91E63"), true, Paint.Align.LEFT);
        Paint pBold  = mkPaint(10f, Color.parseColor("#212121"), true, Paint.Align.LEFT);
        Paint pNorm  = mkPaint(10f, Color.parseColor("#212121"), false, Paint.Align.LEFT);
        Paint pGray  = mkPaint(9f,  Color.parseColor("#757575"), false, Paint.Align.LEFT);
        Paint pGrayR = mkPaint(9f,  Color.parseColor("#757575"), false, Paint.Align.RIGHT);
        Paint pBoldR = mkPaint(10f, Color.parseColor("#212121"), true, Paint.Align.RIGHT);
        Paint pAccR  = mkPaint(11f, Color.parseColor("#E91E63"), true, Paint.Align.RIGHT);
        Paint pLine  = new Paint(); pLine.setColor(Color.parseColor("#DDDDDD")); pLine.setStrokeWidth(0.5f);
        Paint pDark  = new Paint(); pDark.setColor(Color.parseColor("#AAAAAA")); pDark.setStrokeWidth(0.5f);

        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();
        int[] pgNum = {0};
        android.graphics.pdf.PdfDocument.Page[] pg = {null};
        Canvas[] cv = {null};
        int[] y = {MARGIN};

        Runnable nextPage = () -> {
            if (pg[0] != null) pdf.finishPage(pg[0]);
            pg[0] = pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pgNum[0]).create());
            cv[0] = pg[0].getCanvas();
            y[0]  = MARGIN;
        };
        nextPage.run();

        String now    = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir  = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        String period = buildPeriodLabel();

        y[0] += 18; cv[0].drawText("BUTIK KASIR", PAGE_W / 2f, y[0], pTitle);
        y[0] += 16; cv[0].drawText("Laporan Penjualan", PAGE_W / 2f, y[0], pSub);
        y[0] += 14; cv[0].drawText("Periode : " + period, PAGE_W / 2f, y[0], pGray);
        y[0] += 12; cv[0].drawText("Kasir   : " + kasir + "    Dicetak: " + now, PAGE_W / 2f, y[0], pGray);
        y[0] += 10; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 14;

        cv[0].drawText("RINGKASAN", MARGIN, y[0], pHdr); y[0] += 14;
        cv[0].drawText("Total Transaksi  : " + totalCount + " transaksi", MARGIN, y[0], pNorm); y[0] += 13;
        cv[0].drawText("Total Pendapatan : " + CurrencyFormatter.formatRupiah(totalPendapatan), MARGIN, y[0], pBold); y[0] += 13;

        Cursor mc = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (mc != null && mc.moveToFirst()) {
            cv[0].drawText("Breakdown:", MARGIN, y[0], pBold); y[0] += 12;
            do {
                cv[0].drawText("  • " + mc.getString(0) + " : " + mc.getInt(1) + " trx   " +
                    CurrencyFormatter.formatRupiah(mc.getDouble(2)), MARGIN + 6, y[0], pNorm);
                y[0] += 12;
            } while (mc.moveToNext());
            mc.close();
        }
        y[0] += 6; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 14;

        cv[0].drawText("RIWAYAT TRANSAKSI", MARGIN, y[0], pHdr); y[0] += 14;
        int cNo = MARGIN, cTgl = MARGIN + 22, cMet = MARGIN + 167, cTot = PAGE_W - MARGIN;
        cv[0].drawText("No", cNo, y[0], pBold); cv[0].drawText("Tanggal", cTgl, y[0], pBold);
        cv[0].drawText("Metode", cMet, y[0], pBold); cv[0].drawText("Total", cTot, y[0], pBoldR);
        y[0] += 4; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 11;

        int no = 1;
        for (Transaksi t : listTransaksi) {
            if (y[0] + 22 > PAGE_H - MARGIN) {
                pGray.setTextAlign(Paint.Align.CENTER);
                cv[0].drawText("Halaman " + pgNum[0], PAGE_W / 2f, PAGE_H - MARGIN - 4, pGray);
                pGray.setTextAlign(Paint.Align.LEFT);
                nextPage.run();
                cv[0].drawText("No", cNo, y[0], pBold); cv[0].drawText("Tanggal", cTgl, y[0], pBold);
                cv[0].drawText("Metode", cMet, y[0], pBold); cv[0].drawText("Total", cTot, y[0], pBoldR);
                y[0] += 4; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 11;
            }
            String tgl = t.getTanggal(); if (tgl != null && tgl.length() > 19) tgl = tgl.substring(0, 19);
            String met = t.getMetodePembayaran(); if (met != null && met.length() > 16) met = met.substring(0, 14) + "..";
            cv[0].drawText(String.valueOf(no++), cNo, y[0], pGray);
            cv[0].drawText(tgl != null ? tgl : "-", cTgl, y[0], pNorm);
            cv[0].drawText(met != null ? met : "-", cMet, y[0], pNorm);
            cv[0].drawText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()), cTot, y[0], pBoldR);
            y[0] += 4; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLine); y[0] += 11;
        }

        if (y[0] + 30 > PAGE_H - MARGIN) nextPage.run();
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 12;
        cv[0].drawText("TOTAL PENDAPATAN", MARGIN, y[0], pHdr);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), cTot, y[0], pAccR);
        y[0] += 16; cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pDark); y[0] += 12;
        Paint pFoot = mkPaint(8f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
        cv[0].drawText("Laporan dibuat oleh BUTIK KASIR App  •  " + now, PAGE_W / 2f, y[0], pFoot);

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
}
