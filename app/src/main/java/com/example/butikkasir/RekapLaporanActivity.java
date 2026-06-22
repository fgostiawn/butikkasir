package com.example.butikkasir;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Transaksi;
import com.example.butikkasir.utils.BarChartView;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RekapLaporanActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<Transaksi> listTransaksi = new ArrayList<>();

    private String filterFrom    = "";
    private String filterTo      = "";
    private String filterKategori = "Semua";
    private String filterKasir   = "Semua";
    private double totalPendapatan = 0;
    private int totalCount = 0;
    private boolean setupDone = false;

    // Views
    private MaterialButton btnDariRekap, btnSampaiRekap, btnResetRekap;
    private MaterialButton btnRekapBagikan, btnRekapCetak;
    private MaterialButton btnHariIniR, btnMingguIniR, btnBulanIniR;
    private Spinner spinnerKategoriR, spinnerKasirR;
    private LinearLayout metodeBreakdownLayout, topItemsLayout, transaksiListLayout;
    private TextView tvRekapPeriode, tvRekapKasirInfo;
    private TextView tvTotPendapatan, tvTotTrx, tvRataRata, tvRekapCount;
    private View cardTopItems, cardGrafik;
    private BarChartView barChart;

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
        btnHariIniR          = findViewById(R.id.btnHariIniRekap);
        btnMingguIniR        = findViewById(R.id.btnMingguIniRekap);
        btnBulanIniR         = findViewById(R.id.btnBulanIniRekap);
        spinnerKategoriR     = findViewById(R.id.spinnerKategoriRekap);
        spinnerKasirR        = findViewById(R.id.spinnerKasirRekap);
        metodeBreakdownLayout = findViewById(R.id.metodeBreakdownLayout);
        topItemsLayout       = findViewById(R.id.topItemsLayout);
        transaksiListLayout  = findViewById(R.id.transaksiListLayout);
        cardTopItems         = findViewById(R.id.cardTopItems);
        cardGrafik           = findViewById(R.id.cardGrafik);
        barChart             = findViewById(R.id.barChart);
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
        btnRekapCetak.setOnClickListener(v   -> showPrintMenu());
        btnHariIniR.setOnClickListener(v     -> setShortcut("today"));
        btnMingguIniR.setOnClickListener(v   -> setShortcut("week"));
        btnBulanIniR.setOnClickListener(v    -> setShortcut("month"));

        setupSpinners();
    }

    // ── Spinners ────────────────────────────────────────────────────

    private void setupSpinners() {
        List<String> katList = dbHelper.getAllKategoriFromBarang();
        ArrayAdapter<String> katA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, katList);
        katA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKategoriR.setAdapter(katA);
        spinnerKategoriR.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterKategori = katList.get(pos);
                if (setupDone) loadAll();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        List<String> kasirList = dbHelper.getDistinctKasirFromTransaksi();
        ArrayAdapter<String> kasirA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, kasirList);
        kasirA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKasirR.setAdapter(kasirA);
        spinnerKasirR.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterKasir = kasirList.get(pos);
                if (setupDone) loadAll();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setupDone = true;
        loadAll();
    }

    // ── Shortcut ────────────────────────────────────────────────────

    private void setShortcut(String type) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf    = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat uiSdf  = new SimpleDateFormat("dd MMM yy", new Locale("id", "ID"));
        switch (type) {
            case "today":
                filterFrom = filterTo = sdf.format(cal.getTime());
                btnDariRekap.setText("Dari: " + uiSdf.format(cal.getTime()));
                btnSampaiRekap.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
            case "week":
                Calendar ws = Calendar.getInstance();
                ws.add(Calendar.DAY_OF_YEAR, -6); // 7 hari ke belakang termasuk hari ini
                filterFrom = sdf.format(ws.getTime()); filterTo = sdf.format(cal.getTime());
                btnDariRekap.setText("Dari: " + uiSdf.format(ws.getTime()));
                btnSampaiRekap.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
            case "month":
                Calendar ms = Calendar.getInstance();
                ms.set(Calendar.DAY_OF_MONTH, 1);
                filterFrom = sdf.format(ms.getTime()); filterTo = sdf.format(cal.getTime());
                btnDariRekap.setText("Dari: " + uiSdf.format(ms.getTime()));
                btnSampaiRekap.setText("Sampai: " + uiSdf.format(cal.getTime()));
                break;
        }
        loadAll();
    }

    // ── Filter ──────────────────────────────────────────────────────

    private void showFromPicker() {
        Calendar now = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d);
            filterFrom = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.getTime());
            btnDariRekap.setText("Dari: " + fmtShort(picked.getTime()));
            loadAll();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showToPicker() {
        Calendar now = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d);
            filterTo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.getTime());
            btnSampaiRekap.setText("Sampai: " + fmtShort(picked.getTime()));
            loadAll();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void resetFilter() {
        filterFrom = ""; filterTo = ""; filterKategori = "Semua"; filterKasir = "Semua";
        btnDariRekap.setText("Dari: Semua"); btnSampaiRekap.setText("Sampai: Semua");
        spinnerKategoriR.setSelection(0); spinnerKasirR.setSelection(0);
        loadAll();
    }

    private String fmtShort(Date d) { return new SimpleDateFormat("dd MMM yy", new Locale("id", "ID")).format(d); }
    private String fmtDateUI(String s) {
        try { return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s));
        } catch (Exception e) { return s; } }

    // ── Data + render ────────────────────────────────────────────────

    private void loadAll() {
        listTransaksi.clear();
        totalPendapatan = 0; totalCount = 0;

        Cursor c = dbHelper.getLaporanFiltered(filterFrom, filterTo, filterKasir);
        if (c != null && c.moveToFirst()) {
            int idxKasir  = c.getColumnIndex("nama_kasir");
            int idxStatus = c.getColumnIndex("status_transaksi");
            do {
                String kasir  = idxKasir  >= 0 ? c.getString(idxKasir)  : "Kasir";
                String status = idxStatus >= 0 ? c.getString(idxStatus) : "LUNAS";
                if (status == null) status = "LUNAS";
                listTransaksi.add(new Transaksi(
                        c.getInt(0), c.getString(1), c.getDouble(2),
                        c.getString(3), c.getString(4), kasir, status));
                totalPendapatan += c.getDouble(2); totalCount++;
            } while (c.moveToNext());
            c.close();
        }

        // In-memory kategori filter
        if (!"Semua".equals(filterKategori)) {
            List<String> namaBarang = dbHelper.getBarangNamesByKategori(filterKategori);
            Iterator<Transaksi> it = listTransaksi.iterator();
            while (it.hasNext()) {
                Transaksi t = it.next(); boolean found = false;
                String detail = t.getDetailBarang();
                if (detail != null) for (String n : namaBarang) { if (detail.contains(n)) { found = true; break; } }
                if (!found) it.remove();
            }
            // Recalculate totals after filter
            totalPendapatan = 0; totalCount = 0;
            for (Transaksi t : listTransaksi) { totalPendapatan += t.getTotalBelanja(); totalCount++; }
        }

        renderHeader(); renderStats(); renderMetodeBreakdown();
        renderBarChart(); renderTopItems(); renderTransactionList();
    }

    private void renderHeader() {
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        String now   = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        tvRekapPeriode.setText("Periode: " + buildPeriodLabel());
        tvRekapKasirInfo.setText("Kasir: " + kasir + "  |  Dicetak: " + now);
    }

    private void renderStats() {
        tvTotPendapatan.setText(CurrencyFormatter.formatRupiah(totalPendapatan));
        tvTotTrx.setText(String.valueOf(totalCount));
        tvRataRata.setText(CurrencyFormatter.formatRupiah(totalCount > 0 ? totalPendapatan / totalCount : 0));
        tvRekapCount.setText(totalCount + " data");
    }

    private void renderMetodeBreakdown() {
        metodeBreakdownLayout.removeAllViews();
        if (totalPendapatan == 0) { addInfoLabel(metodeBreakdownLayout, "Belum ada data"); return; }
        Cursor mc = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (mc == null || !mc.moveToFirst()) { addInfoLabel(metodeBreakdownLayout, "Belum ada data"); return; }
        do {
            addMetodeBar(mc.getString(0), mc.getInt(1), mc.getDouble(2),
                totalPendapatan > 0 ? (int) Math.round(mc.getDouble(2) / totalPendapatan * 100) : 0,
                colorForMetode(mc.getString(0)));
        } while (mc.moveToNext());
        mc.close();
    }

    private void renderBarChart() {
        Cursor sc = dbHelper.getSalesPerDay(filterFrom, filterTo, filterKasir);
        List<BarChartView.Entry> entries = new ArrayList<>();
        if (sc != null && sc.moveToFirst()) {
            do {
                String day = sc.getString(0);
                float total = (float) sc.getDouble(1);
                // Short label: dd/MM
                String label = day != null && day.length() >= 10 ? day.substring(5).replace("-", "/") : day;
                entries.add(new BarChartView.Entry(label != null ? label : "", total));
            } while (sc.moveToNext());
            sc.close();
        }
        barChart.setData(entries);
        cardGrafik.setVisibility(entries.size() > 1 ? View.VISIBLE : View.GONE);
    }

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
        if (itemCount.isEmpty()) { cardTopItems.setVisibility(View.GONE); return; }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemCount.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        if (sorted.size() > 5) sorted = sorted.subList(0, 5);
        cardTopItems.setVisibility(View.VISIBLE);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) addTopItemRow(rank++, entry.getKey(), entry.getValue(), sorted.get(0).getValue());
    }

    private void renderTransactionList() {
        transaksiListLayout.removeAllViews();
        if (listTransaksi.isEmpty()) { addInfoLabel(transaksiListLayout, "Belum ada transaksi"); return; }
        for (Transaksi t : listTransaksi) transaksiListLayout.addView(buildTransaksiCard(t));
    }

    // ── UI helpers ───────────────────────────────────────────────────

    private void addMetodeBar(String metode, int count, double total, int pct, int barColor) {
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        irP.topMargin = dpToPx(10); infoRow.setLayoutParams(irP);

        View dot = new View(this);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(10));
        dotP.rightMargin = dpToPx(8); dot.setLayoutParams(dotP);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL); dotBg.setColor(barColor);
        dot.setBackground(dotBg);

        TextView tvM = new TextView(this);
        tvM.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvM.setText(metode); tvM.setTextColor(Color.parseColor("#212121")); tvM.setTextSize(13f);
        tvM.setTypeface(null, Typeface.BOLD);

        TextView tvS = new TextView(this);
        tvS.setText(count + " trx  " + CurrencyFormatter.formatRupiah(total));
        tvS.setTextColor(Color.parseColor("#757575")); tvS.setTextSize(11f);

        infoRow.addView(dot); infoRow.addView(tvM); infoRow.addView(tvS);

        LinearLayout barCont = new LinearLayout(this);
        barCont.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bcP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        bcP.topMargin = dpToPx(5); bcP.bottomMargin = dpToPx(2); barCont.setLayoutParams(bcP);

        GradientDrawable filledBg = new GradientDrawable(); filledBg.setColor(barColor); filledBg.setCornerRadius(dpToPx(4));
        GradientDrawable emptyBg = new GradientDrawable(); emptyBg.setColor(Color.parseColor("#EEEEEE")); emptyBg.setCornerRadius(dpToPx(4));
        View filled = new View(this); filled.setBackground(filledBg);
        filled.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct > 0 ? pct : 1));
        View empty = new View(this); empty.setBackground(emptyBg);
        empty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - pct));
        barCont.addView(filled); barCont.addView(empty);

        TextView tvPct = new TextView(this);
        tvPct.setText(pct + "%"); tvPct.setTextColor(barColor); tvPct.setTextSize(10f);
        tvPct.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams pctP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pctP.topMargin = dpToPx(2); tvPct.setLayoutParams(pctP);

        metodeBreakdownLayout.addView(infoRow);
        metodeBreakdownLayout.addView(barCont);
        metodeBreakdownLayout.addView(tvPct);
    }

    private void addTopItemRow(int rank, String name, int qty, int maxQty) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dpToPx(8); row.setLayoutParams(rp);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvRank = new TextView(this);
        tvRank.setText(rank + ". "); tvRank.setTextColor(Color.parseColor("#E91E63"));
        tvRank.setTextSize(12f); tvRank.setTypeface(null, Typeface.BOLD);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name); tvName.setTextColor(Color.parseColor("#212121")); tvName.setTextSize(12f);
        tvName.setMaxLines(1); tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView tvQty = new TextView(this);
        tvQty.setText(qty + " pcs"); tvQty.setTextColor(Color.parseColor("#E91E63"));
        tvQty.setTextSize(12f); tvQty.setTypeface(null, Typeface.BOLD);

        nameRow.addView(tvRank); nameRow.addView(tvName); nameRow.addView(tvQty);

        LinearLayout miniBar = new LinearLayout(this);
        miniBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(4));
        mbp.topMargin = dpToPx(3); miniBar.setLayoutParams(mbp);
        int pct = maxQty > 0 ? qty * 100 / maxQty : 0;
        GradientDrawable fd = new GradientDrawable(); fd.setColor(Color.parseColor("#E91E63")); fd.setCornerRadius(dpToPx(2));
        View fv = new View(this); fv.setBackground(fd);
        fv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct));
        GradientDrawable ed = new GradientDrawable(); ed.setColor(Color.parseColor("#F5F5F5"));
        View ev = new View(this); ev.setBackground(ed);
        ev.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - pct));
        miniBar.addView(fv); miniBar.addView(ev);

        row.addView(nameRow); row.addView(miniBar);
        topItemsLayout.addView(row);
    }

    private View buildTransaksiCard(Transaksi t) {
        CardView card = new CardView(this);
        card.setRadius(dpToPx(12)); card.setCardElevation(dpToPx(2)); card.setCardBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(8); card.setLayoutParams(cardParams);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvId = new TextView(this);
        tvId.setText("#" + t.getIdTransaksi()); tvId.setTextColor(Color.parseColor("#9E9E9E"));
        tvId.setTextSize(11f); tvId.setPadding(0, 0, dpToPx(8), 0);

        TextView tvTgl = new TextView(this);
        tvTgl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        String tgl = t.getTanggal();
        if (tgl != null && tgl.length() > 16) tgl = tgl.substring(0, 16);
        tvTgl.setText(tgl != null ? tgl : "-"); tvTgl.setTextColor(Color.parseColor("#616161")); tvTgl.setTextSize(11f);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()));
        tvTotal.setTextColor(Color.parseColor("#E91E63")); tvTotal.setTextSize(15f);
        tvTotal.setTypeface(null, Typeface.BOLD);

        headerRow.addView(tvId); headerRow.addView(tvTgl); headerRow.addView(tvTotal);

        TextView tvMetode = new TextView(this);
        tvMetode.setText(t.getMetodePembayaran()); tvMetode.setTextColor(Color.WHITE);
        tvMetode.setTextSize(10f); tvMetode.setTypeface(null, Typeface.BOLD);
        tvMetode.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(colorForMetode(t.getMetodePembayaran())); chipBg.setCornerRadius(dpToPx(12));
        tvMetode.setBackground(chipBg);
        LinearLayout.LayoutParams mpChip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mpChip.topMargin = dpToPx(6); mpChip.bottomMargin = dpToPx(4);
        tvMetode.setLayoutParams(mpChip);

        // Kasir label
        TextView tvKasirLabel = new TextView(this);
        tvKasirLabel.setText("Kasir: " + t.getNamaKasir());
        tvKasirLabel.setTextColor(Color.parseColor("#9E9E9E")); tvKasirLabel.setTextSize(10f);
        LinearLayout.LayoutParams klP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        klP.bottomMargin = dpToPx(4); tvKasirLabel.setLayoutParams(klP);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F5F5F5"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));

        LinearLayout itemsLayout = new LinearLayout(this);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = dpToPx(6); itemsLayout.setLayoutParams(ilp);
        parseAndAddItems(itemsLayout, t.getDetailBarang());

        inner.addView(headerRow); inner.addView(tvMetode); inner.addView(tvKasirLabel);
        inner.addView(divider); inner.addView(itemsLayout);
        card.addView(inner);
        card.setOnClickListener(v -> showDetail(t));
        return card;
    }

    private void parseAndAddItems(LinearLayout container, String detail) {
        if (detail == null || detail.trim().isEmpty()) { addInfoLabel(container, "—"); return; }
        Pattern pat = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)");
        Matcher mat = pat.matcher(detail);
        boolean found = false;
        while (mat.find()) {
            found = true;
            String name = mat.group(1).trim(); String size = mat.group(2); int qty = Integer.parseInt(mat.group(3));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dpToPx(3); row.setLayoutParams(rp);

            TextView bullet = new TextView(this);
            bullet.setText("• "); bullet.setTextColor(Color.parseColor("#BDBDBD")); bullet.setTextSize(11f);
            TextView tvItem = new TextView(this);
            tvItem.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvItem.setText(name + "  (" + size + ")"); tvItem.setTextColor(Color.parseColor("#424242"));
            tvItem.setTextSize(12f); tvItem.setMaxLines(1); tvItem.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView tvQty = new TextView(this);
            tvQty.setText("× " + qty + " pcs"); tvQty.setTextColor(Color.parseColor("#757575")); tvQty.setTextSize(11f);

            row.addView(bullet); row.addView(tvItem); row.addView(tvQty);
            container.addView(row);
        }
        if (!found) {
            TextView tv = new TextView(this);
            tv.setText(detail); tv.setTextColor(Color.parseColor("#757575")); tv.setTextSize(11f);
            container.addView(tv);
        }
    }

    private void addInfoLabel(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(Color.parseColor("#BDBDBD")); tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER); tv.setPadding(0, dpToPx(8), 0, dpToPx(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tv);
    }

    private int colorForMetode(String m) {
        if (m == null) return Color.parseColor("#9C27B0");
        String lo = m.toLowerCase();
        if (lo.contains("tunai") || lo.contains("cash"))  return Color.parseColor("#4CAF50");
        if (lo.contains("debit") || lo.contains("transfer")) return Color.parseColor("#2196F3");
        if (lo.contains("qris") || lo.contains("gopay") || lo.contains("ovo")
                || lo.contains("dana") || lo.contains("shopeepay")) return Color.parseColor("#FF9800");
        return Color.parseColor("#9C27B0");
    }

    // ── Share ────────────────────────────────────────────────────────

    private void shareReport() {
        if (listTransaksi.isEmpty()) { Toast.makeText(this, "Tidak ada data", Toast.LENGTH_SHORT).show(); return; }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, buildTextReport());
        startActivity(Intent.createChooser(intent, "Bagikan Rekap via"));
    }

    private String buildTextReport() {
        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko == null || namaToko.isEmpty()) namaToko = "BUTIK KASIR";
        String now   = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String kasir = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        double rata  = totalCount > 0 ? totalPendapatan / totalCount : 0;

        int countLunas = 0, countHutang = 0;
        double totalLunas = 0;
        for (Transaksi t : listTransaksi) {
            if ("HUTANG".equals(t.getStatus())) countHutang++;
            else { totalLunas += t.getTotalBelanja(); countLunas++; }
        }

        StringBuilder sb = new StringBuilder();
        String sep = "─────────────────────────────────────\n";
        sb.append("=====================================\n");
        sb.append("  ").append(namaToko.toUpperCase()).append("\n");
        sb.append("       REKAP PENJUALAN\n");
        sb.append("=====================================\n");
        sb.append("Kasir   : ").append(kasir).append("\n");
        sb.append("Periode : ").append(buildPeriodLabel()).append("\n");
        if (!"Semua".equals(filterKategori)) sb.append("Kategori: ").append(filterKategori).append("\n");
        if (!"Semua".equals(filterKasir))    sb.append("Filter  : ").append(filterKasir).append("\n");
        sb.append("Dicetak : ").append(now).append("\n");
        sb.append(sep);

        sb.append("RINGKASAN\n");
        sb.append(String.format("%-18s: %s\n", "Total Pendapatan", CurrencyFormatter.formatRupiah(totalPendapatan)));
        sb.append(String.format("%-18s: %d transaksi\n", "Total Transaksi", totalCount));
        sb.append(String.format("%-18s: %s\n", "Rata-rata / Trx", CurrencyFormatter.formatRupiah(rata)));
        sb.append(String.format("%-18s: %d trx  %s\n", "Lunas", countLunas, CurrencyFormatter.formatRupiah(totalLunas)));
        if (countHutang > 0)
            sb.append(String.format("%-18s: %d trx\n", "Hutang", countHutang));
        sb.append(sep);

        sb.append("METODE PEMBAYARAN\n");
        Cursor mc = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (mc != null && mc.moveToFirst()) {
            do {
                int pct = totalPendapatan > 0 ? (int)(mc.getDouble(2) / totalPendapatan * 100) : 0;
                sb.append(String.format("• %-14s %d trx  %s  (%d%%)\n",
                    shortenMetode(mc.getString(0)), mc.getInt(1),
                    CurrencyFormatter.formatRupiah(mc.getDouble(2)), pct));
            } while (mc.moveToNext());
            mc.close();
        }
        sb.append(sep);

        Map<String, Integer> itemMap = new HashMap<>();
        Pattern pat = Pattern.compile("([^(]+)\\([^)]+\\)x(\\d+)");
        for (Transaksi t : listTransaksi) {
            if (t.getDetailBarang() == null) continue;
            Matcher mat = pat.matcher(t.getDetailBarang());
            while (mat.find()) itemMap.put(mat.group(1).trim(), itemMap.getOrDefault(mat.group(1).trim(), 0) + Integer.parseInt(mat.group(2)));
        }
        if (!itemMap.isEmpty()) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemMap.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            sb.append("ITEM TERLARIS\n");
            int rank = 1;
            for (Map.Entry<String, Integer> e : sorted.subList(0, Math.min(5, sorted.size())))
                sb.append(rank++).append(". ").append(e.getKey()).append(" — ").append(e.getValue()).append(" pcs\n");
            sb.append(sep);
        }

        sb.append("RIWAYAT TRANSAKSI\n");
        int no = 1;
        for (Transaksi t : listTransaksi) {
            String rawTgl = t.getTanggal();
            String tglFmt = rawTgl != null && rawTgl.length() >= 16
                    ? rawTgl.substring(5, 10).replace("-", "/") + " " + rawTgl.substring(11, 16) : "-";
            sb.append(String.format("%2d. %s  %-14s  %-7s  %s\n",
                no++, tglFmt,
                shortenMetode(t.getMetodePembayaran()),
                "HUTANG".equals(t.getStatus()) ? "HUTANG" : "LUNAS",
                CurrencyFormatter.formatRupiah(t.getTotalBelanja())));
        }
        sb.append("=====================================\n");
        sb.append("TOTAL  : ").append(CurrencyFormatter.formatRupiah(totalPendapatan)).append("\n");
        sb.append("=====================================\n");
        sb.append(namaToko).append("  •  ").append(now);
        return sb.toString();
    }

    // ── Print / PDF / CSV menu ───────────────────────────────────────

    private void showPrintMenu() {
        if (listTransaksi.isEmpty()) { Toast.makeText(this, "Tidak ada data", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this)
            .setTitle("Simpan / Cetak Rekap")
            .setItems(new CharSequence[]{"Simpan PDF ke Perangkat", "Cetak ke Printer", "Export CSV / Excel"}, (d, which) -> {
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                if (which == 0) {
                    PdfSaver.save(this, buildPdf(), "Rekap_Penjualan_" + ts + ".pdf",
                        new PdfSaver.Callback() {
                            @Override public void onSuccess(Uri uri, String name) { PdfSaver.showSuccessDialog(RekapLaporanActivity.this, uri, name, null); }
                            @Override public void onError(String msg) { Toast.makeText(RekapLaporanActivity.this, "Gagal: " + msg, Toast.LENGTH_LONG).show(); }
                        });
                } else if (which == 1) {
                    PrintUtils.printPdf(this, buildPdf(), "Rekap_Penjualan_" + ts);
                } else {
                    CsvExporter.export(this, listTransaksi, "Rekap_Penjualan",
                        new CsvExporter.Callback() {
                            @Override public void onSuccess(Uri uri, String name) { Toast.makeText(RekapLaporanActivity.this, "CSV tersimpan: " + name, Toast.LENGTH_LONG).show(); }
                            @Override public void onError(String msg) { Toast.makeText(RekapLaporanActivity.this, "Gagal: " + msg, Toast.LENGTH_LONG).show(); }
                        });
                }
            }).show();
    }

    private String buildPeriodLabel() {
        if (filterFrom.isEmpty() && filterTo.isEmpty()) return "Semua Waktu";
        if (!filterFrom.isEmpty() && !filterTo.isEmpty()) return fmtDateUI(filterFrom) + " s/d " + fmtDateUI(filterTo);
        if (!filterFrom.isEmpty()) return "Dari " + fmtDateUI(filterFrom);
        return "s/d " + fmtDateUI(filterTo);
    }

    // ── PDF builder ──────────────────────────────────────────────────

    private android.graphics.pdf.PdfDocument buildPdf() {
        final int W = 595, H = 842, M = 40;
        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();

        // Paint definitions
        Paint pTitle  = mkPaint(20f, Color.parseColor("#E91E63"), true,  Paint.Align.CENTER);
        Paint pSubC   = mkPaint(9f,  Color.parseColor("#757575"), false, Paint.Align.CENTER);
        Paint pSec    = mkPaint(10f, Color.parseColor("#E91E63"), true,  Paint.Align.LEFT);
        Paint pBold   = mkPaint(9f,  Color.parseColor("#212121"), true,  Paint.Align.LEFT);
        Paint pNorm   = mkPaint(9f,  Color.parseColor("#212121"), false, Paint.Align.LEFT);
        Paint pGray   = mkPaint(8f,  Color.parseColor("#757575"), false, Paint.Align.LEFT);
        Paint pBoldR  = mkPaint(9f,  Color.parseColor("#212121"), true,  Paint.Align.RIGHT);
        Paint pPinkR  = mkPaint(11f, Color.parseColor("#E91E63"), true,  Paint.Align.RIGHT);
        Paint pGreen  = mkPaint(8f,  Color.parseColor("#388E3C"), true,  Paint.Align.LEFT);
        Paint pRed    = mkPaint(8f,  Color.parseColor("#F44336"), true,  Paint.Align.LEFT);
        Paint pLine   = new Paint(); pLine.setColor(Color.parseColor("#EEEEEE")); pLine.setStrokeWidth(0.5f);
        Paint pDark   = new Paint(); pDark.setColor(Color.parseColor("#BDBDBD")); pDark.setStrokeWidth(0.7f);

        int[] pgNum = {0};
        android.graphics.pdf.PdfDocument.Page[] pg = {null};
        Canvas[] cv = {null};
        int[] y = {M};

        Runnable newPage = () -> {
            if (pg[0] != null) pdf.finishPage(pg[0]);
            pg[0] = pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(W, H, ++pgNum[0]).create());
            cv[0] = pg[0].getCanvas();
            y[0] = M;
        };
        newPage.run();

        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko == null || namaToko.isEmpty()) namaToko = "BUTIK KASIR";
        String kasir  = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
        String now    = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date());
        String period = buildPeriodLabel();

        // ── HEADER ──────────────────────────────────────────────────
        y[0] += 20;
        cv[0].drawText(namaToko.toUpperCase(), W / 2f, y[0], pTitle);
        y[0] += 13; cv[0].drawText("REKAP PENJUALAN", W / 2f, y[0], pSubC);
        y[0] += 11; cv[0].drawText("Periode: " + period, W / 2f, y[0], pSubC);
        y[0] += 10; cv[0].drawText("Kasir: " + kasir + "   |   " + now, W / 2f, y[0], pSubC);
        y[0] += 12; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;

        // ── RINGKASAN STATISTIK ──────────────────────────────────────
        cv[0].drawText("RINGKASAN", M, y[0], pSec); y[0] += 13;

        double rata = totalCount > 0 ? totalPendapatan / totalCount : 0;
        // Hitung lunas vs hutang
        double totalLunas = 0; int countLunas = 0, countHutang = 0;
        for (Transaksi t : listTransaksi) {
            if ("HUTANG".equals(t.getStatus())) countHutang++;
            else { totalLunas += t.getTotalBelanja(); countLunas++; }
        }

        // Baris stat: 2 kolom
        int col1 = M, col2 = M + 260;
        cv[0].drawText("Total Pendapatan", col1, y[0], pBold);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), col2, y[0], pBold);
        y[0] += 12;
        cv[0].drawText("Total Transaksi", col1, y[0], pNorm);
        cv[0].drawText(totalCount + " transaksi", col2, y[0], pNorm);
        y[0] += 12;
        cv[0].drawText("Rata-rata / Trx", col1, y[0], pNorm);
        cv[0].drawText(CurrencyFormatter.formatRupiah(rata), col2, y[0], pNorm);
        y[0] += 12;
        cv[0].drawText("Lunas", col1, y[0], pNorm);
        cv[0].drawText(countLunas + " trx  " + CurrencyFormatter.formatRupiah(totalLunas), col2, y[0], pNorm);
        y[0] += 12;
        if (countHutang > 0) {
            cv[0].drawText("Hutang (Belum Lunas)", col1, y[0], pNorm);
            cv[0].drawText(countHutang + " trx", col2, y[0], pNorm);
            y[0] += 12;
        }
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 12;

        // ── BREAKDOWN METODE PEMBAYARAN ──────────────────────────────
        cv[0].drawText("METODE PEMBAYARAN", M, y[0], pSec); y[0] += 13;
        Cursor mc = dbHelper.getMetodeSummaryFiltered(filterFrom, filterTo, filterKasir);
        if (mc != null && mc.moveToFirst()) {
            // header mini-tabel
            cv[0].drawText("Metode", M, y[0], pBold);
            cv[0].drawText("Trx", M + 260, y[0], pBold);
            cv[0].drawText("Total", M + 310, y[0], pBold);
            cv[0].drawText("%", W - M, y[0], pBoldR);
            y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 10;
            do {
                String met = mc.getString(0);
                String metShort = shortenMetode(met);
                int pct = totalPendapatan > 0 ? (int) Math.round(mc.getDouble(2) / totalPendapatan * 100) : 0;
                cv[0].drawText(metShort, M, y[0], pNorm);
                cv[0].drawText(String.valueOf(mc.getInt(1)), M + 260, y[0], pNorm);
                cv[0].drawText(CurrencyFormatter.formatRupiah(mc.getDouble(2)), M + 310, y[0], pNorm);
                cv[0].drawText(pct + "%", W - M, y[0], pBoldR);
                y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 10;
            } while (mc.moveToNext());
            mc.close();
        }
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 12;

        // ── ITEM TERLARIS ────────────────────────────────────────────
        Map<String, Integer> itemMap = new LinkedHashMap<>();
        Pattern pat = Pattern.compile("([^(]+)\\([^)]+\\)x(\\d+)");
        for (Transaksi t : listTransaksi) {
            if (t.getDetailBarang() == null) continue;
            Matcher mat = pat.matcher(t.getDetailBarang());
            while (mat.find()) {
                String nm = mat.group(1).trim();
                itemMap.put(nm, itemMap.getOrDefault(nm, 0) + Integer.parseInt(mat.group(2)));
            }
        }
        if (!itemMap.isEmpty()) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(itemMap.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            sorted = sorted.subList(0, Math.min(5, sorted.size()));
            cv[0].drawText("ITEM TERLARIS", M, y[0], pSec); y[0] += 13;
            int rk = 1;
            for (Map.Entry<String, Integer> e : sorted) {
                cv[0].drawText(rk + ". " + e.getKey(), M, y[0], pNorm);
                cv[0].drawText(e.getValue() + " pcs", W - M, y[0], pBoldR);
                y[0] += 11; rk++;
            }
            y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 12;
        }

        // ── RIWAYAT TRANSAKSI ────────────────────────────────────────
        cv[0].drawText("RIWAYAT TRANSAKSI", M, y[0], pSec); y[0] += 13;

        // Kolom: No | Tanggal | Barang | Metode | Status | Total
        final int cNo  = M;           // 40
        final int cTgl = M + 20;      // 60  — "dd/MM HH:mm"
        final int cBrg = M + 85;      // 125 — nama barang (lebar 190px)
        final int cMet = M + 275;     // 315 — metode pembayaran (lebar 105px)
        final int cStt = M + 380;     // 420 — status lunas/hutang
        final int cTot = W - M;       // 555 — total (right-aligned)

        Runnable drawTableHeader = () -> {
            cv[0].drawText("No",      cNo,  y[0], pBold);
            cv[0].drawText("Tanggal", cTgl, y[0], pBold);
            cv[0].drawText("Barang",  cBrg, y[0], pBold);
            cv[0].drawText("Metode",  cMet, y[0], pBold);
            cv[0].drawText("Status",  cStt, y[0], pBold);
            cv[0].drawText("Total",   cTot, y[0], pBoldR);
            y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 11;
        };
        drawTableHeader.run();

        Pattern ptnItem = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)");
        Paint pBrgPaint = mkPaint(7f, Color.parseColor("#616161"), false, Paint.Align.LEFT);
        final float brgMaxW = cMet - cBrg - 6;   // lebar kolom Barang

        final float metMaxW = cStt - cMet - 6;
        int no = 1;
        for (Transaksi t : listTransaksi) {
            // Parse item list
            java.util.List<String> items = new java.util.ArrayList<>();
            String det = t.getDetailBarang();
            if (det != null && !det.isEmpty()) {
                Matcher mIt = ptnItem.matcher(det);
                while (mIt.find()) {
                    String nama = mIt.group(1).trim().replaceAll("^[,\\s]+", "");
                    String qty  = mIt.group(3).trim();
                    if (nama.isEmpty()) continue;
                    String line = nama + " ×" + qty;
                    // Truncate to fit kolom Barang
                    if (pBrgPaint.measureText(line) > brgMaxW) {
                        while (line.length() > 1 && pBrgPaint.measureText(line + "..") > brgMaxW)
                            line = line.substring(0, line.length() - 1).trim();
                        line = line + "..";
                    }
                    items.add(line);
                }
            }
            if (items.isEmpty()) items.add("-");

            // Total tinggi baris = 1 baris utama + baris item ke-2 dst
            int extraLines = Math.max(0, items.size() - 1);
            int rowH = 12 + extraLines * 9;

            if (y[0] + rowH + 4 > H - M - 20) {
                Paint pFoot2 = mkPaint(7f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
                cv[0].drawText("Halaman " + pgNum[0] + "  —  " + namaToko, W / 2f, H - M + 4, pFoot2);
                newPage.run();
                drawTableHeader.run();
            }

            // Format tanggal
            String rawTgl = t.getTanggal();
            String tglFmt = "-";
            if (rawTgl != null && rawTgl.length() >= 16)
                tglFmt = rawTgl.substring(5, 10).replace("-", "/") + " " + rawTgl.substring(11, 16);

            String met = shortenMetode(t.getMetodePembayaran());
            if (pNorm.measureText(met) > metMaxW) met = met.substring(0, Math.min(met.length(), 14)) + "..";

            boolean hutang = "HUTANG".equals(t.getStatus());

            // Baris utama: semua kolom + item pertama di kolom Barang
            cv[0].drawText(String.valueOf(no++), cNo,  y[0], pGray);
            cv[0].drawText(tglFmt,               cTgl, y[0], pNorm);
            cv[0].drawText(items.get(0),         cBrg, y[0], pBrgPaint);
            cv[0].drawText(met,                  cMet, y[0], pNorm);
            cv[0].drawText(hutang ? "HUTANG" : "LUNAS", cStt, y[0], hutang ? pRed : pGreen);
            cv[0].drawText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()), cTot, y[0], pBoldR);
            y[0] += 9;

            // Item ke-2 dan seterusnya: hanya kolom Barang, sisanya kosong
            for (int i = 1; i < items.size(); i++) {
                cv[0].drawText(items.get(i), cBrg, y[0], pBrgPaint);
                y[0] += 9;
            }

            y[0] += 3; cv[0].drawLine(M, y[0], W - M, y[0], pLine); y[0] += 9;
        }

        // ── TOTAL AKHIR ──────────────────────────────────────────────
        if (y[0] + 32 > H - M) newPage.run();
        y[0] += 4; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 14;
        cv[0].drawText("TOTAL PENDAPATAN", M, y[0], pSec);
        cv[0].drawText(CurrencyFormatter.formatRupiah(totalPendapatan), cTot, y[0], pPinkR);
        y[0] += 14; cv[0].drawLine(M, y[0], W - M, y[0], pDark); y[0] += 12;

        Paint pFoot = mkPaint(7.5f, Color.parseColor("#9E9E9E"), false, Paint.Align.CENTER);
        cv[0].drawText("Rekap Penjualan  •  " + namaToko + "  •  " + now, W / 2f, y[0], pFoot);

        pdf.finishPage(pg[0]);
        return pdf;
    }

    private String buildItemSummary(Pattern ptn, String detail) {
        if (detail == null || detail.isEmpty()) return "";
        Matcher m = ptn.matcher(detail);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            // Strip leading commas/spaces from nama (detail_barang separator)
            String nama = m.group(1).trim().replaceAll("^[,\\s]+", "");
            String qty  = m.group(3).trim();
            if (nama.isEmpty()) continue;
            if (nama.length() > 18) nama = nama.substring(0, 17) + ".";
            if (sb.length() > 0) sb.append("   ");
            sb.append(nama).append(" ×").append(qty);
        }
        return sb.toString();
    }

    private String shortenMetode(String m) {
        if (m == null) return "-";
        String lo = m.toLowerCase();
        if (lo.startsWith("split")) {
            // "Split — Tunai: Rp X + QRIS: Rp Y" → "Split (Tunai+QRIS)"
            return "Split (Tunai+QRIS)";
        }
        if (lo.contains("qris") && lo.contains("(")) {
            // "QRIS (ShopeePay)" → "QRIS - ShopeePay"
            int s = m.indexOf('('), e = m.indexOf(')');
            if (s > 0 && e > s) return "QRIS - " + m.substring(s + 1, e).trim();
        }
        if (lo.contains("debit") || lo.contains("kredit")) {
            // "Debit/Kredit - Visa" → keep as-is (short enough)
            return m.length() > 20 ? m.substring(0, 19) + ".." : m;
        }
        return m;
    }

    private Paint mkPaint(float sz, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(sz); p.setColor(color);
        if (bold) p.setFakeBoldText(true);
        p.setTextAlign(align);
        return p;
    }

    private int dpToPx(int dp) { return Math.round(getResources().getDisplayMetrics().density * dp); }

    private void showDetail(Transaksi t) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View root = getLayoutInflater().inflate(R.layout.layout_detail_riwayat, null);
        sheet.setContentView(root);

        ((android.widget.TextView) root.findViewById(R.id.detailRiwayatTvId))
                .setText("TRX #" + t.getIdTransaksi());
        ((android.widget.TextView) root.findViewById(R.id.detailRiwayatTvTanggal))
                .setText(t.getTanggal() != null ? t.getTanggal() : "-");
        ((android.widget.TextView) root.findViewById(R.id.detailRiwayatTvTotal))
                .setText(com.example.butikkasir.utils.CurrencyFormatter.formatRupiah(t.getTotalBelanja()));
        ((android.widget.TextView) root.findViewById(R.id.detailRiwayatTvMetode))
                .setText(t.getMetodePembayaran() != null ? t.getMetodePembayaran() : "-");
        ((android.widget.TextView) root.findViewById(R.id.detailRiwayatTvKasir))
                .setText(t.getNamaKasir() != null ? t.getNamaKasir() : "-");

        android.widget.TextView tvStatus = root.findViewById(R.id.detailRiwayatTvStatus);
        boolean hutang = "HUTANG".equals(t.getStatus());
        tvStatus.setText(hutang ? "HUTANG" : "LUNAS");
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dpToPx(6));
        badge.setColor(hutang ? Color.parseColor("#FF5722") : Color.parseColor("#4CAF50"));
        tvStatus.setBackground(badge);

        LinearLayout itemContainer = root.findViewById(R.id.detailRiwayatItemContainer);
        itemContainer.removeAllViews();
        String detail = t.getDetailBarang();
        if (detail != null && !detail.isEmpty()) {
            Matcher mat = Pattern.compile("([^(]+)\\(([^)]+)\\)x(\\d+)").matcher(detail);
            boolean any = false;
            while (mat.find()) {
                any = true;
                String nama = mat.group(1).trim().replaceAll("^[,\\s]+", "");
                String ukuran = mat.group(2).trim();
                String qty = mat.group(3).trim();
                android.widget.TextView tv = new android.widget.TextView(this);
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
                android.widget.TextView tv = new android.widget.TextView(this);
                tv.setText(detail);
                tv.setTextColor(Color.parseColor("#757575"));
                tv.setTextSize(13f);
                itemContainer.addView(tv);
            }
        } else {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText("—");
            tv.setTextColor(Color.parseColor("#9E9E9E"));
            tv.setTextSize(13f);
            itemContainer.addView(tv);
        }

        root.findViewById(R.id.detailRiwayatBtnTutup).setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }
}
