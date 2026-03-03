package com.nduyuwilson.thitima.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.model.PaymentMethod;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.PaymentViewHolder> {

    private List<PaymentMethod> methods = new ArrayList<>();
    private boolean isEditing = false;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public void setMethods(List<PaymentMethod> methods) {
        this.methods = methods;
        notifyDataSetChanged();
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
        notifyDataSetChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_method, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentMethod method = methods.get(position);
        holder.textViewLabel.setText(method.label);
        holder.textViewDetails.setText(method.getDisplayText().replace(method.label + ": ", ""));
        
        switch (method.type) {
            case BANK: holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_agenda); break;
            case PAYBILL: holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_send); break;
            case TILL: holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_save); break;
        }

        holder.buttonDelete.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return methods.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewLabel, textViewDetails;
        ImageButton buttonDelete;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewPaymentType);
            textViewLabel = itemView.findViewById(R.id.textViewPaymentLabel);
            textViewDetails = itemView.findViewById(R.id.textViewPaymentDetails);
            buttonDelete = itemView.findViewById(R.id.buttonDeletePayment);
        }
    }
}
