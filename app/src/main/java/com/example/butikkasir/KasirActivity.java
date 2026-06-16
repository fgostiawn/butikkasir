package com.example.butikkasir;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.adapter.BarangAdapter;
import com.example.butikkasir.adapter.KeranjangAdapter;
import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.Barang;
import com.example.butikkasir.model.CartItem;
import com.example.butikkasir.model.Pelanggan;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class KasirActivity extends AppCompatActivity {

    private static final String KATEGORI_SEMUA = "Semua";
    private static final int REQUEST_PELANGGAN = 101;

    private RecyclerView rvBarang, rvKeranjang;
    private TextView tvTotalTagihan, tvEmptyKatalog, tvKeranjangBadge, tvPelangganAktif;
    private TextInputEditText etCariBarang;
    private MaterialButton btnLanjutPembayaran;
    private DatabaseHelper dbHelper;

    private final List<CartItem> cartList = new ArrayList<>();
    private final List<Barang> katalogList = new ArrayList<>();
    private final List<Barang> katalogListFull = new ArrayList<>();
    private BarangAdapter barangAdapter;
    private KeranjangAdapter keranjangAdapter;
    private double totalBelanjaSekarang = 0.0;
    private String kategoriAktif = KATEGORI_SEMUA;
    private Pelanggan selectedPelanggan = null;

    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kasir);

        MaterialToolbar toolbar = findViewById(R.id.toolbarKasir);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);

        rvBarang = findViewById(R.id.rvBarangKasir);
        rvKeranjang = findViewById(R.id.rvKeranjang);
        tvTotalTagihan = findViewById(R.id.tvTotalTagihan);
        tvEmptyKatalog = findViewById(R.id.tvEmptyKatalog);
        tvKeranjangBadge = findViewById(R.id.tvKeranjangBadge);
        etCariBarang = findViewById(R.id.etCariBarang);
        btnLanjutPembayaran = findViewById(R.id.btnLanjutPembayaran);

        View bottomSheet = findViewById(R.id.bottomSheetKeranjang);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        tvPelangganAktif = findViewById(R.id.tvPelangganAktif);
        findViewById(R.id.cardPelanggan).setOnClickListener(v -> {
            Intent i = new Intent(KasirActivity.this, PelangganActivity.class);
            startActivityForResult(i, REQUEST_PELANGGAN);
        });

        setupKeranjang();
        setupSearch();
        setupKategoriChips();
        setupSwipeDelete();
        loadKatalogDariDB();

        btnLanjutPembayaran.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Tas belanja kosong!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(KasirActivity.this, PaymentActivity.class);
            intent.putExtra("TOTAL_BELANJA", totalBelanjaSekarang);
            intent.putExtra("DETAIL_TRANSAKSI", generateDetailTransaksi());
            intent.putExtra("CART_ITEMS", new ArrayList<>(cartList));
            if (selectedPelanggan != null) {
                intent.putExtra("PELANGGAN", selectedPelanggan);
            }
            startActivityForResult(intent, 100);
        });
    }

    private void setupSearch() {
        etCariBarang.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString().trim());
            }
        });
    }

    private void setupKategoriChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupKategori);
        String[] daftarKategori = {KATEGORI_SEMUA, "Atasan", "Bawahan", "Dress", "Outer", "Aksesoris", "Lainnya"};
        for (String kat : daftarKategori) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_kategori, chipGroup, false);
            chip.setText(kat);
            chip.setChecked(kat.equals(KATEGORI_SEMUA));
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    kategoriAktif = kat;
                    String q = etCariBarang.getText() != null ? etCariBarang.getText().toString().trim() : "";
                    applyFilter(q);
                }
            });
            chipGroup.addView(chip);
        }
    }

    private void setupSwipeDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                cartList.remove(pos);
                keranjangAdapter.notifyItemRemoved(pos);
                hitungTotal();
                Toast.makeText(KasirActivity.this, "Item dihapus dari keranjang", Toast.LENGTH_SHORT).show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvKeranjang);
    }

    private void applyFilter(String query) {
        katalogList.clear();
        String q = query.toLowerCase();
        for (Barang b : katalogListFull) {
            boolean matchSearch = query.isEmpty() || b.getNamaBarang().toLowerCase().contains(q);
            boolean matchKat = KATEGORI_SEMUA.equals(kategoriAktif) || kategoriAktif.equals(b.getKategori());
            if (matchSearch && matchKat) katalogList.add(b);
        }
        if (barangAdapter != null) barangAdapter.notifyDataSetChanged();
        boolean kosong = katalogList.isEmpty();
        if (tvEmptyKatalog != null) tvEmptyKatalog.setVisibility(kosong ? View.VISIBLE : View.GONE);
        if (rvBarang != null) rvBarang.setVisibility(kosong ? View.GONE : View.VISIBLE);
    }

    private void loadKatalogDariDB() {
        katalogListFull.clear();
        Cursor c = dbHelper.getBarangAktif();
        if (c != null && c.moveToFirst()) {
            do {
                int id         = c.getInt(c.getColumnIndexOrThrow("id_barang"));
                String nama    = c.getString(c.getColumnIndexOrThrow("nama_barang"));
                double harga   = c.getDouble(c.getColumnIndexOrThrow("harga"));
                int stok       = c.getInt(c.getColumnIndexOrThrow("stok"));
                String detail  = c.getString(c.getColumnIndexOrThrow("detail_barang"));
                String ukuranStr = c.getString(c.getColumnIndexOrThrow("ukuran"));
                String kategori  = c.getString(c.getColumnIndexOrThrow("kategori"));
                if (detail == null) detail = "";
                if (ukuranStr == null || ukuranStr.isEmpty()) ukuranStr = "S,M,L,XL";
                if (kategori == null || kategori.isEmpty()) kategori = "Lainnya";
                String[] ukuran = ukuranStr.split(",");
                for (int i = 0; i < ukuran.length; i++) ukuran[i] = ukuran[i].trim();
                katalogListFull.add(new Barang(id, nama, detail, harga, R.mipmap.ic_launcher, ukuran, stok, kategori));
            } while (c.moveToNext());
            c.close();
        }

        String currentSearch = etCariBarang != null && etCariBarang.getText() != null
                ? etCariBarang.getText().toString().trim() : "";
        applyFilter(currentSearch);

        if (barangAdapter == null) {
            barangAdapter = new BarangAdapter(katalogList, this::tampilkanDetailBottomSheet);
            rvBarang.setLayoutManager(new LinearLayoutManager(this));
            rvBarang.setAdapter(barangAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadKatalogDariDB();
    }

    private void setupKeranjang() {
        keranjangAdapter = new KeranjangAdapter(cartList, this::hitungTotal);
        rvKeranjang.setLayoutManager(new LinearLayoutManager(this));
        rvKeranjang.setAdapter(keranjangAdapter);
    }

    private void hitungTotal() {
        totalBelanjaSekarang = 0;
        int totalItem = 0;
        for (CartItem item : cartList) {
            totalBelanjaSekarang += item.getSubtotal();
            totalItem += item.getQuantity();
        }
        tvTotalTagihan.setText("Total: " + CurrencyFormatter.formatRupiah(totalBelanjaSekarang));
        if (tvKeranjangBadge != null) {
            tvKeranjangBadge.setVisibility(totalItem > 0 ? View.VISIBLE : View.GONE);
            tvKeranjangBadge.setText(String.valueOf(totalItem));
        }
    }

    private void tampilkanDetailBottomSheet(Barang barang) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_detail_barang, null);
        bottomSheetDialog.setContentView(view);

        ImageView imgBarang = view.findViewById(R.id.detailImgBarang);
        TextView tvNama = view.findViewById(R.id.detailTvNama);
        TextView tvHarga = view.findViewById(R.id.detailTvHarga);
        TextView tvDeskripsi = view.findViewById(R.id.detailTvDeskripsi);
        RadioGroup rgUkuran = view.findViewById(R.id.rgUkuran);
        MaterialButton btnMasukkanTas = view.findViewById(R.id.btnKonfirmasiKeranjang);

        TextView tvQty = view.findViewById(R.id.detailTvQty);
        MaterialButton btnMinus = view.findViewById(R.id.btnDetailMinus);
        MaterialButton btnPlus = view.findViewById(R.id.btnDetailPlus);
        final int[] qtyAwal = {1};

        imgBarang.setImageResource(barang.getGambarResId());
        tvNama.setText(barang.getNamaBarang());
        tvHarga.setText(CurrencyFormatter.formatRupiah(barang.getHarga()));
        tvDeskripsi.setText(barang.getDetailBarang());

        for (String ukuran : barang.getUkuranTersedia()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(ukuran);
            rb.setPadding(20, 10, 20, 10);
            rgUkuran.addView(rb);
        }

        btnMinus.setOnClickListener(v -> {
            if (qtyAwal[0] > 1) {
                qtyAwal[0]--;
                tvQty.setText(String.valueOf(qtyAwal[0]));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (qtyAwal[0] < barang.getStok()) {
                qtyAwal[0]++;
                tvQty.setText(String.valueOf(qtyAwal[0]));
            } else {
                Toast.makeText(this, "Stok hanya " + barang.getStok() + " pcs", Toast.LENGTH_SHORT).show();
            }
        });

        btnMasukkanTas.setOnClickListener(v -> {
            int selectedId = rgUkuran.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(KasirActivity.this, "Pilih ukuran dulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRb = view.findViewById(selectedId);
            String ukuranPilihan = selectedRb.getText().toString();

            boolean sudahAda = false;
            for (CartItem item : cartList) {
                if (item.getBarang().getId() == barang.getId() && item.getUkuran().equals(ukuranPilihan)) {
                    item.setQuantity(item.getQuantity() + qtyAwal[0]);
                    sudahAda = true;
                    break;
                }
            }

            if (!sudahAda) {
                cartList.add(new CartItem(barang, ukuranPilihan, qtyAwal[0]));
            }

            keranjangAdapter.notifyDataSetChanged();
            hitungTotal();
            Toast.makeText(KasirActivity.this, "Masuk tas!", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            rvKeranjang.scrollToPosition(cartList.size() - 1);
        });

        bottomSheetDialog.show();
    }

    private String generateDetailTransaksi() {
        StringBuilder detail = new StringBuilder();
        for (CartItem item : cartList) {
            detail.append(item.getBarang().getNamaBarang())
                    .append("(").append(item.getUkuran()).append(")x")
                    .append(item.getQuantity()).append(", ");
        }
        return detail.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            selectedPelanggan = null;
            tvPelangganAktif.setText("Tamu (Tanpa Akun)");
            resetKeranjang();
        }
        if (requestCode == REQUEST_PELANGGAN && resultCode == RESULT_OK && data != null) {
            selectedPelanggan = (Pelanggan) data.getSerializableExtra("PELANGGAN");
            if (selectedPelanggan != null) {
                tvPelangganAktif.setText(selectedPelanggan.getNama()
                        + "  •  " + selectedPelanggan.getPoin() + " poin");
            }
        }
    }

    private void resetKeranjang() {
        cartList.clear();
        keranjangAdapter.notifyDataSetChanged();
        hitungTotal();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_kasir, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_laporan) {
            startActivity(new Intent(KasirActivity.this, RekapLaporanActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            prosesLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prosesLogout() {
        SharedPreferences sharedPreferences = getSharedPreferences("ButikSession", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(KasirActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}
