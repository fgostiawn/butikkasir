package com.example.butikkasir.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.butikkasir.R;
import com.example.butikkasir.model.Transaksi;
import com.example.butikkasir.utils.CurrencyFormatter;
import java.util.List;

public class LaporanAdapter extends RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder> {

    private List<Transaksi> listTransaksi;

    public LaporanAdapter(List<Transaksi> listTransaksi) {
        this.listTransaksi = listTransaksi;
    }

    @NonNull
    @Override
    public LaporanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_laporan, parent, false);
        return new LaporanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LaporanViewHolder holder, int position) {
        Transaksi t = listTransaksi.get(position);
        holder.tvTanggal.setText(t.getTanggal());
        holder.tvMetode.setText(t.getMetodePembayaran());
        holder.tvDetail.setText(t.getDetailBarang());
        holder.tvId.setText("ID Transaksi: #" + t.getIdTransaksi());
        holder.tvTotal.setText(CurrencyFormatter.formatRupiah(t.getTotalBelanja()));
    }

    @Override
    public int getItemCount() { return listTransaksi.size(); }

    public static class LaporanViewHolder extends RecyclerView.ViewHolder {
        TextView tvTanggal, tvMetode, tvDetail, tvId, tvTotal;

        public LaporanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTanggal = itemView.findViewById(R.id.lapTvTanggal);
            tvMetode = itemView.findViewById(R.id.lapTvMetode);
            tvDetail = itemView.findViewById(R.id.lapTvDetail);
            tvId = itemView.findViewById(R.id.lapTvId);
            tvTotal = itemView.findViewById(R.id.lapTvTotal);
        }
    }
}