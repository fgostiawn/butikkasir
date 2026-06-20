package com.example.butikkasir;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.CartItem;
import com.example.butikkasir.model.Pelanggan;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.example.butikkasir.utils.PdfSaver;
import com.example.butikkasir.utils.PrintUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private static final int POIN_PER_RUPIAH = 10000;
    private static final int NILAI_POIN = 100;

    private double totalBelanja;
    private double totalAkhir;
    private double diskonPoin = 0;
    private double diskonVoucher = 0;
    private int poinDigunakan = 0;
    private int idVoucher = -1;
    private String kodeVoucher = null;
    private String detailTransaksi;
    private List<CartItem> cartItems;
    private DatabaseHelper dbHelper;
    private Pelanggan pelanggan = null;

    // split payment state
    private double splitTunaiAmt = 0;
    private double splitQrisAmt = 0;

    private String namaToko;
    private String nomorWa;
    private String tagline;

    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        dbHelper = new DatabaseHelper(this);

        // Load pengaturan toko dari DB
        namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko.isEmpty()) namaToko = "BUTIK DEA";
        nomorWa = dbHelper.getPengaturan("nomor_wa");
        if (nomorWa.isEmpty()) nomorWa = "0812-XXXX-XXXX";
        tagline = dbHelper.getPengaturan("tagline");
        if (tagline.isEmpty()) tagline = "Terima kasih telah berbelanja!";

        totalBelanja = getIntent().getDoubleExtra("TOTAL_BELANJA", 0);
        totalAkhir = totalBelanja;
        detailTransaksi = getIntent().getStringExtra("DETAIL_TRANSAKSI");
        if (detailTransaksi == null) detailTransaksi = "";

        //noinspection unchecked
        cartItems = (List<CartItem>) getIntent().getSerializableExtra("CART_ITEMS");
        if (cartItems == null) cartItems = new ArrayList<>();

        pelanggan = (Pelanggan) getIntent().getSerializableExtra("PELANGGAN");

        MaterialToolbar toolbar = findViewById(R.id.toolbarPayment);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTotal = findViewById(R.id.payTvTotal);
        tvTotal.setText(CurrencyFormatter.formatRupiah(totalBelanja));

        // Populate ringkasan belanja
        LinearLayout payItemContainer = findViewById(R.id.payItemContainer);
        for (CartItem item : cartItems) {
            TextView tv = new TextView(this);
            tv.setText("• " + item.getBarang().getNamaBarang()
                    + " (" + item.getUkuran() + ") × " + item.getQuantity()
                    + "  =  " + CurrencyFormatter.formatRupiah(item.getSubtotal()));
            tv.setTextColor(0xFF424242);
            tv.setTextSize(13f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int)(6 * getResources().getDisplayMetrics().density);
            tv.setLayoutParams(lp);
            payItemContainer.addView(tv);
        }

        setupPoinCard();
        setupVoucherCard();

        findViewById(R.id.payBtnCash).setOnClickListener(v -> dialogCash());
        findViewById(R.id.payBtnDebit).setOnClickListener(v -> dialogDebit());
        findViewById(R.id.payBtnQris).setOnClickListener(v -> dialogPilihEWallet());
        findViewById(R.id.payBtnSplit).setOnClickListener(v -> dialogSplitPayment());
        findViewById(R.id.payBtnHutang).setOnClickListener(v -> dialogHutang());
    }

    // ──────────────────────────────────────────────────────────────
    //  Poin setup
    // ──────────────────────────────────────────────────────────────

    private void setupPoinCard() {
        if (pelanggan == null) return;

        View cardPoin = findViewById(R.id.cardPoinPelanggan);
        cardPoin.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.payTvNamaPelanggan)).setText(pelanggan.getNama());
        ((TextView) findViewById(R.id.payTvPoinPelanggan))
                .setText(pelanggan.getPoin() + " poin tersedia  (= "
                        + CurrencyFormatter.formatRupiah(pelanggan.getPoin() * (double) NILAI_POIN) + ")");

        CheckBox checkBox = findViewById(R.id.checkboxGunakanPoin);
        TextView tvInfo = findViewById(R.id.tvInfoPoin);

        if (pelanggan.getPoin() == 0) {
            checkBox.setEnabled(false);
            checkBox.setText("Tidak ada poin untuk digunakan");
        } else {
            double maxDiskon = Math.min((double) pelanggan.getPoin() * NILAI_POIN, totalBelanja);
            int maxPoin = (int) (maxDiskon / NILAI_POIN);
            checkBox.setText("Gunakan " + maxPoin + " poin (hemat "
                    + CurrencyFormatter.formatRupiah(maxDiskon) + ")");

            checkBox.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    diskonPoin = maxDiskon;
                    poinDigunakan = maxPoin;
                } else {
                    diskonPoin = 0;
                    poinDigunakan = 0;
                }
                recalcTotal(tvInfo);
            });
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Voucher setup
    // ──────────────────────────────────────────────────────────────

    private void setupVoucherCard() {
        TextInputEditText etKode = findViewById(R.id.etKodeVoucher);
        MaterialButton btnTerapkan = findViewById(R.id.btnTerapkanVoucher);
        TextView tvInfo = findViewById(R.id.tvInfoVoucher);

        btnTerapkan.setOnClickListener(v -> {
            String kode = etKode.getText() != null ? etKode.getText().toString().trim().toUpperCase() : "";
            if (kode.isEmpty()) {
                Toast.makeText(this, "Masukkan kode voucher", Toast.LENGTH_SHORT).show();
                return;
            }

            Cursor c = dbHelper.getVoucherByKode(kode);
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                tvInfo.setText("Voucher tidak ditemukan atau tidak aktif");
                tvInfo.setTextColor(Color.parseColor("#F44336"));
                tvInfo.setVisibility(View.VISIBLE);
                diskonVoucher = 0;
                idVoucher = -1;
                kodeVoucher = null;
                recalcTotal(null);
                return;
            }

            int vId = c.getInt(c.getColumnIndexOrThrow("id_voucher"));
            String jenis = c.getString(c.getColumnIndexOrThrow("jenis"));
            double nilai = c.getDouble(c.getColumnIndexOrThrow("nilai"));
            double minBelanja = c.getDouble(c.getColumnIndexOrThrow("min_belanja"));
            int maxPenggunaan = c.getInt(c.getColumnIndexOrThrow("max_penggunaan"));
            int sudahPakai = c.getInt(c.getColumnIndexOrThrow("sudah_dipakai"));
            c.close();

            if (totalBelanja < minBelanja) {
                tvInfo.setText("Min. belanja " + CurrencyFormatter.formatRupiah(minBelanja) + " untuk voucher ini");
                tvInfo.setTextColor(Color.parseColor("#F44336"));
                tvInfo.setVisibility(View.VISIBLE);
                return;
            }
            if (maxPenggunaan > 0 && sudahPakai >= maxPenggunaan) {
                tvInfo.setText("Voucher sudah habis digunakan");
                tvInfo.setTextColor(Color.parseColor("#F44336"));
                tvInfo.setVisibility(View.VISIBLE);
                return;
            }

            double diskon;
            if ("persen".equals(jenis)) {
                diskon = totalBelanja * nilai / 100.0;
            } else {
                diskon = nilai;
            }
            diskon = Math.min(diskon, totalBelanja);

            diskonVoucher = diskon;
            idVoucher = vId;
            kodeVoucher = kode;

            String desc = "persen".equals(jenis)
                    ? "(" + (int)nilai + "%) "
                    : "";
            tvInfo.setText("✓ Hemat " + desc + CurrencyFormatter.formatRupiah(diskon) + " dengan voucher " + kode);
            tvInfo.setTextColor(Color.parseColor("#388E3C"));
            tvInfo.setVisibility(View.VISIBLE);
            recalcTotal(null);
        });
    }

    private void recalcTotal(TextView tvInfoPoin) {
        totalAkhir = totalBelanja - diskonPoin - diskonVoucher;
        if (totalAkhir < 0) totalAkhir = 0;
        tvTotal.setText(CurrencyFormatter.formatRupiah(totalAkhir));

        if (tvInfoPoin != null) {
            if (diskonPoin > 0) {
                tvInfoPoin.setText("Diskon poin: -" + CurrencyFormatter.formatRupiah(diskonPoin));
                tvInfoPoin.setVisibility(View.VISIBLE);
            } else {
                tvInfoPoin.setVisibility(View.GONE);
            }
        }
    }

    private void updatePoinPelanggan() {
        if (pelanggan == null) return;
        int poinDiperoleh = (int) (totalAkhir / POIN_PER_RUPIAH);
        dbHelper.updatePoinSetelahTransaksi(pelanggan.getId(), poinDigunakan, poinDiperoleh);
    }

    private void markVoucherUsed() {
        if (idVoucher > 0) {
            dbHelper.incrementVoucherPakai(idVoucher);
        }
    }

    private String kasirName() {
        return getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
    }

    private int pelangganId() {
        return pelanggan != null ? pelanggan.getId() : -1;
    }

    // ──────────────────────────────────────────────────────────────
    //  Payment dialogs
    // ──────────────────────────────────────────────────────────────

    private void dialogCash() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Quick amount buttons
        long[] quickAmounts = {50000, 100000, 200000, 500000};
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams quickRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        quickRowLp.bottomMargin = dpToPx(10);
        quickRow.setLayoutParams(quickRowLp);

        final TextInputEditText inputUang = new TextInputEditText(this);
        inputUang.setInputType(InputType.TYPE_CLASS_PHONE);
        inputUang.setHint("Masukkan Nominal Uang");

        MaterialButton btnUangPas = new MaterialButton(this);
        btnUangPas.setText("UANG PAS");
        btnUangPas.setCornerRadius(12);
        LinearLayout.LayoutParams pasBtnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pasBtnLp.bottomMargin = dpToPx(8);
        btnUangPas.setLayoutParams(pasBtnLp);
        btnUangPas.setOnClickListener(v -> inputUang.setText(String.format("%.0f", totalAkhir)));

        for (long amt : quickAmounts) {
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            String label = amt >= 1000000
                    ? (amt / 1000000) + "jt"
                    : (amt / 1000) + "k";
            btn.setText(label);
            btn.setCornerRadius(8);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(dpToPx(4));
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> inputUang.setText(String.valueOf(amt)));
            quickRow.addView(btn);
        }

        inputUang.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                inputUang.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("[.,\\s]", "");
                if (!clean.isEmpty()) {
                    DecimalFormat fmt = new DecimalFormat("#,###");
                    DecimalFormatSymbols sym = new DecimalFormatSymbols();
                    sym.setGroupingSeparator('.');
                    fmt.setDecimalFormatSymbols(sym);
                    String formatted = fmt.format(Long.parseLong(clean));
                    current = formatted;
                    inputUang.setText(formatted);
                    inputUang.setSelection(formatted.length());
                } else {
                    current = "";
                }
                inputUang.addTextChangedListener(this);
            }
        });

        layout.addView(btnUangPas);
        layout.addView(quickRow);
        layout.addView(inputUang);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Pembayaran Tunai — " + CurrencyFormatter.formatRupiah(totalAkhir))
            .setView(layout)
            .setPositiveButton("Bayar", null)
            .setNegativeButton("Batal", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Editable e = inputUang.getText();
            String clean = (e != null ? e.toString() : "").replaceAll("[.,\\s]", "");
            if (clean.isEmpty()) {
                Toast.makeText(this, "Masukkan nominal uang!", Toast.LENGTH_SHORT).show();
                return;
            }
            double bayar = Double.parseDouble(clean);
            if (bayar < totalAkhir) {
                Toast.makeText(this,
                    "Uang kurang " + CurrencyFormatter.formatRupiah(totalAkhir - bayar),
                    Toast.LENGTH_SHORT).show();
            } else {
                double kembalian = bayar - totalAkhir;
                long transId = dbHelper.insertTransaksiDanKurangiStokFull(
                        totalAkhir, "Tunai", detailTransaksi, kasirName(),
                        "LUNAS", pelangganId(), cartItems);
                updatePoinPelanggan();
                markVoucherUsed();
                dialog.dismiss();
                tampilkanStruk(transId, "Tunai", bayar, kembalian, "LUNAS");
            }
        }));
        dialog.show();
    }

    private void dialogDebit() {
        View view = getLayoutInflater().inflate(R.layout.dialog_form_kartu, null);

        ((TextView) view.findViewById(R.id.tvTotalKartu))
                .setText(CurrencyFormatter.formatRupiah(totalAkhir));

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupJenisKartu);
        String[] jenisKartu = {"Visa", "Mastercard", "GPN", "BCA", "Mandiri", "BRI", "BNI", "Lainnya"};
        for (int i = 0; i < jenisKartu.length; i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_kategori, chipGroup, false);
            chip.setText(jenisKartu[i]);
            chip.setChecked(i == 0);
            chipGroup.addView(chip);
        }

        TextInputEditText etNomor = view.findViewById(R.id.etNomorKartu);
        etNomor.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                etNomor.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("\\s", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < clean.length() && i < 16; i++) {
                    if (i > 0 && i % 4 == 0) formatted.append(" ");
                    formatted.append(clean.charAt(i));
                }
                current = formatted.toString();
                etNomor.setText(current);
                etNomor.setSelection(current.length());
                etNomor.addTextChangedListener(this);
            }
        });

        TextInputEditText etExpiry = view.findViewById(R.id.etMasaBerlaku);
        etExpiry.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                etExpiry.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("[^\\d]", "");
                if (clean.length() >= 2) {
                    current = clean.substring(0, 2) + "/" + clean.substring(2, Math.min(clean.length(), 4));
                } else {
                    current = clean;
                }
                etExpiry.setText(current);
                etExpiry.setSelection(current.length());
                etExpiry.addTextChangedListener(this);
            }
        });

        TextInputEditText etNama = view.findViewById(R.id.etNamaPemegang);
        TextInputEditText etCvv  = view.findViewById(R.id.etCvv);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Pembayaran Kartu Debit / Kredit")
                .setView(view)
                .setPositiveButton("Bayar", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nomorBersih  = etNomor.getText() != null ? etNomor.getText().toString().replaceAll("\\s", "") : "";
            String namaPemegang = etNama.getText() != null ? etNama.getText().toString().trim() : "";
            String expiry       = etExpiry.getText() != null ? etExpiry.getText().toString().trim() : "";
            String cvv          = etCvv.getText() != null ? etCvv.getText().toString().trim() : "";

            int checkedId = chipGroup.getCheckedChipId();
            Chip checkedChip = checkedId != View.NO_ID ? view.findViewById(checkedId) : null;
            String jenisKartuPilihan = checkedChip != null ? checkedChip.getText().toString() : "Kartu";

            if (nomorBersih.length() < 16) {
                Toast.makeText(this, "Nomor kartu harus 16 digit", Toast.LENGTH_SHORT).show();
                return;
            }
            if (namaPemegang.isEmpty()) {
                Toast.makeText(this, "Masukkan nama pemegang kartu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!expiry.matches("\\d{2}/\\d{2}")) {
                Toast.makeText(this, "Format masa berlaku: MM/YY", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cvv.length() != 3) {
                Toast.makeText(this, "CVV harus 3 digit", Toast.LENGTH_SHORT).show();
                return;
            }

            String metode = "Debit/Kredit - " + jenisKartuPilihan;
            long transId = dbHelper.insertTransaksiDanKurangiStokFull(
                    totalAkhir, metode, detailTransaksi, kasirName(),
                    "LUNAS", pelangganId(), cartItems);
            updatePoinPelanggan();
            markVoucherUsed();
            dialog.dismiss();
            tampilkanStruk(transId, metode, 0, 0, "LUNAS");
        }));

        dialog.show();
    }

    private void dialogPilihEWallet() {
        String[] ewallets = {"DANA", "OVO", "GoPay", "ShopeePay", "LinkAja", "BCA Mobile / Bank Lainnya"};
        new AlertDialog.Builder(this)
            .setTitle("Pilih Aplikasi Pembeli")
            .setItems(ewallets, (d, w) -> {
                Intent intent = new Intent(this, QrisActivity.class);
                intent.putExtra("EWALLET", ewallets[w]);
                intent.putExtra("TOTAL_BELANJA", totalAkhir);
                intent.putExtra("DETAIL_TRANSAKSI", detailTransaksi);
                intent.putExtra("CART_ITEMS", new ArrayList<>(cartItems));
                startActivityForResult(intent, 200);
            })
            .show();
    }

    private void dialogSplitPayment() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Total: " + CurrencyFormatter.formatRupiah(totalAkhir)
                + "\nMasukkan jumlah yang dibayar tunai:");
        tvInfo.setTextColor(Color.parseColor("#424242"));
        tvInfo.setTextSize(14f);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.bottomMargin = dpToPx(12);
        tvInfo.setLayoutParams(tvLp);

        final TextInputEditText etTunai = new TextInputEditText(this);
        etTunai.setInputType(InputType.TYPE_CLASS_PHONE);
        etTunai.setHint("Nominal tunai (Rp)");

        final TextView tvSisa = new TextView(this);
        tvSisa.setText("Sisa via QRIS: Rp 0");
        tvSisa.setTextColor(Color.parseColor("#1565C0"));
        tvSisa.setTextSize(14f);
        LinearLayout.LayoutParams sisaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sisaLp.topMargin = dpToPx(8);
        tvSisa.setLayoutParams(sisaLp);

        etTunai.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(current)) return;
                etTunai.removeTextChangedListener(this);
                String clean = s.toString().replaceAll("[.,\\s]", "");
                String formatted = "";
                if (!clean.isEmpty()) {
                    DecimalFormat fmt = new DecimalFormat("#,###");
                    DecimalFormatSymbols sym = new DecimalFormatSymbols();
                    sym.setGroupingSeparator('.');
                    fmt.setDecimalFormatSymbols(sym);
                    formatted = fmt.format(Long.parseLong(clean));
                    double tunai = Double.parseDouble(clean);
                    double sisa = totalAkhir - tunai;
                    tvSisa.setText("Sisa via QRIS: " + CurrencyFormatter.formatRupiah(Math.max(0, sisa)));
                } else {
                    tvSisa.setText("Sisa via QRIS: Rp 0");
                }
                current = formatted;
                etTunai.setText(formatted);
                etTunai.setSelection(formatted.length());
                etTunai.addTextChangedListener(this);
            }
        });

        layout.addView(tvInfo);
        layout.addView(etTunai);
        layout.addView(tvSisa);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Split Payment")
            .setView(layout)
            .setPositiveButton("Konfirmasi", null)
            .setNegativeButton("Batal", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cleanTunai = etTunai.getText() != null
                    ? etTunai.getText().toString().replaceAll("[.,\\s]", "") : "";
            if (cleanTunai.isEmpty()) {
                Toast.makeText(this, "Masukkan nominal tunai", Toast.LENGTH_SHORT).show();
                return;
            }
            double tunaiAmt = Double.parseDouble(cleanTunai);
            if (tunaiAmt <= 0 || tunaiAmt >= totalAkhir) {
                Toast.makeText(this,
                        "Nominal tunai harus antara Rp 1 dan " + CurrencyFormatter.formatRupiah(totalAkhir),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            splitTunaiAmt = tunaiAmt;
            splitQrisAmt = totalAkhir - tunaiAmt;
            dialog.dismiss();

            // Buka halaman QRIS untuk membayar sisa
            Intent intent = new Intent(this, QrisActivity.class);
            intent.putExtra("IS_SPLIT", true);
            intent.putExtra("EWALLET", "QRIS");
            intent.putExtra("TOTAL_BELANJA", splitQrisAmt);
            intent.putExtra("DETAIL_TRANSAKSI", detailTransaksi);
            intent.putExtra("CART_ITEMS", new ArrayList<>(cartItems));
            startActivityForResult(intent, 201);
        }));

        dialog.show();
    }

    private void dialogHutang() {
        if (pelanggan == null) {
            Toast.makeText(this,
                    "Pilih pelanggan terlebih dahulu untuk mencatat hutang",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Bayar Nanti (Hutang)")
            .setMessage("Catat transaksi Rp " + CurrencyFormatter.formatRupiah(totalAkhir)
                    + " sebagai hutang atas nama " + pelanggan.getNama()
                    + "?\n\nStok akan langsung dikurangi. Admin dapat menandai lunas nanti.")
            .setPositiveButton("Catat Hutang", (d, w) -> {
                long transId = dbHelper.insertTransaksiDanKurangiStokFull(
                        totalAkhir, "Hutang", detailTransaksi, kasirName(),
                        "HUTANG", pelangganId(), cartItems);
                tampilkanStruk(transId, "Hutang", 0, 0, "HUTANG");
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    // ──────────────────────────────────────────────────────────────
    //  Struk (receipt) bottom sheet
    // ──────────────────────────────────────────────────────────────

    private void tampilkanStruk(long transId, String metode, double bayar, double kembalian, String status) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_struk, null);
        sheet.setContentView(root);
        sheet.setCancelable(false);

        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", new Locale("id", "ID"))
                .format(new Date());
        String kasirN = kasirName();

        populateStrukView(root, transId, tanggal, kasirN, metode, bayar, kembalian, status);

        root.findViewById(R.id.strukBtnPrint).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirN, metode, bayar, kembalian, status);
            PrintUtils.printBitmap(PaymentActivity.this, bmp, "Struk_" + transId);
        });

        root.findViewById(R.id.strukBtnWhatsapp).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirN, metode, bayar, kembalian, status);
            shareViaWhatsApp(bmp, transId, metode, bayar, kembalian, status);
        });

        root.findViewById(R.id.strukBtnShare).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirN, metode, bayar, kembalian, status);
            shareReceiptBitmap(bmp, transId);
        });

        root.findViewById(R.id.strukBtnCetak).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirN, metode, bayar, kembalian, status);
            saveReceiptPdf(bmp, transId);
        });

        root.findViewById(R.id.strukBtnTutup).setOnClickListener(v -> {
            sheet.dismiss();
            setResult(RESULT_OK);
            finish();
        });

        sheet.show();
    }

    // ──────────────────────────────────────────────────────────────
    //  Format ID transaksi: TRX-YYYYMMDD-XXXX
    // ──────────────────────────────────────────────────────────────

    private String formatTransId(long transId) {
        if (transId <= 0) return "—";
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return String.format("TRX-%s-%04d", date, transId);
    }

    // ──────────────────────────────────────────────────────────────
    //  Populate receipt view
    // ──────────────────────────────────────────────────────────────

    private void populateStrukView(View root, long transId, String tanggal,
                                   String kasirN, String metode,
                                   double bayar, double kembalian, String status) {
        // Store header dari DB
        TextView tvNamaToko = root.findViewById(R.id.strukTvNamaToko);
        if (tvNamaToko != null) tvNamaToko.setText(namaToko);
        TextView tvWA = root.findViewById(R.id.strukTvNomorWA);
        if (tvWA != null) tvWA.setText("WA: " + nomorWa);
        TextView tvTagline = root.findViewById(R.id.strukTvTagline);
        if (tvTagline != null) tvTagline.setText(tagline);

        ((TextView) root.findViewById(R.id.strukTvId)).setText(formatTransId(transId));
        ((TextView) root.findViewById(R.id.strukTvTanggal)).setText(tanggal);
        ((TextView) root.findViewById(R.id.strukTvKasir)).setText(kasirN);
        ((TextView) root.findViewById(R.id.strukTvMetode)).setText(metode);
        ((TextView) root.findViewById(R.id.strukTvTotal)).setText(CurrencyFormatter.formatRupiah(totalAkhir));

        // Status HUTANG badge
        View statusBadge = root.findViewById(R.id.strukTvStatusHutang);
        if (statusBadge != null) {
            statusBadge.setVisibility("HUTANG".equals(status) ? View.VISIBLE : View.GONE);
        }

        if (pelanggan != null) {
            root.findViewById(R.id.strukLayoutPelanggan).setVisibility(View.VISIBLE);
            ((TextView) root.findViewById(R.id.strukTvPelanggan)).setText(pelanggan.getNama());
        }

        // Diskon rows
        boolean adaDiskon = diskonPoin > 0 || diskonVoucher > 0;
        View layoutSubtotal = root.findViewById(R.id.strukLayoutSubtotal);
        View layoutDiskonPoin = root.findViewById(R.id.strukLayoutDiskonPoin);
        View layoutDiskonVoucher = root.findViewById(R.id.strukLayoutDiskonVoucher);

        if (layoutSubtotal != null) layoutSubtotal.setVisibility(adaDiskon ? View.VISIBLE : View.GONE);
        if (adaDiskon && layoutSubtotal != null) {
            ((TextView) root.findViewById(R.id.strukTvSubtotal))
                    .setText(CurrencyFormatter.formatRupiah(totalBelanja));
        }

        if (layoutDiskonPoin != null) {
            layoutDiskonPoin.setVisibility(diskonPoin > 0 ? View.VISIBLE : View.GONE);
            if (diskonPoin > 0) {
                ((TextView) root.findViewById(R.id.strukTvDiskonPoin))
                        .setText("-" + CurrencyFormatter.formatRupiah(diskonPoin));
            }
        }

        if (layoutDiskonVoucher != null) {
            layoutDiskonVoucher.setVisibility(diskonVoucher > 0 ? View.VISIBLE : View.GONE);
            if (diskonVoucher > 0) {
                ((TextView) root.findViewById(R.id.strukTvDiskonVoucher))
                        .setText("-" + CurrencyFormatter.formatRupiah(diskonVoucher));
            }
        }

        if (metode.equals("Tunai") && bayar > 0) {
            root.findViewById(R.id.strukLayoutBayar).setVisibility(View.VISIBLE);
            root.findViewById(R.id.strukLayoutKembalian).setVisibility(View.VISIBLE);
            ((TextView) root.findViewById(R.id.strukTvBayar)).setText(CurrencyFormatter.formatRupiah(bayar));
            ((TextView) root.findViewById(R.id.strukTvKembalian)).setText(CurrencyFormatter.formatRupiah(kembalian));
        }

        LinearLayout itemContainer = root.findViewById(R.id.strukItemContainer);
        itemContainer.removeAllViews();
        for (CartItem item : cartItems) {
            addItemRow(itemContainer, item);
        }
    }

    private void addItemRow(LinearLayout container, CartItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dpToPx(10);
        row.setLayoutParams(rowParams);

        TextView tvNama = new TextView(this);
        tvNama.setText(item.getBarang().getNamaBarang());
        tvNama.setTextColor(Color.parseColor("#212121"));
        tvNama.setTextSize(13f);
        tvNama.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDetail = new TextView(this);
        String detail = "Size " + item.getUkuran()
                + "  ×  " + item.getQuantity() + " pcs"
                + "  @" + CurrencyFormatter.formatRupiah(item.getBarang().getHarga());
        tvDetail.setText(detail);
        tvDetail.setTextColor(Color.parseColor("#757575"));
        tvDetail.setTextSize(12f);

        LinearLayout subtotalRow = new LinearLayout(this);
        subtotalRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvSubLabel = new TextView(this);
        tvSubLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvSubLabel.setText("Subtotal");
        tvSubLabel.setTextColor(Color.parseColor("#9E9E9E"));
        tvSubLabel.setTextSize(12f);

        TextView tvSubtotal = new TextView(this);
        tvSubtotal.setText(CurrencyFormatter.formatRupiah(item.getSubtotal()));
        tvSubtotal.setTextColor(Color.parseColor("#212121"));
        tvSubtotal.setTextSize(12f);
        tvSubtotal.setTypeface(null, android.graphics.Typeface.BOLD);

        subtotalRow.addView(tvSubLabel);
        subtotalRow.addView(tvSubtotal);

        row.addView(tvNama);
        row.addView(tvDetail);
        row.addView(subtotalRow);
        container.addView(row);
    }

    // ──────────────────────────────────────────────────────────────
    //  Bitmap generation for share / print
    // ──────────────────────────────────────────────────────────────

    private Bitmap buildReceiptBitmap(long transId, String tanggal, String kasirN,
                                      String metode, double bayar, double kembalian, String status) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View printView = inflater.inflate(R.layout.layout_struk_content, null);
        populateStrukView(printView, transId, tanggal, kasirN, metode, bayar, kembalian, status);

        int widthPx = dpToPx(360);
        printView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        printView.layout(0, 0, printView.getMeasuredWidth(), printView.getMeasuredHeight());

        Bitmap bmp = Bitmap.createBitmap(
                printView.getMeasuredWidth(), printView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        printView.draw(new Canvas(bmp));
        return bmp;
    }

    // ──────────────────────────────────────────────────────────────
    //  Share as image
    // ──────────────────────────────────────────────────────────────

    private void shareReceiptBitmap(Bitmap bmp, long transId) {
        try {
            File dir = new File(getCacheDir(), "struk");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "struk_" + transId + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            Uri uri = FileProvider.getUriForFile(this, "com.example.butikkasir.provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, buildTextReceipt(transId));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Bagikan Struk via"));
        } catch (IOException e) {
            Toast.makeText(this, "Gagal membuat file struk", Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Share directly to WhatsApp
    // ──────────────────────────────────────────────────────────────

    private void shareViaWhatsApp(Bitmap bmp, long transId, String metode,
                                   double bayar, double kembalian, String status) {
        try {
            File dir = new File(getCacheDir(), "struk");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "struk_wa_" + transId + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            Uri uri = FileProvider.getUriForFile(this, "com.example.butikkasir.provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, buildWhatsAppText(transId, metode, bayar, kembalian, status));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage("com.whatsapp");
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e1) {
                intent.setPackage("com.whatsapp.w4b");
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e2) {
                    Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Gagal membuat file struk", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildWhatsAppText(long transId, String metode, double bayar, double kembalian, String status) {
        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"))
                .format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(namaToko).append("*\n");
        sb.append(formatTransId(transId)).append("\n\n");
        sb.append("Tanggal : ").append(tanggal).append("\n");
        sb.append("Kasir   : ").append(kasirName()).append("\n");
        if (pelanggan != null) sb.append("Pelanggan : ").append(pelanggan.getNama()).append("\n");
        sb.append("\n*Item Belanja:*\n");
        for (CartItem item : cartItems) {
            sb.append("• ").append(item.getBarang().getNamaBarang())
              .append(" (").append(item.getUkuran()).append(")")
              .append(" × ").append(item.getQuantity())
              .append(" = ").append(CurrencyFormatter.formatRupiah(item.getSubtotal())).append("\n");
        }
        if (diskonPoin > 0) sb.append("\nSubtotal : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("\n")
                .append("Diskon Poin : -").append(CurrencyFormatter.formatRupiah(diskonPoin)).append("\n");
        if (diskonVoucher > 0) sb.append("Diskon Voucher (").append(kodeVoucher).append("): -")
                .append(CurrencyFormatter.formatRupiah(diskonVoucher)).append("\n");
        sb.append("\n*Total  : ").append(CurrencyFormatter.formatRupiah(totalAkhir)).append("*\n");
        sb.append("Metode  : ").append(metode).append("\n");
        if (metode.equals("Tunai") && bayar > 0) {
            sb.append("Bayar   : ").append(CurrencyFormatter.formatRupiah(bayar)).append("\n");
            sb.append("Kembali : ").append(CurrencyFormatter.formatRupiah(kembalian)).append("\n");
        }
        if ("HUTANG".equals(status)) sb.append("\n⚠ STATUS: BELUM LUNAS\n");
        sb.append("\n").append(tagline);
        sb.append("\nWA: ").append(nomorWa);
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────
    //  Save receipt as PDF
    // ──────────────────────────────────────────────────────────────

    private void saveReceiptPdf(Bitmap bmp, long transId) {
        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.Page page = pdf.startPage(
            new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                bmp.getWidth(), bmp.getHeight(), 1).create());
        page.getCanvas().drawBitmap(bmp, 0, 0, null);
        pdf.finishPage(page);

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Struk_" + transId + "_" + ts + ".pdf";

        PdfSaver.save(this, pdf, fileName, new PdfSaver.Callback() {
            @Override
            public void onSuccess(Uri uri, String displayName) {
                PdfSaver.showSuccessDialog(PaymentActivity.this, uri, displayName, null);
            }
            @Override
            public void onError(String message) {
                Toast.makeText(PaymentActivity.this,
                    "Gagal menyimpan PDF: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    //  Plain-text receipt
    // ──────────────────────────────────────────────────────────────

    private String buildTextReceipt(long transId) {
        StringBuilder sb = new StringBuilder();
        sb.append("====== ").append(namaToko).append(" ======\n");
        sb.append("No. Transaksi : ").append(formatTransId(transId)).append("\n");
        sb.append("Tanggal       : ")
          .append(new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date()))
          .append("\n");
        if (pelanggan != null) sb.append("Pelanggan     : ").append(pelanggan.getNama()).append("\n");
        sb.append("─────────────────────────\n");
        for (CartItem item : cartItems) {
            sb.append(item.getBarang().getNamaBarang())
              .append(" (").append(item.getUkuran()).append(")\n");
            sb.append("  ").append(item.getQuantity()).append(" × ")
              .append(CurrencyFormatter.formatRupiah(item.getBarang().getHarga()))
              .append(" = ").append(CurrencyFormatter.formatRupiah(item.getSubtotal())).append("\n");
        }
        sb.append("─────────────────────────\n");
        if (diskonPoin > 0) sb.append("Subtotal      : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("\n")
                .append("Diskon Poin   : -").append(CurrencyFormatter.formatRupiah(diskonPoin)).append("\n");
        if (diskonVoucher > 0) sb.append("Diskon Voucher: -").append(CurrencyFormatter.formatRupiah(diskonVoucher)).append("\n");
        sb.append("Total         : ").append(CurrencyFormatter.formatRupiah(totalAkhir)).append("\n");
        sb.append("=========================\n");
        sb.append(tagline);
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            // QRIS normal
            updatePoinPelanggan();
            markVoucherUsed();
            setResult(RESULT_OK);
            finish();
        } else if (requestCode == 201 && resultCode == RESULT_OK) {
            // Split payment: QRIS selesai, simpan transaksi sekarang
            String metode = "Split — Tunai: " + CurrencyFormatter.formatRupiah(splitTunaiAmt)
                    + " + QRIS: " + CurrencyFormatter.formatRupiah(splitQrisAmt);
            long transId = dbHelper.insertTransaksiDanKurangiStokFull(
                    totalAkhir, metode, detailTransaksi, kasirName(),
                    "LUNAS", pelangganId(), cartItems);
            updatePoinPelanggan();
            markVoucherUsed();
            tampilkanStruk(transId, metode, splitTunaiAmt, 0, "LUNAS");
        }
    }
}
