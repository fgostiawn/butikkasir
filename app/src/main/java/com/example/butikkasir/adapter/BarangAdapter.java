package com.example.butikkasir.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.butikkasir.R;
import com.example.butikkasir.model.Barang;
import com.example.butikkasir.utils.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class BarangAdapter extends RecyclerView.Adapter<BarangAdapter.BarangViewHolder> {

    private List<Barang> listBarang;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onBarangClick(Barang barang);
    }

    public BarangAdapter(List<Barang> listBarang, OnItemClickListener listener) {
        this.listBarang = listBarang;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BarangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barang, parent, false);
        return new BarangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BarangViewHolder holder, int position) {
        Barang barang = listBarang.get(position);
        holder.tvNama.setText(barang.getNamaBarang());
        holder.tvDetail.setText(barang.getDetailBarang());

        // Memanggil format rupiah yang baru dibuat
        holder.tvHarga.setText(CurrencyFormatter.formatRupiah(barang.getHarga()));
        holder.imgBarang.setImageResource(barang.getGambarResId());

        holder.btnPilih.setOnClickListener(v -> listener.onBarangClick(barang));
        holder.itemView.setOnClickListener(v -> listener.onBarangClick(barang));
    }

    @Override
    public int getItemCount() {
        return listBarang.size();
    }

    public static class BarangViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBarang;
        TextView tvNama, tvDetail, tvHarga;
        MaterialButton btnPilih;

        public BarangViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBarang = itemView.findViewById(R.id.imgBarang);
            tvNama = itemView.findViewById(R.id.tvNamaBarang);
            tvDetail = itemView.findViewById(R.id.tvDetailBarang);
            tvHarga = itemView.findViewById(R.id.tvHargaBarang);
            btnPilih = itemView.findViewById(R.id.btnTambahKeranjang);
        }
    }
}