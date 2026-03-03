package com.nduyuwilson.thitima.ui.projects;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;
import com.nduyuwilson.thitima.viewmodel.RulesTemplateViewModel;

import java.util.List;

public class AddProjectFragment extends Fragment {

    private TextInputEditText editTextName, editTextLocation, editTextDescription, editTextClientName, editTextClientContact, editTextLabourCost, editTextLabourPercent, editTextRules;
    private TextInputLayout textInputLayoutRules;
    private ProjectViewModel projectViewModel;
    private RulesTemplateViewModel rulesTemplateViewModel;
    private int projectId = -1;
    private Project existingProject;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        rulesTemplateViewModel = new ViewModelProvider(this).get(RulesTemplateViewModel.class);

        editTextName = view.findViewById(R.id.editTextProjectName);
        editTextLocation = view.findViewById(R.id.editTextLocation);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextClientName = view.findViewById(R.id.editTextClientName);
        editTextClientContact = view.findViewById(R.id.editTextClientContact);
        editTextLabourCost = view.findViewById(R.id.editTextLabourCost);
        editTextLabourPercent = view.findViewById(R.id.editTextLabourPercent);
        editTextRules = view.findViewById(R.id.editTextRules);
        textInputLayoutRules = view.findViewById(R.id.textInputLayoutRules);

        // Set up template selection icon click
        textInputLayoutRules.setEndIconOnClickListener(v -> showTemplateSelectionDialog());

        Button buttonSave = view.findViewById(R.id.buttonSaveProject);
        TextView textViewHeader = view.findViewById(R.id.textViewHeader);

        if (projectId != -1) {
            textViewHeader.setText("Edit Project Details");
            buttonSave.setText("Update Project");
            projectViewModel.getProjectById(projectId).observe(getViewLifecycleOwner(), project -> {
                if (project != null) {
                    existingProject = project;
                    populateFields(project);
                }
            });
        }

        buttonSave.setOnClickListener(v -> saveProject(v));
    }

    private void showTemplateSelectionDialog() {
        rulesTemplateViewModel.getAllTemplates().observe(getViewLifecycleOwner(), templates -> {
            if (templates == null || templates.isEmpty()) {
                Snackbar.make(requireView(), "No templates found. Add them in Settings.", Snackbar.LENGTH_LONG)
                        .setAction("Settings", v -> Navigation.findNavController(requireView()).navigate(R.id.navigation_settings))
                        .show();
                return;
            }

            String[] titles = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                titles[i] = templates.get(i).getTitle();
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Rules Template")
                    .setItems(titles, (dialog, which) -> {
                        RulesTemplate selected = templates.get(which);
                        editTextRules.setText(selected.getContent());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void populateFields(Project project) {
        editTextName.setText(project.getName());
        editTextLocation.setText(project.getLocation());
        editTextDescription.setText(project.getDescription());
        editTextClientName.setText(project.getClientName());
        editTextClientContact.setText(project.getClientContact());
        editTextLabourCost.setText(String.valueOf(project.getLabourCost()));
        editTextLabourPercent.setText(String.valueOf(project.getLabourPercentage()));
        editTextRules.setText(project.getRulesOfEngagement());
    }

    private void saveProject(View view) {
        String name = editTextName.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String clientName = editTextClientName.getText().toString().trim();
        String clientContact = editTextClientContact.getText().toString().trim();
        String labourCostStr = editTextLabourCost.getText().toString().trim();
        String labourPercentStr = editTextLabourPercent.getText().toString().trim();
        String rules = editTextRules.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(clientName)) {
            Snackbar.make(view, "Please fill in all required fields", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double labourCost = 0;
        if (!TextUtils.isEmpty(labourCostStr)) {
            try {
                labourCost = Double.parseDouble(labourCostStr);
            } catch (NumberFormatException ignored) {}
        }

        double labourPercent = 0;
        if (!TextUtils.isEmpty(labourPercentStr)) {
            try {
                labourPercent = Double.parseDouble(labourPercentStr);
            } catch (NumberFormatException ignored) {}
        }

        if (projectId != -1 && existingProject != null) {
            existingProject.setName(name);
            existingProject.setLocation(location);
            existingProject.setDescription(description);
            existingProject.setClientName(clientName);
            existingProject.setClientContact(clientContact);
            existingProject.setLabourCost(labourCost);
            existingProject.setLabourPercentage(labourPercent);
            existingProject.setRulesOfEngagement(rules);
            projectViewModel.update(existingProject);
            Snackbar.make(view, "Project updated successfully", Snackbar.LENGTH_SHORT).show();
        } else {
            Project project = new Project(name, location, description, clientName, clientContact);
            project.setLabourCost(labourCost);
            project.setLabourPercentage(labourPercent);
            project.setRulesOfEngagement(rules);
            projectViewModel.insert(project);
            Snackbar.make(view, "Project created successfully", Snackbar.LENGTH_SHORT).show();
        }

        Navigation.findNavController(view).navigateUp();
    }
}
