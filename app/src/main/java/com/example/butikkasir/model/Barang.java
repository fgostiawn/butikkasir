package com.example.butikkasir.model;

public class Barang implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String namaBarang;
    private String detailBarang;
    private double harga;
    private int gambarResId;
    private String[] ukuranTersedia;
    private int stok;

    public Barang(int id, String namaBarang, String detailBarang, double harga, int gambarResId, String[] ukuranTersedia, int stok) {
        this.id = id;
        this.namaBarang = namaBarang;
        this.detailBarang = detailBarang;
        this.harga = harga;
        this.gambarResId = gambarResId;
        this.ukuranTersedia = ukuranTersedia;
        this.stok = stok;
    }

    public int getId() { return id; }
    public String getNamaBarang() { return namaBarang; }
    public String getDetailBarang() { return detailBarang; }
    public double getHarga() { return harga; }
    public int getGambarResId() { return gambarResId; }
    public String[] getUkuranTersedia() { return ukuranTersedia; }
    public int getStok() { return stok; }
}