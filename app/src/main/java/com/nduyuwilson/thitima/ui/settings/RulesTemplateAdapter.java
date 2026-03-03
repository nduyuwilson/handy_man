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
import com.nduyuwilson.thitima.data.entity.RulesTemplate;

public class RulesTemplateAdapter extends ListAdapter<RulesTemplate, RulesTemplateAdapter.TemplateViewHolder> {

    public interface OnTemplateClickListener {
        void onTemplateClick(RulesTemplate template);
    }

    private final OnTemplateClickListener listener;

    protected RulesTemplateAdapter(OnTemplateClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<RulesTemplate> DIFF_CALLBACK = new DiffUtil.ItemCallback<RulesTemplate>() {
        @Override
        public boolean areItemsTheSame(@NonNull RulesTemplate oldItem, @NonNull RulesTemplate newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RulesTemplate oldItem, @NonNull RulesTemplate newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getContent().equals(newItem.getContent());
        }
    };

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        RulesTemplate template = getItem(position);
        holder.textViewTitle.setText(template.getTitle());
        holder.textViewSubtitle.setText(template.getContent());
        holder.itemView.setOnClickListener(v -> listener.onTemplateClick(template));
    }

    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewSubtitle;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewProjectName);
            textViewSubtitle = itemView.findViewById(R.id.textViewProjectLocation);
        }
    }
}
