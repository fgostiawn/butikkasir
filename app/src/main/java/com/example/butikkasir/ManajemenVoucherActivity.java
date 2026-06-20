package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ManajemenVoucherActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rv;
    private TextView tvEmpty;
    private VoucherAdapter adapter;
    private final List<VoucherItem> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_voucher);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarVoucher);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvVoucherEmpty);
        rv = findViewById(R.id.rvVoucher);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VoucherAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabTambahVoucher);
        fab.setOnClickListener(v -> showDialogTambah());

        loadData();
    }

    private void loadData() {
        list.clear();
        Cursor c = dbHelper.getAllVoucher();
        if (c != null && c.moveToFirst()) {
            do {
                int id          = c.getInt(c.getColumnIndexOrThrow("id_voucher"));
                String kode     = c.getString(c.getColumnIndexOrThrow("kode"));
                String jenis    = c.getString(c.getColumnIndexOrThrow("jenis"));
                double nilai    = c.getDouble(c.getColumnIndexOrThrow("nilai"));
                double minBeli  = c.getDouble(c.getColumnIndexOrThrow("min_belanja"));
                int maxPakai    = c.getInt(c.getColumnIndexOrThrow("max_penggunaan"));
                int sudahPakai  = c.getInt(c.getColumnIndexOrThrow("sudah_dipakai"));
                int aktif       = c.getInt(c.getColumnIndexOrThrow("aktif"));
                list.add(new VoucherItem(id, kode, jenis, nilai, minBeli, maxPakai, sudahPakai, aktif));
            } while (c.moveToNext());
            c.close();
        }
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void showDialogTambah() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_tambah_voucher, null);
        sheet.setContentView(root);

        ChipGroup cgJenis   = root.findViewById(R.id.chipGroupJenis);
        ChipGroup cgPersen  = root.findViewById(R.id.chipGroupPersen);
        ChipGroup cgNominal = root.findViewById(R.id.chipGroupNominal);
        ChipGroup cgMin     = root.findViewById(R.id.chipGroupMin);
        ChipGroup cgMaks    = root.findViewById(R.id.chipGroupMaks);
        View layoutPersen   = root.findViewById(R.id.layoutPresetPersen);
        View layoutNominal  = root.findViewById(R.id.layoutPresetNominal);
        TextInputEditText etNilai = root.findViewById(R.id.etNilai);
        TextInputEditText etKode  = root.findViewById(R.id.etKode);
        MaterialButton btnAuto    = root.findViewById(R.id.btnAutoKode);
        MaterialButton btnSimpan  = root.findViewById(R.id.btnSimpanVoucher);

        // helper: apakah jenis persen?
        Runnable[] updateJenisUI = {null};
        updateJenisUI[0] = () -> {
            boolean isPersen = ((Chip) root.findViewById(R.id.chipPersen)).isChecked();
            layoutPersen.setVisibility(isPersen ? View.VISIBLE : View.GONE);
            layoutNominal.setVisibility(isPersen ? View.GONE : View.VISIBLE);
            etNilai.setText("");
            cgPersen.clearCheck();
            cgNominal.clearCheck();
            autoUpdateKode(root, etKode);
        };

        cgJenis.setOnCheckedStateChangeListener((group, ids) -> updateJenisUI[0].run());

        // preset persen → isi etNilai + auto kode
        cgPersen.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip chip = root.findViewById(ids.get(0));
            if (chip == null) return;
            String label = chip.getText().toString().replace("%", "").trim();
            etNilai.setText(label);
            autoUpdateKode(root, etKode);
        });

        // preset nominal → isi etNilai + auto kode
        cgNominal.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip chip = root.findViewById(ids.get(0));
            if (chip == null) return;
            String label = chip.getText().toString()
                    .replace("Rp ", "").replace(".", "").replace(",", "").trim();
            etNilai.setText(label);
            autoUpdateKode(root, etKode);
        });

        // manual nilai → auto kode
        etNilai.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                autoUpdateKode(root, etKode);
            }
        });

        // tombol auto-generate kode
        btnAuto.setOnClickListener(v -> autoUpdateKode(root, etKode));

        btnSimpan.setOnClickListener(v -> {
            String kode     = etKode.getText() != null ? etKode.getText().toString().trim().toUpperCase() : "";
            String nilaiStr = etNilai.getText() != null ? etNilai.getText().toString().trim() : "";

            if (kode.isEmpty()) {
                Toast.makeText(this, "Kode voucher wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nilaiStr.isEmpty()) {
                Toast.makeText(this, "Pilih atau masukkan besar diskon", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isPersen = ((Chip) root.findViewById(R.id.chipPersen)).isChecked();
            String jenis = isPersen ? "persen" : "nominal";
            double nilai;
            try { nilai = Double.parseDouble(nilaiStr); }
            catch (NumberFormatException e) {
                Toast.makeText(this, "Nilai tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isPersen && (nilai <= 0 || nilai > 100)) {
                Toast.makeText(this, "Persen harus antara 1–100", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPersen && nilai <= 0) {
                Toast.makeText(this, "Nominal harus lebih dari 0", Toast.LENGTH_SHORT).show();
                return;
            }

            double min = resolveMinBelanja(root);
            int maxPakai = resolveMaksPakai(root);

            boolean ok = dbHelper.insertVoucher(kode, jenis, nilai, min, maxPakai);
            if (ok) {
                Toast.makeText(this, "Voucher " + kode + " ditambahkan!", Toast.LENGTH_SHORT).show();
                sheet.dismiss();
                loadData();
            } else {
                Toast.makeText(this, "Kode " + kode + " sudah dipakai, ganti kode lain", Toast.LENGTH_SHORT).show();
            }
        });

        sheet.show();
    }

    private void autoUpdateKode(View root, TextInputEditText etKode) {
        boolean isPersen = ((Chip) root.findViewById(R.id.chipPersen)).isChecked();
        TextInputEditText etNilai = root.findViewById(R.id.etNilai);
        String nilaiStr = etNilai.getText() != null ? etNilai.getText().toString().trim() : "";
        if (nilaiStr.isEmpty()) return;
        try {
            double nilai = Double.parseDouble(nilaiStr);
            String kode;
            if (isPersen) {
                kode = "DISKON" + (int) nilai;
            } else {
                long rbu = (long) nilai / 1000;
                kode = "HEMAT" + rbu + "K";
            }
            etKode.setText(kode);
        } catch (NumberFormatException ignored) {}
    }

    private double resolveMinBelanja(View root) {
        int checkedId = ((ChipGroup) root.findViewById(R.id.chipGroupMin)).getCheckedChipId();
        if (checkedId == R.id.chipMin50)  return 50_000;
        if (checkedId == R.id.chipMin100) return 100_000;
        if (checkedId == R.id.chipMin200) return 200_000;
        if (checkedId == R.id.chipMin500) return 500_000;
        return 0;
    }

    private int resolveMaksPakai(View root) {
        int checkedId = ((ChipGroup) root.findViewById(R.id.chipGroupMaks)).getCheckedChipId();
        if (checkedId == R.id.chipMaks1)  return 1;
        if (checkedId == R.id.chipMaks5)  return 5;
        if (checkedId == R.id.chipMaks10) return 10;
        if (checkedId == R.id.chipMaks20) return 20;
        return 0; // tak terbatas
    }

    // ──────────────────────────────────────────────────────────────

    static class VoucherItem {
        int id, maxPakai, sudahPakai, aktif;
        String kode, jenis;
        double nilai, minBeli;
        VoucherItem(int id, String kode, String jenis, double nilai, double minBeli,
                    int maxPakai, int sudahPakai, int aktif) {
            this.id = id; this.kode = kode; this.jenis = jenis;
            this.nilai = nilai; this.minBeli = minBeli;
            this.maxPakai = maxPakai; this.sudahPakai = sudahPakai; this.aktif = aktif;
        }
    }

    class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_voucher, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            VoucherItem item = list.get(position);
            h.tvKode.setText(item.kode);
            String nilaiStr = "persen".equals(item.jenis)
                    ? (int)item.nilai + "%"
                    : CurrencyFormatter.formatRupiah(item.nilai);
            h.tvDetail.setText("Diskon " + nilaiStr
                    + (item.minBeli > 0 ? " | Min. " + CurrencyFormatter.formatRupiah(item.minBeli) : ""));
            h.tvPakai.setText("Dipakai: " + item.sudahPakai
                    + (item.maxPakai > 0 ? "/" + item.maxPakai : " (tak terbatas)"));
            h.btnHapus.setOnClickListener(v ->
                new AlertDialog.Builder(ManajemenVoucherActivity.this)
                    .setTitle("Hapus Voucher")
                    .setMessage("Hapus voucher " + item.kode + "?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        dbHelper.deleteVoucher(item.id);
                        loadData();
                    })
                    .setNegativeButton("Batal", null)
                    .show()
            );
        }
        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvKode, tvDetail, tvPakai;
            MaterialButton btnHapus;
            VH(View v) {
                super(v);
                tvKode   = v.findViewById(R.id.voucherTvKode);
                tvDetail = v.findViewById(R.id.voucherTvDetail);
                tvPakai  = v.findViewById(R.id.voucherTvPakai);
                btnHapus = v.findViewById(R.id.voucherBtnHapus);
            }
        }
    }
}
