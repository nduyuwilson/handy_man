package com.nduyuwilson.thitima.ui.projects;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

import java.util.Locale;

public class ProjectItemAdapter extends ListAdapter<ProjectItem, ProjectItemAdapter.ProjectItemViewHolder> {

    private final ItemViewModel itemViewModel;
    private final LifecycleOwner lifecycleOwner;

    protected ProjectItemAdapter(ItemViewModel itemViewModel, LifecycleOwner lifecycleOwner) {
        super(DIFF_CALLBACK);
        this.itemViewModel = itemViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    private static final DiffUtil.ItemCallback<ProjectItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProjectItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProjectItem oldItem, @NonNull ProjectItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProjectItem oldItem, @NonNull ProjectItem newItem) {
            return oldItem.getQuantity() == newItem.getQuantity() &&
                    oldItem.getQuotedPrice() == newItem.getQuotedPrice();
        }
    };

    @NonNull
    @Override
    public ProjectItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_component, parent, false);
        return new ProjectItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectItemViewHolder holder, int position) {
        ProjectItem projectItem = getItem(position);
        
        itemViewModel.getItemById(projectItem.getItemId()).observe(lifecycleOwner, item -> {
            if (item != null) {
                holder.textViewName.setText(item.getName());
                holder.textViewDetails.setText(String.format(Locale.getDefault(), "Qty: %d x %s", 
                        projectItem.getQuantity(), Formatter.formatPrice(holder.itemView.getContext(), projectItem.getQuotedPrice())));
                holder.textViewTotal.setText(Formatter.formatPrice(holder.itemView.getContext(), projectItem.getQuantity() * projectItem.getQuotedPrice()));
            }
        });
    }

    static class ProjectItemViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewDetails, textViewTotal;

        public ProjectItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewComponentName);
            textViewDetails = itemView.findViewById(R.id.textViewComponentDetails);
            textViewTotal = itemView.findViewById(R.id.textViewComponentTotal);
        }
    }
}
