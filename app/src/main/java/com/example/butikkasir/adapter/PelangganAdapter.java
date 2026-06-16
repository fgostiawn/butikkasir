package com.example.butikkasir.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.butikkasir.R;
import com.example.butikkasir.model.Pelanggan;

import java.util.List;

public class PelangganAdapter extends RecyclerView.Adapter<PelangganAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Pelanggan pelanggan);
    }

    private final List<Pelanggan> list;
    private final OnItemClickListener listener;

    public PelangganAdapter(List<Pelanggan> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pelanggan, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pelanggan p = list.get(position);
        String inisial = p.getNama().isEmpty() ? "P" : p.getNama().substring(0, 1).toUpperCase();
        holder.tvInisial.setText(inisial);
        holder.tvNama.setText(p.getNama());
        holder.tvNoHp.setText(p.getNoHp().isEmpty() ? "Tidak ada no. HP" : p.getNoHp());
        holder.tvPoin.setText(p.getPoin() + " poin");
        holder.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInisial, tvNama, tvNoHp, tvPoin;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInisial = itemView.findViewById(R.id.tvInisialPelanggan);
            tvNama = itemView.findViewById(R.id.tvNamaPelanggan);
            tvNoHp = itemView.findViewById(R.id.tvNoHpPelanggan);
            tvPoin = itemView.findViewById(R.id.tvPoinPelanggan);
        }
    }
}
