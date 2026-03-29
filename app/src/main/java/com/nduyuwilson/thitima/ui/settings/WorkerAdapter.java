package com.nduyuwilson.thitima.ui.settings;

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

public class WorkerAdapter extends ListAdapter<Worker, WorkerAdapter.WorkerViewHolder> {

    public interface OnWorkerClickListener {
        void onWorkerClick(Worker worker);
    }

    private final OnWorkerClickListener listener;

    protected WorkerAdapter(OnWorkerClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Worker> DIFF_CALLBACK = new DiffUtil.ItemCallback<Worker>() {
        @Override
        public boolean areItemsTheSame(@NonNull Worker oldItem, @NonNull Worker newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Worker oldItem, @NonNull Worker newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getPhone().equals(newItem.getPhone()) &&
                    oldItem.getRole().equals(newItem.getRole());
        }
    };

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Worker worker = getItem(position);
        holder.tvName.setText(worker.getName());
        holder.tvRole.setText(worker.getRole() + " • " + worker.getPhone());
        holder.itemView.setOnClickListener(v -> listener.onWorkerClick(worker));
    }

    static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole;

        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWorkerName);
            tvRole = itemView.findViewById(R.id.tvWorkerRole);
        }
    }
}
