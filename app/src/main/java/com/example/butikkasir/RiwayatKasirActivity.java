package com.example.butikkasir;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;

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
                list.add(new RiwayatItem(id, tgl, amt, met, stat));
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

    // ──────────────────────────────────────────────────────────────

    static class RiwayatItem {
        long id;
        String tanggal, metode, status;
        double total;
        RiwayatItem(long id, String tanggal, double total, String metode, String status) {
            this.id = id; this.tanggal = tanggal; this.total = total;
            this.metode = metode; this.status = status;
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
            h.tvId.setText("TRX #" + item.id);
            h.tvWaktu.setText(item.tanggal);
            h.tvTotal.setText(CurrencyFormatter.formatRupiah(item.total));
            h.tvMetode.setText(item.metode);
            boolean hutang = "HUTANG".equals(item.status);
            h.tvStatus.setText(hutang ? "HUTANG" : "LUNAS");
            h.tvStatus.setTextColor(hutang ? 0xFFF44336 : 0xFF388E3C);
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
