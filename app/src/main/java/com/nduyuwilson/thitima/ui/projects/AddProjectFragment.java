package com.nduyuwilson.thitima.ui.projects;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

public class AddProjectFragment extends Fragment {

    private TextInputEditText editTextName, editTextLocation, editTextDescription, editTextClientName, editTextClientContact, editTextLabourCost, editTextLabourPercent, editTextRules;
    private ProjectViewModel projectViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);

        editTextName = view.findViewById(R.id.editTextProjectName);
        editTextLocation = view.findViewById(R.id.editTextLocation);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextClientName = view.findViewById(R.id.editTextClientName);
        editTextClientContact = view.findViewById(R.id.editTextClientContact);
        editTextLabourCost = view.findViewById(R.id.editTextLabourCost);
        editTextLabourPercent = view.findViewById(R.id.editTextLabourPercent);
        editTextRules = view.findViewById(R.id.editTextRules);

        Button buttonSave = view.findViewById(R.id.buttonSaveProject);
        buttonSave.setOnClickListener(v -> saveProject(v));
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

        Project project = new Project(name, location, description, clientName, clientContact);
        project.setLabourCost(labourCost);
        project.setLabourPercentage(labourPercent);
        project.setRulesOfEngagement(rules);
        
        projectViewModel.insert(project);

        Snackbar.make(view, "Project created successfully", Snackbar.LENGTH_SHORT).show();
        Navigation.findNavController(view).navigateUp();
    }
}
