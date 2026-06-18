package com.example.butikkasir;

import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText etKode = new EditText(this);
        etKode.setHint("Kode Voucher (cth: DISKON10)");
        etKode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        RadioGroup rgJenis = new RadioGroup(this);
        rgJenis.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbPersen = new RadioButton(this);
        rbPersen.setText("Persen (%)");
        rbPersen.setId(View.generateViewId());
        RadioButton rbNominal = new RadioButton(this);
        rbNominal.setText("Nominal (Rp)");
        rbNominal.setId(View.generateViewId());
        rbPersen.setChecked(true);
        rgJenis.addView(rbPersen);
        rgJenis.addView(rbNominal);

        EditText etNilai   = new EditText(this); etNilai.setHint("Nilai (mis: 10 untuk 10% atau 20000 untuk Rp 20.000)");
        etNilai.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText etMin     = new EditText(this); etMin.setHint("Min. belanja (0 = tanpa syarat)");
        etMin.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText etMaxPakai = new EditText(this); etMaxPakai.setHint("Maks penggunaan (0 = tak terbatas)");
        etMaxPakai.setInputType(InputType.TYPE_CLASS_NUMBER);

        layout.addView(etKode);
        layout.addView(rgJenis);
        layout.addView(etNilai);
        layout.addView(etMin);
        layout.addView(etMaxPakai);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tambah Voucher")
                .setView(layout)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String kode = etKode.getText().toString().trim().toUpperCase();
            String nilaiStr = etNilai.getText().toString().trim();
            String minStr   = etMin.getText().toString().trim();
            String maxStr   = etMaxPakai.getText().toString().trim();

            if (kode.isEmpty() || nilaiStr.isEmpty()) {
                Toast.makeText(this, "Kode dan nilai wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            String jenis = rbPersen.isChecked() ? "persen" : "nominal";
            double nilai = Double.parseDouble(nilaiStr);
            double min   = minStr.isEmpty() ? 0 : Double.parseDouble(minStr);
            int maxPakai = maxStr.isEmpty() ? 0 : Integer.parseInt(maxStr);

            if (jenis.equals("persen") && (nilai <= 0 || nilai > 100)) {
                Toast.makeText(this, "Persen harus antara 1-100", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = dbHelper.insertVoucher(kode, jenis, nilai, min, maxPakai);
            if (ok) {
                Toast.makeText(this, "Voucher ditambahkan", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadData();
            } else {
                Toast.makeText(this, "Kode voucher sudah ada", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
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
