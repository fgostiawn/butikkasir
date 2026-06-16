package com.example.butikkasir.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ButikDB";
    private static final int DATABASE_VERSION = 6;

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
                + KEY_KASIR_TRX + " TEXT DEFAULT 'Kasir'" + ")");

        db.execSQL(CREATE_TABLE_KASIR);
        db.execSQL("INSERT INTO " + TABLE_KASIR + " (username, password, nama_kasir) VALUES ('kasir', '12345', 'Kasir')");
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