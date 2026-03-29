package com.nduyuwilson.thitima.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.textfield.TextInputEditText;
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
        String[] options = {"Edit Template", "Delete Template"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(template.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddEditDialog(template);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete Template")
                                .setMessage("Are you sure you want to remove this template?")
                                .setPositiveButton("Delete", (d, w) -> viewModel.delete(template))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showAddEditDialog(@Nullable RulesTemplate existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_template, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTemplateTitle);
        TextInputEditText etContent = dialogView.findViewById(R.id.etTemplateContent);

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            etContent.setText(existing.getContent());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? "Create New Template" : "Edit Template")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();

                    if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
                        if (existing == null) {
                            viewModel.insert(new RulesTemplate(title, content));
                        } else {
                            existing.setTitle(title);
                            existing.setContent(content);
                            viewModel.update(existing);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Both title and content are required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
