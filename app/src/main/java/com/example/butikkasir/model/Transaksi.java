package com.example.butikkasir.model;

public class Transaksi {
    private int idTransaksi;
    private String tanggal;
    private double totalBelanja;
    private String metodePembayaran;
    private String detailBarang;
    private String namaKasir;

    public Transaksi(int idTransaksi, String tanggal, double totalBelanja, String metodePembayaran, String detailBarang) {
        this(idTransaksi, tanggal, totalBelanja, metodePembayaran, detailBarang, "Kasir");
    }

    public Transaksi(int idTransaksi, String tanggal, double totalBelanja, String metodePembayaran, String detailBarang, String namaKasir) {
        this.idTransaksi = idTransaksi;
        this.tanggal = tanggal;
        this.totalBelanja = totalBelanja;
        this.metodePembayaran = metodePembayaran;
        this.detailBarang = detailBarang;
        this.namaKasir = namaKasir != null ? namaKasir : "Kasir";
    }

    public int getIdTransaksi() { return idTransaksi; }
    public String getTanggal() { return tanggal; }
    public double getTotalBelanja() { return totalBelanja; }
    public String getMetodePembayaran() { return metodePembayaran; }
    public String getDetailBarang() { return detailBarang; }
    public String getNamaKasir() { return namaKasir; }
}