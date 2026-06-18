package com.example.butikkasir;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.butikkasir.database.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("ButikSession", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);

        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            String role = sharedPreferences.getString("role", "");
            if (role.equals("kasir")) {
                startActivity(new Intent(this, KasirActivity.class));
                finish();
                return;
            } else if (role.equals("admin")) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> prosesLogin());
    }

    private void prosesLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- JALUR LOGIN KASIR (cek dari DB) ---
        String namaKasir = dbHelper.getKasirNama(username, password);
        if (namaKasir != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putString("role", "kasir");
            editor.putString("namaKasir", namaKasir);
            editor.putString("username", username);
            editor.apply();

            Toast.makeText(this, "Login Kasir Berhasil", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, KasirActivity.class));
            finish();

        // --- JALUR LOGIN ADMIN ---
        } else {
            String namaAdmin = dbHelper.getAdminNama(username, password);
            if (namaAdmin != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("role", "admin");
                editor.putString("namaKasir", namaAdmin);
                editor.putString("username", username);
                editor.apply();

                Toast.makeText(this, "Login Admin Berhasil", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Username atau Password salah!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // FUNGSI UNTUK MENUTUP KEYBOARD SAAT KLIK DI LUAR AREA TEKS
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}