package com.example.butikkasir;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.butikkasir.database.DatabaseHelper;
import com.example.butikkasir.model.CartItem;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.example.butikkasir.utils.PdfSaver;
import com.example.butikkasir.utils.PrintUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QrisActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private double totalBelanja;
    private String detailTransaksi, eWalletName;
    private List<CartItem> cartItems;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qris);

        dbHelper = new DatabaseHelper(this);
        eWalletName = getIntent().getStringExtra("EWALLET");
        totalBelanja = getIntent().getDoubleExtra("TOTAL_BELANJA", 0);
        detailTransaksi = getIntent().getStringExtra("DETAIL_TRANSAKSI");
        if (detailTransaksi == null) detailTransaksi = "";

        //noinspection unchecked
        cartItems = (List<CartItem>) getIntent().getSerializableExtra("CART_ITEMS");
        if (cartItems == null) cartItems = new ArrayList<>();

        MaterialToolbar toolbar = findViewById(R.id.toolbarQris);
        toolbar.setNavigationOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            finish();
        });

        TextView tvEwallet = findViewById(R.id.qrisTvEwallet);
        TextView tvTotal   = findViewById(R.id.qrisTvTotal);
        TextView tvTimer   = findViewById(R.id.qrisTvTimer);
        ImageView imgBarcode = findViewById(R.id.qrisImgBarcode);

        tvEwallet.setText(eWalletName);
        tvTotal.setText(CurrencyFormatter.formatRupiah(totalBelanja));

        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qr = encoder.encodeBitmap(
                "QRIS-" + eWalletName + "-RP:" + (long) totalBelanja,
                BarcodeFormat.QR_CODE, 600, 600);
            imgBarcode.setImageBitmap(qr);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuat QR Code", Toast.LENGTH_SHORT).show();
        }

        countDownTimer = new CountDownTimer(300_000, 1000) {
            public void onTick(long ms) {
                tvTimer.setText(String.format("%02d:%02d", (ms / 1000) / 60, (ms / 1000) % 60));
            }
            public void onFinish() {
                tvTimer.setText("KEDALUWARSA");
                Toast.makeText(QrisActivity.this, "Waktu pembayaran habis!", Toast.LENGTH_LONG).show();
            }
        }.start();

        findViewById(R.id.qrisBtnKonfirmasi).setOnClickListener(v -> {
            countDownTimer.cancel();
            String metode = "QRIS (" + eWalletName + ")";
            String kasirQ = getSharedPreferences("ButikSession", MODE_PRIVATE).getString("namaKasir", "Kasir");
            long transId = dbHelper.insertTransaksiDanKurangiStok(totalBelanja, metode, detailTransaksi, kasirQ, cartItems);
            tampilkanStruk(transId, metode);
        });
    }

    // ──────────────────────────────────────────────────────────────
    //  Struk (receipt) bottom sheet
    // ──────────────────────────────────────────────────────────────

    private void tampilkanStruk(long transId, String metode) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = getLayoutInflater().inflate(R.layout.layout_struk, null);
        sheet.setContentView(root);
        sheet.setCancelable(false);

        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", new Locale("id", "ID"))
                .format(new Date());
        String kasirName = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");

        populateStrukView(root, transId, tanggal, kasirName, metode);

        root.findViewById(R.id.strukBtnPrint).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode);
            PrintUtils.printBitmap(QrisActivity.this, bmp, "Struk_" + transId);
        });

        root.findViewById(R.id.strukBtnWhatsapp).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode);
            shareViaWhatsApp(bmp, transId, metode);
        });

        root.findViewById(R.id.strukBtnShare).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode);
            shareReceiptBitmap(bmp, transId);
        });

        root.findViewById(R.id.strukBtnCetak).setOnClickListener(v -> {
            Bitmap bmp = buildReceiptBitmap(transId, tanggal, kasirName, metode);
            saveReceiptPdf(bmp, transId);
        });

        root.findViewById(R.id.strukBtnTutup).setOnClickListener(v -> {
            sheet.dismiss();
            setResult(RESULT_OK);
            finish();
        });

        sheet.show();
    }

    private void populateStrukView(View root, long transId, String tanggal,
                                   String kasirName, String metode) {
        // Update store header dari DB
        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko.isEmpty()) namaToko = "BUTIK DEA";
        String nomorWa = dbHelper.getPengaturan("nomor_wa");
        if (nomorWa.isEmpty()) nomorWa = "0812-XXXX-XXXX";
        String tagline = dbHelper.getPengaturan("tagline");
        if (tagline.isEmpty()) tagline = "Terima kasih telah berbelanja!";

        TextView tvNamaToko = root.findViewById(R.id.strukTvNamaToko);
        if (tvNamaToko != null) tvNamaToko.setText(namaToko);
        TextView tvWA = root.findViewById(R.id.strukTvNomorWA);
        if (tvWA != null) tvWA.setText("WA: " + nomorWa);
        TextView tvTagline = root.findViewById(R.id.strukTvTagline);
        if (tvTagline != null) tvTagline.setText(tagline);

        String dateStr = new SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new Date());
        String formattedId = transId > 0
                ? String.format("TRX-%s-%04d", dateStr, transId) : "—";

        ((TextView) root.findViewById(R.id.strukTvId)).setText(formattedId);
        ((TextView) root.findViewById(R.id.strukTvTanggal)).setText(tanggal);
        ((TextView) root.findViewById(R.id.strukTvKasir)).setText(kasirName);
        ((TextView) root.findViewById(R.id.strukTvMetode)).setText(metode);
        ((TextView) root.findViewById(R.id.strukTvTotal)).setText(CurrencyFormatter.formatRupiah(totalBelanja));

        LinearLayout itemContainer = root.findViewById(R.id.strukItemContainer);
        itemContainer.removeAllViews();
        for (CartItem item : cartItems) {
            addItemRow(itemContainer, item);
        }
    }

    private void addItemRow(LinearLayout container, CartItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dpToPx(10);
        row.setLayoutParams(params);

        TextView tvNama = new TextView(this);
        tvNama.setText(item.getBarang().getNamaBarang());
        tvNama.setTextColor(Color.parseColor("#212121"));
        tvNama.setTextSize(13f);
        tvNama.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDetail = new TextView(this);
        tvDetail.setText("Size " + item.getUkuran()
                + "  ×  " + item.getQuantity() + " pcs"
                + "  @" + CurrencyFormatter.formatRupiah(item.getBarang().getHarga()));
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
    //  Bitmap generation
    // ──────────────────────────────────────────────────────────────

    private Bitmap buildReceiptBitmap(long transId, String tanggal, String kasirName, String metode) {
        View printView = LayoutInflater.from(this).inflate(R.layout.layout_struk_content, null);
        populateStrukView(printView, transId, tanggal, kasirName, metode);

        int w = dpToPx(360);
        printView.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        printView.layout(0, 0, printView.getMeasuredWidth(), printView.getMeasuredHeight());

        Bitmap bmp = Bitmap.createBitmap(
                printView.getMeasuredWidth(), printView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        printView.draw(new Canvas(bmp));
        return bmp;
    }

    // ──────────────────────────────────────────────────────────────
    //  Share
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

    private void shareViaWhatsApp(Bitmap bmp, long transId, String metode) {
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
            intent.putExtra(Intent.EXTRA_TEXT, buildWhatsAppText(transId, metode));
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

    private String buildWhatsAppText(long transId, String metode) {
        String kasirName = getSharedPreferences("ButikSession", MODE_PRIVATE)
                .getString("namaKasir", "Kasir");
        String tanggal = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"))
                .format(new Date());
        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko.isEmpty()) namaToko = "BUTIK DEA";
        String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String formattedId = String.format("TRX-%s-%04d", dateStr, transId);
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(namaToko).append("*\n");
        sb.append(formattedId).append("\n\n");
        sb.append("Tanggal : ").append(tanggal).append("\n");
        sb.append("Kasir   : ").append(kasirName).append("\n\n");
        sb.append("*Item Belanja:*\n");
        for (CartItem item : cartItems) {
            sb.append("• ").append(item.getBarang().getNamaBarang())
              .append(" (").append(item.getUkuran()).append(")")
              .append(" × ").append(item.getQuantity())
              .append(" = ").append(CurrencyFormatter.formatRupiah(item.getSubtotal())).append("\n");
        }
        sb.append("\n*Total  : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("*\n");
        sb.append("Metode  : ").append(metode).append("\n");
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
                PdfSaver.showSuccessDialog(QrisActivity.this, uri, displayName, null);
            }
            @Override
            public void onError(String message) {
                Toast.makeText(QrisActivity.this,
                    "Gagal menyimpan PDF: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildTextReceipt(long transId) {
        String namaToko = dbHelper.getPengaturan("nama_toko");
        if (namaToko.isEmpty()) namaToko = "BUTIK DEA";
        String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String formattedId = String.format("TRX-%s-%04d", dateStr, transId);
        StringBuilder sb = new StringBuilder();
        sb.append("====== ").append(namaToko).append(" ======\n");
        sb.append("No. Transaksi : ").append(formattedId).append("\n");
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
        sb.append("Total         : ").append(CurrencyFormatter.formatRupiah(totalBelanja)).append("\n");
        sb.append("Metode        : QRIS (").append(eWalletName).append(")\n");
        sb.append("=========================\n");
        sb.append("Terima kasih telah berbelanja!");
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
