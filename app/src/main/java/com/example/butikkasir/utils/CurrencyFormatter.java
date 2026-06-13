package com.example.butikkasir.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CurrencyFormatter {
    public static String formatRupiah(double number) {
        DecimalFormat kursIndonesia = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols formatRp = new DecimalFormatSymbols();

        formatRp.setCurrencySymbol("Rp ");
        formatRp.setMonetaryDecimalSeparator(',');
        formatRp.setGroupingSeparator('.');

        kursIndonesia.setDecimalFormatSymbols(formatRp);
        // Mengatur format agar menampilkan titik ribuan tanpa angka desimal di belakang koma
        kursIndonesia.applyPattern("Rp #,##0");

        return kursIndonesia.format(number);
    }
}