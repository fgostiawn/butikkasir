package com.example.butikkasir.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.butikkasir.R;
import com.example.butikkasir.model.CartItem;
import com.example.butikkasir.utils.CurrencyFormatter;
import java.util.List;

public class KeranjangAdapter extends RecyclerView.Adapter<KeranjangAdapter.CartViewHolder> {

    private List<CartItem> cartList;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onQuantityChanged();
    }

    public KeranjangAdapter(List<CartItem> cartList, OnCartChangeListener listener) {
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_keranjang, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.tvNama.setText(item.getBarang().getNamaBarang());

        // Menggunakan format rupiah untuk detail & subtotal
        String hargaSatuan = CurrencyFormatter.formatRupiah(item.getBarang().getHarga());
        holder.tvDetail.setText("Size: " + item.getUkuran() + " | @" + hargaSatuan);
        holder.tvSubtotal.setText(CurrencyFormatter.formatRupiah(item.getSubtotal()));

        holder.tvQty.setText(String.valueOf(item.getQuantity()));

        holder.btnPlus.setOnClickListener(v -> {
            int stokTersedia = item.getBarang().getStok();
            if (item.getQuantity() < stokTersedia) {
                item.setQuantity(item.getQuantity() + 1);
                notifyItemChanged(position);
                listener.onQuantityChanged();
            } else {
                Context ctx = holder.itemView.getContext();
                Toast.makeText(ctx, "Stok hanya " + stokTersedia + " pcs", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyItemChanged(position);
            } else {
                cartList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cartList.size());
            }
            listener.onQuantityChanged();
        });
    }

    @Override
    public int getItemCount() { return cartList.size(); }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvDetail, tvSubtotal, tvQty, btnPlus, btnMinus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama    = itemView.findViewById(R.id.cartTvNama);
            tvDetail  = itemView.findViewById(R.id.cartTvDetail);
            tvSubtotal = itemView.findViewById(R.id.cartTvSubtotal);
            tvQty     = itemView.findViewById(R.id.cartTvQty);
            btnPlus   = itemView.findViewById(R.id.btnCartPlus);
            btnMinus  = itemView.findViewById(R.id.btnCartMinus);
        }
    }
}