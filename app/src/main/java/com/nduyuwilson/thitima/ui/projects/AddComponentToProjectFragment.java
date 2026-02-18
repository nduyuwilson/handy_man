package com.nduyuwilson.thitima.ui.projects;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.ui.catalogue.CatalogueAdapter;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

public class AddComponentToProjectFragment extends Fragment {

    private int projectId;
    private ItemViewModel itemViewModel;
    private ProjectViewModel projectViewModel;

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
        return inflater.inflate(R.layout.fragment_add_component_to_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSelectItems);
        
        CatalogueAdapter adapter = new CatalogueAdapter();
        adapter.setOnItemClickListener(item -> showQuantityDialog(item, view));
        recyclerView.setAdapter(adapter);

        itemViewModel.getAllItems().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void showQuantityDialog(Item item, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Quantity for " + item.getName());

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Quantity");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String qtyStr = input.getText().toString();
            if (!qtyStr.isEmpty()) {
                try {
                    int qty = Integer.parseInt(qtyStr);
                    if (qty > 0) {
                        ProjectItem projectItem = new ProjectItem(projectId, item.getId(), qty, item.getSellingPrice());
                        projectViewModel.insertProjectItem(projectItem);
                        Snackbar.make(view, "Added " + qty + " x " + item.getName(), Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigateUp();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
