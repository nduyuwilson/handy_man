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
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.util.Formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LabourActivityAdapter extends ListAdapter<LabourActivity, LabourActivityAdapter.LabourViewHolder> {

    public interface OnLabourClickListener {
        void onLabourClick(LabourActivity activity);
    }

    private final OnLabourClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    protected LabourActivityAdapter(OnLabourClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<LabourActivity> DIFF_CALLBACK = new DiffUtil.ItemCallback<LabourActivity>() {
        @Override
        public boolean areItemsTheSame(@NonNull LabourActivity oldItem, @NonNull LabourActivity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull LabourActivity oldItem, @NonNull LabourActivity newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getCost() == newItem.getCost() &&
                    oldItem.getDate() == newItem.getDate();
        }
    };

    @NonNull
    @Override
    public LabourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_component, parent, false);
        return new LabourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabourViewHolder holder, int position) {
        LabourActivity activity = getItem(position);
        holder.textViewName.setText(activity.getName());
        holder.textViewDetails.setText(dateFormat.format(new Date(activity.getDate())));
        holder.textViewTotal.setText(Formatter.formatPrice(holder.itemView.getContext(), activity.getCost()));
        
        holder.itemView.setOnClickListener(v -> listener.onLabourClick(activity));
    }

    static class LabourViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewDetails, textViewTotal;

        public LabourViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewComponentName);
            textViewDetails = itemView.findViewById(R.id.textViewComponentDetails);
            textViewTotal = itemView.findViewById(R.id.textViewComponentTotal);
        }
    }
}
