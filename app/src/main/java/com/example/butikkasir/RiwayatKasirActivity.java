package com.example.butikkasir;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiwayatKasirActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvRiwayat;
    private TextView tvTotalHariIni, tvJumlahTransaksi, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_kasir);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarRiwayat);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTotalHariIni   = findViewById(R.id.tvRiwayatTotal);
        tvJumlahTransaksi = findViewById(R.id.tvRiwayatJumlah);
        tvEmpty          = findViewById(R.id.tvRiwayatEmpty);
        rvRiwayat        = findViewById(R.id.rvRiwayat);
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));

        loadData();
    }

    private void loadData() {
        SharedPreferences prefs = getSharedPreferences("ButikSession", MODE_PRIVATE);
        String kasirName = prefs.getString("namaKasir", "Kasir");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String todayDisplay = new SimpleDateFormat("dd MMMM yyyy", new Locale("id","ID")).format(new Date());
        toolbar().setSubtitle(kasirName + " — " + todayDisplay);

        Cursor c = dbHelper.getTodayTransaksiByKasir(kasirName);
        List<RiwayatItem> list = new ArrayList<>();
        double total = 0;

        if (c != null && c.moveToFirst()) {
            do {
                long id     = c.getLong(c.getColumnIndexOrThrow("id_transaksi"));
                String tgl  = c.getString(c.getColumnIndexOrThrow("tanggal"));
                double amt  = c.getDouble(c.getColumnIndexOrThrow("total_belanja"));
                String met  = c.getString(c.getColumnIndexOrThrow("metode_pembayaran"));
                String stat = "";
                int colStat = c.getColumnIndex("status_transaksi");
                if (colStat >= 0) stat = c.getString(colStat);
                if (stat == null) stat = "LUNAS";
                String detail = "";
                int colDetail = c.getColumnIndex("detail_barang");
                if (colDetail >= 0) detail = c.getString(colDetail);
                String kasir = "";
                int colKasir = c.getColumnIndex("nama_kasir");
                if (colKasir >= 0) kasir = c.getString(colKasir);
                list.add(new RiwayatItem(id, tgl, amt, met, stat, detail, kasir));
                total += amt;
            } while (c.moveToNext());
            c.close();
        }

        tvTotalHariIni.setText(CurrencyFormatter.formatRupiah(total));
        tvJumlahTransaksi.setText(list.size() + " transaksi");
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvRiwayat.setAdapter(new RiwayatAdapter(list));
    }

    private MaterialToolbar toolbar() {
        return findViewById(R.id.toolbarRiwayat);
    }

    private void showDetail(RiwayatItem item) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_detail_riwayat, null);
        sheet.setContentView(root);

        // Format TRX ID dari tanggal transaksi
        String dateTag = "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.tanggal);
            if (d == null) d = new Date();
            dateTag = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(d);
        } catch (Exception e) {
            dateTag = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        }
        ((TextView) root.findViewById(R.id.detailRiwayatTvId))
                .setText(String.format("TRX-%s-%04d", dateTag, item.id));
        ((TextView) root.findViewById(R.id.detailRiwayatTvTanggal)).setText(item.tanggal);
        ((TextView) root.findViewById(R.id.detailRiwayatTvTotal))
                .setText(CurrencyFormatter.formatRupiah(item.total));
        ((TextView) root.findViewById(R.id.detailRiwayatTvMetode)).setText(item.metode);
        ((TextView) root.findViewById(R.id.detailRiwayatTvKasir)).setText(item.kasir);

        // Status badge dengan warna
        TextView tvStatus = root.findViewById(R.id.detailRiwayatTvStatus);
        boolean hutang = "HUTANG".equals(item.status);
        tvStatus.setText(hutang ? "HUTANG" : "LUNAS");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(20));
        bg.setColor(hutang ? Color.parseColor("#F44336") : Color.parseColor("#388E3C"));
        tvStatus.setBackground(bg);

        // Parse detail barang: format "Nama(ukuran)xQty, "
        LinearLayout container = root.findViewById(R.id.detailRiwayatItemContainer);
        container.removeAllViews();
        if (!item.detail.isEmpty()) {
            String[] entries = item.detail.split(",\\s*");
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dpToPx(6);
                row.setLayoutParams(lp);

                // Parse "NamaBarang(ukuran)xQty"
                String nama = entry;
                String info = "";
                try {
                    int paren = entry.indexOf('(');
                    int parenEnd = entry.indexOf(')');
                    int xIdx = entry.indexOf('x', parenEnd);
                    if (paren > 0 && parenEnd > paren && xIdx > parenEnd) {
                        nama = entry.substring(0, paren).trim();
                        String ukuran = entry.substring(paren + 1, parenEnd);
                        String qty = entry.substring(xIdx + 1).trim();
                        info = "Size " + ukuran + "  ×  " + qty + " pcs";
                    }
                } catch (Exception ignored) {}

                TextView tvNama = new TextView(this);
                tvNama.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvNama.setText(nama);
                tvNama.setTextColor(Color.parseColor("#212121"));
                tvNama.setTextSize(13f);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(info);
                tvInfo.setTextColor(Color.parseColor("#757575"));
                tvInfo.setTextSize(12f);

                row.addView(tvNama);
                row.addView(tvInfo);
                container.addView(row);
            }
        } else {
            TextView tvKosong = new TextView(this);
            tvKosong.setText("Detail barang tidak tersedia");
            tvKosong.setTextColor(Color.parseColor("#9E9E9E"));
            tvKosong.setTextSize(12f);
            container.addView(tvKosong);
        }

        ((MaterialButton) root.findViewById(R.id.detailRiwayatBtnTutup))
                .setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    // ──────────────────────────────────────────────────────────────

    static class RiwayatItem {
        long id;
        String tanggal, metode, status, detail, kasir;
        double total;
        RiwayatItem(long id, String tanggal, double total, String metode,
                    String status, String detail, String kasir) {
            this.id = id; this.tanggal = tanggal; this.total = total;
            this.metode = metode; this.status = status;
            this.detail = detail != null ? detail : "";
            this.kasir = kasir != null ? kasir : "Kasir";
        }
    }

    class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.VH> {
        private final List<RiwayatItem> data;
        RiwayatAdapter(List<RiwayatItem> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_riwayat_kasir, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            RiwayatItem item = data.get(position);
            String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            // Coba ambil tanggal dari item.tanggal untuk format TRX ID
            h.tvId.setText(String.format("TRX-%s-%04d", dateStr, item.id));
            h.tvWaktu.setText(item.tanggal);
            h.tvTotal.setText(CurrencyFormatter.formatRupiah(item.total));
            h.tvMetode.setText(item.metode);
            boolean hutang = "HUTANG".equals(item.status);
            h.tvStatus.setText(hutang ? "HUTANG" : "LUNAS");
            h.tvStatus.setTextColor(hutang ? 0xFFF44336 : 0xFF388E3C);
            h.itemView.setOnClickListener(v -> showDetail(item));
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvId, tvWaktu, tvTotal, tvMetode, tvStatus;
            VH(View v) {
                super(v);
                tvId     = v.findViewById(R.id.riwayatTvId);
                tvWaktu  = v.findViewById(R.id.riwayatTvWaktu);
                tvTotal  = v.findViewById(R.id.riwayatTvTotal);
                tvMetode = v.findViewById(R.id.riwayatTvMetode);
                tvStatus = v.findViewById(R.id.riwayatTvStatus);
            }
        }
    }
}
