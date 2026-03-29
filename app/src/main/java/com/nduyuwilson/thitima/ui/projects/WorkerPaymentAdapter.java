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
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.viewmodel.WorkerViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkerPaymentAdapter extends ListAdapter<WorkerPayment, WorkerPaymentAdapter.WorkerPaymentViewHolder> {

    public interface OnWorkerPaymentClickListener {
        void onPaymentClick(WorkerPayment payment);
    }

    private final OnWorkerPaymentClickListener listener;
    private final WorkerViewModel workerViewModel;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    protected WorkerPaymentAdapter(WorkerViewModel workerViewModel, OnWorkerPaymentClickListener listener) {
        super(DIFF_CALLBACK);
        this.workerViewModel = workerViewModel;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<WorkerPayment> DIFF_CALLBACK = new DiffUtil.ItemCallback<WorkerPayment>() {
        @Override
        public boolean areItemsTheSame(@NonNull WorkerPayment oldItem, @NonNull WorkerPayment newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull WorkerPayment oldItem, @NonNull WorkerPayment newItem) {
            return oldItem.getWage() == newItem.getWage() &&
                    oldItem.getTransport() == newItem.getTransport() &&
                    oldItem.getDate() == newItem.getDate();
        }
    };

    @NonNull
    @Override
    public WorkerPaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_component, parent, false);
        return new WorkerPaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerPaymentViewHolder holder, int position) {
        WorkerPayment payment = getItem(position);
        
        // Find worker name from ViewModel/List (simplified for now by observing)
        workerViewModel.getAllWorkers().observeForever(workers -> {
            if (workers != null) {
                for (Worker w : workers) {
                    if (w.getId() == payment.getWorkerId()) {
                        holder.textViewName.setText(w.getName());
                        break;
                    }
                }
            }
        });

        double total = payment.getWage() + payment.getTransport();
        holder.textViewDetails.setText(String.format(Locale.getDefault(), "Wage: %s | Trans: %s", 
                Formatter.formatNumber(payment.getWage()), Formatter.formatNumber(payment.getTransport())));
        holder.textViewTotal.setText(Formatter.formatPrice(holder.itemView.getContext(), total));
        
        holder.itemView.setOnClickListener(v -> listener.onPaymentClick(payment));
    }

    static class WorkerPaymentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewDetails, textViewTotal;

        public WorkerPaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewComponentName);
            textViewDetails = itemView.findViewById(R.id.textViewComponentDetails);
            textViewTotal = itemView.findViewById(R.id.textViewComponentTotal);
        }
    }
}
