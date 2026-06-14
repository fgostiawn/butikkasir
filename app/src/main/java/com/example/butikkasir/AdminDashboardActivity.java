package com.example.butikkasir;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        SharedPreferences prefs = getSharedPreferences("ButikSession", MODE_PRIVATE);
        String namaAdmin = prefs.getString("namaKasir", "Admin");

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText("Halo, " + namaAdmin + "!");

        MaterialCardView cardLaporan  = findViewById(R.id.cardLaporan);
        MaterialCardView cardRekap    = findViewById(R.id.cardRekap);
        MaterialCardView cardStok     = findViewById(R.id.cardStok);
        MaterialCardView cardBarang   = findViewById(R.id.cardBarang);
        MaterialCardView cardKasir    = findViewById(R.id.cardKasir);

        cardLaporan.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanActivity.class)));
        cardRekap.setOnClickListener(v ->
                startActivity(new Intent(this, RekapLaporanActivity.class)));
        cardStok.setOnClickListener(v ->
                startActivity(new Intent(this, LaporanStokActivity.class)));
        cardBarang.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenBarangActivity.class)));
        cardKasir.setOnClickListener(v ->
                startActivity(new Intent(this, ManajemenKasirActivity.class)));

        findViewById(R.id.btnLogoutAdmin).setOnClickListener(v -> konfirmasiLogout());
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
