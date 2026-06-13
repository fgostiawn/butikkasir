package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.List;

public class ManajemenKasirActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<KasirItem> listKasir = new ArrayList<>();
    private KasirAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_kasir);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarKasir);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvKasir);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new KasirAdapter(listKasir);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabTambahKasir);
        fab.setOnClickListener(v -> showFormDialog(null));

        loadData();
    }

    private void loadData() {
        listKasir.clear();
        Cursor c = dbHelper.getAllKasir();
        if (c != null && c.moveToFirst()) {
            do {
                listKasir.add(new KasirItem(
                        c.getInt(c.getColumnIndexOrThrow("id_kasir")),
                        c.getString(c.getColumnIndexOrThrow("username")),
                        c.getString(c.getColumnIndexOrThrow("password")),
                        c.getString(c.getColumnIndexOrThrow("nama_kasir"))
                ));
            } while (c.moveToNext());
            c.close();
        }
        adapter.notifyDataSetChanged();

        View emptyView = findViewById(R.id.tvEmptyKasir);
        emptyView.setVisibility(listKasir.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showFormDialog(KasirItem existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_form_kasir, null);
        EditText etNama     = view.findViewById(R.id.etNamaKasirForm);
        EditText etUsername = view.findViewById(R.id.etUsernameKasirForm);
        EditText etPassword = view.findViewById(R.id.etPasswordKasirForm);

        if (existing != null) {
            etNama.setText(existing.namaKasir);
            etUsername.setText(existing.username);
            etPassword.setText(existing.password);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Tambah Kasir" : "Edit Kasir")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nama     = etNama.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(nama) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 4) {
                Toast.makeText(this, "Password minimal 4 karakter", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok;
            if (existing == null) {
                ok = dbHelper.insertKasir(username, password, nama);
                if (ok) Toast.makeText(this, "Kasir berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, "Username sudah digunakan", Toast.LENGTH_SHORT).show();
            } else {
                ok = dbHelper.updateKasir(existing.id, username, password, nama);
                if (ok) Toast.makeText(this, "Data kasir diperbarui", Toast.LENGTH_SHORT).show();
            }

            if (ok) {
                loadData();
                dialog.dismiss();
            }
        }));

        dialog.show();
    }

    private void showDeleteDialog(KasirItem item) {
        if (listKasir.size() <= 1) {
            new AlertDialog.Builder(this)
                    .setTitle("Tidak Dapat Dihapus")
                    .setMessage("Harus ada minimal satu akun kasir.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hapus Kasir")
                .setMessage("Hapus akun kasir \"" + item.namaKasir + "\" (" + item.username + ")?")
                .setPositiveButton("Hapus", (d, w) -> {
                    if (dbHelper.deleteKasir(item.id)) {
                        Toast.makeText(this, "Akun kasir dihapus", Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ── Inner model ──────────────────────────────────────────────────

    static class KasirItem {
        int id;
        String username, password, namaKasir;

        KasirItem(int id, String username, String password, String namaKasir) {
            this.id        = id;
            this.username  = username;
            this.password  = password;
            this.namaKasir = namaKasir;
        }
    }

    // ── Inner adapter ────────────────────────────────────────────────

    class KasirAdapter extends RecyclerView.Adapter<KasirAdapter.VH> {
        private final List<KasirItem> list;

        KasirAdapter(List<KasirItem> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kasir, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            KasirItem k = list.get(pos);
            h.tvNama.setText(k.namaKasir);
            h.tvUsername.setText("@" + k.username);
            h.btnEdit.setOnClickListener(v -> showFormDialog(k));
            h.btnHapus.setOnClickListener(v -> showDeleteDialog(k));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvNama, tvUsername;
            MaterialButton btnEdit, btnHapus;

            VH(@NonNull View v) {
                super(v);
                tvNama     = v.findViewById(R.id.tvNamaKasir);
                tvUsername = v.findViewById(R.id.tvUsernameKasir);
                btnEdit    = v.findViewById(R.id.btnEditKasir);
                btnHapus   = v.findViewById(R.id.btnHapusKasir);
            }
        }
    }
}
