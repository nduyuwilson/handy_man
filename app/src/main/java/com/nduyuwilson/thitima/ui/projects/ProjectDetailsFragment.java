package com.nduyuwilson.thitima.ui.projects;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
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
    private TextView textViewTitle, textViewLocation, textViewClient, textViewMaterialTotal, textViewLabour, textViewGrandTotal, textViewRules;
    private ProjectItemAdapter adapter;
    private double currentLabourCost = 0;
    
    private Project currentProject;
    private List<ProjectItem> currentProjectItems;
    private Map<Integer, Item> itemMap = new HashMap<>();

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

        textViewTitle = view.findViewById(R.id.textViewProjectTitle);
        textViewLocation = view.findViewById(R.id.textViewProjectLocation);
        textViewClient = view.findViewById(R.id.textViewClientDetails);
        textViewMaterialTotal = view.findViewById(R.id.textViewMaterialTotal);
        textViewLabour = view.findViewById(R.id.textViewLabourCost);
        textViewGrandTotal = view.findViewById(R.id.textViewGrandTotal);
        textViewRules = view.findViewById(R.id.textViewRules);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewProjectItems);
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        adapter = new ProjectItemAdapter(itemViewModel, getViewLifecycleOwner());
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
            
            // Pre-load items for PDF generation
            if (projectItems != null) {
                for (ProjectItem pi : projectItems) {
                    itemViewModel.getItemById(pi.getItemId()).observe(getViewLifecycleOwner(), item -> {
                        if (item != null) itemMap.put(item.getId(), item);
                    });
                }
            }
        });

        view.findViewById(R.id.buttonGenerateInvoice).setOnClickListener(v -> generateAndSharePdf());
        
        view.findViewById(R.id.buttonAddComponent).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", projectId);
            Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addComponentToProjectFragment, bundle);
        });
    }

    private void displayProjectDetails(Project project) {
        textViewTitle.setText(project.getName());
        textViewLocation.setText(project.getLocation());
        textViewClient.setText(String.format("%s\n%s", project.getClientName(), project.getClientContact()));
        currentLabourCost = project.getLabourCost();
        textViewLabour.setText(Formatter.formatPrice(requireContext(), currentLabourCost));
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
        textViewGrandTotal.setText(Formatter.formatPrice(requireContext(), materialTotal + currentLabourCost));
    }

    private void generateAndSharePdf() {
        if (currentProject == null || currentProjectItems == null) {
            Toast.makeText(requireContext(), "Project data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        File pdfFile = PdfGenerator.generateInvoice(requireContext(), currentProject, currentProjectItems, itemMap);
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
