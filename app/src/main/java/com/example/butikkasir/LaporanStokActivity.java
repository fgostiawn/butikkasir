package com.example.butikkasir;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class LaporanStokActivity extends AppCompatActivity {

    private LinearLayout stokListLayout;
    private TextView tvTotalSku, tvLowStok;
    private Spinner spinnerKategoriStok;
    private DatabaseHelper dbHelper;
    private String filterKategori = "Semua";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan_stok);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarStok);
        toolbar.setNavigationOnClickListener(v -> finish());

        stokListLayout   = findViewById(R.id.stokListLayout);
        tvTotalSku       = findViewById(R.id.tvTotalSku);
        tvLowStok        = findViewById(R.id.tvLowStok);
        spinnerKategoriStok = findViewById(R.id.spinnerKategoriStok);

        setupKategoriSpinner();
        loadData();
    }

    private void setupKategoriSpinner() {
        List<String> kategoriList = dbHelper.getAllKategoriFromBarang();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kategoriList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKategoriStok.setAdapter(adapter);
        spinnerKategoriStok.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterKategori = kategoriList.get(pos);
                loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void loadData() {
        stokListLayout.removeAllViews();
        int totalSku = 0, lowCount = 0;

        Cursor c = dbHelper.getAllBarangForStok(filterKategori);
        if (c == null || !c.moveToFirst()) {
            addEmptyLabel();
            tvTotalSku.setText("0 SKU");
            tvLowStok.setText("0 item kritis");
            return;
        }

        // Column indices
        int idxNama     = c.getColumnIndex("nama_barang");
        int idxHarga    = c.getColumnIndex("harga");
        int idxStok     = c.getColumnIndex("stok");
        int idxKategori = c.getColumnIndex("kategori");

        do {
            String nama     = c.getString(idxNama);
            double harga    = c.getDouble(idxHarga);
            int    stok     = c.getInt(idxStok);
            String kategori = c.getString(idxKategori);
            totalSku++;
            if (stok <= 5) lowCount++;
            addStokRow(nama, kategori, harga, stok);
        } while (c.moveToNext());
        c.close();

        tvTotalSku.setText(totalSku + " SKU");
        tvLowStok.setText(lowCount + " item kritis");
    }

    private void addStokRow(String nama, String kategori, double harga, int stok) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(12));
        cardBg.setStroke(dp(1), Color.parseColor("#F0F0F0"));
        card.setBackground(cardBg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);

        // Stok indicator dot
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(stokColor(stok));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotP.rightMargin = dp(12);
        dot.setLayoutParams(dotP);

        // Name + category
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvNama = new TextView(this);
        tvNama.setText(nama);
        tvNama.setTextColor(Color.parseColor("#212121"));
        tvNama.setTextSize(13f);
        tvNama.setTypeface(null, Typeface.BOLD);
        tvNama.setMaxLines(1);
        tvNama.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView tvKat = new TextView(this);
        tvKat.setText(kategori + "  •  " + CurrencyFormatter.formatRupiah(harga));
        tvKat.setTextColor(Color.parseColor("#9E9E9E"));
        tvKat.setTextSize(11f);

        info.addView(tvNama);
        info.addView(tvKat);

        // Stok badge
        TextView tvStok = new TextView(this);
        tvStok.setText(stok + "\npcs");
        tvStok.setGravity(Gravity.CENTER);
        tvStok.setTextColor(stok <= 0 ? Color.WHITE : (stok <= 5 ? Color.parseColor("#E65100") : Color.parseColor("#1B5E20")));
        tvStok.setTextSize(13f);
        tvStok.setTypeface(null, Typeface.BOLD);
        tvStok.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(stokBadgeBg(stok));
        badgeBg.setCornerRadius(dp(8));
        tvStok.setBackground(badgeBg);

        card.addView(dot);
        card.addView(info);
        card.addView(tvStok);
        stokListLayout.addView(card);
    }

    private int stokColor(int stok) {
        if (stok <= 0)  return Color.parseColor("#F44336");
        if (stok <= 5)  return Color.parseColor("#FF9800");
        if (stok <= 20) return Color.parseColor("#FFC107");
        return Color.parseColor("#4CAF50");
    }

    private int stokBadgeBg(int stok) {
        if (stok <= 0)  return Color.parseColor("#FFEBEE");
        if (stok <= 5)  return Color.parseColor("#FFF3E0");
        if (stok <= 20) return Color.parseColor("#FFFDE7");
        return Color.parseColor("#E8F5E9");
    }

    private void addEmptyLabel() {
        TextView tv = new TextView(this);
        tv.setText("Belum ada data barang");
        tv.setTextColor(Color.parseColor("#BDBDBD"));
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(40), 0, dp(40));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        stokListLayout.addView(tv);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
