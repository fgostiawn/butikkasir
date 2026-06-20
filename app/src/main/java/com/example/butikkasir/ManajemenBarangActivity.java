package com.example.butikkasir;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ManajemenBarangActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private final List<BarangItem> listBarang = new ArrayList<>();
    private final List<BarangItem> filteredList = new ArrayList<>();
    private BarangAdminAdapter adapter;

    private String currentSearch = "";
    private String currentKategori = "Semua";

    // Image picking state
    private ImageView dialogImgPreview;
    private TextView dialogTvHapusGambar;
    private String pendingGambarPath = "";
    private String tempCameraFilePath = null;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manajemen_barang);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarBarang);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvBarangAdmin);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BarangAdminAdapter(filteredList);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabTambahBarang);
        fab.setOnClickListener(v -> showFormDialog(null));

        setupImageLaunchers();
        setupSearch();
        setupCategoryChips();
        loadData();
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && tempCameraFilePath != null) {
                        pendingGambarPath = tempCameraFilePath;
                        if (dialogImgPreview != null) {
                            loadImageIntoView(dialogImgPreview, pendingGambarPath);
                            updateHapusGambarVisibility();
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String copied = copyUriToInternal(uri);
                        if (copied != null) {
                            pendingGambarPath = copied;
                            if (dialogImgPreview != null) {
                                loadImageIntoView(dialogImgPreview, pendingGambarPath);
                                updateHapusGambarVisibility();
                            }
                        } else {
                            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) launchCamera();
                    else Toast.makeText(this, "Izin kamera dibutuhkan untuk mengambil foto", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.etSearchBarang);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().trim();
                applyFilter();
            }
        });
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupKategoriAdmin);
        String[] allCategories = new String[]{"Semua", "Atasan", "Bawahan", "Dress", "Outer", "Aksesoris", "Lainnya"};

        for (String cat : allCategories) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_kategori, chipGroup, false);
            chip.setText(cat);
            chip.setChecked("Semua".equals(cat));
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    currentKategori = cat;
                    applyFilter();
                }
            });
            chipGroup.addView(chip);
        }
    }

    private void loadData() {
        listBarang.clear();
        Cursor c = dbHelper.getAllBarang();
        if (c != null && c.moveToFirst()) {
            do {
                String kategori = c.getString(c.getColumnIndexOrThrow("kategori"));
                int gambarCol = c.getColumnIndex("gambar_path");
                String gambarPath = gambarCol >= 0 ? c.getString(gambarCol) : "";
                listBarang.add(new BarangItem(
                        c.getInt(c.getColumnIndexOrThrow("id_barang")),
                        c.getString(c.getColumnIndexOrThrow("nama_barang")),
                        c.getDouble(c.getColumnIndexOrThrow("harga")),
                        c.getInt(c.getColumnIndexOrThrow("stok")),
                        c.getString(c.getColumnIndexOrThrow("detail_barang")),
                        c.getString(c.getColumnIndexOrThrow("ukuran")),
                        kategori != null ? kategori : "Lainnya",
                        gambarPath
                ));
            } while (c.moveToNext());
            c.close();
        }
        applyFilter();
    }

    private void applyFilter() {
        filteredList.clear();
        for (BarangItem b : listBarang) {
            boolean matchSearch = currentSearch.isEmpty()
                    || b.nama.toLowerCase().contains(currentSearch.toLowerCase());
            boolean matchKat = "Semua".equals(currentKategori)
                    || b.kategori.equalsIgnoreCase(currentKategori);
            if (matchSearch && matchKat) filteredList.add(b);
        }
        adapter.notifyDataSetChanged();

        View emptyView = findViewById(R.id.tvEmptyBarang);
        RecyclerView rv = findViewById(R.id.rvBarangAdmin);
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }

    private static final String[] DAFTAR_KATEGORI = {"Atasan", "Bawahan", "Dress", "Outer", "Aksesoris", "Lainnya"};

    private void showFormDialog(BarangItem existing) {
        pendingGambarPath = existing != null ? existing.gambarPath : "";

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_form_barang, null);
        EditText etNama              = view.findViewById(R.id.etNamaBarangForm);
        EditText etHarga             = view.findViewById(R.id.etHargaBarangForm);
        EditText etStok              = view.findViewById(R.id.etStokBarangForm);
        EditText etDetail            = view.findViewById(R.id.etDetailBarangForm);
        AutoCompleteTextView actvKat = view.findViewById(R.id.actvKategoriForm);

        android.widget.CheckBox cbS   = view.findViewById(R.id.cbUkuranS);
        android.widget.CheckBox cbM   = view.findViewById(R.id.cbUkuranM);
        android.widget.CheckBox cbL   = view.findViewById(R.id.cbUkuranL);
        android.widget.CheckBox cbXL  = view.findViewById(R.id.cbUkuranXL);
        android.widget.CheckBox cbXXL = view.findViewById(R.id.cbUkuranXXL);

        dialogImgPreview    = view.findViewById(R.id.ivPreviewGambar);
        dialogTvHapusGambar = view.findViewById(R.id.tvHapusGambar);
        MaterialButton btnKamera = view.findViewById(R.id.btnKameraForm);
        MaterialButton btnGaleri = view.findViewById(R.id.btnGaleriForm);

        // Load existing image if available
        loadImageIntoView(dialogImgPreview, pendingGambarPath);
        updateHapusGambarVisibility();

        btnKamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGaleri.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        dialogTvHapusGambar.setOnClickListener(v -> {
            pendingGambarPath = "";
            dialogImgPreview.setImageResource(R.drawable.ic_image_placeholder);
            updateHapusGambarVisibility();
        });

        ArrayAdapter<String> katAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DAFTAR_KATEGORI);
        actvKat.setAdapter(katAdapter);

        // Format ribuan saat ketik harga
        etHarga.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                etHarga.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("[.\\s]", "");
                if (!clean.isEmpty()) {
                    java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
                    java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
                    sym.setGroupingSeparator('.');
                    fmt.setDecimalFormatSymbols(sym);
                    String formatted = fmt.format(Long.parseLong(clean));
                    current = formatted;
                    etHarga.setText(formatted);
                    etHarga.setSelection(formatted.length());
                } else {
                    current = "";
                }
                etHarga.addTextChangedListener(this);
            }
        });

        if (existing != null) {
            etNama.setText(existing.nama);
            // Set harga dengan format ribuan
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
            java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
            sym.setGroupingSeparator('.');
            fmt.setDecimalFormatSymbols(sym);
            etHarga.setText(fmt.format((long) existing.harga));
            etStok.setText(String.valueOf(existing.stok));
            etDetail.setText(existing.detail);
            actvKat.setText(existing.kategori, false);
            String ukuranExisting = existing.ukuran != null ? existing.ukuran.toUpperCase() : "";
            cbS.setChecked(ukuranExisting.contains("S") && !ukuranExisting.contains("XS"));
            cbM.setChecked(ukuranExisting.contains("M") && !ukuranExisting.contains("XM"));
            cbL.setChecked(ukuranExisting.contains("L") && !ukuranExisting.contains("XL") && !ukuranExisting.contains("XXL"));
            cbXL.setChecked(ukuranExisting.contains("XL") && !ukuranExisting.contains("XXL"));
            cbXXL.setChecked(ukuranExisting.contains("XXL"));
        } else {
            actvKat.setText("Lainnya", false);
            cbS.setChecked(true);
            cbM.setChecked(true);
            cbL.setChecked(true);
            cbXL.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Tambah Barang" : "Edit Barang")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", (d, w) -> {
                    dialogImgPreview = null;
                    dialogTvHapusGambar = null;
                })
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nama     = etNama.getText().toString().trim();
            String hargaStr = etHarga.getText().toString().trim().replaceAll("\\.", "");
            String stokStr  = etStok.getText().toString().trim();
            String detail   = etDetail.getText().toString().trim();
            String kategori = actvKat.getText().toString().trim();

            if (TextUtils.isEmpty(nama) || TextUtils.isEmpty(hargaStr) || TextUtils.isEmpty(stokStr)) {
                Toast.makeText(this, "Nama, harga, dan stok wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder ukuranBuilder = new StringBuilder();
            if (cbS.isChecked())   ukuranBuilder.append("S,");
            if (cbM.isChecked())   ukuranBuilder.append("M,");
            if (cbL.isChecked())   ukuranBuilder.append("L,");
            if (cbXL.isChecked())  ukuranBuilder.append("XL,");
            if (cbXXL.isChecked()) ukuranBuilder.append("XXL,");
            String ukuran = ukuranBuilder.toString();
            if (ukuran.endsWith(",")) ukuran = ukuran.substring(0, ukuran.length() - 1);
            if (TextUtils.isEmpty(ukuran)) ukuran = "S,M,L,XL";

            if (TextUtils.isEmpty(kategori)) kategori = "Lainnya";

            double harga = Double.parseDouble(hargaStr);
            int stok = Integer.parseInt(stokStr);
            boolean ok;

            if (existing == null) {
                ok = dbHelper.insertBarang(nama, harga, stok, detail, ukuran, kategori, pendingGambarPath);
                if (ok) Toast.makeText(this, "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show();
            } else {
                ok = dbHelper.updateBarang(existing.id, nama, harga, stok, detail, ukuran, kategori, pendingGambarPath);
                if (ok) Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show();
            }

            if (ok) {
                dialogImgPreview = null;
                dialogTvHapusGambar = null;
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

    // ── Image helpers ─────────────────────────────────────────────────

    private void launchCamera() {
        try {
            File dir = new File(getFilesDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File imageFile = new File(dir, "cam_" + System.currentTimeMillis() + ".jpg");
            tempCameraFilePath = imageFile.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, "com.example.butikkasir.provider", imageFile);
            cameraLauncher.launch(uri);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show();
        }
    }

    private String copyUriToInternal(Uri uri) {
        File dir = new File(getFilesDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, "galeri_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) return null;
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            return dest.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void loadImageIntoView(ImageView iv, String path) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeFile(path, opts);
            if (bm != null) {
                iv.setImageBitmap(bm);
                return;
            }
        }
        iv.setImageResource(R.drawable.ic_image_placeholder);
    }

    private void updateHapusGambarVisibility() {
        if (dialogTvHapusGambar != null) {
            dialogTvHapusGambar.setVisibility(
                    (pendingGambarPath != null && !pendingGambarPath.isEmpty())
                            ? View.VISIBLE : View.GONE);
        }
    }

    // ── Inner model ──────────────────────────────────────────────────

    static class BarangItem {
        int id;
        String nama, detail, ukuran, kategori, gambarPath;
        double harga;
        int stok;

        BarangItem(int id, String nama, double harga, int stok, String detail,
                   String ukuran, String kategori, String gambarPath) {
            this.id         = id;
            this.nama       = nama;
            this.harga      = harga;
            this.stok       = stok;
            this.detail     = detail != null ? detail : "";
            this.ukuran     = ukuran != null ? ukuran : "S,M,L,XL";
            this.kategori   = kategori != null ? kategori : "Lainnya";
            this.gambarPath = gambarPath != null ? gambarPath : "";
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
            h.tvKategori.setText(b.kategori);
            h.tvUkuran.setText("Ukuran: " + b.ukuran);

            // Stock color indicator
            h.tvStok.setText("Stok: " + b.stok);
            if (b.stok <= 3) {
                h.tvStok.setTextColor(getResources().getColor(R.color.stockColorLow, getTheme()));
            } else if (b.stok <= 10) {
                h.tvStok.setTextColor(getResources().getColor(R.color.stockColorMedium, getTheme()));
            } else {
                h.tvStok.setTextColor(getResources().getColor(R.color.stockColorHigh, getTheme()));
            }

            // Load image thumbnail
            loadImageIntoView(h.ivBarang, b.gambarPath);

            h.btnEdit.setOnClickListener(v -> showFormDialog(b));
            h.btnHapus.setOnClickListener(v -> showDeleteDialog(b));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivBarang;
            TextView tvNama, tvHarga, tvStok, tvKategori, tvUkuran;
            MaterialButton btnEdit, btnHapus;

            VH(@NonNull View v) {
                super(v);
                ivBarang   = v.findViewById(R.id.ivBarangAdmin);
                tvNama     = v.findViewById(R.id.tvNamaBarangAdmin);
                tvHarga    = v.findViewById(R.id.tvHargaBarangAdmin);
                tvStok     = v.findViewById(R.id.tvStokBarangAdmin);
                tvKategori = v.findViewById(R.id.tvKategoriAdmin);
                tvUkuran   = v.findViewById(R.id.tvUkuranAdmin);
                btnEdit    = v.findViewById(R.id.btnEditBarang);
                btnHapus   = v.findViewById(R.id.btnHapusBarang);
            }
        }
    }
}
