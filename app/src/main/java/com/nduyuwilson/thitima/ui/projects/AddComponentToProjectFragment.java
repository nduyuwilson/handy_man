package com.nduyuwilson.thitima.ui.projects;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.ui.catalogue.CatalogueAdapter;
import com.nduyuwilson.thitima.ui.catalogue.ItemVariantAdapter;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

import java.util.ArrayList;
import java.util.List;

public class AddComponentToProjectFragment extends Fragment {

    private int projectId;
    private ItemViewModel itemViewModel;
    private ProjectViewModel projectViewModel;
    private CatalogueAdapter adapter;
    private List<Item> allItems = new ArrayList<>();

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
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        adapter = new CatalogueAdapter();
        adapter.setOnItemClickListener(item -> {
            itemViewModel.getVariantsForItem(item.getId()).observe(getViewLifecycleOwner(), variants -> {
                if (variants == null || variants.isEmpty()) {
                    showQuantityDialog(item, null, view);
                } else {
                    showVariantSelectionGrid(item, variants, view);
                }
            });
        });
        recyclerView.setAdapter(adapter);

        itemViewModel.getAllItems().observe(getViewLifecycleOwner(), items -> {
            allItems = items;
            adapter.submitList(items);
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setQueryHint("Search components...");
                
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) { return false; }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterItems(newText);
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) { return false; }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void filterItems(String query) {
        if (query == null || query.isEmpty()) {
            adapter.submitList(allItems);
            return;
        }
        List<Item> filtered = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.submitList(filtered);
    }

    private void showVariantSelectionGrid(Item item, List<ItemVariant> variants, View view) {
        RecyclerView gridRecyclerView = new RecyclerView(requireContext());
        gridRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        gridRecyclerView.setPadding(16, 16, 16, 16);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Brand for " + item.getName())
                .setView(gridRecyclerView)
                .setNegativeButton("Cancel", null)
                .create();

        ItemVariantAdapter variantAdapter = new ItemVariantAdapter(variant -> {
            dialog.dismiss();
            showQuantityDialog(item, variant, view);
        });
        
        variantAdapter.submitList(variants);
        gridRecyclerView.setAdapter(variantAdapter);

        dialog.show();
    }

    private void showQuantityDialog(Item item, ItemVariant variant, View view) {
        String displayName = (variant != null ? variant.getBrandName() : item.getName());
        double price = variant != null ? variant.getSellingPrice() : item.getSellingPrice();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quantity, null);
        TextInputEditText editTextQty = dialogView.findViewById(R.id.editTextDialogQuantity);
        TextView textViewPriceInfo = dialogView.findViewById(R.id.textViewPriceInfo);
        TextView textViewSubtitle = dialogView.findViewById(R.id.textViewDialogSubtitle);

        textViewSubtitle.setText("Set the quantity for " + displayName);
        textViewPriceInfo.setText("Unit Price: " + Formatter.formatPrice(requireContext(), price));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enter Quantity")
                .setView(dialogView)
                .setPositiveButton("Add to Project", (dialog, which) -> {
                    String qtyStr = editTextQty.getText().toString();
                    if (!TextUtils.isEmpty(qtyStr)) {
                        try {
                            int qty = Integer.parseInt(qtyStr);
                            if (qty > 0) {
                                checkAndAddProjectItem(item, variant, qty, price, view);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkAndAddProjectItem(Item item, ItemVariant variant, int qty, double price, View view) {
        Integer variantId = variant != null ? variant.getId() : null;
        
        // Use the repository to check if it exists (synchronously for the dialog)
        ProjectItem existing = projectViewModel.getExistingProjectItem(projectId, item.getId(), variantId);
        
        if (existing != null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Duplicate Item")
                    .setMessage(item.getName() + (variant != null ? " (" + variant.getBrandName() + ")" : "") + 
                            " is already in the project. Would you like to add this quantity to the existing entry?")
                    .setPositiveButton("Add to Existing", (dialog, which) -> {
                        existing.setQuantity(existing.getQuantity() + qty);
                        projectViewModel.updateProjectItem(existing);
                        Snackbar.make(view, "Quantity updated", Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(view).navigateUp();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            ProjectItem projectItem = new ProjectItem(projectId, item.getId(), qty, price);
            if (variantId != null) projectItem.setVariantId(variantId);
            projectViewModel.insertProjectItem(projectItem);
            Snackbar.make(view, "Added to project", Snackbar.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
        }
    }
}
