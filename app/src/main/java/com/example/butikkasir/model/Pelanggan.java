package com.example.butikkasir.model;

public class Pelanggan implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String nama;
    private String noHp;
    private int poin;

    public Pelanggan(int id, String nama, String noHp, int poin) {
        this.id = id;
        this.nama = nama;
        this.noHp = noHp;
        this.poin = poin;
    }

    public int getId() { return id; }
    public String getNama() { return nama; }
    public String getNoHp() { return noHp; }
    public int getPoin() { return poin; }
    public void setPoin(int poin) { this.poin = poin; }
}
