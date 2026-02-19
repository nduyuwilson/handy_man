package com.nduyuwilson.thitima.ui.catalogue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.util.Formatter;

public class ItemVariantAdapter extends ListAdapter<ItemVariant, ItemVariantAdapter.VariantViewHolder> {

    public interface OnVariantClickListener {
        void onVariantClick(ItemVariant variant);
    }

    private final OnVariantClickListener listener;

    public ItemVariantAdapter(OnVariantClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ItemVariant> DIFF_CALLBACK = new DiffUtil.ItemCallback<ItemVariant>() {
        @Override
        public boolean areItemsTheSame(@NonNull ItemVariant oldItem, @NonNull ItemVariant newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ItemVariant oldItem, @NonNull ItemVariant newItem) {
            return oldItem.getBrandName().equals(newItem.getBrandName()) &&
                    oldItem.getSellingPrice() == newItem.getSellingPrice() &&
                    (oldItem.getImageUri() == null ? newItem.getImageUri() == null : oldItem.getImageUri().equals(newItem.getImageUri()));
        }
    };

    @NonNull
    @Override
    public VariantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_variant, parent, false);
        return new VariantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VariantViewHolder holder, int position) {
        ItemVariant variant = getItem(position);
        holder.textViewBrand.setText(variant.getBrandName());
        holder.textViewPrice.setText(Formatter.formatPrice(holder.itemView.getContext(), variant.getSellingPrice()));

        Glide.with(holder.itemView.getContext())
                .load(variant.getImageUri())
                .placeholder(R.drawable.ic_splash_logo)
                .error(R.drawable.ic_splash_logo)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> listener.onVariantClick(variant));
    }

    static class VariantViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewBrand, textViewPrice;

        public VariantViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewVariant);
            textViewBrand = itemView.findViewById(R.id.textViewBrandName);
            textViewPrice = itemView.findViewById(R.id.textViewVariantPrice);
        }
    }
}
