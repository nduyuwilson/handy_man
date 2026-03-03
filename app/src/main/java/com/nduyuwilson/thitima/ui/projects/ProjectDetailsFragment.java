package com.nduyuwilson.thitima.ui.projects;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.util.PdfGenerator;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectDetailsFragment extends Fragment {

    private ProjectViewModel projectViewModel;
    private ItemViewModel itemViewModel;
    private int projectId;
    private TextView textViewTitle, textViewLocation, textViewClient, textViewMaterialTotal, textViewMaterialTotalTop, textViewLabourTotalRow, textViewLabourTop, textViewGrandTotal, textViewRules;
    private ProjectItemAdapter itemAdapter;
    private LabourActivityAdapter labourAdapter;
    private double currentLabourTotal = 0;
    private double currentMaterialTotal = 0;
    
    private Project currentProject;
    private List<ProjectItem> currentProjectItems;
    private List<LabourActivity> currentLabourActivities;
    private Map<Integer, Item> itemMap = new HashMap<>();
    private Map<Integer, ItemVariant> variantMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        textViewTitle = view.findViewById(R.id.tvProjectTitle);
        textViewLocation = view.findViewById(R.id.tvProjectLocation);
        textViewClient = view.findViewById(R.id.tvClientDetails);
        
        // Financial views
        textViewMaterialTotalTop = view.findViewById(R.id.tvMaterialTotalTop);
        textViewLabourTop = view.findViewById(R.id.tvLabourTotalTop);
        textViewGrandTotal = view.findViewById(R.id.tvGrandTotalTop);
        
        // Table views
        textViewMaterialTotal = view.findViewById(R.id.tvMaterialTotalTable);
        textViewLabourTotalRow = view.findViewById(R.id.tvLabourTotalTable);
        
        textViewRules = view.findViewById(R.id.tvRules);

        RecyclerView recyclerViewItems = view.findViewById(R.id.recyclerViewProjectItems);
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        
        itemAdapter = new ProjectItemAdapter(itemViewModel, getViewLifecycleOwner(), this::showProjectItemOptions);
        recyclerViewItems.setAdapter(itemAdapter);

        RecyclerView recyclerViewLabour = view.findViewById(R.id.recyclerViewLabourActivities);
        labourAdapter = new LabourActivityAdapter(this::showLabourActivityOptions);
        recyclerViewLabour.setAdapter(labourAdapter);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        projectViewModel.getProjectById(projectId).observe(getViewLifecycleOwner(), project -> {
            if (project != null) {
                currentProject = project;
                displayProjectBasicInfo(project);
                calculateAllTotals();
            }
        });

        projectViewModel.getItemsForProject(projectId).observe(getViewLifecycleOwner(), projectItems -> {
            currentProjectItems = projectItems;
            itemAdapter.submitList(projectItems);
            calculateAllTotals();
            
            if (projectItems != null) {
                for (ProjectItem pi : projectItems) {
                    itemViewModel.getItemById(pi.getItemId()).observe(getViewLifecycleOwner(), item -> {
                        if (item != null) itemMap.put(item.getId(), item);
                    });
                    if (pi.getVariantId() != null) {
                        itemViewModel.getVariantById(pi.getVariantId()).observe(getViewLifecycleOwner(), variant -> {
                            if (variant != null) variantMap.put(variant.getId(), variant);
                        });
                    }
                }
            }
        });

        projectViewModel.getActivitiesForProject(projectId).observe(getViewLifecycleOwner(), activities -> {
            currentLabourActivities = activities;
            labourAdapter.submitList(activities);
            calculateAllTotals();
        });

        view.findViewById(R.id.buttonGenerateInvoice).setOnClickListener(v -> generateAndSharePdf(false));
        view.findViewById(R.id.buttonGenerateLabourInvoice).setOnClickListener(v -> generateAndSharePdf(true));
        
        view.findViewById(R.id.buttonAddComponent).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", projectId);
            Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addComponentToProjectFragment, bundle);
        });

        view.findViewById(R.id.buttonAddLabour).setOnClickListener(v -> showAddLabourDialog(null));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.project_details_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_edit_project) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("projectId", projectId);
                    Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addProjectFragment, bundle);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void displayProjectBasicInfo(Project project) {
        textViewTitle.setText(project.getName());
        textViewLocation.setText(project.getLocation());
        textViewClient.setText(String.format("%s\n%s", project.getClientName(), project.getClientContact()));
        textViewRules.setText(project.getRulesOfEngagement() != null && !project.getRulesOfEngagement().isEmpty() ? 
                project.getRulesOfEngagement() : "N/A");
    }

    private void calculateAllTotals() {
        currentMaterialTotal = 0;
        if (currentProjectItems != null) {
            for (ProjectItem item : currentProjectItems) {
                currentMaterialTotal += (item.getQuantity() * item.getQuotedPrice());
            }
        }

        double specificLabourTotal = 0;
        if (currentLabourActivities != null) {
            for (LabourActivity activity : currentLabourActivities) {
                specificLabourTotal += activity.getCost();
            }
        }

        double baseLabour = 0;
        if (currentProject != null) {
            if (currentProject.getLabourPercentage() > 0) {
                baseLabour = (currentProject.getLabourPercentage() / 100.0) * currentMaterialTotal;
            } else {
                baseLabour = currentProject.getLabourCost();
            }
        }

        currentLabourTotal = baseLabour + specificLabourTotal;

        String matTotalStr = Formatter.formatPrice(requireContext(), currentMaterialTotal);
        if (textViewMaterialTotal != null) textViewMaterialTotal.setText(matTotalStr);
        if (textViewMaterialTotalTop != null) textViewMaterialTotalTop.setText(matTotalStr);
        
        String formattedLabour = Formatter.formatPrice(requireContext(), currentLabourTotal);
        if (textViewLabourTop != null) textViewLabourTop.setText(formattedLabour);
        if (textViewLabourTotalRow != null) textViewLabourTotalRow.setText(formattedLabour);
        
        textViewGrandTotal.setText(Formatter.formatPrice(requireContext(), currentMaterialTotal + currentLabourTotal));
    }

    private void showAddLabourDialog(@Nullable LabourActivity existingActivity) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existingActivity == null ? "Add Labour Activity" : "Edit Labour Activity");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editName = new EditText(requireContext());
        editName.setHint("Activity Name (e.g. Consultation)");
        if (existingActivity != null) editName.setText(existingActivity.getName());
        layout.addView(editName);

        final EditText editCost = new EditText(requireContext());
        editCost.setHint("Cost");
        editCost.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingActivity != null) editCost.setText(String.valueOf(existingActivity.getCost()));
        layout.addView(editCost);

        builder.setView(layout);

        builder.setPositiveButton(existingActivity == null ? "Add" : "Update", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String costStr = editCost.getText().toString().trim();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(costStr)) {
                try {
                    double cost = Double.parseDouble(costStr);
                    if (existingActivity == null) {
                        projectViewModel.insertLabourActivity(new LabourActivity(projectId, name, cost));
                    } else {
                        existingActivity.setName(name);
                        existingActivity.setCost(cost);
                        projectViewModel.updateLabourActivity(existingActivity);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLabourActivityOptions(LabourActivity activity) {
        String[] options = {"Edit Activity", "Remove Activity"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(activity.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddLabourDialog(activity);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Remove Activity")
                                .setMessage("Delete this labour activity?")
                                .setPositiveButton("Delete", (d, w) -> projectViewModel.deleteLabourActivity(activity))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showProjectItemOptions(ProjectItem projectItem) {
        String[] options = {"Edit Quantity", "Remove Item"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Item")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditQuantityDialog(projectItem);
                    } else {
                        confirmRemoveItem(projectItem);
                    }
                })
                .show();
    }

    private void showEditQuantityDialog(ProjectItem projectItem) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quantity, null);
        TextInputEditText editTextQty = dialogView.findViewById(R.id.editTextDialogQuantity);
        if (editTextQty != null) {
            editTextQty.setText(String.valueOf(projectItem.getQuantity()));
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Quantity")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    if (editTextQty != null) {
                        String qtyStr = editTextQty.getText().toString();
                        if (!qtyStr.isEmpty()) {
                            try {
                                int qty = Integer.parseInt(qtyStr);
                                if (qty > 0) {
                                    projectItem.setQuantity(qty);
                                    projectViewModel.updateProjectItem(projectItem);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRemoveItem(ProjectItem projectItem) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove this item from the project?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    projectViewModel.deleteProjectItem(projectItem);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndSharePdf(boolean labourOnly) {
        if (currentProject == null) {
            Toast.makeText(requireContext(), "Project data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        File pdfFile = PdfGenerator.generateInvoice(requireContext(), currentProject, 
                labourOnly ? null : currentProjectItems, 
                currentLabourActivities, 
                itemMap, variantMap);
                
        if (pdfFile != null) {
            Uri contentUri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_SUBJECT, (labourOnly ? "Labour Invoice - " : "Quotation - ") + currentProject.getName());
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF via"));
        } else {
            Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
