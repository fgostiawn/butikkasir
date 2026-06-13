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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.adapter.LaporanAdapter;
import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Transaksi;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.example.butikkasir.utils.PdfSaver;
import com.example.butikkasir.utils.PrintUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LaporanActivity extends AppCompatActivity {

    private RecyclerView rvLaporan;
    private TextView tvTotalPendapatan, tvTotalTransaksi, tvJumlahTransaksi;
    private MaterialButton btnDariTanggal, btnSampaiTanggal, btnResetFilter;
    private MaterialButton btnBagikanLaporan, btnCetakLaporan;
    private LinearLayout summaryMetodeContainer;

    private DatabaseHelper dbHelper;
    private final List<Transaksi> listTransaksi = new ArrayList<>();
    private LaporanAdapter adapter;

    private String filterFrom = "";
    private String filterTo   = "";
    private int    totalCount = 0;
    private double totalPendapatan = 0;

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
        summaryMetodeContainer = findViewById(R.id.summaryMetodeContainer);

        rvLaporan.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LaporanAdapter(listTransaksi);
        rvLaporan.setAdapter(adapter);

        loadData();

        btnDariTanggal.setOnClickListener(v    -> showFromDatePicker());
        btnSampaiTanggal.setOnClickListener(v  -> showToDatePicker());
        btnResetFilter.setOnClickListener(v    -> resetFilter());
        btnBagikanLaporan.setOnClickListener(v -> shareReport());
        btnCetakLaporan.setOnClickListener(v   -> printReport());
    }

    // ──────────────────────────────────────────────────────────────
    //  Filter
    // ──────────────────────────────────────────────────────────────

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
        btnDariTanggal.setText("Dari: Semua");
        btnSampaiTanggal.setText("Sampai: Semua");
        loadData();
    }

    private String formatShort(Date date) {
        return new SimpleDateFormat("dd MMM yy", new Locale("id", "ID")).format(date);
    }

    private String formatDateUI(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd);
            return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(d);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Data load
    // ──────────────────────────────────────────────────────────────

    private void loadData() {
        listTransaksi.clear();
        totalPendapatan = 0;
        totalCount      = 0;

        Cursor cursor = dbHelper.getLaporanByDateRange(filterFrom, filterTo);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int    id     = cursor.getInt(0);
                String tanggal = cursor.getString(1);
                double total  = cursor.getDouble(2);
                String metode = cursor.getString(3);
                String detail = cursor.getString(4);
                listTransaksi.add(new Transaksi(id, tanggal, total, metode, detail));
                totalPendapatan += total;
                totalCount++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter.notifyDataSetChanged();
        tvTotalTransaksi.setText(String.valueOf(totalCount));
        tvTotalPendapatan.setText(CurrencyFormatter.formatRupiah(totalPendapatan));
        tvJumlahTransaksi.setText(totalCount + " data");

        updateMetodeSummary();
    }

    private void updateMetodeSummary() {
        summaryMetodeContainer.removeAllViews();
        Cursor c = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (c == null || !c.moveToFirst()) {
            addMetodeRow("Belum ada data", 0, 0);
            return;
        }
        do {
            String metode = c.getString(0);
            int    count  = c.getInt(1);
            double total  = c.getDouble(2);
            addMetodeRow(metode, count, total);
        } while (c.moveToNext());
        c.close();
    }

    private void addMetodeRow(String metode, int count, double total) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dpToPx(4);
        row.setLayoutParams(rowParams);

        TextView tvMetode = new TextView(this);
        tvMetode.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvMetode.setText("• " + metode);
        tvMetode.setTextColor(Color.parseColor("#880E4F"));
        tvMetode.setTextSize(12f);

        TextView tvCount = new TextView(this);
        tvCount.setText(count + " trx");
        tvCount.setTextColor(Color.parseColor("#880E4F"));
        tvCount.setTextSize(12f);
        tvCount.setPadding(dpToPx(8), 0, dpToPx(8), 0);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(CurrencyFormatter.formatRupiah(total));
        tvTotal.setTextColor(Color.parseColor("#E91E63"));
        tvTotal.setTextSize(12f);
        tvTotal.setTypeface(null, Typeface.BOLD);

        row.addView(tvMetode);
        row.addView(tvCount);
        row.addView(tvTotal);
        summaryMetodeContainer.addView(row);
    }

    // ──────────────────────────────────────────────────────────────
    //  Share (teks laporan)
    // ──────────────────────────────────────────────────────────────

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
        String now = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");

        String period;
        if (filterFrom.isEmpty() && filterTo.isEmpty()) {
            period = "Semua Waktu";
        } else if (!filterFrom.isEmpty() && !filterTo.isEmpty()) {
            period = formatDateUI(filterFrom) + " s/d " + formatDateUI(filterTo);
        } else if (!filterFrom.isEmpty()) {
            period = "Dari " + formatDateUI(filterFrom);
        } else {
            period = "s/d " + formatDateUI(filterTo);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("          BUTIK KASIR\n");
        sb.append("        Laporan Penjualan\n");
        sb.append("=====================================\n");
        sb.append("Kasir    : ").append(kasir).append("\n");
        sb.append("Periode  : ").append(period).append("\n");
        sb.append("Dicetak  : ").append(now).append("\n");
        sb.append("-------------------------------------\n");
        sb.append("RINGKASAN\n");
        sb.append("Total Transaksi  : ").append(totalCount).append(" transaksi\n");
        sb.append("Total Pendapatan : ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n\n");

        Cursor c = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (c != null && c.moveToFirst()) {
            sb.append("Breakdown Pembayaran:\n");
            do {
                String metode = c.getString(0);
                int    count  = c.getInt(1);
                double total  = c.getDouble(2);
                sb.append("  • ").append(metode)
                  .append(" : ").append(count).append(" trx")
                  .append("  ").append(CurrencyFormatter.formatRupiah(total)).append("\n");
            } while (c.moveToNext());
            c.close();
        }

        sb.append("-------------------------------------\n");
        sb.append("RIWAYAT TRANSAKSI\n");
        sb.append("-------------------------------------\n");
        int no = 1;
        for (Transaksi t : listTransaksi) {
            sb.append(String.format(Locale.getDefault(), "%2d. ", no++));
            sb.append(t.getTanggal()).append("\n");
            sb.append("    ").append(t.getMetodePembayaran()).append("\n");
            sb.append("    ").append(CurrencyFormatter.formatRupiah(t.getTotalBelanja())).append("\n");
            if (t.getDetailBarang() != null && !t.getDetailBarang().isEmpty()) {
                sb.append("    ").append(t.getDetailBarang()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("=====================================\n");
        sb.append("Total: ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n");
        sb.append("=====================================\n");
        sb.append("Laporan dibuat oleh BUTIK KASIR App");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────
    //  Save PDF to device Downloads
    // ──────────────────────────────────────────────────────────────

    private void printReport() {
        if (listTransaksi.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk dicetak", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Cetak / Simpan Laporan")
            .setItems(new CharSequence[]{
                    "Simpan PDF ke Perangkat",
                    "Cetak ke Printer"
            }, (d, which) -> {
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                if (which == 0) {
                    android.graphics.pdf.PdfDocument pdf = buildReportPdf();
                    PdfSaver.save(this, pdf, "Laporan_Penjualan_" + ts + ".pdf",
                        new PdfSaver.Callback() {
                            @Override public void onSuccess(Uri uri, String name) {
                                PdfSaver.showSuccessDialog(LaporanActivity.this, uri, name, null);
                            }
                            @Override public void onError(String msg) {
                                Toast.makeText(LaporanActivity.this,
                                    "Gagal menyimpan: " + msg, Toast.LENGTH_LONG).show();
                            }
                        });
                } else {
                    android.graphics.pdf.PdfDocument pdf = buildReportPdf();
                    PrintUtils.printPdf(this, pdf, "Laporan_Penjualan_" + ts);
                }
            })
            .show();
    }

    // ──────────────────────────────────────────────────────────────
    //  PDF builder (A4, multi-page)
    // ──────────────────────────────────────────────────────────────

    private android.graphics.pdf.PdfDocument buildReportPdf() {
        final int PAGE_W  = 595;
        final int PAGE_H  = 842;
        final int MARGIN  = 40;
        final int CONTENT_W = PAGE_W - 2 * MARGIN;

        // ── Paints ──────────────────────────────────────────────
        Paint pTitle = makePaint(20f, Color.parseColor("#E91E63"), true);
        pTitle.setTextAlign(Paint.Align.CENTER);

        Paint pSub = makePaint(11f, Color.parseColor("#757575"), false);
        pSub.setTextAlign(Paint.Align.CENTER);

        Paint pHeader = makePaint(13f, Color.parseColor("#E91E63"), true);
        Paint pBold   = makePaint(10f, Color.parseColor("#212121"), true);
        Paint pNormal = makePaint(10f, Color.parseColor("#212121"), false);
        Paint pGray   = makePaint(9f,  Color.parseColor("#757575"), false);
        Paint pGrayR  = makePaint(9f,  Color.parseColor("#757575"), false);
        pGrayR.setTextAlign(Paint.Align.RIGHT);
        Paint pBoldR  = makePaint(10f, Color.parseColor("#212121"), true);
        pBoldR.setTextAlign(Paint.Align.RIGHT);
        Paint pAccentR = makePaint(11f, Color.parseColor("#E91E63"), true);
        pAccentR.setTextAlign(Paint.Align.RIGHT);

        Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLine.setColor(Color.parseColor("#DDDDDD"));
        pLine.setStrokeWidth(0.5f);
        pLine.setStyle(Paint.Style.STROKE);

        Paint pLineDark = new Paint(pLine);
        pLineDark.setColor(Color.parseColor("#AAAAAA"));

        // ── Page state ─────────────────────────────────────────
        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();
        int[] pageNum = {0};
        android.graphics.pdf.PdfDocument.Page[] curPage = {null};
        Canvas[] cv = {null};
        int[] y = {MARGIN};

        Runnable nextPage = () -> {
            if (curPage[0] != null) pdf.finishPage(curPage[0]);
            curPage[0] = pdf.startPage(
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNum[0]).create());
            cv[0] = curPage[0].getCanvas();
            y[0]  = MARGIN;
        };

        nextPage.run();

        String now = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");

        String period;
        if (filterFrom.isEmpty() && filterTo.isEmpty()) {
            period = "Semua Waktu";
        } else if (!filterFrom.isEmpty() && !filterTo.isEmpty()) {
            period = formatDateUI(filterFrom) + " s/d " + formatDateUI(filterTo);
        } else if (!filterFrom.isEmpty()) {
            period = "Dari " + formatDateUI(filterFrom);
        } else {
            period = "s/d " + formatDateUI(filterTo);
        }

        // ── Header ──────────────────────────────────────────────
        y[0] += 18;
        cv[0].drawText("BUTIK KASIR", PAGE_W / 2f, y[0], pTitle);
        y[0] += 16;
        cv[0].drawText("Laporan Penjualan", PAGE_W / 2f, y[0], pSub);
        y[0] += 14;
        cv[0].drawText("Periode : " + period, PAGE_W / 2f, y[0], pGray);
        y[0] += 12;
        cv[0].drawText("Kasir   : " + kasir + "    Dicetak: " + now, PAGE_W / 2f, y[0], pGray);
        y[0] += 10;
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
        y[0] += 14;

        // ── Ringkasan ───────────────────────────────────────────
        cv[0].drawText("RINGKASAN", MARGIN, y[0], pHeader);
        y[0] += 14;
        cv[0].drawText("Total Transaksi  : " + totalCount + " transaksi", MARGIN, y[0], pNormal);
        y[0] += 13;
        cv[0].drawText("Total Pendapatan : " + CurrencyFormatter.formatRupiah(totalPendapatan), MARGIN, y[0], pBold);
        y[0] += 13;

        Cursor mc = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (mc != null && mc.moveToFirst()) {
            cv[0].drawText("Breakdown:", MARGIN, y[0], pBold);
            y[0] += 12;
            do {
                String met = mc.getString(0);
                int    cnt = mc.getInt(1);
                double tot = mc.getDouble(2);
                cv[0].drawText("  • " + met + " : " + cnt + " trx   " +
                        CurrencyFormatter.formatRupiah(tot), MARGIN + 6, y[0], pNormal);
                y[0] += 12;
            } while (mc.moveToNext());
            mc.close();
        }
        y[0] += 6;
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
        y[0] += 14;

        // ── Tabel header ────────────────────────────────────────
        cv[0].drawText("RIWAYAT TRANSAKSI", MARGIN, y[0], pHeader);
        y[0] += 14;

        int cNo  = MARGIN;
        int cTgl = cNo  + 22;
        int cMet = cTgl + 145;
        int cTot = PAGE_W - MARGIN;  // right-aligned

        cv[0].drawText("No", cNo, y[0], pBold);
        cv[0].drawText("Tanggal", cTgl, y[0], pBold);
        cv[0].drawText("Metode", cMet, y[0], pBold);
        cv[0].drawText("Total", cTot, y[0], pBoldR);
        y[0] += 4;
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
        y[0] += 11;

        // ── Baris transaksi ─────────────────────────────────────
        int no = 1;
        for (Transaksi t : listTransaksi) {
            // Page break check (need 22pt for a row)
            if (y[0] + 22 > PAGE_H - MARGIN) {
                cv[0].drawLine(MARGIN, PAGE_H - MARGIN - 14, PAGE_W - MARGIN, PAGE_H - MARGIN - 14, pLine);
                pGray.setTextAlign(Paint.Align.CENTER);
                cv[0].drawText("Halaman " + pageNum[0], PAGE_W / 2f, PAGE_H - MARGIN - 4, pGray);
                pGray.setTextAlign(Paint.Align.LEFT);
                nextPage.run();

                // Repeat table header on new page
                cv[0].drawText("No", cNo, y[0], pBold);
                cv[0].drawText("Tanggal", cTgl, y[0], pBold);
                cv[0].drawText("Metode", cMet, y[0], pBold);
                cv[0].drawText("Total", cTot, y[0], pBoldR);
                y[0] += 4;
                cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
                y[0] += 11;
            }

            String tgl = t.getTanggal();
            if (tgl != null && tgl.length() > 19) tgl = tgl.substring(0, 19);
            String met = t.getMetodePembayaran();
            if (met != null && met.length() > 16) met = met.substring(0, 14) + "..";

            cv[0].drawText(String.valueOf(no++), cNo, y[0], pGray);
            cv[0].drawText(tgl != null ? tgl : "-", cTgl, y[0], pNormal);
            cv[0].drawText(met != null ? met : "-", cMet, y[0], pNormal);
            cv[0].drawText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()), cTot, y[0], pBoldR);
            y[0] += 4;
            cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLine);
            y[0] += 11;
        }

        // ── Total footer ────────────────────────────────────────
        if (y[0] + 30 > PAGE_H - MARGIN) {
            nextPage.run();
        }
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
        y[0] += 12;
        cv[0].drawText("TOTAL PENDAPATAN", MARGIN, y[0], pHeader);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), cTot, y[0], pAccentR);
        y[0] += 16;
        cv[0].drawLine(MARGIN, y[0], PAGE_W - MARGIN, y[0], pLineDark);
        y[0] += 12;

        Paint pFooter = makePaint(8f, Color.parseColor("#9E9E9E"), false);
        pFooter.setTextAlign(Paint.Align.CENTER);
        cv[0].drawText("Laporan dibuat oleh BUTIK KASIR App  •  " + now,
                PAGE_W / 2f, y[0], pFooter);

        pdf.finishPage(curPage[0]);
        return pdf;
    }

    private Paint makePaint(float textSizePt, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(textSizePt);
        p.setColor(color);
        if (bold) p.setFakeBoldText(true);
        return p;
    }

    // ──────────────────────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}
