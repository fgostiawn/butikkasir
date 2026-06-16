package com.example.butikkasir;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.adapter.PelangganAdapter;
import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Pelanggan;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class PelangganActivity extends AppCompatActivity {

    private final List<Pelanggan> pelangganList = new ArrayList<>();
    private final List<Pelanggan> pelangganListFull = new ArrayList<>();
    private PelangganAdapter adapter;
    private DatabaseHelper dbHelper;
    private TextView tvEmpty;
    private RecyclerView rvPelanggan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pelanggan);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPelanggan);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmptyPelanggan);
        rvPelanggan = findViewById(R.id.rvPelanggan);
        TextInputEditText etCari = findViewById(R.id.etCariPelanggan);
        FloatingActionButton fab = findViewById(R.id.fabTambahPelanggan);

        adapter = new PelangganAdapter(pelangganList, pelanggan -> {
            Intent result = new Intent();
            result.putExtra("PELANGGAN", pelanggan);
            setResult(RESULT_OK, result);
            finish();
        });
        rvPelanggan.setLayoutManager(new LinearLayoutManager(this));
        rvPelanggan.setAdapter(adapter);

        etCari.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterPelanggan(s.toString().trim());
            }
        });

        fab.setOnClickListener(v -> showDialogTambah());

        loadPelanggan();
    }

    private void loadPelanggan() {
        pelangganListFull.clear();
        Cursor c = dbHelper.getAllPelanggan();
        if (c != null && c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndexOrThrow("id_pelanggan"));
                String nama = c.getString(c.getColumnIndexOrThrow("nama"));
                String noHp = c.getString(c.getColumnIndexOrThrow("no_hp"));
                int poin = c.getInt(c.getColumnIndexOrThrow("poin"));
                if (noHp == null) noHp = "";
                pelangganListFull.add(new Pelanggan(id, nama, noHp, poin));
            } while (c.moveToNext());
            c.close();
        }
        filterPelanggan("");
    }

    private void filterPelanggan(String query) {
        pelangganList.clear();
        String q = query.toLowerCase();
        for (Pelanggan p : pelangganListFull) {
            if (q.isEmpty()
                    || p.getNama().toLowerCase().contains(q)
                    || p.getNoHp().contains(q)) {
                pelangganList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(pelangganList.isEmpty() ? View.VISIBLE : View.GONE);
        rvPelanggan.setVisibility(pelangganList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showDialogTambah() {
        View view = getLayoutInflater().inflate(R.layout.dialog_form_pelanggan, null);
        TextInputEditText etNama = view.findViewById(R.id.etNamaPelangganForm);
        TextInputEditText etHp = view.findViewById(R.id.etNoHpPelangganForm);

        new AlertDialog.Builder(this)
                .setTitle("Tambah Pelanggan")
                .setView(view)
                .setPositiveButton("Simpan", (d, w) -> {
                    String nama = etNama.getText() != null ? etNama.getText().toString().trim() : "";
                    String hp = etHp.getText() != null ? etHp.getText().toString().trim() : "";
                    if (nama.isEmpty()) {
                        Toast.makeText(this, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.insertPelanggan(nama, hp);
                    loadPelanggan();
                    Toast.makeText(this, nama + " berhasil ditambah", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
