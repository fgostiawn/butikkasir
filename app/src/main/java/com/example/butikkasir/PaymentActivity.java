package com.example.butikkasir;

import android.content.Intent;
import android.content.SharedPreferences;
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

    private static final String NAMA_TOKO = "BUTIK DEA";
    private static final String NOMOR_WA   = "0812-XXXX-XXXX";
    private static final int POIN_PER_RUPIAH = 10000;
    private static final int NILAI_POIN = 100;

    private double totalBelanja;
    private double totalAkhir;
    private double diskonPoin = 0;
    private int poinDigunakan = 0;
    private String detailTransaksi;
    private List<CartItem> cartItems;
    private DatabaseHelper dbHelper;
    private Pelanggan pelanggan = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        dbHelper = new DatabaseHelper(this);
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

        TextView tvTotal = findViewById(R.id.payTvTotal);
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

        setupPoinCard(tvTotal);

        findViewById(R.id.payBtnCash).setOnClickListener(v -> dialogCash());
        findViewById(R.id.payBtnDebit).setOnClickListener(v -> dialogDebit());
        findViewById(R.id.payBtnQris).setOnClickListener(v -> dialogPilihEWallet());
    }

    // ──────────────────────────────────────────────────────────────
    //  Poin setup
    // ──────────────────────────────────────────────────────────────

    private void setupPoinCard(TextView tvTotal) {
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
                    totalAkhir = totalBelanja - diskonPoin;
                    tvTotal.setText(CurrencyFormatter.formatRupiah(totalAkhir));
                    tvInfo.setText("Diskon poin: -" + CurrencyFormatter.formatRupiah(diskonPoin));
                    tvInfo.setVisibility(View.VISIBLE);
                } else {
                    diskonPoin = 0;
                    poinDigunakan = 0;
                    totalAkhir = totalBelanja;
                    tvTotal.setText(CurrencyFormatter.formatRupiah(totalAkhir));
                    tvInfo.setVisibility(View.GONE);
                }
            });
        }
    }

    private void updatePoinPelanggan() {
        if (pelanggan == null) return;
        int poinDiperoleh = (int) (totalAkhir / POIN_PER_RUPIAH);
        dbHelper.updatePoinSetelahTransaksi(pelanggan.getId(), poinDigunakan, poinDiperoleh);
    }

    // ──────────────────────────────────────────────────────────────
    //  Payment dialogs
    // ──────────────────────────────────────────────────────────────

    private void dialogCash() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        MaterialButton btnUangPas = new MaterialButton(this);
        btnUangPas.setText("UANG PAS (" + CurrencyFormatter.formatRupiah(totalAkhir) + ")");
        btnUangPas.setCornerRadius(12);

        final TextInputEditText inputUang = new TextInputEditText(this);
        inputUang.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputUang.setHint("Masukkan Nominal Uang");

        btnUangPas.setOnClickListener(v -> inputUang.setText(String.format("%.0f", totalAkhir)));

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
        layout.addView(inputUang);

        new AlertDialog.Builder(this)
            .setTitle("Pembayaran Tunai")
            .setView(layout)
            .setPositiveButton("Bayar", (d, w) -> {
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
                    String kasirName0 = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
                    long transId = dbHelper.insertTransaksiDanKurangiStok(totalAkhir, "Tunai", detailTransaksi, kasirName0, cartItems);
                    updatePoinPelanggan();
                    tampilkanStruk(transId, "Tunai", bayar, kembalian);
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void dialogDebit() {
        new AlertDialog.Builder(this)
            .setTitle("Kartu Debit / Kredit")
            .setMessage("Silakan gesek/masukkan kartu pada mesin EDC sejumlah\n"
                + CurrencyFormatter.formatRupiah(totalAkhir))
            .setPositiveButton("Transaksi Berhasil", (d, w) -> {
                String kasirName1 = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
                long transId = dbHelper.insertTransaksiDanKurangiStok(totalAkhir, "Debit/Kredit", detailTransaksi, kasirName1, cartItems);
                updatePoinPelanggan();
                tampilkanStruk(transId, "Debit/Kredit", 0, 0);
            })
            .setNegativeButton("Batal", null)
            .show();
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

    // ──────────────────────────────────────────────────────────────
    //  Struk (receipt) bottom sheet
    // ──────────────────────────────────────────────────────────────

    private void tampilkanStruk(long transId, String metode, double bayar, double kembalian) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_struk, null);
        sheet.setContentView(root);
        sheet.setCancelable(false);

        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", new Locale("id", "ID"))
                .format(new Date());
        String kasirName = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");

        populateStrukView(root, transId, tanggal, kasirName, metode, bayar, kembalian);

        // Print button
        root.findViewById(R.id.strukBtnPrint).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode, bayar, kembalian);
            PrintUtils.printBitmap(PaymentActivity.this, bmp, "Struk_" + transId);
        });

        // WhatsApp direct share
        root.findViewById(R.id.strukBtnWhatsapp).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode, bayar, kembalian);
            shareViaWhatsApp(bmp, transId, metode, bayar, kembalian);
        });

        // Share button
        root.findViewById(R.id.strukBtnShare).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode, bayar, kembalian);
            shareReceiptBitmap(bmp, transId);
        });

        // Simpan PDF button
        root.findViewById(R.id.strukBtnCetak).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode, bayar, kembalian);
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
    //  Populate receipt view with all data
    // ──────────────────────────────────────────────────────────────

    private void populateStrukView(View root, long transId, String tanggal,
                                   String kasirName, String metode,
                                   double bayar, double kembalian) {
        ((TextView) root.findViewById(R.id.strukTvId)).setText("#" + (transId > 0 ? transId : "—"));
        ((TextView) root.findViewById(R.id.strukTvTanggal)).setText(tanggal);
        ((TextView) root.findViewById(R.id.strukTvKasir)).setText(kasirName);
        ((TextView) root.findViewById(R.id.strukTvMetode)).setText(metode);
        ((TextView) root.findViewById(R.id.strukTvTotal)).setText(CurrencyFormatter.formatRupiah(totalAkhir));

        if (metode.equals("Tunai")) {
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

        // Item name
        TextView tvNama = new TextView(this);
        tvNama.setText(item.getBarang().getNamaBarang());
        tvNama.setTextColor(Color.parseColor("#212121"));
        tvNama.setTextSize(13f);
        tvNama.setTypeface(null, android.graphics.Typeface.BOLD);

        // Size + qty + price
        TextView tvDetail = new TextView(this);
        String detail = "Size " + item.getUkuran()
                + "  ×  " + item.getQuantity() + " pcs"
                + "  @" + CurrencyFormatter.formatRupiah(item.getBarang().getHarga());
        tvDetail.setText(detail);
        tvDetail.setTextColor(Color.parseColor("#757575"));
        tvDetail.setTextSize(12f);

        // Subtotal
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

    private Bitmap buildReceiptBitmap(long transId, String tanggal, String kasirName,
                                      String metode, double bayar, double kembalian) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View printView = inflater.inflate(R.layout.layout_struk_content, null);
        populateStrukView(printView, transId, tanggal, kasirName, metode, bayar, kembalian);

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
                                   double bayar, double kembalian) {
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
            intent.putExtra(Intent.EXTRA_TEXT, buildWhatsAppText(transId, metode, bayar, kembalian));
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

    private String buildWhatsAppText(long transId, String metode, double bayar, double kembalian) {
        String kasirName = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");
        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"))
                .format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("*" + NAMA_TOKO + "*\n");
        sb.append("Struk #").append(transId).append("\n\n");
        sb.append("Tanggal : ").append(tanggal).append("\n");
        sb.append("Kasir   : ").append(kasirName).append("\n\n");
        sb.append("*Item Belanja:*\n");
        for (CartItem item : cartItems) {
            sb.append("• ").append(item.getBarang().getNamaBarang())
              .append(" (").append(item.getUkuran()).append(")")
              .append(" × ").append(item.getQuantity())
              .append(" = ").append(CurrencyFormatter.formatRupiah(item.getSubtotal())).append("\n");
        }
        if (diskonPoin > 0) {
            sb.append("\nSubtotal : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("\n");
            sb.append("Diskon Poin : -").append(CurrencyFormatter.formatRupiah(diskonPoin)).append("\n");
        }
        sb.append("\n*Total  : ").append(CurrencyFormatter.formatRupiah(totalAkhir)).append("*\n");
        sb.append("Metode  : ").append(metode).append("\n");
        if (metode.equals("Tunai")) {
            sb.append("Bayar   : ").append(CurrencyFormatter.formatRupiah(bayar)).append("\n");
            sb.append("Kembali : ").append(CurrencyFormatter.formatRupiah(kembalian)).append("\n");
        }
        sb.append("\nTerima kasih telah berbelanja!");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────
    //  Save receipt as PDF to device Downloads
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
    //  Plain-text receipt (sent alongside the image when sharing)
    // ──────────────────────────────────────────────────────────────

    private String buildTextReceipt(long transId) {
        StringBuilder sb = new StringBuilder();
        sb.append("====== " + NAMA_TOKO + " ======\n");
        sb.append("No. Transaksi : #").append(transId).append("\n");
        sb.append("Tanggal       : ")
          .append(new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID")).format(new Date()))
          .append("\n");
        sb.append("─────────────────────────\n");
        for (CartItem item : cartItems) {
            sb.append(item.getBarang().getNamaBarang())
              .append(" (").append(item.getUkuran()).append(")\n");
            sb.append("  ").append(item.getQuantity()).append(" × ")
              .append(CurrencyFormatter.formatRupiah(item.getBarang().getHarga()))
              .append(" = ").append(CurrencyFormatter.formatRupiah(item.getSubtotal())).append("\n");
        }
        sb.append("─────────────────────────\n");
        if (diskonPoin > 0) {
            sb.append("Subtotal      : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("\n");
            sb.append("Diskon Poin   : -").append(CurrencyFormatter.formatRupiah(diskonPoin)).append("\n");
        }
        sb.append("Total         : ").append(CurrencyFormatter.formatRupiah(totalAkhir)).append("\n");
        sb.append("=========================\n");
        sb.append("Terima kasih telah berbelanja!");
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            updatePoinPelanggan();
            setResult(RESULT_OK);
            finish();
        }
    }
}
