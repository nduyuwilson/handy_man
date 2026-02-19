package com.nduyuwilson.thitima.ui.projects;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    private TextView textViewTitle, textViewLocation, textViewClient, textViewMaterialTotal, textViewLabour, textViewLabourRow, textViewGrandTotal, textViewRules;
    private ProjectItemAdapter adapter;
    private double currentLabourCost = 0;
    
    private Project currentProject;
    private List<ProjectItem> currentProjectItems;
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

        textViewTitle = view.findViewById(R.id.textViewProjectTitle);
        textViewLocation = view.findViewById(R.id.textViewProjectLocation);
        textViewClient = view.findViewById(R.id.textViewClientDetails);
        textViewMaterialTotal = view.findViewById(R.id.textViewMaterialTotal);
        textViewLabour = view.findViewById(R.id.textViewLabourCost);
        textViewLabourRow = view.findViewById(R.id.textViewLabourCostRow);
        textViewGrandTotal = view.findViewById(R.id.textViewGrandTotal);
        textViewRules = view.findViewById(R.id.textViewRules);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewProjectItems);
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        
        adapter = new ProjectItemAdapter(itemViewModel, getViewLifecycleOwner(), this::showProjectItemOptions);
        recyclerView.setAdapter(adapter);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        
        projectViewModel.getProjectById(projectId).observe(getViewLifecycleOwner(), project -> {
            if (project != null) {
                currentProject = project;
                displayProjectDetails(project);
            }
        });

        projectViewModel.getItemsForProject(projectId).observe(getViewLifecycleOwner(), projectItems -> {
            currentProjectItems = projectItems;
            adapter.submitList(projectItems);
            calculateTotals(projectItems);
            
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

        view.findViewById(R.id.buttonGenerateInvoice).setOnClickListener(v -> generateAndSharePdf());
        
        view.findViewById(R.id.buttonAddComponent).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", projectId);
            Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addComponentToProjectFragment, bundle);
        });

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

    private void displayProjectDetails(Project project) {
        textViewTitle.setText(project.getName());
        textViewLocation.setText(project.getLocation());
        textViewClient.setText(String.format("%s\n%s", project.getClientName(), project.getClientContact()));
        
        projectViewModel.getItemsForProject(projectId).observe(getViewLifecycleOwner(), items -> {
            double materialTotal = 0;
            if (items != null) {
                for (ProjectItem item : items) {
                    materialTotal += (item.getQuantity() * item.getQuotedPrice());
                }
            }
            if (project.getLabourPercentage() > 0) {
                currentLabourCost = (project.getLabourPercentage() / 100.0) * materialTotal;
            } else {
                currentLabourCost = project.getLabourCost();
            }
            String formattedLabour = Formatter.formatPrice(requireContext(), currentLabourCost);
            textViewLabour.setText(formattedLabour);
            if (textViewLabourRow != null) textViewLabourRow.setText(formattedLabour);
            
            textViewGrandTotal.setText(Formatter.formatPrice(requireContext(), materialTotal + currentLabourCost));
        });

        textViewRules.setText(project.getRulesOfEngagement() != null && !project.getRulesOfEngagement().isEmpty() ? 
                project.getRulesOfEngagement() : "N/A");
    }

    private void calculateTotals(List<ProjectItem> items) {
        double materialTotal = 0;
        if (items != null) {
            for (ProjectItem item : items) {
                materialTotal += (item.getQuantity() * item.getQuotedPrice());
            }
        }
        textViewMaterialTotal.setText(Formatter.formatPrice(requireContext(), materialTotal));
        
        if (currentProject != null) {
            if (currentProject.getLabourPercentage() > 0) {
                currentLabourCost = (currentProject.getLabourPercentage() / 100.0) * materialTotal;
            } else {
                currentLabourCost = currentProject.getLabourCost();
            }
        }
        
        String formattedLabour = Formatter.formatPrice(requireContext(), currentLabourCost);
        textViewLabour.setText(formattedLabour);
        if (textViewLabourRow != null) textViewLabourRow.setText(formattedLabour);
        
        textViewGrandTotal.setText(Formatter.formatPrice(requireContext(), materialTotal + currentLabourCost));
    }

    private void generateAndSharePdf() {
        if (currentProject == null || currentProjectItems == null) {
            Toast.makeText(requireContext(), "Project data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        File pdfFile = PdfGenerator.generateInvoice(requireContext(), currentProject, currentProjectItems, itemMap, variantMap);
        if (pdfFile != null) {
            Uri contentUri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", pdfFile);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Quotation - " + currentProject.getName());
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF via"));
        } else {
            Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
