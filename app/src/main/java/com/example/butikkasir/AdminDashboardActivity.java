package com.example.butikkasir;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.butikkasir.database.DatabaseHelper;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("ButikSession", MODE_PRIVATE);
        String namaAdmin = prefs.getString("namaKasir", "Admin");

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText("Halo, " + namaAdmin + "!");

        MaterialCardView cardLaporan        = findViewById(R.id.cardLaporan);
        MaterialCardView cardRekap          = findViewById(R.id.cardRekap);
        MaterialCardView cardStok           = findViewById(R.id.cardStok);
        MaterialCardView cardBarang         = findViewById(R.id.cardBarang);
        MaterialCardView cardPelanggan      = findViewById(R.id.cardPelanggan);
        MaterialCardView cardKasir          = findViewById(R.id.cardKasir);
        MaterialCardView cardUbahProfil     = findViewById(R.id.cardUbahProfilAdmin);
        MaterialCardView cardBackup         = findViewById(R.id.cardBackup);
        MaterialCardView cardPengaturanToko = findViewById(R.id.cardPengaturanToko);
        MaterialCardView cardVoucher        = findViewById(R.id.cardVoucherAdmin);
        MaterialCardView cardHutang         = findViewById(R.id.cardHutangAdmin);

        cardLaporan.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanActivity.class)));
        cardRekap.setOnClickListener(v ->
                startActivity(new Intent(this, RekapLaporanActivity.class)));
        cardStok.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanStokActivity.class)));
        cardBarang.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenBarangActivity.class)));
        cardPelanggan.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenPelangganActivity.class)));
        cardKasir.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenKasirActivity.class)));
        cardUbahProfil.setOnClickListener(v -> showUbahProfilAdmin());
        cardBackup.setOnClickListener(v ->
                startActivity(new Intent(this, BackupActivity.class)));
        cardPengaturanToko.setOnClickListener(v -> showPengaturanToko());
        cardVoucher.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenVoucherActivity.class)));
        cardHutang.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenHutangActivity.class)));

        findViewById(R.id.btnLogoutAdmin).setOnClickListener(v -> konfirmasiLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("ButikSession", MODE_PRIVATE);
        String namaAdmin = prefs.getString("namaKasir", "Admin");
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText("Halo, " + namaAdmin + "!");
    }

    private void showUbahProfilAdmin() {
        SharedPreferences prefs = getSharedPreferences("ButikSession", MODE_PRIVATE);
        String usernameLogin = prefs.getString("username", "admin");

        Cursor c = dbHelper.getAdminByUsername(usernameLogin);
        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            Toast.makeText(this, "Data profil tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }
        int adminId         = c.getInt(c.getColumnIndexOrThrow("id_admin"));
        String namaLama     = c.getString(c.getColumnIndexOrThrow("nama_admin"));
        String usernameLama = c.getString(c.getColumnIndexOrThrow("username_admin"));
        String passwordLama = c.getString(c.getColumnIndexOrThrow("password_admin"));
        c.close();

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_form_kasir, null);
        EditText etNama     = view.findViewById(R.id.etNamaKasirForm);
        EditText etUsername = view.findViewById(R.id.etUsernameKasirForm);
        EditText etPassword = view.findViewById(R.id.etPasswordKasirForm);

        etNama.setText(namaLama);
        etUsername.setText(usernameLama);
        etPassword.setText(passwordLama);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ubah Profil Admin")
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

            boolean ok = dbHelper.updateAdmin(adminId, username, password, nama);
            if (ok) {
                prefs.edit()
                        .putString("namaKasir", nama)
                        .putString("username", username)
                        .apply();
                TextView tvGreeting = findViewById(R.id.tvGreeting);
                tvGreeting.setText("Halo, " + nama + "!");
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Gagal memperbarui profil. Username mungkin sudah digunakan.", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private void showPengaturanToko() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pengaturan_toko, null);
        EditText etNama    = view.findViewById(R.id.etNamaToko);
        EditText etWa      = view.findViewById(R.id.etNomorWA);
        EditText etTagline = view.findViewById(R.id.etTagline);

        etNama.setText(dbHelper.getPengaturan("nama_toko"));
        etWa.setText(dbHelper.getPengaturan("nomor_wa"));
        etTagline.setText(dbHelper.getPengaturan("tagline"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Pengaturan Toko")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nama    = etNama.getText().toString().trim();
            String wa      = etWa.getText().toString().trim();
            String tagline = etTagline.getText().toString().trim();

            if (nama.isEmpty()) {
                Toast.makeText(this, "Nama toko tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.setPengaturan("nama_toko", nama);
            dbHelper.setPengaturan("nomor_wa", wa);
            dbHelper.setPengaturan("tagline", tagline.isEmpty() ? "Terima kasih telah berbelanja!" : tagline);
            Toast.makeText(this, "Pengaturan toko disimpan", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void konfirmasiLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Yakin ingin keluar dari akun admin?")
                .setPositiveButton("Logout", (d, w) -> {
                    getSharedPreferences("ButikSession", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
