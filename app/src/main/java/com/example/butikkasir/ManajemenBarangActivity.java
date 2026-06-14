package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ManajemenBarangActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<BarangItem> listBarang = new ArrayList<>();
    private BarangAdminAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_barang);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarBarang);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvBarangAdmin);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BarangAdminAdapter(listBarang);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabTambahBarang);
        fab.setOnClickListener(v -> showFormDialog(null));

        loadData();
    }

    private void loadData() {
        listBarang.clear();
        Cursor c = dbHelper.getAllBarang();
        if (c != null && c.moveToFirst()) {
            do {
                String kategori = c.getString(c.getColumnIndexOrThrow("kategori"));
                listBarang.add(new BarangItem(
                        c.getInt(c.getColumnIndexOrThrow("id_barang")),
                        c.getString(c.getColumnIndexOrThrow("nama_barang")),
                        c.getDouble(c.getColumnIndexOrThrow("harga")),
                        c.getInt(c.getColumnIndexOrThrow("stok")),
                        c.getString(c.getColumnIndexOrThrow("detail_barang")),
                        c.getString(c.getColumnIndexOrThrow("ukuran")),
                        kategori != null ? kategori : "Lainnya"
                ));
            } while (c.moveToNext());
            c.close();
        }
        adapter.notifyDataSetChanged();

        View emptyView = findViewById(R.id.tvEmptyBarang);
        emptyView.setVisibility(listBarang.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static final String[] DAFTAR_KATEGORI = {"Atasan", "Bawahan", "Dress", "Outer", "Aksesoris", "Lainnya"};

    private void showFormDialog(BarangItem existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_form_barang, null);
        EditText etNama              = view.findViewById(R.id.etNamaBarangForm);
        EditText etHarga             = view.findViewById(R.id.etHargaBarangForm);
        EditText etStok              = view.findViewById(R.id.etStokBarangForm);
        EditText etDetail            = view.findViewById(R.id.etDetailBarangForm);
        EditText etUkuran            = view.findViewById(R.id.etUkuranBarangForm);
        AutoCompleteTextView actvKat = view.findViewById(R.id.actvKategoriForm);

        ArrayAdapter<String> katAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DAFTAR_KATEGORI);
        actvKat.setAdapter(katAdapter);

        if (existing != null) {
            etNama.setText(existing.nama);
            etHarga.setText(String.valueOf((long) existing.harga));
            etStok.setText(String.valueOf(existing.stok));
            etDetail.setText(existing.detail);
            etUkuran.setText(existing.ukuran);
            actvKat.setText(existing.kategori, false);
        } else {
            actvKat.setText("Lainnya", false);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Tambah Barang" : "Edit Barang")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nama     = etNama.getText().toString().trim();
            String hargaStr = etHarga.getText().toString().trim();
            String stokStr  = etStok.getText().toString().trim();
            String detail   = etDetail.getText().toString().trim();
            String ukuran   = etUkuran.getText().toString().trim();
            String kategori = actvKat.getText().toString().trim();

            if (TextUtils.isEmpty(nama) || TextUtils.isEmpty(hargaStr) || TextUtils.isEmpty(stokStr)) {
                Toast.makeText(this, "Nama, harga, dan stok wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(ukuran)) ukuran = "S,M,L,XL";
            if (TextUtils.isEmpty(kategori)) kategori = "Lainnya";

            double harga = Double.parseDouble(hargaStr);
            int stok = Integer.parseInt(stokStr);
            boolean ok;

            if (existing == null) {
                ok = dbHelper.insertBarang(nama, harga, stok, detail, ukuran, kategori);
                if (ok) Toast.makeText(this, "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show();
            } else {
                ok = dbHelper.updateBarang(existing.id, nama, harga, stok, detail, ukuran, kategori);
                if (ok) Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show();
            }

            if (ok) {
                loadData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private void showDeleteDialog(BarangItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Barang")
                .setMessage("Hapus barang \"" + item.nama + "\"?")
                .setPositiveButton("Hapus", (d, w) -> {
                    if (dbHelper.deleteBarang(item.id)) {
                        Toast.makeText(this, "Barang dihapus", Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ── Inner model ──────────────────────────────────────────────────

    static class BarangItem {
        int id;
        String nama, detail, ukuran, kategori;
        double harga;
        int stok;

        BarangItem(int id, String nama, double harga, int stok, String detail, String ukuran, String kategori) {
            this.id       = id;
            this.nama     = nama;
            this.harga    = harga;
            this.stok     = stok;
            this.detail   = detail != null ? detail : "";
            this.ukuran   = ukuran != null ? ukuran : "S,M,L,XL";
            this.kategori = kategori != null ? kategori : "Lainnya";
        }
    }

    // ── Inner adapter ────────────────────────────────────────────────

    class BarangAdminAdapter extends RecyclerView.Adapter<BarangAdminAdapter.VH> {
        private final List<BarangItem> list;

        BarangAdminAdapter(List<BarangItem> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_barang_admin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BarangItem b = list.get(pos);
            h.tvNama.setText(b.nama);
            h.tvHarga.setText(CurrencyFormatter.formatRupiah(b.harga));
            h.tvStok.setText("Stok: " + b.stok);
            h.btnEdit.setOnClickListener(v -> showFormDialog(b));
            h.btnHapus.setOnClickListener(v -> showDeleteDialog(b));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvNama, tvHarga, tvStok;
            MaterialButton btnEdit, btnHapus;

            VH(@NonNull View v) {
                super(v);
                tvNama  = v.findViewById(R.id.tvNamaBarangAdmin);
                tvHarga = v.findViewById(R.id.tvHargaBarangAdmin);
                tvStok  = v.findViewById(R.id.tvStokBarangAdmin);
                btnEdit = v.findViewById(R.id.btnEditBarang);
                btnHapus = v.findViewById(R.id.btnHapusBarang);
            }
        }
    }
}
