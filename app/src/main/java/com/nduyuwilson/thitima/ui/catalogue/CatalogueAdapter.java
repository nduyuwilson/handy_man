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
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.util.Formatter;

public class CatalogueAdapter extends ListAdapter<Item, CatalogueAdapter.CatalogueViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Item item);
    }

    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public CatalogueAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getDescription().equals(newItem.getDescription()) &&
                    oldItem.getSellingPrice() == newItem.getSellingPrice() &&
                    (oldItem.getImageUri() == null ? newItem.getImageUri() == null : oldItem.getImageUri().equals(newItem.getImageUri()));
        }
    };

    @NonNull
    @Override
    public CatalogueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_catalogue, parent, false);
        return new CatalogueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CatalogueViewHolder holder, int position) {
        Item item = getItem(position);
        holder.textViewItemName.setText(item.getName());
        holder.textViewItemDescription.setText(item.getDescription());
        holder.textViewItemPrice.setText(Formatter.formatPrice(holder.itemView.getContext(), item.getSellingPrice()));

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUri())
                .placeholder(R.drawable.ic_splash_logo)
                .error(R.drawable.ic_splash_logo)
                .into(holder.imageViewItem);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    public static class CatalogueViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageViewItem;
        public TextView textViewItemName, textViewItemDescription, textViewItemPrice;

        public CatalogueViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
            textViewItemName = itemView.findViewById(R.id.textViewItemName);
            textViewItemDescription = itemView.findViewById(R.id.textViewItemDescription);
            textViewItemPrice = itemView.findViewById(R.id.textViewItemPrice);
        }
    }
}
