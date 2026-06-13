package com.example.butikkasir.model;

public class Transaksi {
    private int idTransaksi;
    private String tanggal;
    private double totalBelanja;
    private String metodePembayaran;
    private String detailBarang;

    public Transaksi(int idTransaksi, String tanggal, double totalBelanja, String metodePembayaran, String detailBarang) {
        this.idTransaksi = idTransaksi;
        this.tanggal = tanggal;
        this.totalBelanja = totalBelanja;
        this.metodePembayaran = metodePembayaran;
        this.detailBarang = detailBarang;
    }

    public int getIdTransaksi() { return idTransaksi; }
    public String getTanggal() { return tanggal; }
    public double getTotalBelanja() { return totalBelanja; }
    public String getMetodePembayaran() { return metodePembayaran; }
    public String getDetailBarang() { return detailBarang; }
}