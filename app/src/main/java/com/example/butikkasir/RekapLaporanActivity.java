package com.example.butikkasir;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Transaksi;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.example.butikkasir.utils.PdfSaver;
import com.example.butikkasir.utils.PrintUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RekapLaporanActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<Transaksi> listTransaksi = new ArrayList<>();

    private String filterFrom = "", filterTo = "";
    private double totalPendapatan = 0;
    private int totalCount = 0;

    // Static views
    private MaterialButton btnDariRekap, btnSampaiRekap, btnResetRekap;
    private MaterialButton btnRekapBagikan, btnRekapCetak;
    private LinearLayout metodeBreakdownLayout, topItemsLayout, transaksiListLayout;
    private TextView tvRekapPeriode, tvRekapKasirInfo;
    private TextView tvTotPendapatan, tvTotTrx, tvRataRata, tvRekapCount;
    private View cardTopItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_laporan);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarRekap);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnDariRekap         = findViewById(R.id.btnDariRekap);
        btnSampaiRekap       = findViewById(R.id.btnSampaiRekap);
        btnResetRekap        = findViewById(R.id.btnResetRekap);
        btnRekapBagikan      = findViewById(R.id.btnRekapBagikan);
        btnRekapCetak        = findViewById(R.id.btnRekapCetak);
        metodeBreakdownLayout = findViewById(R.id.metodeBreakdownLayout);
        topItemsLayout       = findViewById(R.id.topItemsLayout);
        transaksiListLayout  = findViewById(R.id.transaksiListLayout);
        cardTopItems         = findViewById(R.id.cardTopItems);
        tvRekapPeriode       = findViewById(R.id.tvRekapPeriode);
        tvRekapKasirInfo     = findViewById(R.id.tvRekapKasirInfo);
        tvTotPendapatan      = findViewById(R.id.tvTotPendapatan);
        tvTotTrx             = findViewById(R.id.tvTotTrx);
        tvRataRata           = findViewById(R.id.tvRataRata);
        tvRekapCount         = findViewById(R.id.tvRekapCount);

        btnDariRekap.setOnClickListener(v    -> showFromPicker());
        btnSampaiRekap.setOnClickListener(v  -> showToPicker());
        btnResetRekap.setOnClickListener(v   -> resetFilter());
        btnRekapBagikan.setOnClickListener(v -> shareReport());
        btnRekapCetak.setOnClickListener(v   -> printReport());

        loadAll();
    }

    // ──────────────────────────────────────────────────────────────
    //  Filter
    // ──────────────────────────────────────────────────────────────

    private void showFromPicker() {
        android.app.DatePickerDialog dlg;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        dlg = new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            filterFrom = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            btnDariRekap.setText("Dari: " + fmtShort(cal.getTime()));
            loadAll();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void showToPicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            filterTo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            btnSampaiRekap.setText("Sampai: " + fmtShort(cal.getTime()));
            loadAll();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void resetFilter() {
        filterFrom = "";
        filterTo   = "";
        btnDariRekap.setText("Dari: Semua");
        btnSampaiRekap.setText("Sampai: Semua");
        loadAll();
    }

    private String fmtShort(Date d) {
        return new SimpleDateFormat("dd MMM yy", new Locale("id", "ID")).format(d);
    }

    private String fmtDateUI(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd);
            return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(d);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Data + render pipeline
    // ──────────────────────────────────────────────────────────────

    private void loadAll() {
        listTransaksi.clear();
        totalPendapatan = 0;
        totalCount      = 0;

        Cursor c = dbHelper.getLaporanByDateRange(filterFrom, filterTo);
        if (c != null && c.moveToFirst()) {
            do {
                listTransaksi.add(new Transaksi(
                    c.getInt(0), c.getString(1), c.getDouble(2), c.getString(3), c.getString(4)));
                totalPendapatan += c.getDouble(2);
                totalCount++;
            } while (c.moveToNext());
            c.close();
        }

        renderHeader();
        renderStats();
        renderMetodeBreakdown();
        renderTopItems();
        renderTransactionList();
    }

    // ── 1. Header card ──────────────────────────────────────────

    private void renderHeader() {
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");
        String now   = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());

        String period;
        if (filterFrom.isEmpty() && filterTo.isEmpty()) {
            period = "Semua Waktu";
        } else if (!filterFrom.isEmpty() && !filterTo.isEmpty()) {
            period = fmtDateUI(filterFrom) + " s/d " + fmtDateUI(filterTo);
        } else if (!filterFrom.isEmpty()) {
            period = "Dari " + fmtDateUI(filterFrom);
        } else {
            period = "s/d " + fmtDateUI(filterTo);
        }

        tvRekapPeriode.setText("Periode: " + period);
        tvRekapKasirInfo.setText("Kasir: " + kasir + "  |  Dicetak: " + now);
    }

    // ── 2. Stats ────────────────────────────────────────────────

    private void renderStats() {
        tvTotPendapatan.setText(CurrencyFormatter.formatRupiah(totalPendapatan));
        tvTotTrx.setText(String.valueOf(totalCount));
        double rata = totalCount > 0 ? totalPendapatan / totalCount : 0;
        tvRataRata.setText(CurrencyFormatter.formatRupiah(rata));
        tvRekapCount.setText(totalCount + " data");
    }

    // ── 3. Metode breakdown ──────────────────────────────────────

    private void renderMetodeBreakdown() {
        metodeBreakdownLayout.removeAllViews();
        if (totalPendapatan == 0) {
            addInfoLabel(metodeBreakdownLayout, "Belum ada data");
            return;
        }

        Cursor mc = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (mc == null || !mc.moveToFirst()) {
            addInfoLabel(metodeBreakdownLayout, "Belum ada data");
            return;
        }

        do {
            String metode = mc.getString(0);
            int    count  = mc.getInt(1);
            double total  = mc.getDouble(2);
            int    pct    = totalPendapatan > 0 ? (int) Math.round(total / totalPendapatan * 100) : 0;
            int    color  = colorForMetode(metode);
            addMetodeBar(metode, count, total, pct, color);
        } while (mc.moveToNext());
        mc.close();
    }

    private void addMetodeBar(String metode, int count, double total, int pct, int barColor) {
        // Name + stats row
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        irParams.topMargin = dpToPx(10);
        infoRow.setLayoutParams(irParams);

        // Colored dot
        View dot = new View(this);
        int dotSize = dpToPx(10);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotP.rightMargin = dpToPx(8);
        dot.setLayoutParams(dotP);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(barColor);
        dot.setBackground(dotBg);

        TextView tvMetode = new TextView(this);
        tvMetode.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvMetode.setText(metode);
        tvMetode.setTextColor(Color.parseColor("#212121"));
        tvMetode.setTextSize(13f);
        tvMetode.setTypeface(null, Typeface.BOLD);

        TextView tvStats = new TextView(this);
        tvStats.setText(count + " trx  " + CurrencyFormatter.formatRupiah(total));
        tvStats.setTextColor(Color.parseColor("#757575"));
        tvStats.setTextSize(11f);

        infoRow.addView(dot);
        infoRow.addView(tvMetode);
        infoRow.addView(tvStats);

        // Progress bar
        LinearLayout barContainer = new LinearLayout(this);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bcParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        bcParams.topMargin = dpToPx(5);
        bcParams.bottomMargin = dpToPx(2);
        barContainer.setLayoutParams(bcParams);

        GradientDrawable filledBg = new GradientDrawable();
        filledBg.setColor(barColor);
        filledBg.setCornerRadius(dpToPx(4));

        GradientDrawable emptyBg = new GradientDrawable();
        emptyBg.setColor(Color.parseColor("#EEEEEE"));
        emptyBg.setCornerRadius(dpToPx(4));

        View filled = new View(this);
        filled.setBackground(filledBg);
        filled.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct > 0 ? pct : 1));

        View empty = new View(this);
        empty.setBackground(emptyBg);
        empty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - pct));

        barContainer.addView(filled);
        barContainer.addView(empty);

        // Percentage label
        TextView tvPct = new TextView(this);
        tvPct.setText(pct + "%");
        tvPct.setTextColor(barColor);
        tvPct.setTextSize(10f);
        tvPct.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams pctP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pctP.topMargin = dpToPx(2);
        tvPct.setLayoutParams(pctP);

        metodeBreakdownLayout.addView(infoRow);
        metodeBreakdownLayout.addView(barContainer);
        metodeBreakdownLayout.addView(tvPct);
    }

    private int colorForMetode(String m) {
        if (m == null) return Color.parseColor("#9C27B0");
        String lo = m.toLowerCase();
        if (lo.contains("tunai") || lo.contains("cash")) return Color.parseColor("#4CAF50");
        if (lo.contains("debit") || lo.contains("transfer")) return Color.parseColor("#2196F3");
        if (lo.contains("qris") || lo.contains("gopay") || lo.contains("ovo")
                || lo.contains("dana") || lo.contains("shopeepay")) return Color.parseColor("#FF9800");
        return Color.parseColor("#9C27B0");
    }

    // ── 4. Top items ────────────────────────────────────────────

    private void renderTopItems() {
        topItemsLayout.removeAllViews();
        Map<String, Integer> itemCount = new HashMap<>();
        Pattern pat = Pattern.compile("([^(]+)\\([^)]+\\)x(\\d+)");

        for (Transaksi t : listTransaksi) {
            String detail = t.getDetailBarang();
            if (detail == null || detail.isEmpty()) continue;
            Matcher mat = pat.matcher(detail);
            while (mat.find()) {
                String name = mat.group(1).trim();
                int qty = Integer.parseInt(mat.group(2));
                itemCount.put(name, itemCount.getOrDefault(name, 0) + qty);
            }
        }

        if (itemCount.isEmpty()) {
            cardTopItems.setVisibility(View.GONE);
            return;
        }

        // Sort descending by qty
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemCount.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        if (sorted.size() > 5) sorted = sorted.subList(0, 5);

        cardTopItems.setVisibility(View.VISIBLE);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            addTopItemRow(rank++, entry.getKey(), entry.getValue(), sorted.get(0).getValue());
        }
    }

    private void addTopItemRow(int rank, String name, int qty, int maxQty) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dpToPx(8);
        row.setLayoutParams(rp);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvRank = new TextView(this);
        tvRank.setText(rank + ". ");
        tvRank.setTextColor(Color.parseColor("#E91E63"));
        tvRank.setTextSize(12f);
        tvRank.setTypeface(null, Typeface.BOLD);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#212121"));
        tvName.setTextSize(12f);
        tvName.setMaxLines(1);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView tvQty = new TextView(this);
        tvQty.setText(qty + " pcs");
        tvQty.setTextColor(Color.parseColor("#E91E63"));
        tvQty.setTextSize(12f);
        tvQty.setTypeface(null, Typeface.BOLD);

        nameRow.addView(tvRank);
        nameRow.addView(tvName);
        nameRow.addView(tvQty);

        // Mini bar
        LinearLayout miniBar = new LinearLayout(this);
        miniBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(4));
        mbp.topMargin = dpToPx(3);
        miniBar.setLayoutParams(mbp);

        int pct = maxQty > 0 ? qty * 100 / maxQty : 0;
        GradientDrawable filledD = new GradientDrawable();
        filledD.setColor(Color.parseColor("#E91E63"));
        filledD.setCornerRadius(dpToPx(2));
        View fv = new View(this);
        fv.setBackground(filledD);
        fv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct));

        GradientDrawable emptyD = new GradientDrawable();
        emptyD.setColor(Color.parseColor("#F5F5F5"));
        View ev = new View(this);
        ev.setBackground(emptyD);
        ev.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - pct));

        miniBar.addView(fv);
        miniBar.addView(ev);

        row.addView(nameRow);
        row.addView(miniBar);
        topItemsLayout.addView(row);
    }

    // ── 5. Transaction list ──────────────────────────────────────

    private void renderTransactionList() {
        transaksiListLayout.removeAllViews();

        if (listTransaksi.isEmpty()) {
            addInfoLabel(transaksiListLayout, "Belum ada transaksi");
            return;
        }

        for (Transaksi t : listTransaksi) {
            transaksiListLayout.addView(buildTransaksiCard(t));
        }
    }

    private View buildTransaksiCard(Transaksi t) {
        CardView card = new CardView(this);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(8);
        card.setLayoutParams(cardParams);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        // Header row: #ID | date | total
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvId = new TextView(this);
        tvId.setText("#" + t.getIdTransaksi());
        tvId.setTextColor(Color.parseColor("#9E9E9E"));
        tvId.setTextSize(11f);
        tvId.setPadding(0, 0, dpToPx(8), 0);

        TextView tvTgl = new TextView(this);
        tvTgl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        String tgl = t.getTanggal();
        if (tgl != null && tgl.length() > 16) tgl = tgl.substring(0, 16);
        tvTgl.setText(tgl != null ? tgl : "-");
        tvTgl.setTextColor(Color.parseColor("#616161"));
        tvTgl.setTextSize(11f);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()));
        tvTotal.setTextColor(Color.parseColor("#E91E63"));
        tvTotal.setTextSize(15f);
        tvTotal.setTypeface(null, Typeface.BOLD);

        headerRow.addView(tvId);
        headerRow.addView(tvTgl);
        headerRow.addView(tvTotal);

        // Method chip
        TextView tvMetode = new TextView(this);
        tvMetode.setText(t.getMetodePembayaran());
        tvMetode.setTextColor(Color.WHITE);
        tvMetode.setTextSize(10f);
        tvMetode.setTypeface(null, Typeface.BOLD);
        tvMetode.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(colorForMetode(t.getMetodePembayaran()));
        chipBg.setCornerRadius(dpToPx(12));
        tvMetode.setBackground(chipBg);
        LinearLayout.LayoutParams mpChip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mpChip.topMargin = dpToPx(6);
        mpChip.bottomMargin = dpToPx(6);
        tvMetode.setLayoutParams(mpChip);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F5F5F5"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));

        // Items section
        LinearLayout itemsLayout = new LinearLayout(this);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dpToPx(6);
        itemsLayout.setLayoutParams(ilp);

        parseAndAddItems(itemsLayout, t.getDetailBarang());

        inner.addView(headerRow);
        inner.addView(tvMetode);
        inner.addView(divider);
        inner.addView(itemsLayout);
        card.addView(inner);
        return card;
    }

    private void parseAndAddItems(LinearLayout container, String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            addInfoLabel(container, "—");
            return;
        }

        Pattern pat = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)");
        Matcher mat = pat.matcher(detail);
        boolean found = false;
        while (mat.find()) {
            found = true;
            String name = mat.group(1).trim();
            String size = mat.group(2);
            int    qty  = Integer.parseInt(mat.group(3));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dpToPx(3);
            row.setLayoutParams(rp);

            TextView bullet = new TextView(this);
            bullet.setText("• ");
            bullet.setTextColor(Color.parseColor("#BDBDBD"));
            bullet.setTextSize(11f);

            TextView tvItem = new TextView(this);
            tvItem.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvItem.setText(name + "  (" + size + ")");
            tvItem.setTextColor(Color.parseColor("#424242"));
            tvItem.setTextSize(12f);
            tvItem.setMaxLines(1);
            tvItem.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView tvQty = new TextView(this);
            tvQty.setText("× " + qty + " pcs");
            tvQty.setTextColor(Color.parseColor("#757575"));
            tvQty.setTextSize(11f);

            row.addView(bullet);
            row.addView(tvItem);
            row.addView(tvQty);
            container.addView(row);
        }

        if (!found) {
            TextView tv = new TextView(this);
            tv.setText(detail);
            tv.setTextColor(Color.parseColor("#757575"));
            tv.setTextSize(11f);
            container.addView(tv);
        }
    }

    // ── Helper ──────────────────────────────────────────────────

    private void addInfoLabel(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#BDBDBD"));
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(8), 0, dpToPx(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tv);
    }

    // ──────────────────────────────────────────────────────────────
    //  Share
    // ──────────────────────────────────────────────────────────────

    private void shareReport() {
        if (listTransaksi.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk dibagikan", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, buildTextReport());
        startActivity(Intent.createChooser(intent, "Bagikan Rekap via"));
    }

    private String buildTextReport() {
        String now   = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");

        String period;
        if (filterFrom.isEmpty() && filterTo.isEmpty()) period = "Semua Waktu";
        else if (!filterFrom.isEmpty() && !filterTo.isEmpty()) period = fmtDateUI(filterFrom) + " s/d " + fmtDateUI(filterTo);
        else if (!filterFrom.isEmpty()) period = "Dari " + fmtDateUI(filterFrom);
        else period = "s/d " + fmtDateUI(filterTo);

        double rata = totalCount > 0 ? totalPendapatan / totalCount : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n")
          .append("         BUTIK KASIR\n")
          .append("       REKAP PENJUALAN\n")
          .append("=====================================\n")
          .append("Kasir   : ").append(kasir).append("\n")
          .append("Periode : ").append(period).append("\n")
          .append("Dicetak : ").append(now).append("\n")
          .append("-------------------------------------\n")
          .append("RINGKASAN\n")
          .append(String.format("%-20s: %s\n", "Total Pendapatan", CurrencyFormatter.formatRupiah(totalPendapatan)))
          .append(String.format("%-20s: %d transaksi\n", "Total Transaksi", totalCount))
          .append(String.format("%-20s: %s\n", "Rata-rata / Trx", CurrencyFormatter.formatRupiah(rata)))
          .append("-------------------------------------\n")
          .append("BREAKDOWN PEMBAYARAN\n");

        Cursor mc = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (mc != null && mc.moveToFirst()) {
            do {
                String met = mc.getString(0);
                int    cnt = mc.getInt(1);
                double tot = mc.getDouble(2);
                int    pct = totalPendapatan > 0 ? (int)(tot / totalPendapatan * 100) : 0;
                sb.append(String.format("• %-16s: %d trx  %s  (%d%%)\n", met, cnt,
                        CurrencyFormatter.formatRupiah(tot), pct));
            } while (mc.moveToNext());
            mc.close();
        }

        // Top items
        Map<String, Integer> itemMap = new HashMap<>();
        Pattern pat = Pattern.compile("([^(]+)\\([^)]+\\)x(\\d+)");
        for (Transaksi t : listTransaksi) {
            if (t.getDetailBarang() == null) continue;
            Matcher mat = pat.matcher(t.getDetailBarang());
            while (mat.find()) {
                String n = mat.group(1).trim();
                itemMap.put(n, itemMap.getOrDefault(n, 0) + Integer.parseInt(mat.group(2)));
            }
        }
        if (!itemMap.isEmpty()) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemMap.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            sb.append("-------------------------------------\n").append("ITEM TERLARIS\n");
            int rank = 1;
            for (Map.Entry<String, Integer> e : sorted.subList(0, Math.min(5, sorted.size()))) {
                sb.append(rank++).append(". ").append(e.getKey())
                  .append(" — ").append(e.getValue()).append(" pcs\n");
            }
        }

        sb.append("-------------------------------------\n")
          .append("RIWAYAT TRANSAKSI\n")
          .append("-------------------------------------\n");
        int no = 1;
        for (Transaksi t : listTransaksi) {
            sb.append(no++).append(". ").append(t.getTanggal()).append("\n")
              .append("   Metode : ").append(t.getMetodePembayaran()).append("\n")
              .append("   Total  : ").append(CurrencyFormatter.formatRupiah(t.getTotalBelanja())).append("\n");
            if (t.getDetailBarang() != null && !t.getDetailBarang().isEmpty()) {
                sb.append("   Items  : ").append(t.getDetailBarang().trim()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("=====================================\n")
          .append("TOTAL: ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n")
          .append("=====================================\n")
          .append("Dibuat oleh BUTIK KASIR App");
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
            .setTitle("Cetak / Simpan Rekap")
            .setItems(new CharSequence[]{
                    "Simpan PDF ke Perangkat",
                    "Cetak ke Printer"
            }, (d, which) -> {
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                if (which == 0) {
                    android.graphics.pdf.PdfDocument pdf = buildPdf();
                    PdfSaver.save(this, pdf, "Rekap_Penjualan_" + ts + ".pdf",
                        new PdfSaver.Callback() {
                            @Override public void onSuccess(Uri uri, String name) {
                                PdfSaver.showSuccessDialog(RekapLaporanActivity.this, uri, name, null);
                            }
                            @Override public void onError(String msg) {
                                Toast.makeText(RekapLaporanActivity.this,
                                    "Gagal menyimpan: " + msg, Toast.LENGTH_LONG).show();
                            }
                        });
                } else {
                    android.graphics.pdf.PdfDocument pdf = buildPdf();
                    PrintUtils.printPdf(this, pdf, "Rekap_Penjualan_" + ts);
                }
            })
            .show();
    }

    private android.graphics.pdf.PdfDocument buildPdf() {
        final int W = 595, H = 842, M = 40;
        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();

        Paint pPink  = mkPaint(18f, Color.parseColor("#E91E63"), true, Paint.Align.CENTER);
        Paint pGrayC = mkPaint(10f, Color.parseColor("#757575"), false, Paint.Align.CENTER);
        Paint pSec   = mkPaint(11f, Color.parseColor("#E91E63"), true, Paint.Align.LEFT);
        Paint pBold  = mkPaint(10f, Color.parseColor("#212121"), true, Paint.Align.LEFT);
        Paint pNorm  = mkPaint(10f, Color.parseColor("#212121"), false, Paint.Align.LEFT);
        Paint pGray  = mkPaint(9f,  Color.parseColor("#757575"), false, Paint.Align.LEFT);
        Paint pBoldR = mkPaint(10f, Color.parseColor("#212121"), true, Paint.Align.RIGHT);
        Paint pPinkR = mkPaint(11f, Color.parseColor("#E91E63"), true, Paint.Align.RIGHT);
        Paint pLine  = new Paint(); pLine.setColor(Color.parseColor("#DDDDDD")); pLine.setStrokeWidth(0.5f);
        Paint pDark  = new Paint(); pDark.setColor(Color.parseColor("#AAAAAA")); pDark.setStrokeWidth(0.5f);

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

        String kasir  = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        String now    = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id","ID")).format(new Date());
        String period;
        if (filterFrom.isEmpty() && filterTo.isEmpty()) period = "Semua Waktu";
        else if (!filterFrom.isEmpty() && !filterTo.isEmpty()) period = fmtDateUI(filterFrom) + " s/d " + fmtDateUI(filterTo);
        else if (!filterFrom.isEmpty()) period = "Dari " + fmtDateUI(filterFrom);
        else period = "s/d " + fmtDateUI(filterTo);

        // ─── Header ───
        y[0] += 16;
        cv[0].drawText("BUTIK KASIR", W / 2f, y[0], pPink);
        y[0] += 14; cv[0].drawText("REKAP PENJUALAN", W / 2f, y[0], pGrayC);
        y[0] += 12; cv[0].drawText("Periode: " + period, W / 2f, y[0], pGrayC);
        y[0] += 11; cv[0].drawText("Kasir: " + kasir + "  |  " + now, W / 2f, y[0], pGrayC);
        y[0] += 10; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;

        // ─── Ringkasan ───
        cv[0].drawText("RINGKASAN", M, y[0], pSec); y[0] += 14;
        cv[0].drawText("Total Pendapatan  : " + CurrencyFormatter.formatRupiah(totalPendapatan), M, y[0], pBold); y[0] += 13;
        cv[0].drawText("Total Transaksi   : " + totalCount + " transaksi", M, y[0], pNorm); y[0] += 13;
        double rata = totalCount > 0 ? totalPendapatan / totalCount : 0;
        cv[0].drawText("Rata-rata / Trx   : " + CurrencyFormatter.formatRupiah(rata), M, y[0], pNorm); y[0] += 13;

        // ─── Metode ───
        Cursor mc = dbHelper.getMetodeSummary(filterFrom, filterTo);
        if (mc != null && mc.moveToFirst()) {
            cv[0].drawText("Metode Pembayaran:", M, y[0], pBold); y[0] += 13;
            do {
                String met = mc.getString(0); int cnt = mc.getInt(1); double tot = mc.getDouble(2);
                int pct = totalPendapatan > 0 ? (int)(tot / totalPendapatan * 100) : 0;
                cv[0].drawText("  • " + met + " : " + cnt + " trx  " +
                        CurrencyFormatter.formatRupiah(tot) + "  (" + pct + "%)", M + 6, y[0], pNorm);
                y[0] += 12;
            } while (mc.moveToNext());
            mc.close();
        }

        // ─── Top Items ───
        Map<String, Integer> itemMap = new LinkedHashMap<>();
        Pattern pat = Pattern.compile("([^(]+)\\([^)]+\\)x(\\d+)");
        for (Transaksi t : listTransaksi) {
            if (t.getDetailBarang() == null) continue;
            Matcher mat = pat.matcher(t.getDetailBarang());
            while (mat.find()) {
                String n = mat.group(1).trim();
                itemMap.put(n, itemMap.getOrDefault(n, 0) + Integer.parseInt(mat.group(2)));
            }
        }
        if (!itemMap.isEmpty()) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemMap.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            cv[0].drawText("Item Terlaris:", M, y[0], pBold); y[0] += 13;
            int rk = 1;
            for (Map.Entry<String, Integer> e : sorted.subList(0, Math.min(5, sorted.size()))) {
                cv[0].drawText("  " + rk++ + ". " + e.getKey() + " — " + e.getValue() + " pcs",
                        M + 6, y[0], pNorm); y[0] += 12;
            }
        }
        y[0] += 6; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;

        // ─── Tabel transaksi ───
        cv[0].drawText("RIWAYAT TRANSAKSI", M, y[0], pSec); y[0] += 14;
        int cNo = M, cTgl = M + 22, cMet = M + 170, cTot = W - M;
        cv[0].drawText("No", cNo, y[0], pBold);
        cv[0].drawText("Tanggal", cTgl, y[0], pBold);
        cv[0].drawText("Metode", cMet, y[0], pBold);
        cv[0].drawText("Total", cTot, y[0], pBoldR);
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;

        int no = 1;
        for (Transaksi t : listTransaksi) {
            int rowH = 14 + (t.getDetailBarang() != null && !t.getDetailBarang().isEmpty() ? 12 : 0);
            if (y[0] + rowH > H - M) {
                pGrayC.setTextAlign(Paint.Align.CENTER);
                cv[0].drawText("Halaman " + pgNum[0], W / 2f, H - M - 4, pGrayC);
                newPage.run();
                cv[0].drawText("No", cNo, y[0], pBold); cv[0].drawText("Tanggal", cTgl, y[0], pBold);
                cv[0].drawText("Metode", cMet, y[0], pBold); cv[0].drawText("Total", cTot, y[0], pBoldR);
                y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;
            }
            String tgl = t.getTanggal(); if (tgl != null && tgl.length() > 19) tgl = tgl.substring(0, 19);
            String met = t.getMetodePembayaran(); if (met != null && met.length() > 14) met = met.substring(0, 13) + "..";
            cv[0].drawText(String.valueOf(no++), cNo, y[0], pGray);
            cv[0].drawText(tgl != null ? tgl : "-", cTgl, y[0], pNorm);
            cv[0].drawText(met != null ? met : "-", cMet, y[0], pNorm);
            cv[0].drawText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()), cTot, y[0], pBoldR);
            y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 11;
        }

        // ─── Footer ───
        if (y[0] + 30 > H - M) newPage.run();
        cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 13;
        cv[0].drawText("TOTAL PENDAPATAN", M, y[0], pSec);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), cTot, y[0], pPinkR);
        y[0] += 16; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;
        Paint pFoot = mkPaint(8f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
        cv[0].drawText("Rekap Penjualan  •  BUTIK KASIR App  •  " + now, W / 2f, y[0], pFoot);

        pdf.finishPage(pg[0]);
        return pdf;
    }

    private Paint mkPaint(float sz, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(sz);
        p.setColor(color);
        if (bold) p.setFakeBoldText(true);
        p.setTextAlign(align);
        return p;
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}
