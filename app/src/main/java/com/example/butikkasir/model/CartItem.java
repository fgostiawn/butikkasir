package com.example.butikkasir.model;

public class CartItem implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Barang barang;
    private String ukuran;
    private int quantity;

    public CartItem(Barang barang, String ukuran, int quantity) {
        this.barang = barang;
        this.ukuran = ukuran;
        this.quantity = quantity;
    }

    public Barang getBarang() { return barang; }
    public String getUkuran() { return ukuran; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    // Subtotal = harga barang * jumlah
    public double getSubtotal() { return barang.getHarga() * quantity; }
}