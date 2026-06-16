package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.database.DatabaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ManajemenPelangganActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<PelangganItem> listAll = new ArrayList<>();
    private final List<PelangganItem> listFilter = new ArrayList<>();
    private PelangganAdminAdapter adapter;
    private TextView tvEmpty;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_pelanggan);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarManajemenPelanggan);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmptyPelangganAdmin);
        rv = findViewById(R.id.rvPelangganAdmin);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PelangganAdminAdapter(listFilter);
        rv.setAdapter(adapter);

        TextInputEditText etCari = findViewById(R.id.etCariPelangganAdmin);
        etCari.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterData(s.toString().trim());
            }
        });

        FloatingActionButton fab = findViewById(R.id.fabTambahPelangganAdmin);
        fab.setOnClickListener(v -> showFormDialog(null));

        loadData();
    }

    private void loadData() {
        listAll.clear();
        Cursor c = dbHelper.getAllPelanggan();
        if (c != null && c.moveToFirst()) {
            do {
                listAll.add(new PelangganItem(
                        c.getInt(c.getColumnIndexOrThrow("id_pelanggan")),
                        c.getString(c.getColumnIndexOrThrow("nama")),
                        c.getString(c.getColumnIndexOrThrow("no_hp")),
                        c.getInt(c.getColumnIndexOrThrow("poin"))
                ));
            } while (c.moveToNext());
            c.close();
        }
        filterData("");
    }

    private void filterData(String query) {
        listFilter.clear();
        String q = query.toLowerCase();
        for (PelangganItem p : listAll) {
            if (q.isEmpty()
                    || p.nama.toLowerCase().contains(q)
                    || p.noHp.contains(q)) {
                listFilter.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(listFilter.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(listFilter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showFormDialog(PelangganItem existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_form_pelanggan, null);
        EditText etNama = view.findViewById(R.id.etNamaPelangganForm);
        EditText etHp   = view.findViewById(R.id.etNoHpPelangganForm);

        if (existing != null) {
            etNama.setText(existing.nama);
            etHp.setText(existing.noHp);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Tambah Pelanggan" : "Edit Pelanggan")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nama = etNama.getText().toString().trim();
            String hp   = etHp.getText().toString().trim();

            if (TextUtils.isEmpty(nama)) {
                Toast.makeText(this, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok;
            if (existing == null) {
                ok = dbHelper.insertPelanggan(nama, hp);
                if (ok) Toast.makeText(this, nama + " berhasil ditambah", Toast.LENGTH_SHORT).show();
            } else {
                ok = dbHelper.updatePelanggan(existing.id, nama, hp);
                if (ok) Toast.makeText(this, "Data pelanggan diperbarui", Toast.LENGTH_SHORT).show();
            }

            if (ok) {
                loadData();
                dialog.dismiss();
            }
        }));

        dialog.show();
    }

    private void showDeleteDialog(PelangganItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Pelanggan")
                .setMessage("Hapus pelanggan \"" + item.nama + "\"?\nPoin sebanyak " + item.poin + " akan ikut terhapus.")
                .setPositiveButton("Hapus", (d, w) -> {
                    if (dbHelper.deletePelanggan(item.id)) {
                        Toast.makeText(this, item.nama + " dihapus", Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ── Inner model ──────────────────────────────────────────────────

    static class PelangganItem {
        int id, poin;
        String nama, noHp;

        PelangganItem(int id, String nama, String noHp, int poin) {
            this.id    = id;
            this.nama  = nama;
            this.noHp  = noHp != null ? noHp : "";
            this.poin  = poin;
        }
    }

    // ── Inner adapter ────────────────────────────────────────────────

    class PelangganAdminAdapter extends RecyclerView.Adapter<PelangganAdminAdapter.VH> {
        private final List<PelangganItem> list;

        PelangganAdminAdapter(List<PelangganItem> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pelanggan_admin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            PelangganItem p = list.get(pos);
            String inisial = p.nama.isEmpty() ? "P" : p.nama.substring(0, 1).toUpperCase();
            h.tvInisial.setText(inisial);
            h.tvNama.setText(p.nama);
            h.tvNoHp.setText(p.noHp.isEmpty() ? "Tidak ada no. HP" : p.noHp);
            h.tvPoin.setText(p.poin + " poin");
            h.btnEdit.setOnClickListener(v -> showFormDialog(p));
            h.btnHapus.setOnClickListener(v -> showDeleteDialog(p));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvInisial, tvNama, tvNoHp, tvPoin;
            MaterialButton btnEdit, btnHapus;

            VH(@NonNull View v) {
                super(v);
                tvInisial = v.findViewById(R.id.tvInisialPelangganAdmin);
                tvNama    = v.findViewById(R.id.tvNamaPelangganAdmin);
                tvNoHp    = v.findViewById(R.id.tvNoHpPelangganAdmin);
                tvPoin    = v.findViewById(R.id.tvPoinPelangganAdmin);
                btnEdit   = v.findViewById(R.id.btnEditPelanggan);
                btnHapus  = v.findViewById(R.id.btnHapusPelanggan);
            }
        }
    }
}
