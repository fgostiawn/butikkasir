package com.example.butikkasir.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ButikDB";
    private static final int DATABASE_VERSION = 9;

    // Table Barang
    private static final String TABLE_BARANG = "barang";
    private static final String KEY_ID_BARANG = "id_barang";
    private static final String KEY_NAMA_BARANG = "nama_barang";
    private static final String KEY_HARGA = "harga";
    private static final String KEY_STOK = "stok";
    private static final String KEY_DETAIL_BARANG = "detail_barang";
    private static final String KEY_UKURAN = "ukuran";
    private static final String KEY_KATEGORI = "kategori";
    private static final String KEY_GAMBAR_PATH = "gambar_path";

    // Table Transaksi (Laporan Kasir)
    private static final String TABLE_TRANSAKSI = "transaksi";
    private static final String KEY_ID_TRANS = "id_transaksi";
    private static final String KEY_TANGGAL = "tanggal";
    private static final String KEY_TOTAL = "total_belanja";
    private static final String KEY_METODE = "metode_pembayaran";
    private static final String KEY_DETAIL = "detail_barang";
    private static final String KEY_KASIR_TRX = "nama_kasir";

    // Table Pelanggan
    private static final String TABLE_PELANGGAN = "pelanggan";
    private static final String KEY_ID_PELANGGAN = "id_pelanggan";
    private static final String KEY_NAMA_PELANGGAN = "nama";
    private static final String KEY_NO_HP = "no_hp";
    private static final String KEY_POIN = "poin";

    private static final String CREATE_TABLE_PELANGGAN =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PELANGGAN + "("
            + KEY_ID_PELANGGAN + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_NAMA_PELANGGAN + " TEXT,"
            + KEY_NO_HP + " TEXT DEFAULT '',"
            + KEY_POIN + " INTEGER DEFAULT 0,"
            + "tanggal_daftar DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";

    // Table Admin
    private static final String TABLE_ADMIN = "admin";
    private static final String KEY_ID_ADMIN = "id_admin";
    private static final String KEY_USERNAME_ADMIN = "username_admin";
    private static final String KEY_PASSWORD_ADMIN = "password_admin";
    private static final String KEY_NAMA_ADMIN = "nama_admin";

    private static final String CREATE_TABLE_ADMIN =
            "CREATE TABLE IF NOT EXISTS " + TABLE_ADMIN + "("
            + KEY_ID_ADMIN + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USERNAME_ADMIN + " TEXT UNIQUE,"
            + KEY_PASSWORD_ADMIN + " TEXT,"
            + KEY_NAMA_ADMIN + " TEXT" + ")";

    // Table Kasir
    private static final String TABLE_KASIR = "kasir";
    private static final String KEY_ID_KASIR = "id_kasir";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NAMA_KASIR = "nama_kasir";

    private static final String CREATE_TABLE_KASIR =
            "CREATE TABLE IF NOT EXISTS " + TABLE_KASIR + "("
            + KEY_ID_KASIR + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USERNAME + " TEXT UNIQUE,"
            + KEY_PASSWORD + " TEXT,"
            + KEY_NAMA_KASIR + " TEXT" + ")";

    // Table Pengaturan Toko
    private static final String TABLE_PENGATURAN = "pengaturan_toko";

    private static final String CREATE_TABLE_PENGATURAN =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PENGATURAN + "("
            + "key TEXT PRIMARY KEY, value TEXT)";

    // Table Voucher
    private static final String TABLE_VOUCHER = "voucher";

    private static final String CREATE_TABLE_VOUCHER =
            "CREATE TABLE IF NOT EXISTS " + TABLE_VOUCHER + "("
            + "id_voucher INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "kode TEXT UNIQUE,"
            + "jenis TEXT,"
            + "nilai REAL,"
            + "min_belanja REAL DEFAULT 0,"
            + "max_penggunaan INTEGER DEFAULT 0,"
            + "sudah_dipakai INTEGER DEFAULT 0,"
            + "aktif INTEGER DEFAULT 1)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_BARANG + "("
                + KEY_ID_BARANG + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAMA_BARANG + " TEXT,"
                + KEY_HARGA + " REAL,"
                + KEY_STOK + " INTEGER,"
                + KEY_DETAIL_BARANG + " TEXT DEFAULT '',"
                + KEY_UKURAN + " TEXT DEFAULT 'S,M,L,XL',"
                + KEY_KATEGORI + " TEXT DEFAULT 'Lainnya',"
                + KEY_GAMBAR_PATH + " TEXT DEFAULT ''" + ")");

        db.execSQL("CREATE TABLE " + TABLE_TRANSAKSI + "("
                + KEY_ID_TRANS + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TANGGAL + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_TOTAL + " REAL,"
                + KEY_METODE + " TEXT,"
                + KEY_DETAIL + " TEXT,"
                + KEY_KASIR_TRX + " TEXT DEFAULT 'Kasir',"
                + "status_transaksi TEXT DEFAULT 'LUNAS',"
                + "id_pelanggan_trx INTEGER DEFAULT -1" + ")");

        db.execSQL(CREATE_TABLE_KASIR);
        db.execSQL("INSERT INTO " + TABLE_KASIR + " (username, password, nama_kasir) VALUES ('kasir', '12345', 'Kasir')");
        db.execSQL(CREATE_TABLE_PELANGGAN);
        db.execSQL(CREATE_TABLE_ADMIN);
        db.execSQL("INSERT INTO " + TABLE_ADMIN + " (username_admin, password_admin, nama_admin) VALUES ('admin', 'admin123', 'Admin')");
        db.execSQL(CREATE_TABLE_PENGATURAN);
        db.execSQL("INSERT INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('nama_toko', 'BUTIK DEA')");
        db.execSQL("INSERT INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('nomor_wa', '0812-XXXX-XXXX')");
        db.execSQL("INSERT INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('tagline', 'Terima kasih telah berbelanja!')");
        db.execSQL(CREATE_TABLE_VOUCHER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_KASIR);
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_KASIR
                    + " (username, password, nama_kasir) VALUES ('kasir', '12345', 'Kasir')");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_BARANG + " ADD COLUMN " + KEY_DETAIL_BARANG + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_BARANG + " ADD COLUMN " + KEY_UKURAN + " TEXT DEFAULT 'S,M,L,XL'");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_BARANG + " ADD COLUMN " + KEY_KATEGORI + " TEXT DEFAULT 'Lainnya'");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSAKSI + " ADD COLUMN " + KEY_KASIR_TRX + " TEXT DEFAULT 'Kasir'");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_BARANG + " ADD COLUMN " + KEY_GAMBAR_PATH + " TEXT DEFAULT ''");
        }
        if (oldVersion < 7) {
            db.execSQL(CREATE_TABLE_PELANGGAN);
        }
        if (oldVersion < 8) {
            db.execSQL(CREATE_TABLE_ADMIN);
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_ADMIN
                    + " (username_admin, password_admin, nama_admin) VALUES ('admin', 'admin123', 'Admin')");
        }
        if (oldVersion < 9) {
            try { db.execSQL("ALTER TABLE " + TABLE_TRANSAKSI + " ADD COLUMN status_transaksi TEXT DEFAULT 'LUNAS'"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_TRANSAKSI + " ADD COLUMN id_pelanggan_trx INTEGER DEFAULT -1"); } catch (Exception ignored) {}
            db.execSQL(CREATE_TABLE_PENGATURAN);
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('nama_toko', 'BUTIK DEA')");
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('nomor_wa', '0812-XXXX-XXXX')");
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_PENGATURAN + " (key, value) VALUES ('tagline', 'Terima kasih telah berbelanja!')");
            db.execSQL(CREATE_TABLE_VOUCHER);
        }
    }

    // --- CRUD BARANG ---

    public boolean insertBarang(String nama, double harga, int stok, String detail, String ukuran, String kategori, String gambarPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAMA_BARANG, nama);
        values.put(KEY_HARGA, harga);
        values.put(KEY_STOK, stok);
        values.put(KEY_DETAIL_BARANG, detail);
        values.put(KEY_UKURAN, ukuran);
        values.put(KEY_KATEGORI, kategori != null ? kategori : "Lainnya");
        values.put(KEY_GAMBAR_PATH, gambarPath != null ? gambarPath : "");
        return db.insert(TABLE_BARANG, null, values) != -1;
    }

    public Cursor getAllBarang() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_BARANG + " ORDER BY " + KEY_NAMA_BARANG + " ASC", null);
    }

    public Cursor getBarangAktif() {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_BARANG + " WHERE " + KEY_STOK + " > 0 ORDER BY " + KEY_NAMA_BARANG + " ASC", null);
    }

    public boolean updateBarang(int id, String nama, double harga, int stok, String detail, String ukuran, String kategori, String gambarPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAMA_BARANG, nama);
        values.put(KEY_HARGA, harga);
        values.put(KEY_STOK, stok);
        values.put(KEY_DETAIL_BARANG, detail);
        values.put(KEY_UKURAN, ukuran);
        values.put(KEY_KATEGORI, kategori != null ? kategori : "Lainnya");
        values.put(KEY_GAMBAR_PATH, gambarPath != null ? gambarPath : "");
        return db.update(TABLE_BARANG, values, KEY_ID_BARANG + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteBarang(int id) {
        return this.getWritableDatabase()
                .delete(TABLE_BARANG, KEY_ID_BARANG + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public void kurangiStok(int idBarang, int qty) {
        this.getWritableDatabase().execSQL(
                "UPDATE " + TABLE_BARANG + " SET " + KEY_STOK + " = MAX(0, " + KEY_STOK + " - ?) WHERE " + KEY_ID_BARANG + " = ?",
                new Object[]{qty, idBarang});
    }

    // --- CRUD KASIR ---

    public boolean insertKasir(String username, String password, String namaKasir) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_NAMA_KASIR, namaKasir);
        long result = db.insert(TABLE_KASIR, null, values);
        return result != -1;
    }

    public Cursor getAllKasir() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_KASIR, null);
    }

    public boolean updateKasir(int id, String username, String password, String namaKasir) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        values.put(KEY_NAMA_KASIR, namaKasir);
        return db.update(TABLE_KASIR, values, KEY_ID_KASIR + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteKasir(int id) {
        return this.getWritableDatabase()
                .delete(TABLE_KASIR, KEY_ID_KASIR + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getKasirByUsername(String username) {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_KASIR + " WHERE " + KEY_USERNAME + " = ?",
                new String[]{username});
    }

    public String getKasirNama(String username, String password) {
        Cursor c = this.getReadableDatabase().rawQuery(
                "SELECT " + KEY_NAMA_KASIR + " FROM " + TABLE_KASIR
                        + " WHERE " + KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?",
                new String[]{username, password});
        if (c != null && c.moveToFirst()) {
            String nama = c.getString(0);
            c.close();
            return nama;
        }
        if (c != null) c.close();
        return null;
    }

    // --- CRUD ADMIN ---

    public String getAdminNama(String username, String password) {
        Cursor c = this.getReadableDatabase().rawQuery(
                "SELECT " + KEY_NAMA_ADMIN + " FROM " + TABLE_ADMIN
                        + " WHERE " + KEY_USERNAME_ADMIN + "=? AND " + KEY_PASSWORD_ADMIN + "=?",
                new String[]{username, password});
        if (c != null && c.moveToFirst()) {
            String nama = c.getString(0);
            c.close();
            return nama;
        }
        if (c != null) c.close();
        return null;
    }

    public Cursor getAdminByUsername(String username) {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_ADMIN + " WHERE " + KEY_USERNAME_ADMIN + " = ?",
                new String[]{username});
    }

    public boolean updateAdmin(int id, String username, String password, String nama) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME_ADMIN, username);
        values.put(KEY_PASSWORD_ADMIN, password);
        values.put(KEY_NAMA_ADMIN, nama);
        return db.update(TABLE_ADMIN, values, KEY_ID_ADMIN + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    // --- FITUR KASIR & LAPORAN ---

    public long insertTransaksiDanKurangiStok(double total, String metode, String detail, String kasir,
                                              java.util.List<com.example.butikkasir.model.CartItem> cartItems) {
        long transId = insertTransaksi(total, metode, detail, kasir);
        if (transId > 0 && cartItems != null) {
            for (com.example.butikkasir.model.CartItem item : cartItems) {
                kurangiStok(item.getBarang().getId(), item.getQuantity());
            }
        }
        return transId;
    }

    public long insertTransaksi(double total, String metode, String detail, String kasir) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TOTAL, total);
        values.put(KEY_METODE, metode);
        values.put(KEY_DETAIL, detail);
        values.put(KEY_KASIR_TRX, kasir != null ? kasir : "Kasir");
        return db.insert(TABLE_TRANSAKSI, null, values);
    }

    public Cursor getLaporanKasir() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRANSAKSI + " ORDER BY " + KEY_TANGGAL + " DESC", null);
    }
    // Mengambil laporan dengan filter tanggal (YYYY-MM-DD)
    public Cursor getLaporanByDate(String dateStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (dateStr == null || dateStr.isEmpty()) {
            return db.rawQuery("SELECT * FROM " + TABLE_TRANSAKSI + " ORDER BY " + KEY_TANGGAL + " DESC", null);
        } else {
            return db.rawQuery("SELECT * FROM " + TABLE_TRANSAKSI + " WHERE date(" + KEY_TANGGAL + ") = ? ORDER BY " + KEY_TANGGAL + " DESC", new String[]{dateStr});
        }
    }

    // Filter berdasarkan rentang tanggal (from / to bisa kosong = tidak ada batas)
    public Cursor getLaporanByDateRange(String from, String to) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean hasFrom = from != null && !from.isEmpty();
        boolean hasTo   = to   != null && !to.isEmpty();
        String base = "SELECT * FROM " + TABLE_TRANSAKSI;
        String order = " ORDER BY " + KEY_TANGGAL + " DESC";
        if (!hasFrom && !hasTo) {
            return db.rawQuery(base + order, null);
        } else if (hasFrom && hasTo) {
            return db.rawQuery(base + " WHERE date(" + KEY_TANGGAL + ") BETWEEN ? AND ?" + order, new String[]{from, to});
        } else if (hasFrom) {
            return db.rawQuery(base + " WHERE date(" + KEY_TANGGAL + ") >= ?" + order, new String[]{from});
        } else {
            return db.rawQuery(base + " WHERE date(" + KEY_TANGGAL + ") <= ?" + order, new String[]{to});
        }
    }

    // Ringkasan pendapatan per metode pembayaran
    public Cursor getMetodeSummary(String from, String to) {
        return getMetodeSummaryFiltered(from, to, null);
    }

    // ── Filtered queries (date + kasir, kategori filtered in-memory) ──

    public Cursor getLaporanFiltered(String from, String to, String kasir) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean hasFrom  = from  != null && !from.isEmpty();
        boolean hasTo    = to    != null && !to.isEmpty();
        boolean hasKasir = kasir != null && !kasir.isEmpty() && !"Semua".equals(kasir);

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(TABLE_TRANSAKSI);
        java.util.List<String> args = new java.util.ArrayList<>();
        boolean hasWhere = false;

        if (hasFrom && hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") BETWEEN ? AND ?");
            args.add(from); args.add(to); hasWhere = true;
        } else if (hasFrom) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") >= ?");
            args.add(from); hasWhere = true;
        } else if (hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") <= ?");
            args.add(to); hasWhere = true;
        }

        if (hasKasir) {
            sql.append(hasWhere ? " AND " : " WHERE ").append(KEY_KASIR_TRX).append(" = ?");
            args.add(kasir);
        }

        sql.append(" ORDER BY ").append(KEY_TANGGAL).append(" DESC");
        return db.rawQuery(sql.toString(), args.toArray(new String[0]));
    }

    public Cursor getMetodeSummaryFiltered(String from, String to, String kasir) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean hasFrom  = from  != null && !from.isEmpty();
        boolean hasTo    = to    != null && !to.isEmpty();
        boolean hasKasir = kasir != null && !kasir.isEmpty() && !"Semua".equals(kasir);

        StringBuilder sql = new StringBuilder(
            "SELECT " + KEY_METODE + ", COUNT(*) AS jml, SUM(" + KEY_TOTAL + ") AS total FROM " + TABLE_TRANSAKSI);
        java.util.List<String> args = new java.util.ArrayList<>();
        boolean hasWhere = false;

        if (hasFrom && hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") BETWEEN ? AND ?");
            args.add(from); args.add(to); hasWhere = true;
        } else if (hasFrom) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") >= ?");
            args.add(from); hasWhere = true;
        } else if (hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") <= ?");
            args.add(to); hasWhere = true;
        }

        if (hasKasir) {
            sql.append(hasWhere ? " AND " : " WHERE ").append(KEY_KASIR_TRX).append(" = ?");
            args.add(kasir);
        }

        sql.append(" GROUP BY ").append(KEY_METODE);
        return db.rawQuery(sql.toString(), args.toArray(new String[0]));
    }

    // Returns daily sales: date(tanggal), SUM(total_belanja)
    public Cursor getSalesPerDay(String from, String to, String kasir) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean hasFrom  = from  != null && !from.isEmpty();
        boolean hasTo    = to    != null && !to.isEmpty();
        boolean hasKasir = kasir != null && !kasir.isEmpty() && !"Semua".equals(kasir);

        StringBuilder sql = new StringBuilder(
            "SELECT date(" + KEY_TANGGAL + ") AS day, SUM(" + KEY_TOTAL + ") AS total FROM " + TABLE_TRANSAKSI);
        java.util.List<String> args = new java.util.ArrayList<>();
        boolean hasWhere = false;

        if (hasFrom && hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") BETWEEN ? AND ?");
            args.add(from); args.add(to); hasWhere = true;
        } else if (hasFrom) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") >= ?");
            args.add(from); hasWhere = true;
        } else if (hasTo) {
            sql.append(" WHERE date(").append(KEY_TANGGAL).append(") <= ?");
            args.add(to); hasWhere = true;
        }

        if (hasKasir) {
            sql.append(hasWhere ? " AND " : " WHERE ").append(KEY_KASIR_TRX).append(" = ?");
            args.add(kasir);
        }

        sql.append(" GROUP BY day ORDER BY day ASC LIMIT 30");
        return db.rawQuery(sql.toString(), args.toArray(new String[0]));
    }

    // Returns distinct kasir names that appear in transaksi
    public java.util.List<String> getDistinctKasirFromTransaksi() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("Semua");
        Cursor c = this.getReadableDatabase().rawQuery(
            "SELECT DISTINCT " + KEY_KASIR_TRX + " FROM " + TABLE_TRANSAKSI + " ORDER BY " + KEY_KASIR_TRX, null);
        if (c != null && c.moveToFirst()) {
            do { list.add(c.getString(0)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    // Returns distinct kategori from barang
    public java.util.List<String> getAllKategoriFromBarang() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("Semua");
        Cursor c = this.getReadableDatabase().rawQuery(
            "SELECT DISTINCT " + KEY_KATEGORI + " FROM " + TABLE_BARANG + " ORDER BY " + KEY_KATEGORI, null);
        if (c != null && c.moveToFirst()) {
            do { list.add(c.getString(0)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    // --- CRUD PELANGGAN ---

    public boolean insertPelanggan(String nama, String noHp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAMA_PELANGGAN, nama);
        values.put(KEY_NO_HP, noHp != null ? noHp : "");
        values.put(KEY_POIN, 0);
        return db.insert(TABLE_PELANGGAN, null, values) != -1;
    }

    public Cursor getAllPelanggan() {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_PELANGGAN + " ORDER BY " + KEY_NAMA_PELANGGAN + " ASC", null);
    }

    public Cursor searchPelanggan(String query) {
        String q = "%" + query + "%";
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_PELANGGAN
                + " WHERE " + KEY_NAMA_PELANGGAN + " LIKE ? OR " + KEY_NO_HP + " LIKE ?"
                + " ORDER BY " + KEY_NAMA_PELANGGAN + " ASC",
                new String[]{q, q});
    }

    public Cursor getPelangganById(int id) {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_PELANGGAN + " WHERE " + KEY_ID_PELANGGAN + " = ?",
                new String[]{String.valueOf(id)});
    }

    public void updatePoinSetelahTransaksi(int idPelanggan, int poinDigunakan, int poinDiperoleh) {
        this.getWritableDatabase().execSQL(
                "UPDATE " + TABLE_PELANGGAN
                + " SET " + KEY_POIN + " = MAX(0, " + KEY_POIN + " - ? + ?)"
                + " WHERE " + KEY_ID_PELANGGAN + " = ?",
                new Object[]{poinDigunakan, poinDiperoleh, idPelanggan});
    }

    public boolean updatePelanggan(int id, String nama, String noHp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAMA_PELANGGAN, nama);
        values.put(KEY_NO_HP, noHp != null ? noHp : "");
        return db.update(TABLE_PELANGGAN, values, KEY_ID_PELANGGAN + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deletePelanggan(int id) {
        return this.getWritableDatabase()
                .delete(TABLE_PELANGGAN, KEY_ID_PELANGGAN + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    // Returns barang names that belong to a given kategori
    public java.util.List<String> getBarangNamesByKategori(String kategori) {
        java.util.List<String> list = new java.util.ArrayList<>();
        Cursor c = this.getReadableDatabase().rawQuery(
            "SELECT " + KEY_NAMA_BARANG + " FROM " + TABLE_BARANG + " WHERE " + KEY_KATEGORI + " = ?",
            new String[]{kategori});
        if (c != null && c.moveToFirst()) {
            do { list.add(c.getString(0)); } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    // --- PENGATURAN TOKO ---

    public String getPengaturan(String key) {
        Cursor c = this.getReadableDatabase().rawQuery(
                "SELECT value FROM " + TABLE_PENGATURAN + " WHERE key = ?", new String[]{key});
        if (c != null && c.moveToFirst()) {
            String v = c.getString(0);
            c.close();
            return v != null ? v : "";
        }
        if (c != null) c.close();
        return "";
    }

    public void setPengaturan(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value != null ? value : "");
        this.getWritableDatabase().insertWithOnConflict(
                TABLE_PENGATURAN, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // --- VOUCHER ---

    public boolean insertVoucher(String kode, String jenis, double nilai, double minBelanja, int maxPenggunaan) {
        ContentValues cv = new ContentValues();
        cv.put("kode", kode.toUpperCase());
        cv.put("jenis", jenis);
        cv.put("nilai", nilai);
        cv.put("min_belanja", minBelanja);
        cv.put("max_penggunaan", maxPenggunaan);
        return this.getWritableDatabase().insert(TABLE_VOUCHER, null, cv) != -1;
    }

    public Cursor getAllVoucher() {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_VOUCHER + " ORDER BY id_voucher DESC", null);
    }

    public Cursor getVoucherByKode(String kode) {
        return this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_VOUCHER + " WHERE kode = ? AND aktif = 1",
                new String[]{kode.toUpperCase()});
    }

    public void incrementVoucherPakai(int id) {
        this.getWritableDatabase().execSQL(
                "UPDATE " + TABLE_VOUCHER + " SET sudah_dipakai = sudah_dipakai + 1 WHERE id_voucher = ?",
                new Object[]{id});
    }

    public boolean deleteVoucher(int id) {
        return this.getWritableDatabase()
                .delete(TABLE_VOUCHER, "id_voucher = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // --- TRANSAKSI FULL (with status & pelanggan) ---

    public long insertTransaksiDanKurangiStokFull(double total, String metode, String detail,
                                                   String kasir, String status, int idPelanggan,
                                                   java.util.List<com.example.butikkasir.model.CartItem> cartItems) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_TOTAL, total);
        cv.put(KEY_METODE, metode);
        cv.put(KEY_DETAIL, detail);
        cv.put(KEY_KASIR_TRX, kasir != null ? kasir : "Kasir");
        cv.put("status_transaksi", status != null ? status : "LUNAS");
        cv.put("id_pelanggan_trx", idPelanggan);
        long transId = db.insert(TABLE_TRANSAKSI, null, cv);
        if (transId > 0 && cartItems != null) {
            for (com.example.butikkasir.model.CartItem item : cartItems) {
                kurangiStok(item.getBarang().getId(), item.getQuantity());
            }
        }
        return transId;
    }

    // --- HUTANG ---

    public Cursor getHutangList() {
        return this.getReadableDatabase().rawQuery(
                "SELECT t.id_transaksi, t.tanggal, t.total_belanja, t.metode_pembayaran, "
                + "t.nama_kasir, t.id_pelanggan_trx, "
                + "COALESCE(p.nama, 'Tamu') AS nama_pelanggan "
                + "FROM " + TABLE_TRANSAKSI + " t "
                + "LEFT JOIN " + TABLE_PELANGGAN + " p ON t.id_pelanggan_trx = p.id_pelanggan "
                + "WHERE t.status_transaksi = 'HUTANG' ORDER BY t.tanggal DESC", null);
    }

    public boolean lunasiHutang(int idTransaksi) {
        ContentValues cv = new ContentValues();
        cv.put("status_transaksi", "LUNAS");
        return this.getWritableDatabase()
                .update(TABLE_TRANSAKSI, cv, "id_transaksi = ?",
                        new String[]{String.valueOf(idTransaksi)}) > 0;
    }

    // --- RIWAYAT KASIR HARI INI ---

    public Cursor getTodayTransaksiByKasir(String kasir) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        return getLaporanFiltered(today, today, kasir);
    }

    // Returns all barang ordered by stok ASC (low stock first)
    public Cursor getAllBarangForStok(String kategori) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (kategori == null || "Semua".equals(kategori)) {
            return db.rawQuery("SELECT * FROM " + TABLE_BARANG + " ORDER BY " + KEY_STOK + " ASC, " + KEY_NAMA_BARANG + " ASC", null);
        } else {
            return db.rawQuery("SELECT * FROM " + TABLE_BARANG + " WHERE " + KEY_KATEGORI + " = ? ORDER BY " + KEY_STOK + " ASC, " + KEY_NAMA_BARANG + " ASC",
                new String[]{kategori});
        }
    }
}