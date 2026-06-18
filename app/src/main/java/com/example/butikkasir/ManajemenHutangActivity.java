package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ManajemenHutangActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rv;
    private TextView tvEmpty, tvTotalHutang;
    private HutangAdapter adapter;
    private final List<HutangItem> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_hutang);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHutang);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty       = findViewById(R.id.tvHutangEmpty);
        tvTotalHutang = findViewById(R.id.tvTotalHutang);
        rv = findViewById(R.id.rvHutang);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HutangAdapter();
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        list.clear();
        Cursor c = dbHelper.getHutangList();
        double total = 0;
        if (c != null && c.moveToFirst()) {
            do {
                int id          = c.getInt(c.getColumnIndexOrThrow("id_transaksi"));
                String tanggal  = c.getString(c.getColumnIndexOrThrow("tanggal"));
                double amt      = c.getDouble(c.getColumnIndexOrThrow("total_belanja"));
                String metode   = c.getString(c.getColumnIndexOrThrow("metode_pembayaran"));
                String kasir    = c.getString(c.getColumnIndexOrThrow("nama_kasir"));
                String namaPelanggan = c.getString(c.getColumnIndexOrThrow("nama_pelanggan"));
                list.add(new HutangItem(id, tanggal, amt, metode, kasir, namaPelanggan));
                total += amt;
            } while (c.moveToNext());
            c.close();
        }
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        tvTotalHutang.setText("Total Hutang: " + CurrencyFormatter.formatRupiah(total));
        adapter.notifyDataSetChanged();
    }

    // ──────────────────────────────────────────────────────────────

    static class HutangItem {
        int id;
        String tanggal, metode, kasir, namaPelanggan;
        double total;
        HutangItem(int id, String tanggal, double total, String metode,
                   String kasir, String namaPelanggan) {
            this.id = id; this.tanggal = tanggal; this.total = total;
            this.metode = metode; this.kasir = kasir;
            this.namaPelanggan = namaPelanggan != null ? namaPelanggan : "Tamu";
        }
    }

    class HutangAdapter extends RecyclerView.Adapter<HutangAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hutang, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            HutangItem item = list.get(position);
            h.tvPelanggan.setText(item.namaPelanggan);
            h.tvTotal.setText(CurrencyFormatter.formatRupiah(item.total));
            h.tvTanggal.setText(item.tanggal);
            h.tvKasir.setText("Kasir: " + item.kasir);

            h.btnLunasi.setOnClickListener(v ->
                new AlertDialog.Builder(ManajemenHutangActivity.this)
                    .setTitle("Tandai Lunas")
                    .setMessage("Hutang " + item.namaPelanggan + " sebesar "
                            + CurrencyFormatter.formatRupiah(item.total) + " sudah dilunasi?")
                    .setPositiveButton("Ya, Lunas", (d, w) -> {
                        boolean ok = dbHelper.lunasiHutang(item.id);
                        if (ok) {
                            Toast.makeText(ManajemenHutangActivity.this,
                                    "Hutang ditandai lunas", Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                    })
                    .setNegativeButton("Batal", null)
                    .show()
            );
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvPelanggan, tvTotal, tvTanggal, tvKasir;
            MaterialButton btnLunasi;
            VH(View v) {
                super(v);
                tvPelanggan = v.findViewById(R.id.hutangTvPelanggan);
                tvTotal     = v.findViewById(R.id.hutangTvTotal);
                tvTanggal   = v.findViewById(R.id.hutangTvTanggal);
                tvKasir     = v.findViewById(R.id.hutangTvKasir);
                btnLunasi   = v.findViewById(R.id.hutangBtnLunasi);
            }
        }
    }
}
