package com.nduyuwilson.thitima.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;
import com.nduyuwilson.thitima.viewmodel.RulesTemplateViewModel;

public class RulesTemplatesFragment extends Fragment {

    private RulesTemplateViewModel viewModel;
    private RulesTemplateAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rules_templates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        viewModel = new ViewModelProvider(this).get(RulesTemplateViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTemplates);
        adapter = new RulesTemplateAdapter(this::showEditDeleteDialog);
        recyclerView.setAdapter(adapter);

        viewModel.getAllTemplates().observe(getViewLifecycleOwner(), templates -> {
            adapter.submitList(templates);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddTemplate);
        fab.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void showEditDeleteDialog(RulesTemplate template) {
        String[] options = {"Edit", "Delete"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(template.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddEditDialog(template);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete Template")
                                .setMessage("Are you sure?")
                                .setPositiveButton("Delete", (d, w) -> viewModel.delete(template))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showAddEditDialog(@Nullable RulesTemplate existing) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existing == null ? "New Template" : "Edit Template");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editTitle = new EditText(requireContext());
        editTitle.setHint("Template Title");
        if (existing != null) editTitle.setText(existing.getTitle());
        layout.addView(editTitle);

        final EditText editContent = new EditText(requireContext());
        editContent.setHint("Rules content...");
        editContent.setMinLines(3);
        if (existing != null) editContent.setText(existing.getContent());
        layout.addView(editContent);

        builder.setView(layout);

        builder.setPositiveButton(existing == null ? "Add" : "Update", (dialog, which) -> {
            String title = editTitle.getText().toString().trim();
            String content = editContent.getText().toString().trim();

            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
                if (existing == null) {
                    viewModel.insert(new RulesTemplate(title, content));
                } else {
                    existing.setTitle(title);
                    existing.setContent(content);
                    viewModel.update(existing);
                }
            } else {
                Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
