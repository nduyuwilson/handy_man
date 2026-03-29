package com.nduyuwilson.thitima.ui.projects;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Payment;
import com.nduyuwilson.thitima.util.Formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentAdapter extends ListAdapter<Payment, PaymentAdapter.PaymentViewHolder> {

    public interface OnPaymentClickListener {
        void onPaymentClick(Payment payment);
    }

    private final OnPaymentClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    protected PaymentAdapter(OnPaymentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Payment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Payment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Payment oldItem, @NonNull Payment newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Payment oldItem, @NonNull Payment newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getReference().equals(newItem.getReference()) &&
                    oldItem.getDate() == newItem.getDate();
        }
    };

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_component, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = getItem(position);
        holder.textViewName.setText(payment.getMethod() + " Payment");
        holder.textViewDetails.setText(dateFormat.format(new Date(payment.getDate())) + " | Ref: " + payment.getReference());
        holder.textViewTotal.setText(Formatter.formatPrice(holder.itemView.getContext(), payment.getAmount()));
        holder.textViewTotal.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // Green for payments
        
        holder.itemView.setOnClickListener(v -> listener.onPaymentClick(payment));
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewDetails, textViewTotal;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewComponentName);
            textViewDetails = itemView.findViewById(R.id.textViewComponentDetails);
            textViewTotal = itemView.findViewById(R.id.textViewComponentTotal);
        }
    }
}
